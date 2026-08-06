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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import org.apache.chemistry.opencmis.client.bindings.impl.CmisBindingsHelper;
import org.apache.chemistry.opencmis.client.bindings.impl.SessionImpl;
import org.apache.chemistry.opencmis.client.bindings.spi.AbstractAuthenticationProvider;
import org.apache.chemistry.opencmis.client.bindings.spi.BindingSession;
import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.exceptions.CmisConnectionException;
import org.apache.chemistry.opencmis.commons.impl.UrlBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * High-churn / pool-pressure / large-body coverage for Apache HttpClient 5.
 */
public class ApacheClientHttpInvokerPoolTest {

    private final List<ExecutorService> executors = new ArrayList<ExecutorService>();

    @BeforeEach
    public void resetSpoolLimiter() {
        AbstractApacheClientHttpInvoker.FORCE_SPILL_FAILURE_FOR_TESTS.set(false);
        AbstractApacheClientHttpInvoker.FORCE_TEMP_DELETE_FAILURE_FOR_TESTS.set(false);
        RequestSpoolLimiter.getInstance().resetForTests();
    }

    @AfterEach
    public void shutdownExecutors() {
        for (ExecutorService executor : executors) {
            executor.shutdownNow();
        }
        executors.clear();
        AbstractApacheClientHttpInvoker.FORCE_SPILL_FAILURE_FOR_TESTS.set(false);
        AbstractApacheClientHttpInvoker.FORCE_TEMP_DELETE_FAILURE_FOR_TESTS.set(false);
        RequestSpoolLimiter.getInstance().resetForTests();
    }

    @Test
    public void manySequentialGetsWithoutReadingDoNotExhaustPool() throws Exception {
        ServerHandle server = startPingServer();
        BindingSession session = new SessionImpl();
        try {
            int port = server.port;
            session.put(SessionParameter.HTTP_MAX_CONNECTIONS_PER_HOST, Integer.valueOf(5), true);
            session.put(SessionParameter.HTTP_MAX_CONNECTIONS, Integer.valueOf(5), true);
            session.put(SessionParameter.HTTP_CONNECTION_REQUEST_TIMEOUT, Integer.valueOf(3000), true);

            ApacheClientHttpInvoker invoker = new ApacheClientHttpInvoker();
            UrlBuilder url = new UrlBuilder("http://127.0.0.1:" + port + "/ping");

            for (int i = 0; i < 40; i++) {
                Response response = invoker.invokeGET(url, session);
                assertEquals(200, response.getResponseCode());
            }
        } finally {
            HttpInvokerSessionResources.close(session);
            server.stop();
        }
    }

    @Test
    public void concurrentGetsCompleteUnderLoad() throws Exception {
        ServerHandle server = startPingServer();
        ExecutorService pool = Executors.newFixedThreadPool(16);
        executors.add(pool);
        BindingSession session = new SessionImpl();
        try {
            int port = server.port;
            session.put(SessionParameter.HTTP_MAX_CONNECTIONS_PER_HOST, Integer.valueOf(32), true);
            session.put(SessionParameter.HTTP_MAX_CONNECTIONS, Integer.valueOf(64), true);
            session.put(SessionParameter.HTTP_CONNECTION_REQUEST_TIMEOUT, Integer.valueOf(10000), true);

            final ApacheClientHttpInvoker invoker = new ApacheClientHttpInvoker();
            final UrlBuilder url = new UrlBuilder("http://127.0.0.1:" + port + "/ping");

            List<Callable<Integer>> tasks = new ArrayList<Callable<Integer>>();
            for (int i = 0; i < 64; i++) {
                tasks.add(new Callable<Integer>() {
                    @Override
                    public Integer call() throws Exception {
                        Response response = invoker.invokeGET(url, session);
                        assertEquals(200, response.getResponseCode());
                        try (InputStream in = response.getStream()) {
                            return Integer.valueOf(in.read());
                        }
                    }
                });
            }

            List<Future<Integer>> futures = pool.invokeAll(tasks, 30, TimeUnit.SECONDS);
            for (Future<Integer> future : futures) {
                assertTrue(future.isDone());
                assertTrue(future.get() >= 0);
            }
        } finally {
            HttpInvokerSessionResources.close(session);
            server.stop();
        }
    }

    @Test
    public void sessionCloseReleasesHttpClient() throws Exception {
        ServerHandle server = startPingServer();
        BindingSession session = new SessionImpl();
        try {
            int port = server.port;
            ApacheClientHttpInvoker invoker = new ApacheClientHttpInvoker();
            Response response = invoker.invokeGET(new UrlBuilder("http://127.0.0.1:" + port + "/ping"), session);
            assertEquals(200, response.getResponseCode());
            response.getStream().close();

            assertTrue(session.get(AbstractApacheClientHttpInvoker.HTTP_CLIENT) != null);
            HttpInvokerSessionResources.close(session);
            assertTrue(session.get(AbstractApacheClientHttpInvoker.HTTP_CLIENT) == null);
        } finally {
            HttpInvokerSessionResources.close(session);
            server.stop();
        }
    }

