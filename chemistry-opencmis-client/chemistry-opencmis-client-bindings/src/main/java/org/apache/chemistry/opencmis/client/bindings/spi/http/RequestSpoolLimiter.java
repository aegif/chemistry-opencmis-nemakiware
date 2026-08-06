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

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.chemistry.opencmis.commons.exceptions.CmisConnectionException;

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
 * Note: a {@code static} singleton is shared only among code that loads this
 * class through the same classloader (for example one web application). It is
 * not a host-wide or multi-webapp JVM limit unless OpenCMIS is loaded from a
 * shared classloader.
 */
final class RequestSpoolLimiter {

    /** Upper bound for a single byte-budget wait (1 day). */
    private static final long MAX_BYTE_WAIT_MS = TimeUnit.DAYS.toMillis(1);

    private static final RequestSpoolLimiter INSTANCE = new RequestSpoolLimiter();

    private final AtomicInteger activeMaterializations = new AtomicInteger();
    private final Object bytesLock = new Object();
    /** Guarded by {@link #bytesLock}. */
    private long activeBytes;
    private final AtomicLong orphanedBytes = new AtomicLong();
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
        if (acquireTimeoutMs < 0) {
            throw new CmisConnectionException("Request body materialization acquire timeout must not be negative");
        }
        int max = Math.max(1, maxConcurrent);
        long totalLimit = maxTotalBytes > 0 ? maxTotalBytes : Long.MAX_VALUE;
        Semaphore sem = configureAndGetSemaphore(max, totalLimit);
        try {
            boolean ok = sem.tryAcquire(Math.max(1L, acquireTimeoutMs), TimeUnit.MILLISECONDS);
            if (!ok) {
                throw new CmisConnectionException(
                        "Too many concurrent HTTP request body materializations (classloader-wide max " + max + ")");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CmisConnectionException("Interrupted while waiting for request body materialization permit", e);
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
                    "HTTP request body materialization classloader-wide limits already set to maxConcurrent="
                            + lockedMax + ", maxTotalBytes=" + lockedTotal
                            + "; refusing conflicting session settings maxConcurrent=" + requestedMax
                            + ", maxTotalBytes=" + requestedTotal);
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
                    + " bytes but the classloader-wide budget is " + limit + " bytes");
        }
        // Clamp so deadline arithmetic cannot overflow when callers configure
        // an effectively infinite timeout (toNanos saturates at Long.MAX_VALUE).
        long clampedWaitMs = Math.min(Math.max(1L, waitTimeoutMs), MAX_BYTE_WAIT_MS);
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(clampedWaitMs);
        synchronized (bytesLock) {
            while (activeBytes + delta > limit) {
                long remainingMs = TimeUnit.NANOSECONDS.toMillis(deadline - System.nanoTime());
                if (remainingMs <= 0) {
                    throw new CmisConnectionException(
                            "HTTP request body materialization total byte limit exceeded (classloader-wide max "
                                    + limit + " bytes); timed out waiting for active bodies to release budget");
                }
                try {
                    bytesLock.wait(remainingMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new CmisConnectionException(
                            "Interrupted while waiting for request body materialization byte budget", e);
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

    private void markOrphaned(long bytesAccounted) {
        if (bytesAccounted > 0) {
            orphanedBytes.addAndGet(bytesAccounted);
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
         * Marks charged bytes as orphaned (temp delete failed) without
         * subtracting them from the active budget.
         */
        void abandonBytesAsOrphan() {
            if (!bytesReleased.compareAndSet(false, true)) {
                return;
            }
            limiter.markOrphaned(accountedBytes.getAndSet(0));
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
        void releasePermitKeepBytes() {
            releasePermitOnly();
            abandonBytesAsOrphan();
        }
    }
}
