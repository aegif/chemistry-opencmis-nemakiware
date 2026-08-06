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
 * A permit is required before any request body bytes are buffered, not only
 * when spilling to disk. The first successful acquire locks
 * {@code maxConcurrent} and {@code maxTotalBytes} for this classloader. Later
 * acquires that request different values are rejected.
 * <p>
 * Note: a {@code static} singleton is shared only among code that loads this
 * class through the same classloader (for example one web application). It is
 * not a host-wide or multi-webapp JVM limit unless OpenCMIS is loaded from a
 * shared classloader.
 */
final class RequestSpoolLimiter {

    private static final RequestSpoolLimiter INSTANCE = new RequestSpoolLimiter();

    private final AtomicInteger activeMaterializations = new AtomicInteger();
    private final AtomicLong activeBytes = new AtomicLong();
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
            activeBytes.set(0);
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
        return activeBytes.get();
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
        return new SpoolLease(this, sem);
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

    void addBytes(long delta) {
        if (delta <= 0) {
            return;
        }
        Long maxTotalBytes = lockedMaxTotalBytes;
        long next = activeBytes.addAndGet(delta);
        if (maxTotalBytes != null && maxTotalBytes.longValue() != Long.MAX_VALUE && next > maxTotalBytes.longValue()) {
            activeBytes.addAndGet(-delta);
            throw new CmisConnectionException(
                    "HTTP request body materialization total byte limit exceeded (classloader-wide max "
                            + maxTotalBytes + " bytes)");
        }
    }

    private void releaseLease(Semaphore semaphore, long bytesAccounted, boolean releaseBytes) {
        if (bytesAccounted > 0) {
            if (releaseBytes) {
                activeBytes.addAndGet(-bytesAccounted);
            } else {
                orphanedBytes.addAndGet(bytesAccounted);
            }
        }
        activeMaterializations.decrementAndGet();
        semaphore.release();
    }

    /**
     * Holds the semaphore acquired for a materialization so release always
     * targets the same instance.
     */
    static final class SpoolLease {
        private final RequestSpoolLimiter limiter;
        private final Semaphore semaphore;
        private final AtomicLong accountedBytes = new AtomicLong();
        private final AtomicBoolean released = new AtomicBoolean();

        SpoolLease(RequestSpoolLimiter limiter, Semaphore semaphore) {
            this.limiter = limiter;
            this.semaphore = semaphore;
        }

        void addBytes(long delta) {
            limiter.addBytes(delta);
            accountedBytes.addAndGet(delta);
        }

        long getAccountedBytes() {
            return accountedBytes.get();
        }

        /** Releases the concurrency permit and subtracts accounted bytes. */
        void release() {
            releaseInternal(true);
        }

        /**
         * Releases the concurrency permit but keeps accounted bytes charged
         * (temp file delete failed; disk still holds the data).
         */
        void releasePermitKeepBytes() {
            releaseInternal(false);
        }

        private void releaseInternal(boolean releaseBytes) {
            if (!released.compareAndSet(false, true)) {
                return;
            }
            limiter.releaseLease(semaphore, accountedBytes.getAndSet(0), releaseBytes);
        }
    }
}