    @Test
    public void largeUploadSpillsToTempFileAndCleansUp() throws Exception {
        final AtomicLong received = new AtomicLong();
        final AtomicReference<String> contentLength = new AtomicReference<String>();
        final AtomicReference<File[]> filesDuringRequest = new AtomicReference<File[]>();
        final AtomicLong fileSizeDuringRequest = new AtomicLong(-1);

        File tempDir = Files.createTempDirectory("opencmis-spool-ok-").toFile();
        try {
            ServerHandle server = startServer("/upload", new HttpHandler() {
                @Override
                public void handle(HttpExchange exchange) throws IOException {
                    contentLength.set(exchange.getRequestHeaders().getFirst("Content-Length"));
                    File[] files = tempDir.listFiles();
                    filesDuringRequest.set(files == null ? new File[0] : files);
                    if (files != null && files.length == 1) {
                        fileSizeDuringRequest.set(files[0].length());
                    }
                    byte[] buf = new byte[64 * 1024];
                    InputStream in = exchange.getRequestBody();
                    int n;
                    long total = 0;
                    while ((n = in.read(buf)) >= 0) {
                        total += n;
                    }
                    received.set(total);
                    exchange.sendResponseHeaders(201, -1);
                    exchange.close();
                }
            });

            BindingSession session = new SessionImpl();
            try {
                session.put(SessionParameter.HTTP_REQUEST_MEMORY_LIMIT, Integer.valueOf(8 * 1024), true);
                session.put(SessionParameter.HTTP_TEMP_DIR, tempDir.getAbsolutePath(), true);
                final int payloadSize = 64 * 1024;
                ApacheClientHttpInvoker invoker = new ApacheClientHttpInvoker();
                Response response = invoker.invokePOST(
                        new UrlBuilder("http://127.0.0.1:" + server.port + "/upload"), "application/octet-stream",
                        new Output() {
                            @Override
                            public void write(OutputStream out) throws Exception {
                                byte[] chunk = new byte[4096];
                                Arrays.fill(chunk, (byte) 'A');
                                int left = payloadSize;
                                while (left > 0) {
                                    int n = Math.min(left, chunk.length);
                                    out.write(chunk, 0, n);
                                    left -= n;
                                }
                            }
                        }, session);
                assertEquals(201, response.getResponseCode());
                assertEquals(payloadSize, received.get());
                assertEquals(String.valueOf(payloadSize), contentLength.get());
                File[] during = filesDuringRequest.get();
                assertNotNull(during);
                assertEquals(1, during.length, "temp file must exist while request is in flight");
                assertEquals(payloadSize, fileSizeDuringRequest.get());
                File[] after = tempDir.listFiles();
                assertTrue(after == null || after.length == 0, "temp file must be deleted after success");
                assertEquals(0, RequestSpoolLimiter.getInstance().getActiveSpools());
                assertEquals(0, RequestSpoolLimiter.getInstance().getActiveBytes());
            } finally {
                HttpInvokerSessionResources.close(session);
                server.stop();
            }
        } finally {
            deleteRecursively(tempDir);
        }
    }

    @Test
    public void failedUploadDeletesTempFile() throws Exception {
        File tempDir = Files.createTempDirectory("opencmis-spool-fail-").toFile();
        BindingSession session = new SessionImpl();
        try {
            session.put(SessionParameter.HTTP_REQUEST_MEMORY_LIMIT, Integer.valueOf(1024), true);
            session.put(SessionParameter.HTTP_TEMP_DIR, tempDir.getAbsolutePath(), true);
            ApacheClientHttpInvoker invoker = new ApacheClientHttpInvoker();
            assertThrows(CmisConnectionException.class, () -> invoker.invokePOST(
                    new UrlBuilder("http://127.0.0.1:1/upload-unreachable"), "application/octet-stream", new Output() {
                        @Override
                        public void write(OutputStream out) throws Exception {
                            byte[] chunk = new byte[4096];
                            Arrays.fill(chunk, (byte) 'X');
                            for (int i = 0; i < 8; i++) {
                                out.write(chunk);
                            }
                        }
                    }, session));
            File[] after = tempDir.listFiles();
            assertTrue(after == null || after.length == 0, "temp file must be deleted after failure");
            assertEquals(0, RequestSpoolLimiter.getInstance().getActiveSpools());
        } finally {
            HttpInvokerSessionResources.close(session);
            deleteRecursively(tempDir);
        }
    }

    @Test
    public void largeUploadSpoolsActualBytesIndependentOfWriterHints() throws Exception {
        final AtomicLong received = new AtomicLong();
        final AtomicReference<String> contentLength = new AtomicReference<String>();
        final AtomicReference<File[]> filesDuringRequest = new AtomicReference<File[]>();

        File tempDir = Files.createTempDirectory("opencmis-spool-actual-").toFile();
        try {
            ServerHandle server = startServer("/upload", new HttpHandler() {
                @Override
                public void handle(HttpExchange exchange) throws IOException {
                    contentLength.set(exchange.getRequestHeaders().getFirst("Content-Length"));
                    File[] files = tempDir.listFiles();
                    filesDuringRequest.set(files == null ? new File[0] : files);
                    received.set(drainCount(exchange.getRequestBody()));
                    exchange.sendResponseHeaders(201, -1);
                    exchange.close();
                }
            });

            BindingSession session = new SessionImpl();
            try {
                session.put(SessionParameter.HTTP_REQUEST_MEMORY_LIMIT, Integer.valueOf(1024), true);
                session.put(SessionParameter.HTTP_TEMP_DIR, tempDir.getAbsolutePath(), true);
                final int actualSize = 32 * 1024;
                ApacheClientHttpInvoker invoker = new ApacheClientHttpInvoker();
                Response response = invoker.invokePOST(
                        new UrlBuilder("http://127.0.0.1:" + server.port + "/upload"), "application/octet-stream",
                        fillingOutput(actualSize / 4096, 4096, (byte) 'D'), session);
                assertEquals(201, response.getResponseCode());
                assertEquals(actualSize, received.get());
                assertEquals(String.valueOf(actualSize), contentLength.get());
                File[] during = filesDuringRequest.get();
                assertNotNull(during);
                assertEquals(1, during.length, "upload over memory limit must spill to temp");
            } finally {
                HttpInvokerSessionResources.close(session);
                server.stop();
            }
        } finally {
            deleteRecursively(tempDir);
        }
    }

