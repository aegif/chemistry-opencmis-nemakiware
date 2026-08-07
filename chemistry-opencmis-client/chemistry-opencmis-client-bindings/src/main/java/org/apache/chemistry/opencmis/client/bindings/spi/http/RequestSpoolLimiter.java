/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.chemistry.opencmis.client.bindings.spi.http;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.chemistry.opencmis.commons.exceptions.CmisConnectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Classloader-wide limits for HTTP request-body materialization (heap and/or
 * disk).
 * <p>
 * A concurrency permit is required while body bytes are being buffered. After
 * materialization finishes, the permit can be released while the byte budget
 * remains charged until the temp file / heap buffer is discarded. When the
 * byte budget is exhausted, charging waits (bounded by the acquire timeout)
 * for active bodies to release their bytes instead of failing immediately;
 * only a body that alone can never fit into the budget fails fast. The first
 * successful acquire locks {@code maxConcurrent} and {@code maxTotalBytes} for
 * this classloader. Later acquires that request different values are rejected.
 * <p>
 * When a temp file cannot be deleted immediately, its bytes stay charged and
 * the path is tracked. Later acquire / addBytes attempts retry deletion and
 * reclaim the budget once the file is gone, so a permanent delete failure does
 * not permanently shrink the classloader-wide budget.
 * <p>
 * Note: a {@code static} singleton is shared only among code that loads this
 * class through the same classloader (for example one web application). It is
 * not a host-wide or multi-webapp JVM limit unless OpenCMIS is loaded from a
 * shared classloader.
 */
final class RequestSpoolLimiter {

    private static final Logger LOG = LoggerFactory.getLogger(RequestSpoolLimiter.class);

    /** Upper bound for a single byte-budget wait (1 day). */
    private static final long MAX_BYTE_WAIT_MS = TimeUnit.DAYS.toMillis(1);

    private static final RequestSpoolLimiter INSTANCE = new RequestSpoolLimiter();

    private final AtomicInteger activeMaterializations = new AtomicInteger();
    private final Object bytesLock = new Object();
    /** Guarded by {@link #bytesLock}. */
    private long activeBytes;
    private final AtomicLong orphanedBytes = new AtomicLong();
    private final ConcurrentLinkedQueue<OrphanedTemp> orphanedTemps = new ConcurrentLinkedQueue<OrphanedTemp>();
    private final Object configLock = new Object();
    private volatile Integer lockedMaxConcurrent;
    private volatile Long lockedMaxTotalBytes;
    private volatile Semaphore concurrentPermits;

    private RequestSpoolLimiter() {
    }

    static RequestSpoolLimiter getInstance() {
        return INSTANCE;
    }

    /** Test-only: must not be called while leases are active. */
    void resetForTests() {
        synchronized (configLock) {
            if (activeMaterializations.get() != 0) {
                throw new IllegalStateException(
                        "Cannot reset RequestSpoolLimiter while materializations are active");
            }
            activeMaterializations.set(0);
            synchronized (bytesLock) {
                activeBytes = 0;
                bytesLock.notifyAll();
            }
            orphanedBytes.set(0);
            orphanedTemps.clear();
            lockedMaxConcurrent = null;
            lockedMaxTotalBytes = null;
            concurrentPermits = null;
        }
    }

    int getActiveSpools() {
        return activeMaterializations.get();
    }

    long getActiveBytes() {
        synchronized (bytesLock) {
            return activeBytes;
        }
    }

    long getOrphanedBytesForTests() {
        return orphanedBytes.get();
    }

    int getOrphanedTempCountForTests() {
        return orphanedTemps.size();
    }

    Integer getLockedMaxConcurrentForTests() {
        return lockedMaxConcurrent;
    }

    Long getLockedMaxTotalBytesForTests() {
        return lockedMaxTotalBytes;
    }

