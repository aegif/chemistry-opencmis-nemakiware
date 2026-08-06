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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import org.apache.chemistry.opencmis.client.bindings.impl.SessionImpl;
import org.apache.chemistry.opencmis.client.bindings.spi.BindingSession;
import org.apache.chemistry.opencmis.client.bindings.spi.http.HttpInvoker;
import org.apache.chemistry.opencmis.client.bindings.spi.http.OkHttpHttpInvoker;
import org.apache.chemistry.opencmis.client.bindings.spi.http.Response;
import org.apache.chemistry.opencmis.commons.impl.UrlBuilder;
import org.junit.jupiter.api.Test;

/**
 * Smoke tests for the OkHttp 5 based invoker.
 */
public class OkHttpHttpInvokerTest {

    @Test
    public void testClassLoads() {
        assertNotNull(new OkHttpHttpInvoker());
        assertTrue(HttpInvoker.class.isAssignableFrom(OkHttpHttpInvoker.class));
    }

    @Test
    public void testGetAgainstLocalServer() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/ping", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                byte[] body = "pong-okhttp".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, body.length);
                OutputStream os = exchange.getResponseBody();
                os.write(body);
                os.close();
            }
        });
        server.start();

        try {
            int port = server.getAddress().getPort();
            BindingSession session = new SessionImpl();
            OkHttpHttpInvoker invoker = new OkHttpHttpInvoker();

            Response getResponse = invoker.invokeGET(new UrlBuilder("http://127.0.0.1:" + port + "/ping"), session);
            assertEquals(200, getResponse.getResponseCode());
            assertEquals("pong-okhttp", readFully(getResponse.getStream()));
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