    @Test
    public void reusedOneShotOutputSendsActualSecondBodyLength() throws Exception {
        final AtomicReference<String> firstLen = new AtomicReference<String>();
        final AtomicReference<String> secondLen = new AtomicReference<String>();
        final AtomicLong calls = new AtomicLong();

        ServerHandle server = startServer("/upload", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                String cl = exchange.getRequestHeaders().getFirst("Content-Length");
                if (calls.getAndIncrement() == 0) {
                    firstLen.set(cl);
                } else {
                    secondLen.set(cl);
                }
                drain(exchange.getRequestBody());
                exchange.sendResponseHeaders(201, -1);
                exchange.close();
            }
        });

        BindingSession session = new SessionImpl();
        try {
            session.put(SessionParameter.HTTP_REQUEST_MEMORY_LIMIT, Integer.valueOf(64 * 1024), true);
            final byte[] payload = "New content".getBytes(StandardCharsets.UTF_8);
            final java.io.ByteArrayInputStream stream = new java.io.ByteArrayInputStream(payload);
            Output once = new Output() {
                @Override
                public void write(OutputStream out) throws Exception {
                    byte[] buf = new byte[64];
                    int n;
                    while ((n = stream.read(buf)) >= 0) {
                        out.write(buf, 0, n);
                    }
                }
            };

            ApacheClientHttpInvoker invoker = new ApacheClientHttpInvoker();
            UrlBuilder url = new UrlBuilder("http://127.0.0.1:" + server.port + "/upload");
            assertEquals(201, invoker.invokePOST(url, "application/octet-stream", once, session).getResponseCode());
            assertEquals(201, invoker.invokePOST(url, "application/octet-stream", once, session).getResponseCode());
            assertEquals(String.valueOf(payload.length), firstLen.get());
            assertEquals("0", secondLen.get(), "second use of exhausted stream must send Content-Length 0");
        } finally {
            HttpInvokerSessionResources.close(session);
            server.stop();
        }
    }

    @Test
    public void concurrentSpoolLimitIsEnforced() throws Exception {
        File tempDir = Files.createTempDirectory("opencmis-spool-limit-").toFile();
        final CountDownLatch firstSpoolReady = new CountDownLatch(1);
        final CountDownLatch releaseFirst = new CountDownLatch(1);

        try {
            ServerHandle server = startServer("/upload", new HttpHandler() {
                @Override
                public void handle(HttpExchange exchange) throws IOException {
                    firstSpoolReady.countDown();
                    try {
                        releaseFirst.await(10, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    drain(exchange.getRequestBody());
                    exchange.sendResponseHeaders(201, -1);
                    exchange.close();
                }
            });

            BindingSession session = new SessionImpl();
            ExecutorService pool = Executors.newFixedThreadPool(2);
            executors.add(pool);
            try {
                session.put(SessionParameter.HTTP_REQUEST_MEMORY_LIMIT, Integer.valueOf(1024), true);
                session.put(SessionParameter.HTTP_TEMP_DIR, tempDir.getAbsolutePath(), true);
                session.put(SessionParameter.HTTP_REQUEST_SPOOL_MAX_CONCURRENT, Integer.valueOf(1), true);
                session.put(SessionParameter.HTTP_CONNECTION_REQUEST_TIMEOUT, Integer.valueOf(500), true);

                final ApacheClientHttpInvoker invoker = new ApacheClientHttpInvoker();
                final UrlBuilder url = new UrlBuilder("http://127.0.0.1:" + server.port + "/upload");
                final Output bigBody = fillingOutput(8, 4096, (byte) 'L');

                Future<Response> first = pool.submit(() -> invoker.invokePOST(url, "application/octet-stream", bigBody,
                        session));
                assertTrue(firstSpoolReady.await(5, TimeUnit.SECONDS));

                assertThrows(CmisConnectionException.class,
                        () -> invoker.invokePOST(url, "application/octet-stream", bigBody, session));

                releaseFirst.countDown();
                assertEquals(201, first.get(10, TimeUnit.SECONDS).getResponseCode());
            } finally {
                releaseFirst.countDown();
                HttpInvokerSessionResources.close(session);
                server.stop();
            }
        } finally {
            deleteRecursively(tempDir);
        }
    }

    @Test
    public void conflictingProcessWideSpoolLimitsAreRejected() throws Exception {
        File tempDir = Files.createTempDirectory("opencmis-conflict-limits-").toFile();
        final CountDownLatch hold = new CountDownLatch(1);
        final CountDownLatch firstHeld = new CountDownLatch(1);
        try {
            ServerHandle server = startServer("/upload", new HttpHandler() {
                @Override
                public void handle(HttpExchange exchange) throws IOException {
                    firstHeld.countDown();
                    try {
                        hold.await(10, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    drain(exchange.getRequestBody());
                    exchange.sendResponseHeaders(201, -1);
                    exchange.close();
                }
            });

            BindingSession sessionMax1 = new SessionImpl();
            BindingSession sessionMax2 = new SessionImpl();
            ExecutorService pool = Executors.newFixedThreadPool(2);
            executors.add(pool);
            try {
                for (BindingSession session : Arrays.asList(sessionMax1, sessionMax2)) {
                    session.put(SessionParameter.HTTP_REQUEST_MEMORY_LIMIT, Integer.valueOf(1024), true);
                    session.put(SessionParameter.HTTP_TEMP_DIR, tempDir.getAbsolutePath(), true);
                    session.put(SessionParameter.HTTP_CONNECTION_REQUEST_TIMEOUT, Integer.valueOf(500), true);
                    session.put(SessionParameter.HTTP_REQUEST_SPOOL_MAX_TOTAL_BYTES, Long.valueOf(64 * 1024 * 1024),
                            true);
                }
                sessionMax1.put(SessionParameter.HTTP_REQUEST_SPOOL_MAX_CONCURRENT, Integer.valueOf(1), true);
                sessionMax2.put(SessionParameter.HTTP_REQUEST_SPOOL_MAX_CONCURRENT, Integer.valueOf(2), true);

                ApacheClientHttpInvoker invoker = new ApacheClientHttpInvoker();
                UrlBuilder url = new UrlBuilder("http://127.0.0.1:" + server.port + "/upload");
                Output body = fillingOutput(4, 4096, (byte) 'M');

                Future<Response> first = pool
                        .submit(() -> invoker.invokePOST(url, "application/octet-stream", body, sessionMax1));
                assertTrue(firstHeld.await(5, TimeUnit.SECONDS));
                assertEquals(Integer.valueOf(1), RequestSpoolLimiter.getInstance().getLockedMaxConcurrentForTests());

                CmisConnectionException conflict = assertThrows(CmisConnectionException.class,
                        () -> invoker.invokePOST(url, "application/octet-stream", body, sessionMax2));
                assertTrue(conflict.getMessage().contains("limits already set"));

                hold.countDown();
                assertEquals(201, first.get(10, TimeUnit.SECONDS).getResponseCode());
                assertEquals(0, RequestSpoolLimiter.getInstance().getActiveSpools());
            } finally {
                hold.countDown();
                HttpInvokerSessionResources.close(sessionMax1);
                HttpInvokerSessionResources.close(sessionMax2);
                server.stop();
            }
        } finally {
            deleteRecursively(tempDir);
        }
    }

    @Test
    public void conflictingProcessWideTotalBytesAreRejected() throws Exception {
        File tempDir = Files.createTempDirectory("opencmis-conflict-total-").toFile();
        BindingSession first = new SessionImpl();
        BindingSession second = new SessionImpl();
        try {
            for (BindingSession session : Arrays.asList(first, second)) {
                session.put(SessionParameter.HTTP_REQUEST_MEMORY_LIMIT, Integer.valueOf(512), true);
                session.put(SessionParameter.HTTP_TEMP_DIR, tempDir.getAbsolutePath(), true);
                session.put(SessionParameter.HTTP_REQUEST_SPOOL_MAX_CONCURRENT, Integer.valueOf(2), true);
            }
            first.put(SessionParameter.HTTP_REQUEST_SPOOL_MAX_TOTAL_BYTES, Long.valueOf(32 * 1024), true);
            second.put(SessionParameter.HTTP_REQUEST_SPOOL_MAX_TOTAL_BYTES, Long.valueOf(64 * 1024), true);

            ApacheClientHttpInvoker invoker = new ApacheClientHttpInvoker();
            // Trigger first lock via a failed connect after spooling.
            assertThrows(CmisConnectionException.class, () -> invoker.invokePOST(
                    new UrlBuilder("http://127.0.0.1:1/upload"), "application/octet-stream",
                    fillingOutput(4, 1024, (byte) 'A'), first));
            assertEquals(Long.valueOf(32 * 1024), RequestSpoolLimiter.getInstance().getLockedMaxTotalBytesForTests());

            CmisConnectionException conflict = assertThrows(CmisConnectionException.class,
                    () -> invoker.invokePOST(new UrlBuilder("http://127.0.0.1:1/upload"), "application/octet-stream",
                            fillingOutput(4, 1024, (byte) 'B'), second));
            assertTrue(conflict.getMessage().contains("limits already set"));
        } finally {
            HttpInvokerSessionResources.close(first);
            HttpInvokerSessionResources.close(second);
            deleteRecursively(tempDir);
        }
    }

    @Test
    public void memoryOnlyMaterializationConcurrentLimitIsEnforced() throws Exception {
        final CountDownLatch hold = new CountDownLatch(1);
        final CountDownLatch firstReady = new CountDownLatch(1);
        ServerHandle server = startServer("/upload", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                firstReady.countDown();
                try {
                    hold.await(10, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                drain(exchange.getRequestBody());
                exchange.sendResponseHeaders(201, -1);
                exchange.close();
            }
        });

        BindingSession session = new SessionImpl();
        ExecutorService pool = Executors.newFixedThreadPool(2);
        executors.add(pool);
        try {
            // Stay under memory limit so this never spills to disk.
            session.put(SessionParameter.HTTP_REQUEST_MEMORY_LIMIT, Integer.valueOf(64 * 1024), true);
            session.put(SessionParameter.HTTP_REQUEST_SPOOL_MAX_CONCURRENT, Integer.valueOf(1), true);
            session.put(SessionParameter.HTTP_REQUEST_SPOOL_MAX_TOTAL_BYTES, Long.valueOf(16 * 1024 * 1024), true);
            session.put(SessionParameter.HTTP_CONNECTION_REQUEST_TIMEOUT, Integer.valueOf(400), true);

            ApacheClientHttpInvoker invoker = new ApacheClientHttpInvoker();
            UrlBuilder url = new UrlBuilder("http://127.0.0.1:" + server.port + "/upload");
            Output smallBody = fillingOutput(1, 1024, (byte) 'm');

            Future<Response> first = pool
                    .submit(() -> invoker.invokePOST(url, "application/octet-stream", smallBody, session));
            assertTrue(firstReady.await(5, TimeUnit.SECONDS));

            assertThrows(CmisConnectionException.class,
                    () -> invoker.invokePOST(url, "application/octet-stream", smallBody, session));

            hold.countDown();
            assertEquals(201, first.get(10, TimeUnit.SECONDS).getResponseCode());
            assertEquals(0, RequestSpoolLimiter.getInstance().getActiveSpools());
            assertEquals(0, RequestSpoolLimiter.getInstance().getActiveBytes());
        } finally {
            hold.countDown();
            HttpInvokerSessionResources.close(session);
            server.stop();
        }
    }

    @Test
    public void memoryOnlyTotalBytesLimitIsEnforced() throws Exception {
        final CountDownLatch hold = new CountDownLatch(1);
        final CountDownLatch firstReady = new CountDownLatch(1);
        ServerHandle server = startServer("/upload", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                firstReady.countDown();
                try {
                    hold.await(10, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                drain(exchange.getRequestBody());
                exchange.sendResponseHeaders(201, -1);
                exchange.close();
            }
        });

        BindingSession session = new SessionImpl();
        ExecutorService pool = Executors.newFixedThreadPool(2);
        executors.add(pool);
        try {
            session.put(SessionParameter.HTTP_REQUEST_MEMORY_LIMIT, Integer.valueOf(64 * 1024), true);
            session.put(SessionParameter.HTTP_REQUEST_SPOOL_MAX_CONCURRENT, Integer.valueOf(4), true);
            session.put(SessionParameter.HTTP_REQUEST_SPOOL_MAX_TOTAL_BYTES, Integer.valueOf(1500), true);
            session.put(SessionParameter.HTTP_CONNECTION_REQUEST_TIMEOUT, Integer.valueOf(400), true);

            ApacheClientHttpInvoker invoker = new ApacheClientHttpInvoker();
            UrlBuilder url = new UrlBuilder("http://127.0.0.1:" + server.port + "/upload");
            Output body = fillingOutput(1, 1024, (byte) 't');

            Future<Response> first = pool
                    .submit(() -> invoker.invokePOST(url, "application/octet-stream", body, session));
            assertTrue(firstReady.await(5, TimeUnit.SECONDS));

            assertThrows(CmisConnectionException.class,
                    () -> invoker.invokePOST(url, "application/octet-stream", body, session));

            hold.countDown();
            assertEquals(201, first.get(10, TimeUnit.SECONDS).getResponseCode());
        } finally {
            hold.countDown();
            HttpInvokerSessionResources.close(session);
            server.stop();
        }
    }

    @Test
    public void failedTempDeleteKeepsBytesInBudget() {
        RequestSpoolLimiter limiter = RequestSpoolLimiter.getInstance();
        RequestSpoolLimiter.SpoolLease lease = limiter.acquire(2, 10_000, 1000);
        lease.addBytes(2500);
        lease.releasePermitKeepBytes();
        assertEquals(0, limiter.getActiveSpools());
        assertEquals(2500, limiter.getActiveBytes());
        assertEquals(2500, limiter.getOrphanedBytesForTests());

        RequestSpoolLimiter.SpoolLease other = limiter.acquire(2, 10_000, 1000);
        assertThrows(CmisConnectionException.class, () -> other.addBytes(8000));
        other.release();
        assertEquals(2500, limiter.getActiveBytes());
    }

    @Test
    public void spillFailureWithFailedTempDeleteKeepsBytesInBudget() throws Exception {
        File tempDir = Files.createTempDirectory("opencmis-spill-orphan-").toFile();
        BindingSession session = new SessionImpl();
        try {
            session.put(SessionParameter.HTTP_REQUEST_MEMORY_LIMIT, Integer.valueOf(1000), true);
            session.put(SessionParameter.HTTP_TEMP_DIR, tempDir.getAbsolutePath(), true);
            session.put(SessionParameter.HTTP_REQUEST_SPOOL_MAX_CONCURRENT, Integer.valueOf(2), true);
            session.put(SessionParameter.HTTP_REQUEST_SPOOL_MAX_TOTAL_BYTES, Long.valueOf(64 * 1024), true);

            AbstractApacheClientHttpInvoker.FORCE_SPILL_FAILURE_FOR_TESTS.set(true);
            AbstractApacheClientHttpInvoker.FORCE_TEMP_DELETE_FAILURE_FOR_TESTS.set(true);

            ApacheClientHttpInvoker invoker = new ApacheClientHttpInvoker();
            // First 800-byte chunk fits in memory (charged); second forces spill of charged bytes.
            assertThrows(CmisConnectionException.class,
                    () -> invoker.invokePOST(new UrlBuilder("http://127.0.0.1:1/upload"), "application/octet-stream",
                            new Output() {
                                @Override
                                public void write(OutputStream out) throws Exception {
                                    byte[] chunk = new byte[800];
                                    Arrays.fill(chunk, (byte) 'O');
                                    out.write(chunk);
                                    out.flush(); // push through BufferedOutputStream into the spool
                                    out.write(chunk);
                                }
                            }, session));

            assertEquals(0, RequestSpoolLimiter.getInstance().getActiveSpools());
            assertEquals(800, RequestSpoolLimiter.getInstance().getActiveBytes(),
                    "orphaned spill path must keep already-charged memory bytes");
            assertEquals(800, RequestSpoolLimiter.getInstance().getOrphanedBytesForTests());
            File[] leftover = tempDir.listFiles();
            assertNotNull(leftover);
            assertTrue(leftover.length >= 1, "undeleted spill temp file should remain");
        } finally {
            AbstractApacheClientHttpInvoker.FORCE_SPILL_FAILURE_FOR_TESTS.set(false);
            AbstractApacheClientHttpInvoker.FORCE_TEMP_DELETE_FAILURE_FOR_TESTS.set(false);
            HttpInvokerSessionResources.close(session);
            // Force-delete leftovers for the test cleanup (production leaves them charged).
            File[] leftover = tempDir.listFiles();
            if (leftover != null) {
                for (File file : leftover) {
                    file.delete();
                }
            }
            deleteRecursively(tempDir);
        }
    }

    @Test
    public void spoolMaxSizeIsEnforced() throws Exception {
        File tempDir = Files.createTempDirectory("opencmis-max-size-").toFile();
        BindingSession session = new SessionImpl();
        try {
            session.put(SessionParameter.HTTP_REQUEST_MEMORY_LIMIT, Integer.valueOf(512), true);
            session.put(SessionParameter.HTTP_TEMP_DIR, tempDir.getAbsolutePath(), true);
            session.put(SessionParameter.HTTP_REQUEST_SPOOL_MAX_SIZE, Integer.valueOf(2048), true);
            ApacheClientHttpInvoker invoker = new ApacheClientHttpInvoker();
            assertThrows(CmisConnectionException.class,
                    () -> invoker.invokePOST(new UrlBuilder("http://127.0.0.1:1/upload"), "application/octet-stream",
                            fillingOutput(8, 1024, (byte) 'S'), session));
            File[] after = tempDir.listFiles();
            assertTrue(after == null || after.length == 0);
            assertEquals(0, RequestSpoolLimiter.getInstance().getActiveSpools());
            assertEquals(0, RequestSpoolLimiter.getInstance().getActiveBytes());
        } finally {
            HttpInvokerSessionResources.close(session);
            deleteRecursively(tempDir);
        }
    }

    @Test
    public void memoryOnlyMaxSizeIsEnforcedWithoutSpill() throws Exception {
        BindingSession session = new SessionImpl();
        try {
            session.put(SessionParameter.HTTP_REQUEST_MEMORY_LIMIT, Integer.valueOf(64 * 1024), true);
            session.put(SessionParameter.HTTP_REQUEST_SPOOL_MAX_SIZE, Integer.valueOf(2048), true);
            ApacheClientHttpInvoker invoker = new ApacheClientHttpInvoker();
            assertThrows(CmisConnectionException.class,
                    () -> invoker.invokePOST(new UrlBuilder("http://127.0.0.1:1/upload"), "application/octet-stream",
                            fillingOutput(4, 1024, (byte) 'M'), session));
            assertEquals(0, RequestSpoolLimiter.getInstance().getActiveSpools());
            assertEquals(0, RequestSpoolLimiter.getInstance().getActiveBytes());
        } finally {
            HttpInvokerSessionResources.close(session);
        }
    }

    @Test
    public void spoolMaxTotalBytesIsEnforced() throws Exception {
        File tempDir = Files.createTempDirectory("opencmis-max-total-").toFile();
        final CountDownLatch hold = new CountDownLatch(1);
        final CountDownLatch firstReady = new CountDownLatch(1);
        try {
            ServerHandle server = startServer("/upload", new HttpHandler() {
                @Override
                public void handle(HttpExchange exchange) throws IOException {
                    firstReady.countDown();
                    try {
                        hold.await(10, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    drain(exchange.getRequestBody());
                    exchange.sendResponseHeaders(201, -1);
                    exchange.close();
                }
            });

            BindingSession session = new SessionImpl();
            ExecutorService pool = Executors.newFixedThreadPool(2);
            executors.add(pool);
            try {
                session.put(SessionParameter.HTTP_REQUEST_MEMORY_LIMIT, Integer.valueOf(512), true);
                session.put(SessionParameter.HTTP_TEMP_DIR, tempDir.getAbsolutePath(), true);
                session.put(SessionParameter.HTTP_REQUEST_SPOOL_MAX_CONCURRENT, Integer.valueOf(4), true);
                session.put(SessionParameter.HTTP_REQUEST_SPOOL_MAX_TOTAL_BYTES, Integer.valueOf(8192), true);
                session.put(SessionParameter.HTTP_CONNECTION_REQUEST_TIMEOUT, Integer.valueOf(500), true);

                ApacheClientHttpInvoker invoker = new ApacheClientHttpInvoker();
                UrlBuilder url = new UrlBuilder("http://127.0.0.1:" + server.port + "/upload");
                Output body = fillingOutput(4, 2048, (byte) 'T'); // 8 KiB each

                Future<Response> first = pool
                        .submit(() -> invoker.invokePOST(url, "application/octet-stream", body, session));
                assertTrue(firstReady.await(5, TimeUnit.SECONDS));

                assertThrows(CmisConnectionException.class,
                        () -> invoker.invokePOST(url, "application/octet-stream", body, session));

                hold.countDown();
                assertEquals(201, first.get(10, TimeUnit.SECONDS).getResponseCode());
            } finally {
                hold.countDown();
                HttpInvokerSessionResources.close(session);
                server.stop();
            }
        } finally {
            deleteRecursively(tempDir);
        }
    }

    @Test
    public void ioExceptionDuringSpoolCleansTempAndLease() throws Exception {
        File tempDir = Files.createTempDirectory("opencmis-ioe-spool-").toFile();
        BindingSession session = new SessionImpl();
        try {
            session.put(SessionParameter.HTTP_REQUEST_MEMORY_LIMIT, Integer.valueOf(512), true);
            session.put(SessionParameter.HTTP_TEMP_DIR, tempDir.getAbsolutePath(), true);
            ApacheClientHttpInvoker invoker = new ApacheClientHttpInvoker();
            assertThrows(CmisConnectionException.class,
                    () -> invoker.invokePOST(new UrlBuilder("http://127.0.0.1:1/upload"), "application/octet-stream",
                            new Output() {
                                @Override
                                public void write(OutputStream out) throws Exception {
                                    byte[] chunk = new byte[1024];
                                    Arrays.fill(chunk, (byte) 'E');
                                    out.write(chunk);
                                    out.write(chunk);
                                    throw new IOException("boom-during-spool");
                                }
                            }, session));
            File[] after = tempDir.listFiles();
            assertTrue(after == null || after.length == 0);
            assertEquals(0, RequestSpoolLimiter.getInstance().getActiveSpools());
            assertEquals(0, RequestSpoolLimiter.getInstance().getActiveBytes());
        } finally {
            HttpInvokerSessionResources.close(session);
            deleteRecursively(tempDir);
        }
    }

    @Test
    public void negativeConnectionRequestTimeoutDoesNotBlockSpoolForever() throws Exception {
        File tempDir = Files.createTempDirectory("opencmis-timeout-norm-").toFile();
        final CountDownLatch hold = new CountDownLatch(1);
        final CountDownLatch firstReady = new CountDownLatch(1);
        try {
            ServerHandle server = startServer("/upload", new HttpHandler() {
                @Override
                public void handle(HttpExchange exchange) throws IOException {
                    firstReady.countDown();
                    try {
                        hold.await(10, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    drain(exchange.getRequestBody());
                    exchange.sendResponseHeaders(201, -1);
                    exchange.close();
                }
            });

            BindingSession session = new SessionImpl();
            ExecutorService pool = Executors.newFixedThreadPool(2);
            executors.add(pool);
            try {
                session.put(SessionParameter.HTTP_REQUEST_MEMORY_LIMIT, Integer.valueOf(512), true);
                session.put(SessionParameter.HTTP_TEMP_DIR, tempDir.getAbsolutePath(), true);
                session.put(SessionParameter.HTTP_REQUEST_SPOOL_MAX_CONCURRENT, Integer.valueOf(1), true);
                // -1 must normalize to a finite wait (not Semaphore.acquire forever)
                session.put(SessionParameter.HTTP_CONNECTION_REQUEST_TIMEOUT, Integer.valueOf(-1), true);
                session.put(SessionParameter.CONNECT_TIMEOUT, Integer.valueOf(300), true);

                ApacheClientHttpInvoker invoker = new ApacheClientHttpInvoker();
                UrlBuilder url = new UrlBuilder("http://127.0.0.1:" + server.port + "/upload");
                Output body = fillingOutput(4, 1024, (byte) 'W');

                Future<Response> first = pool
                        .submit(() -> invoker.invokePOST(url, "application/octet-stream", body, session));
                assertTrue(firstReady.await(5, TimeUnit.SECONDS));

                long started = System.nanoTime();
                assertThrows(CmisConnectionException.class,
                        () -> invoker.invokePOST(url, "application/octet-stream", body, session));
                long waitedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
                assertTrue(waitedMs < 5000, "spool wait with -1 must not block indefinitely, waitedMs=" + waitedMs);

                hold.countDown();
                assertEquals(201, first.get(10, TimeUnit.SECONDS).getResponseCode());
            } finally {
                hold.countDown();
                HttpInvokerSessionResources.close(session);
                server.stop();
            }
        } finally {
            deleteRecursively(tempDir);
        }
    }

    @Test
    public void responseLargerThanBufferLimitHoldsLeaseUntilClosed() throws Exception {
        final int bodySize = 32 * 1024;
        ServerHandle server = startServer("/big", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                byte[] body = new byte[bodySize];
                Arrays.fill(body, (byte) 'B');
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
                exchange.close();
            }
        });

        BindingSession session = new SessionImpl();
        try {
            session.put(SessionParameter.HTTP_RESPONSE_BUFFER_LIMIT, Integer.valueOf(1024), true);
            session.put(SessionParameter.HTTP_MAX_CONNECTIONS_PER_HOST, Integer.valueOf(1), true);
            session.put(SessionParameter.HTTP_MAX_CONNECTIONS, Integer.valueOf(1), true);
            session.put(SessionParameter.HTTP_CONNECTION_REQUEST_TIMEOUT, Integer.valueOf(500), true);

            ApacheClientHttpInvoker invoker = new ApacheClientHttpInvoker();
            UrlBuilder url = new UrlBuilder("http://127.0.0.1:" + server.port + "/big");

            Response r1 = invoker.invokeGET(url, session);
            assertEquals(200, r1.getResponseCode());

            assertThrows(CmisConnectionException.class, () -> invoker.invokeGET(url, session));

            r1.getStream().close();

            Response r2 = invoker.invokeGET(url, session);
            assertEquals(200, r2.getResponseCode());
            assertEquals(bodySize, readFully(r2.getStream()).length);
        } finally {
            HttpInvokerSessionResources.close(session);
            server.stop();
        }
    }

    @Test
    public void errorResponseIsReadableAndReleasesConnection() throws Exception {
        ServerHandle server = startServer("/err", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                byte[] body = "boom".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=UTF-8");
                exchange.sendResponseHeaders(500, body.length);
                exchange.getResponseBody().write(body);
                exchange.close();
            }
        });

        BindingSession session = new SessionImpl();
        try {
            session.put(SessionParameter.HTTP_MAX_CONNECTIONS_PER_HOST, Integer.valueOf(1), true);
            session.put(SessionParameter.HTTP_MAX_CONNECTIONS, Integer.valueOf(1), true);
            session.put(SessionParameter.HTTP_CONNECTION_REQUEST_TIMEOUT, Integer.valueOf(3000), true);

            ApacheClientHttpInvoker invoker = new ApacheClientHttpInvoker();
            UrlBuilder url = new UrlBuilder("http://127.0.0.1:" + server.port + "/err");

            for (int i = 0; i < 5; i++) {
                Response response = invoker.invokeGET(url, session);
                assertEquals(500, response.getResponseCode());
                assertEquals("boom", response.getErrorContent());
            }
        } finally {
            HttpInvokerSessionResources.close(session);
            server.stop();
        }
    }

    @Test
    public void exceptionAfterStreamingStillReleasesConnection() throws Exception {
        final int bodySize = 8 * 1024;
        ServerHandle server = startServer("/stream", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                byte[] body = new byte[bodySize];
                Arrays.fill(body, (byte) 'C');
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
                exchange.close();
            }
        });

        BindingSession session = new SessionImpl();
        try {
            session.put(SessionParameter.HTTP_RESPONSE_BUFFER_LIMIT, Integer.valueOf(64), true);
            session.put(SessionParameter.HTTP_MAX_CONNECTIONS_PER_HOST, Integer.valueOf(1), true);
            session.put(SessionParameter.HTTP_MAX_CONNECTIONS, Integer.valueOf(1), true);
            session.put(SessionParameter.HTTP_CONNECTION_REQUEST_TIMEOUT, Integer.valueOf(3000), true);
            session.put(CmisBindingsHelper.AUTHENTICATION_PROVIDER_OBJECT, new ThrowingAuthProvider(), true);

            ApacheClientHttpInvoker invoker = new ApacheClientHttpInvoker();
            UrlBuilder url = new UrlBuilder("http://127.0.0.1:" + server.port + "/stream");

            assertThrows(CmisConnectionException.class, () -> invoker.invokeGET(url, session));

            session.put(CmisBindingsHelper.AUTHENTICATION_PROVIDER_OBJECT, null, true);
            Response ok = invoker.invokeGET(url, session);
            assertEquals(200, ok.getResponseCode());
            ok.getStream().close();
        } finally {
            HttpInvokerSessionResources.close(session);
            server.stop();
        }
    }

    public static final class ThrowingAuthProvider extends AbstractAuthenticationProvider {
        private static final long serialVersionUID = 1L;

        @Override
        public void putResponseHeaders(String url, int statusCode, java.util.Map<String, List<String>> headers) {
            throw new RuntimeException("auth-provider-boom");
        }
    }

    private ServerHandle startPingServer() throws IOException {
        return startServer("/ping", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                byte[] body = "pong".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=UTF-8");
                exchange.sendResponseHeaders(200, body.length);
                OutputStream os = exchange.getResponseBody();
                os.write(body);
                os.close();
            }
        });
    }

    private ServerHandle startServer(String path, HttpHandler handler) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(path, handler);
        ExecutorService executor = Executors.newCachedThreadPool();
        executors.add(executor);
        server.setExecutor(executor);
        server.start();
        return new ServerHandle(server, server.getAddress().getPort());
    }

    private static void drain(InputStream in) throws IOException {
        drainCount(in);
    }

    private static long drainCount(InputStream in) throws IOException {
        byte[] buf = new byte[4096];
        long total = 0;
        int n;
        while ((n = in.read(buf)) >= 0) {
            total += n;
        }
        return total;
    }

    private static Output fillingOutput(final int chunks, final int chunkSize, final byte fill) {
        return new Output() {
            @Override
            public void write(OutputStream out) throws Exception {
                byte[] chunk = new byte[chunkSize];
                Arrays.fill(chunk, fill);
                for (int i = 0; i < chunks; i++) {
                    out.write(chunk);
                }
            }
        };
    }

    private static void deleteRecursively(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                deleteRecursively(child);
            }
        }
        file.delete();
    }

    private static byte[] readFully(InputStream stream) throws IOException {
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[1024];
            int n;
            while ((n = stream.read(chunk)) >= 0) {
                buffer.write(chunk, 0, n);
            }
            return buffer.toByteArray();
        } finally {
            stream.close();
        }
    }

    private static final class ServerHandle {
        private final HttpServer server;
        private final int port;

        ServerHandle(HttpServer server, int port) {
            this.server = server;
            this.port = port;
        }

        void stop() {
            server.stop(0);
        }
    }
}