    /**
     * Acquires a classloader-wide materialization permit. Never blocks forever;
     * {@code acquireTimeoutMs} must be {@code >= 0}.
     */
    SpoolLease acquire(int maxConcurrent, long maxTotalBytes, long acquireTimeoutMs) {
        reclaimOrphanedTemps();
        if (acquireTimeoutMs < 0) {
            throw new CmisConnectionException(
                    "HTTP request body materialization acquire timeout must not be negative");
        }
        int max = Math.max(1, maxConcurrent);
        long totalLimit = maxTotalBytes > 0 ? maxTotalBytes : Long.MAX_VALUE;
        Semaphore sem = configureAndGetSemaphore(max, totalLimit);
        try {
            boolean ok = sem.tryAcquire(Math.max(1L, acquireTimeoutMs), TimeUnit.MILLISECONDS);
            if (!ok) {
                throw new CmisConnectionException(
                        "Too many concurrent HTTP request body materializations (classloader-wide max "
                                + max + "; raise org.apache.chemistry.opencmis.binding.http.requestspoolmaxconcurrent"
                                + " or use request body mode auto/stream)");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CmisConnectionException(
                    "Interrupted while waiting for HTTP request body materialization permit", e);
        }
        activeMaterializations.incrementAndGet();
        return new SpoolLease(this, sem, Math.max(1L, acquireTimeoutMs));
    }

    private Semaphore configureAndGetSemaphore(int maxConcurrent, long maxTotalBytes) {
        Integer lockedMax = lockedMaxConcurrent;
        Long lockedTotal = lockedMaxTotalBytes;
        Semaphore sem = concurrentPermits;
        if (lockedMax != null && lockedTotal != null && sem != null) {
            assertCompatible(maxConcurrent, maxTotalBytes, lockedMax.intValue(), lockedTotal.longValue());
            return sem;
        }
        synchronized (configLock) {
            if (lockedMaxConcurrent == null) {
                lockedMaxConcurrent = Integer.valueOf(maxConcurrent);
                lockedMaxTotalBytes = Long.valueOf(maxTotalBytes);
                concurrentPermits = new Semaphore(maxConcurrent, true);
                return concurrentPermits;
            }
            assertCompatible(maxConcurrent, maxTotalBytes, lockedMaxConcurrent.intValue(),
                    lockedMaxTotalBytes.longValue());
            return concurrentPermits;
        }
    }

    private static void assertCompatible(int requestedMax, long requestedTotal, int lockedMax, long lockedTotal) {
        if (requestedMax != lockedMax || requestedTotal != lockedTotal) {
            throw new CmisConnectionException(
                    "HTTP request body materialization classloader-wide limits already locked to maxConcurrent="
                            + lockedMax + ", maxTotalBytes=" + lockedTotal
                            + "; this session requested maxConcurrent=" + requestedMax + ", maxTotalBytes="
                            + requestedTotal
                            + " (align all sessions or restart the classloader / JVM)");
        }
    }

    /**
     * Charges {@code delta} bytes against the classloader-wide budget. If the
     * budget is currently exhausted, waits up to {@code waitTimeoutMs} for
     * active bodies to release bytes. Fails immediately only when this body
     * alone ({@code alreadyAccounted + delta}) can never fit into the budget.
     */
    void addBytes(long delta, long alreadyAccounted, long waitTimeoutMs) {
        if (delta <= 0) {
            return;
        }
        reclaimOrphanedTemps();
        Long maxTotalBytes = lockedMaxTotalBytes;
        long limit = maxTotalBytes == null ? Long.MAX_VALUE : maxTotalBytes.longValue();
        if (limit == Long.MAX_VALUE) {
            synchronized (bytesLock) {
                activeBytes += delta;
            }
            return;
        }
        if (alreadyAccounted + delta > limit) {
            throw new CmisConnectionException("HTTP request body materialization total byte limit exceeded: "
                    + "this request body alone needs " + (alreadyAccounted + delta)
                    + " bytes but the classloader-wide budget is " + limit
                    + " bytes (raise org.apache.chemistry.opencmis.binding.http.requestspoolmaxtotalbytes"
                    + " or use request body mode auto/stream)");
        }
        // Clamp so deadline arithmetic cannot overflow when callers configure
        // an effectively infinite timeout (toNanos saturates at Long.MAX_VALUE).
        long clampedWaitMs = Math.min(Math.max(1L, waitTimeoutMs), MAX_BYTE_WAIT_MS);
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(clampedWaitMs);
        synchronized (bytesLock) {
            while (activeBytes + delta > limit) {
                // Another thread may have reclaimed orphans; retry delete while waiting.
                reclaimOrphanedTempsUnlocked();
                if (activeBytes + delta <= limit) {
                    break;
                }
                long remainingMs = TimeUnit.NANOSECONDS.toMillis(deadline - System.nanoTime());
                if (remainingMs <= 0) {
                    throw new CmisConnectionException(
                            "HTTP request body materialization total byte limit exceeded (classloader-wide max "
                                    + limit
                                    + " bytes); timed out waiting for active bodies to release budget"
                                    + " (raise org.apache.chemistry.opencmis.binding.http.requestspoolmaxtotalbytes"
                                    + " / connectionrequesttimeout, or use request body mode auto/stream)");
                }
                try {
                    bytesLock.wait(Math.min(remainingMs, 250L));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new CmisConnectionException(
                            "Interrupted while waiting for HTTP request body materialization byte budget", e);
                }
            }
            activeBytes += delta;
        }
    }

    private void releasePermit(Semaphore semaphore) {
        activeMaterializations.decrementAndGet();
        semaphore.release();
    }

    private void subtractBytes(long bytesAccounted) {
        if (bytesAccounted > 0) {
            synchronized (bytesLock) {
                activeBytes -= bytesAccounted;
                bytesLock.notifyAll();
            }
        }
    }

    /**
     * Tracks an undeleted temp file whose bytes remain charged. Later
     * {@link #reclaimOrphanedTemps()} retries deletion and releases the budget.
     */
    void abandonTempFile(File file, long bytesAccounted) {
        if (bytesAccounted <= 0) {
            return;
        }
        orphanedBytes.addAndGet(bytesAccounted);
        if (file != null) {
            orphanedTemps.add(new OrphanedTemp(file, bytesAccounted));
            if (LOG.isDebugEnabled()) {
                LOG.debug("Tracking undeleted HTTP request temp file {} ({} bytes) for deferred reclaim",
                        file.getAbsolutePath(), Long.valueOf(bytesAccounted));
            }
        } else if (LOG.isWarnEnabled()) {
            LOG.warn(
                    "Orphaned {} HTTP request materialization bytes without a temp path; "
                            + "byte budget cannot be reclaimed automatically until classloader reset",
                    Long.valueOf(bytesAccounted));
        }
    }

    /**
     * Retries deletion of tracked orphan temp files and releases their byte
     * budget when the file is gone. Safe to call concurrently.
     *
     * @return number of orphans reclaimed
     */
    int reclaimOrphanedTemps() {
        if (orphanedTemps.isEmpty()) {
            return 0;
        }
        // Test hook: keep orphans charged while forced delete-failure is active.
        if (AbstractApacheClientHttpInvoker.FORCE_TEMP_DELETE_FAILURE_FOR_TESTS.get()) {
            return 0;
        }
        List<OrphanedTemp> reclaimed = new ArrayList<OrphanedTemp>();
        for (Iterator<OrphanedTemp> it = orphanedTemps.iterator(); it.hasNext();) {
            OrphanedTemp orphan = it.next();
            if (tryDeleteOrphan(orphan)) {
                it.remove();
                reclaimed.add(orphan);
            }
        }
        long released = 0L;
        for (OrphanedTemp orphan : reclaimed) {
            released += orphan.bytes;
        }
        if (released > 0) {
            orphanedBytes.addAndGet(-released);
            subtractBytes(released);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Reclaimed {} orphaned HTTP request temp file(s), {} bytes",
                        Integer.valueOf(reclaimed.size()), Long.valueOf(released));
            }
        }
        return reclaimed.size();
    }

    /**
     * Same as {@link #reclaimOrphanedTemps()} but caller already holds
     * {@link #bytesLock}. Only updates {@link #activeBytes} under the lock;
     * does not call {@link #subtractBytes}.
     */
    private void reclaimOrphanedTempsUnlocked() {
        if (orphanedTemps.isEmpty()) {
            return;
        }
        if (AbstractApacheClientHttpInvoker.FORCE_TEMP_DELETE_FAILURE_FOR_TESTS.get()) {
            return;
        }
        long released = 0L;
        for (Iterator<OrphanedTemp> it = orphanedTemps.iterator(); it.hasNext();) {
            OrphanedTemp orphan = it.next();
            if (tryDeleteOrphan(orphan)) {
                it.remove();
                released += orphan.bytes;
            }
        }
        if (released > 0) {
            orphanedBytes.addAndGet(-released);
            activeBytes -= released;
            bytesLock.notifyAll();
        }
    }

    private static boolean tryDeleteOrphan(OrphanedTemp orphan) {
        File file = orphan.file;
        if (file == null) {
            return false;
        }
        if (!file.exists()) {
            return true;
        }
        if (file.delete()) {
            return true;
        }
        // Still present — leave tracked for a later attempt.
        return false;
    }

    private static final class OrphanedTemp {
        final File file;
        final long bytes;

        OrphanedTemp(File file, long bytes) {
            this.file = file;
            this.bytes = bytes;
        }
    }

    /**
     * Holds the semaphore acquired for a materialization so release always
     * targets the same instance. The concurrency permit and byte budget can be
     * released independently so networking can proceed without blocking other
     * materializations.
     */
    static final class SpoolLease {
        private final RequestSpoolLimiter limiter;
        private final Semaphore semaphore;
        private final long byteWaitTimeoutMs;
        private final AtomicLong accountedBytes = new AtomicLong();
        private final AtomicBoolean permitReleased = new AtomicBoolean();
        private final AtomicBoolean bytesReleased = new AtomicBoolean();

        SpoolLease(RequestSpoolLimiter limiter, Semaphore semaphore, long byteWaitTimeoutMs) {
            this.limiter = limiter;
            this.semaphore = semaphore;
            this.byteWaitTimeoutMs = byteWaitTimeoutMs;
        }

        void addBytes(long delta) {
            limiter.addBytes(delta, accountedBytes.get(), byteWaitTimeoutMs);
            accountedBytes.addAndGet(delta);
        }

        long getAccountedBytes() {
            return accountedBytes.get();
        }

        /** Releases only the concurrency permit (bytes stay charged). */
        void releasePermitOnly() {
            if (!permitReleased.compareAndSet(false, true)) {
                return;
            }
            limiter.releasePermit(semaphore);
        }

        /** Releases charged bytes after the materialized body is discarded. */
        void releaseBytes() {
            if (!bytesReleased.compareAndSet(false, true)) {
                return;
            }
            limiter.subtractBytes(accountedBytes.getAndSet(0));
        }

        /**
         * Keeps charged bytes in the budget and tracks {@code tempFile} for
         * deferred reclaim when immediate delete failed.
         */
        void abandonBytesAsOrphan(File tempFile) {
            if (!bytesReleased.compareAndSet(false, true)) {
                return;
            }
            limiter.abandonTempFile(tempFile, accountedBytes.getAndSet(0));
        }

        /** Full successful cleanup: permit + bytes. */
        void release() {
            releasePermitOnly();
            releaseBytes();
        }

        /**
         * Releases the concurrency permit but keeps accounted bytes charged
         * (temp file delete failed; disk still holds the data).
         */
        void releasePermitKeepBytes(File tempFile) {
            releasePermitOnly();
            abandonBytesAsOrphan(tempFile);
        }
    }
}
