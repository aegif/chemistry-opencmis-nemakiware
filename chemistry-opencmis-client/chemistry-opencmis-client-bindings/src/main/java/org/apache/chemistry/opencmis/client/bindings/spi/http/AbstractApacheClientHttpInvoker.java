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

import static org.apache.chemistry.opencmis.commons.impl.CollectionsHelper.isNotEmpty;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocket;

import org.apache.chemistry.opencmis.client.bindings.impl.ClientVersion;
import org.apache.chemistry.opencmis.client.bindings.impl.CmisBindingsHelper;
import org.apache.chemistry.opencmis.client.bindings.spi.BindingSession;
import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.exceptions.CmisConnectionException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisRuntimeException;
import org.apache.chemistry.opencmis.commons.impl.UrlBuilder;
import org.apache.chemistry.opencmis.commons.spi.AuthenticationProvider;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.cookie.StandardCookieSpec;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.ByteArrayEntity;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link HttpInvoker} that uses The Apache HTTP client.
 */
public abstract class AbstractApacheClientHttpInvoker implements HttpInvoker {

    protected static final Logger LOG = LoggerFactory.getLogger(AbstractApacheClientHttpInvoker.class);

    public static final String HTTP_CLIENT = "org.apache.chemistry.opencmis.client.bindings.spi.http.ApacheClientHttpInvoker.httpClient";
    protected static final int BUFFER_SIZE = 2 * 1024 * 1024;

    @Override
    public Response invokeGET(UrlBuilder url, BindingSession session) {
        return invoke(url, "GET", null, null, null, session, null, null);
    }

    @Override
    public Response invokeGET(UrlBuilder url, BindingSession session, BigInteger offset, BigInteger length) {
        return invoke(url, "GET", null, null, null, session, offset, length);
    }

    @Override
    public Response invokePOST(UrlBuilder url, String contentType, Output writer, BindingSession session) {
        return invoke(url, "POST", contentType, null, writer, session, null, null);
    }

    @Override
    public Response invokePUT(UrlBuilder url, String contentType, Map<String, String> headers, Output writer,
            BindingSession session) {
        return invoke(url, "PUT", contentType, headers, writer, session, null, null);
    }

    @Override
    public Response invokeDELETE(UrlBuilder url, BindingSession session) {
        return invoke(url, "DELETE", null, null, null, session, null, null);
    }

