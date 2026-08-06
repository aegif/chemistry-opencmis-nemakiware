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
package org.apache.chemistry.opencmis.client.bindings.misc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import junit.framework.TestCase;

import org.apache.chemistry.opencmis.client.bindings.impl.SessionImpl;
import org.apache.chemistry.opencmis.client.bindings.spi.BindingSession;
import org.apache.chemistry.opencmis.client.bindings.spi.http.ApacheClientHttpInvoker;
import org.apache.chemistry.opencmis.client.bindings.spi.http.HttpInvoker;
import org.apache.chemistry.opencmis.client.bindings.spi.http.Output;
import org.apache.chemistry.opencmis.client.bindings.spi.http.Response;
import org.apache.chemistry.opencmis.commons.impl.UrlBuilder;

/**
 * Smoke tests for the Apache HttpClient 5 based invoker.
 */
public class ApacheClientHttpInvokerTest extends TestCase {

    public void testClassLoads() {
        assertNotNull(new ApacheClientHttpInvoker());
        assertTrue(HttpInvoker.class.isAssignableFrom(ApacheClientHttpInvoker.class));
    }

    public void testGetAndPostAgainstLocalServer() throws Exception {
        final AtomicReference<String> postedBody = new AtomicReference<String>();

        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/ping", new com.sun.net.httpserver.HttpHandler() {
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
        server.createContext("/echo", new com.sun.net.httpserver.HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                InputStream in = exchange.getRequestBody();
                byte[] chunk = new byte[1024];
                int n;
                while ((n = in.read(chunk)) >= 0) {
                    buffer.write(chunk, 0, n);
                }
                postedBody.set(new String(buffer.toByteArray(), StandardCharsets.UTF_8));

                byte[] body = "ok".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(201, body.length);
                OutputStream os = exchange.getResponseBody();
                os.write(body);
                os.close();
            }
        });
        server.start();

        try {
            int port = server.getAddress().getPort();
            BindingSession session = new SessionImpl();
            ApacheClientHttpInvoker invoker = new ApacheClientHttpInvoker();

            Response getResponse = invoker.invokeGET(new UrlBuilder("http://127.0.0.1:" + port + "/ping"), session);
            assertEquals(200, getResponse.getResponseCode());
            assertEquals("pong", readFully(getResponse.getStream()));

            Response postResponse = invoker.invokePOST(new UrlBuilder("http://127.0.0.1:" + port + "/echo"),
                    "text/plain; charset=UTF-8", new Output() {
                        @Override
                        public void write(OutputStream out) throws Exception {
                            out.write("hello-hc5".getBytes(StandardCharsets.UTF_8));
                        }
                    }, session);
            assertEquals(201, postResponse.getResponseCode());
            assertEquals("ok", readFully(postResponse.getStream()));
            assertEquals("hello-hc5", postedBody.get());
        } finally {
            server.stop(0);
        }
    }

    private static String readFully(InputStream stream) throws IOException {
        assertNotNull(stream);
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[1024];
            int n;
            while ((n = stream.read(chunk)) >= 0) {
                buffer.write(chunk, 0, n);
            }
            return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
        } finally {
            stream.close();
        }
    }
}
