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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.chemistry.opencmis.client.bindings.spi.BindingSession;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.junit.jupiter.api.Test;

import okhttp3.OkHttpClient;

public class HttpInvokerSessionResourcesTest {

    @Test
    public void closeIsNoOpForNullSession() {
        assertDoesNotThrow(() -> HttpInvokerSessionResources.close(null));
    }

    @Test
    public void closeRemovesAndClosesApacheClient() throws Exception {
        CloseableHttpClient client = mock(CloseableHttpClient.class);
        MapSession session = new MapSession();
        session.put(AbstractApacheClientHttpInvoker.HTTP_CLIENT, client, true);

        HttpInvokerSessionResources.close(session);

        assertNull(session.get(AbstractApacheClientHttpInvoker.HTTP_CLIENT));
        verify(client, times(1)).close();
    }

    @Test
    public void closeIsIdempotentWhenApacheAlreadyRemoved() throws Exception {
        CloseableHttpClient client = mock(CloseableHttpClient.class);
        MapSession session = new MapSession();
        session.put(AbstractApacheClientHttpInvoker.HTTP_CLIENT, client, true);

        HttpInvokerSessionResources.close(session);
        HttpInvokerSessionResources.close(session);

        verify(client, times(1)).close();
    }

    @Test
    public void closeContinuesWhenApacheCloseThrows() throws Exception {
        CloseableHttpClient client = mock(CloseableHttpClient.class);
        doThrow(new IOException("boom")).when(client).close();
        MapSession session = new MapSession();
        session.put(AbstractApacheClientHttpInvoker.HTTP_CLIENT, client, true);

        assertDoesNotThrow(() -> HttpInvokerSessionResources.close(session));
        assertNull(session.get(AbstractApacheClientHttpInvoker.HTTP_CLIENT));
        verify(client, times(1)).close();
    }

    @Test
    public void closeClearsOkHttpClientFromSession() {
        OkHttpClient client = new OkHttpClient.Builder().callTimeout(1, TimeUnit.SECONDS).build();
        MapSession session = new MapSession();
        session.put(OkHttpHttpInvoker.HTTP_CLIENT, client, true);

        HttpInvokerSessionResources.close(session);

        assertNull(session.get(OkHttpHttpInvoker.HTTP_CLIENT));
        assertDoesNotThrow(() -> HttpInvokerSessionResources.close(session));
    }

    @Test
    public void closeIgnoresUnknownClientTypes() {
        MapSession session = new MapSession();
        session.put(AbstractApacheClientHttpInvoker.HTTP_CLIENT, "not-a-client", true);
        session.put(OkHttpHttpInvoker.HTTP_CLIENT, Integer.valueOf(1), true);

        assertDoesNotThrow(() -> HttpInvokerSessionResources.close(session));
        assertEquals("not-a-client", session.get(AbstractApacheClientHttpInvoker.HTTP_CLIENT));
        assertSame(Integer.valueOf(1), session.get(OkHttpHttpInvoker.HTTP_CLIENT));
    }

    private static final class MapSession implements BindingSession {
        private static final long serialVersionUID = 1L;
        private final Map<String, Object> values = new HashMap<String, Object>();

        @Override
        public String getSessionId() {
            return "test";
        }

        @Override
        public Collection<String> getKeys() {
            return values.keySet();
        }

        @Override
        public Object get(String key) {
            return values.get(key);
        }

        @Override
        public Object get(String key, Object defValue) {
            Object v = values.get(key);
            return v != null ? v : defValue;
        }

        @Override
        public int get(String key, int defValue) {
            Object v = values.get(key);
            return v instanceof Number ? ((Number) v).intValue() : defValue;
        }

        @Override
        public boolean get(String key, boolean defValue) {
            Object v = values.get(key);
            return v instanceof Boolean ? ((Boolean) v).booleanValue() : defValue;
        }

        @Override
        public void put(String key, Serializable object) {
            values.put(key, object);
        }

        @Override
        public void put(String key, Object object, boolean isTransient) {
            values.put(key, object);
        }

        @Override
        public void remove(String key) {
            values.remove(key);
        }

        @Override
        public void readLock() {
        }

        @Override
        public void readUnlock() {
        }

        @Override
        public void writeLock() {
        }

        @Override
        public void writeUnlock() {
        }
    }
}