    protected Response invoke(UrlBuilder url, String method, String contentType, Map<String, String> headers,
            final Output writer, final BindingSession session, BigInteger offset, BigInteger length) {
        int respCode = -1;

        try {
            // log before connect
            if (LOG.isDebugEnabled()) {
                LOG.debug("Session {}: {} {}", session.getSessionId(), method, url);
            }

            // get HTTP client object from session
            CloseableHttpClient httpclient = (CloseableHttpClient) session.get(HTTP_CLIENT);
            if (httpclient == null) {
                session.writeLock();
                try {
                    httpclient = (CloseableHttpClient) session.get(HTTP_CLIENT);
                    if (httpclient == null) {
                        httpclient = createHttpClient(url, session);
                        session.put(HTTP_CLIENT, httpclient, true);
                    }
                } finally {
                    session.writeUnlock();
                }
            }

            HttpUriRequestBase request;

            if ("GET".equals(method)) {
                request = new HttpGet(url.toString());
            } else if ("POST".equals(method)) {
                request = new HttpPost(url.toString());
            } else if ("PUT".equals(method)) {
                request = new HttpPut(url.toString());
            } else if ("DELETE".equals(method)) {
                request = new HttpDelete(url.toString());
            } else {
                throw new CmisRuntimeException("Invalid HTTP method!");
            }

            request.setConfig(createRequestConfig(session));

            // set content type
            if (contentType != null) {
                request.setHeader("Content-Type", contentType);
            }
            // set other headers
            if (headers != null) {
                for (Map.Entry<String, String> header : headers.entrySet()) {
                    request.addHeader(header.getKey(), header.getValue());
                }
            }

            // authenticate
            AuthenticationProvider authProvider = CmisBindingsHelper.getAuthenticationProvider(session);
            if (authProvider != null) {
                Map<String, List<String>> httpHeaders = authProvider.getHTTPHeaders(url.toString());
                if (httpHeaders != null) {
                    for (Map.Entry<String, List<String>> header : httpHeaders.entrySet()) {
                        if (header.getKey() != null && isNotEmpty(header.getValue())) {
                            String key = header.getKey();
                            if (key.equalsIgnoreCase("user-agent")) {
                                request.setHeader("User-Agent", header.getValue().get(0));
                            } else {
                                for (String value : header.getValue()) {
                                    if (value != null) {
                                        request.addHeader(key, value);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // range
            if ((offset != null) || (length != null)) {
                StringBuilder sb = new StringBuilder("bytes=");

                if ((offset == null) || (offset.signum() == -1)) {
                    offset = BigInteger.ZERO;
                }

                sb.append(offset.toString());
                sb.append('-');

                if ((length != null) && (length.signum() == 1)) {
                    sb.append(offset.add(length.subtract(BigInteger.ONE)).toString());
                }

                request.setHeader("Range", sb.toString());
            }

            // compression
            Object compression = session.get(SessionParameter.COMPRESSION);
            if ((compression != null) && Boolean.parseBoolean(compression.toString())) {
                request.setHeader("Accept-Encoding", "gzip,deflate");
            }

            // locale
            if (session.get(CmisBindingsHelper.ACCEPT_LANGUAGE) instanceof String) {
                request.setHeader("Accept-Language", session.get(CmisBindingsHelper.ACCEPT_LANGUAGE).toString());
            }

            // send data
            if (writer != null) {
                Object clientCompression = session.get(SessionParameter.CLIENT_COMPRESSION);
                final boolean clientCompressionFlag = (clientCompression != null)
                        && Boolean.parseBoolean(clientCompression.toString());
                if (clientCompressionFlag) {
                    request.setHeader("Content-Encoding", "gzip");
                }

                ContentType entityContentType = null;
                if (contentType != null) {
                    try {
                        entityContentType = ContentType.parseLenient(contentType);
                    } catch (Exception e) {
                        entityContentType = null;
                    }
                }

                // Buffer the request body so HC5 can send a known Content-Length.
                // Chunked streaming entities have deadlocked against embedded Tomcat
                // in FIT (Create Document). TCK payloads are small enough to buffer.
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                OutputStream out = bos;
                GZIPOutputStream gzip = null;
                if (clientCompressionFlag) {
                    gzip = new GZIPOutputStream(bos, 4096);
                    out = gzip;
                }
                out = new BufferedOutputStream(out, 64 * 1024);
                try {
                    writer.write(out);
                    out.flush();
                    if (gzip != null) {
                        gzip.finish();
                    }
                } catch (IOException ioe) {
                    throw ioe;
                } catch (Exception e) {
                    throw new IOException(e);
                }
                request.setEntity(new ByteArrayEntity(bos.toByteArray(), entityContentType));
            }

            // connect
            final CloseableHttpResponse response = httpclient.execute(request);
            HttpEntity entity = response.getEntity();

            // get stream, if present
            respCode = response.getCode();
            InputStream inputStream = null;
            InputStream errorStream = null;

            if (respCode == 200 || respCode == 201 || respCode == 203 || respCode == 206) {
                if (entity != null) {
                    inputStream = wrapWithResponseClose(entity.getContent(), response);
                } else {
                    response.close();
                    inputStream = new ByteArrayInputStream(new byte[0]);
                }
            } else {
                if (entity != null) {
                    errorStream = wrapWithResponseClose(entity.getContent(), response);
                } else {
                    response.close();
                    errorStream = new ByteArrayInputStream(new byte[0]);
                }
            }

            // collect headers
            Map<String, List<String>> responseHeaders = new HashMap<String, List<String>>();
            for (Header header : response.getHeaders()) {
                List<String> values = responseHeaders.get(header.getName());
                if (values == null) {
                    values = new ArrayList<String>();
                    responseHeaders.put(header.getName(), values);
                }
                values.add(header.getValue());
            }

            // log after connect
            if (LOG.isTraceEnabled()) {
                LOG.trace("Session {}: {} {} > Headers: {}", session.getSessionId(), method, url,
                        responseHeaders.toString());
            }

            // forward response HTTP headers
            if (authProvider != null) {
                authProvider.putResponseHeaders(url.toString(), respCode, responseHeaders);
            }

            // get the response
            return new Response(respCode, response.getReasonPhrase(), responseHeaders, inputStream, errorStream);
        } catch (Exception e) {
            throw new CmisConnectionException(url.toString(), respCode, e);
        }
    }

    /**
     * Creates default request configuration for the Apache HTTP Client 5.
     */
    protected RequestConfig createRequestConfig(BindingSession session) {
        // Expect: 100-continue + chunked streaming entities can deadlock against
        // some servlet containers (including embedded Tomcat used by FIT).
        RequestConfig.Builder builder = RequestConfig.custom().setCookieSpec(StandardCookieSpec.IGNORE)
                .setExpectContinueEnabled(false);

        int connectTimeout = session.get(SessionParameter.CONNECT_TIMEOUT, -1);
        if (connectTimeout >= 0) {
            builder.setConnectionRequestTimeout(Timeout.ofMilliseconds(connectTimeout));
            builder.setConnectTimeout(Timeout.ofMilliseconds(connectTimeout));
        }

        int readTimeout = session.get(SessionParameter.READ_TIMEOUT, -1);
        if (readTimeout >= 0) {
            builder.setResponseTimeout(Timeout.ofMilliseconds(readTimeout));
        }

        return builder.build();
    }

    /**
     * Wraps an entity stream so closing it also closes the HTTP response.
     */
    protected InputStream wrapWithResponseClose(InputStream stream, final CloseableHttpResponse response) {
        return new FilterInputStream(stream) {
            @Override
            public void close() throws IOException {
                try {
                    super.close();
                } finally {
                    response.close();
                }
            }
        };
    }

    /**
     * Returns the User-Agent string for the given session.
     */
    protected String getUserAgent(BindingSession session) {
        return (String) session.get(SessionParameter.USER_AGENT, ClientVersion.OPENCMIS_USER_AGENT);
    }

    /**
     * Verifies a hostname with the given verifier.
     */
    protected void verify(HostnameVerifier verifier, String host, SSLSocket sslSocket) throws IOException {
        try {
            if (!verifier.verify(host, sslSocket.getSession())) {
                throw new SSLException("Hostname in certificate didn't match: <" + host + ">");
            }
        } catch (IOException ioe) {
            closeSocket(sslSocket);
            throw ioe;
        }
    }

    /**
     * Closes the given socket and ignores exceptions.
     */
    protected void closeSocket(Socket socket) {
        try {
            socket.close();
        } catch (IOException ioe) {
            // ignore
        }
    }

    /**
     * Creates the {@link CloseableHttpClient} instance.
     */
    protected abstract CloseableHttpClient createHttpClient(UrlBuilder url, BindingSession session);
}
