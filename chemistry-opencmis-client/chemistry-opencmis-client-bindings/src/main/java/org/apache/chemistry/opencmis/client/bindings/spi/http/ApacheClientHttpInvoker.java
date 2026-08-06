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

import java.net.ProxySelector;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;

import org.apache.chemistry.opencmis.client.bindings.impl.CmisBindingsHelper;
import org.apache.chemistry.opencmis.client.bindings.spi.BindingSession;
import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.impl.UrlBuilder;
import org.apache.chemistry.opencmis.commons.spi.AuthenticationProvider;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.impl.routing.SystemDefaultRoutePlanner;
import org.apache.hc.client5.http.ssl.DefaultHostnameVerifier;
import org.apache.hc.client5.http.ssl.HttpsSupport;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;

/**
 * A {@link HttpInvoker} that uses The Apache HTTP client.
 */
public class ApacheClientHttpInvoker extends AbstractApacheClientHttpInvoker {

    /** Default max connections per route — sized for concurrent CMIS / TCK load. */
    public static final int DEFAULT_MAX_CONN_PER_ROUTE = 100;

    /** Default max connections in the pool. */
    public static final int DEFAULT_MAX_CONN_TOTAL = 200;

    @Override
    protected CloseableHttpClient createHttpClient(UrlBuilder url, BindingSession session) {
        PoolingHttpClientConnectionManagerBuilder connManagerBuilder = PoolingHttpClientConnectionManagerBuilder
                .create().setSSLSocketFactory(getSSLSocketFactory(url, session));

        ConnectionConfig.Builder connectionConfig = ConnectionConfig.custom()
                .setValidateAfterInactivity(TimeValue.ofMilliseconds(2000))
                .setTimeToLive(TimeValue.ofMinutes(5));

        int connectTimeout = session.get(SessionParameter.CONNECT_TIMEOUT, -1);
        if (connectTimeout >= 0) {
            connectionConfig.setConnectTimeout(Timeout.ofMilliseconds(connectTimeout));
        }

        int readTimeout = session.get(SessionParameter.READ_TIMEOUT, -1);
        if (readTimeout >= 0) {
            connectionConfig.setSocketTimeout(Timeout.ofMilliseconds(readTimeout));
        }

        connManagerBuilder.setDefaultConnectionConfig(connectionConfig.build());

        int maxPerRoute = session.get(SessionParameter.HTTP_MAX_CONNECTIONS_PER_HOST, DEFAULT_MAX_CONN_PER_ROUTE);
        int maxTotal = session.get(SessionParameter.HTTP_MAX_CONNECTIONS, DEFAULT_MAX_CONN_TOTAL);
        if (maxPerRoute < 1) {
            maxPerRoute = DEFAULT_MAX_CONN_PER_ROUTE;
        }
        if (maxTotal < 1) {
            maxTotal = DEFAULT_MAX_CONN_TOTAL;
        }
        if (maxTotal < maxPerRoute) {
            maxTotal = maxPerRoute;
        }
        connManagerBuilder.setMaxConnPerRoute(maxPerRoute);
        connManagerBuilder.setMaxConnTotal(maxTotal);

        PoolingHttpClientConnectionManager connManager = connManagerBuilder.build();

        return HttpClients.custom().setConnectionManager(connManager).setUserAgent(getUserAgent(session))
                .setDefaultRequestConfig(createRequestConfig(session))
                .setRoutePlanner(new SystemDefaultRoutePlanner(ProxySelector.getDefault()))
                .evictExpiredConnections()
                .evictIdleConnections(TimeValue.ofSeconds(30))
                .build();
    }

    /**
     * Builds a SSL Socket Factory for the Apache HTTP Client.
     */
    private SSLConnectionSocketFactory getSSLSocketFactory(final UrlBuilder url, final BindingSession session) {
        // get authentication provider
        AuthenticationProvider authProvider = CmisBindingsHelper.getAuthenticationProvider(session);

        // check SSL Socket Factory
        final SSLSocketFactory sf = authProvider == null ? null : authProvider.getSSLSocketFactory();
        if (sf == null) {
            // no custom factory -> return default factory
            return SSLConnectionSocketFactoryBuilder.create().setHostnameVerifier(HttpsSupport.getDefaultHostnameVerifier())
                    .build();
        }

        // check hostname verifier and use default if not set
        final HostnameVerifier hv = (authProvider.getHostnameVerifier() == null ? new DefaultHostnameVerifier()
                : authProvider.getHostnameVerifier());

        return new SSLConnectionSocketFactory(sf, hv);
    }
}
