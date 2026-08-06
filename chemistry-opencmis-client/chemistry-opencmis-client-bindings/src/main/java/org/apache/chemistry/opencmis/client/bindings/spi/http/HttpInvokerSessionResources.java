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

import java.io.Closeable;
import java.io.IOException;

import org.apache.chemistry.opencmis.client.bindings.spi.BindingSession;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import okhttp3.OkHttpClient;

/**
 * Releases HTTP clients stored on a {@link BindingSession} by Apache / OkHttp
 * invokers. Called from AtomPub and Browser SPI {@code close()}.
 */
public final class HttpInvokerSessionResources {

    private static final Logger LOG = LoggerFactory.getLogger(HttpInvokerSessionResources.class);

    private HttpInvokerSessionResources() {
    }

    /**
     * Closes any Apache HttpClient 5 or OkHttp client cached on the session.
     */
    public static void close(BindingSession session) {
        if (session == null) {
            return;
        }

        closeApacheClient(session);
        closeOkHttpClient(session);
    }

    private static void closeApacheClient(BindingSession session) {
        Object client = session.get(AbstractApacheClientHttpInvoker.HTTP_CLIENT);
        if (!(client instanceof CloseableHttpClient)) {
            return;
        }

        session.remove(AbstractApacheClientHttpInvoker.HTTP_CLIENT);
        try {
            ((CloseableHttpClient) client).close();
        } catch (IOException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Closing Apache HttpClient failed: {}", e.toString());
            }
        }
    }

    private static void closeOkHttpClient(BindingSession session) {
        Object client = session.get(OkHttpHttpInvoker.HTTP_CLIENT);
        if (!(client instanceof OkHttpClient)) {
            return;
        }

        session.remove(OkHttpHttpInvoker.HTTP_CLIENT);
        OkHttpClient okHttpClient = (OkHttpClient) client;
        okHttpClient.dispatcher().executorService().shutdown();
        okHttpClient.connectionPool().evictAll();
        if (okHttpClient.cache() != null) {
            try {
                okHttpClient.cache().close();
            } catch (IOException e) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Closing OkHttp cache failed: {}", e.toString());
                }
            }
        }
        if (client instanceof Closeable) {
            try {
                ((Closeable) client).close();
            } catch (IOException e) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Closing OkHttpClient failed: {}", e.toString());
                }
            }
        }
    }
}
