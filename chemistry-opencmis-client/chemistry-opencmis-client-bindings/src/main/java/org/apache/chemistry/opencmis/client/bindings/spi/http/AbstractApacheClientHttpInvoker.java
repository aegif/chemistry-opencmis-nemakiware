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
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.Socket;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
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
import org.apache.chemistry.opencmis.commons.impl.IOUtils;
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
import org.apache.hc.core5.http.io.entity.AbstractHttpEntity;
import org.apache.hc.core5.http.io.entity.ByteArrayEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.FileEntity;
import org.apache.hc.core5.http.io.entity.InputStreamEntity;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link HttpInvoker} backed by Apache HttpClient 5.
 * <p>
 * Default request-body mode is {@code auto} (stream to the wire). Session
 * parameters under {@code org.apache.chemistry.opencmis.binding.http.*}
 * configure pooling, response buffering, optional {@code materialize} spool
 * limits, and whether redirects are followed (default {@code false}).
 */
public abstract class AbstractApacheClientHttpInvoker implements HttpInvoker {

    protected static final Logger LOG = LoggerFactory.getLogger(AbstractApacheClientHttpInvoker.class);

    public static final String HTTP_CLIENT = "org.apache.chemistry.opencmis.client.bindings.spi.http.ApacheClientHttpInvoker.httpClient";
    protected static final int BUFFER_SIZE = 2 * 1024 * 1024;

    /**
     * Default max bytes to buffer eagerly so pooled connections are released
     * immediately. Kept small so concurrent leases cannot dominate the heap.
     */
    public static final int DEFAULT_RESPONSE_BUFFER_LIMIT = 1024 * 1024;

    /**
     * Default max request bytes kept in heap before spilling to a temp file
     * when {@code materialize} mode is enabled. Preserves a known
     * Content-Length without loading multi-GB uploads entirely into memory.
     */
    public static final int DEFAULT_REQUEST_MEMORY_LIMIT = 1024 * 1024;

    /** Default max size of a single spilled request body on disk. */
    public static final long DEFAULT_REQUEST_SPOOL_MAX_SIZE = 5L * 1024 * 1024 * 1024;

    /** Default max concurrent request-body materializations classloader-wide. */
    public static final int DEFAULT_REQUEST_SPOOL_MAX_CONCURRENT = 32;

    /** Default max total bytes across all active materialized bodies classloader-wide. */
    public static final long DEFAULT_REQUEST_SPOOL_MAX_TOTAL_BYTES = 20L * 1024 * 1024 * 1024;

    /** Default wait for a free pooled connection (never block forever under load). */
    public static final int DEFAULT_CONNECTION_REQUEST_TIMEOUT_MS = 60_000;

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
        CloseableHttpResponse response = null;
        InputStream inputStream = null;
        InputStream errorStream = null;
        File requestTempFile = null;
        RequestSpoolLimiter.SpoolLease requestSpoolLease = null;
        HttpEntity requestEntity = null;

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
                throw new CmisRuntimeException("Unsupported HTTP method: " + method);
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

                PreparedRequestBody prepared = prepareRequestBody(writer, clientCompressionFlag, entityContentType,
                        session);
                requestTempFile = prepared.tempFile;
                requestSpoolLease = prepared.spoolLease;
                // Allow other bodies to materialize while this request is on the wire.
                if (requestSpoolLease != null) {
                    requestSpoolLease.releasePermitOnly();
                }
                requestEntity = prepared.entity;
                request.setEntity(requestEntity);
            }

            // connect
            response = httpclient.execute(request);
            // Request entity was written (or closed by HC5 on failure); drop local ref so
            // finally does not double-close a stream HC5 may still touch during response
            // handling. Streaming InputStreamEntity is closed by HC5 after send.
            requestEntity = null;
            HttpEntity entity = response.getEntity();

            // get stream, if present
            respCode = response.getCode();

            // Collect headers / status text before materializeEntity may close the response.
            final String reasonPhrase = response.getReasonPhrase();
            Map<String, List<String>> responseHeaders = new HashMap<String, List<String>>();
            for (Header header : response.getHeaders()) {
                List<String> values = responseHeaders.get(header.getName());
                if (values == null) {
                    values = new ArrayList<String>();
                    responseHeaders.put(header.getName(), values);
                }
                values.add(header.getValue());
            }

            if (respCode == 200 || respCode == 201 || respCode == 203 || respCode == 206) {
                inputStream = materializeEntity(entity, response, session);
            } else {
                errorStream = materializeEntity(entity, response, session);
            }
            // Ownership of the HTTP response is now with the returned streams
            // (or already closed inside materializeEntity).
            response = null;

            // log after connect
            if (LOG.isTraceEnabled()) {
                LOG.trace("Session {}: {} {} > Headers: {}", session.getSessionId(), method, url,
                        responseHeaders.toString());
            }

            // forward response HTTP headers
            if (authProvider != null) {
                authProvider.putResponseHeaders(url.toString(), respCode, responseHeaders);
            }

            Response result = new Response(respCode, reasonPhrase, responseHeaders, inputStream, errorStream);
            // Response owns the streams now.
            inputStream = null;
            errorStream = null;
            return result;
        } catch (Exception e) {
            throw new CmisConnectionException(url.toString(), respCode, e);
        } finally {
            if (requestEntity != null) {
                // execute() never ran or failed before HC5 took ownership — close so
                // StreamableOutput / InputStreamEntity streams do not leak.
                try {
                    requestEntity.close();
                } catch (Exception closeEx) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Closing request entity failed: {}", closeEx.toString());
                    }
                }
                requestEntity = null;
            }
            if (response != null) {
                try {
                    response.close();
                } catch (IOException ioe) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Closing HTTP response failed: {}", ioe.toString());
                    }
                }
            }
            IOUtils.closeQuietly(inputStream);
            IOUtils.closeQuietly(errorStream);
            boolean tempDeleted = true;
            if (requestTempFile != null && requestTempFile.exists()) {
                tempDeleted = deleteTempFileWithRetries(requestTempFile);
                if (!tempDeleted) {
                    LOG.warn(
                            "Failed to delete HTTP request temp file {}; tracking it so the materialization byte budget can be reclaimed later",
                            requestTempFile.getAbsolutePath());
                }
            }
            if (requestSpoolLease != null) {
                if (tempDeleted) {
                    requestSpoolLease.releaseBytes();
                } else {
                    requestSpoolLease.abandonBytesAsOrphan(requestTempFile);
                }
            }
        }
    }

    /**
     * Tries a few immediate deletes, then {@code deleteOnExit} as a last resort.
     * Persistent failures are tracked by {@link RequestSpoolLimiter} for reclaim.
     */
    private static boolean deleteTempFileWithRetries(File file) {
        if (file == null) {
            return true;
        }
        if (FORCE_TEMP_DELETE_FAILURE_FOR_TESTS.get()) {
            file.deleteOnExit();
            return false;
        }
        if (!file.exists()) {
            return true;
        }
        for (int attempt = 0; attempt < 3; attempt++) {
            if (file.delete() || !file.exists()) {
                return true;
            }
            try {
                Thread.sleep(10L * (attempt + 1));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        file.deleteOnExit();
        return !file.exists();
    }

    /**
     * Builds a request entity. {@code auto} and {@code stream} write once to
     * the wire (like {@link DefaultHttpInvoker}); {@code materialize} spools
     * to memory/disk under classloader-wide limits for a known Content-Length
     * and a repeatable entity.
     */
    protected PreparedRequestBody prepareRequestBody(final Output writer, final boolean gzip,
            final ContentType entityContentType, BindingSession session) throws IOException {
        if (shouldStreamRequestBody(writer, gzip, session)) {
            return prepareStreamingRequestBody(writer, gzip, entityContentType);
        }
        RequestBodySpool spool = spoolRequestBody(writer, gzip, session);
        try {
            HttpEntity entity = spool.toEntity(entityContentType);
            File tempFile = spool.getTempFile();
            RequestSpoolLimiter.SpoolLease lease = spool.transferLease();
            return new PreparedRequestBody(entity, tempFile, lease);
        } catch (RuntimeException ex) {
            spool.discard();
            throw ex;
        } catch (Error err) {
            spool.discard();
            throw err;
        }
    }

    /**
     * {@code true} when the body should be written once to the socket instead of
     * being fully materialized first. {@code auto} and {@code stream} both
     * stream (including on-the-fly gzip); only {@code materialize} buffers.
     */
    protected boolean shouldStreamRequestBody(Output writer, boolean gzip, BindingSession session) {
        return !SessionParameter.HTTP_REQUEST_BODY_MODE_MATERIALIZE.equals(resolveRequestBodyMode(session));
    }

    protected static String resolveRequestBodyMode(BindingSession session) {
        Object raw = session.get(SessionParameter.HTTP_REQUEST_BODY_MODE);
        if (raw == null) {
            return SessionParameter.HTTP_REQUEST_BODY_MODE_AUTO;
        }
        String mode = raw.toString().trim().toLowerCase();
        if (SessionParameter.HTTP_REQUEST_BODY_MODE_STREAM.equals(mode)
                || SessionParameter.HTTP_REQUEST_BODY_MODE_MATERIALIZE.equals(mode)
                || SessionParameter.HTTP_REQUEST_BODY_MODE_AUTO.equals(mode)) {
            return mode;
        }
        LOG.warn("Unknown {} value '{}'; expected auto|stream|materialize — using auto",
                SessionParameter.HTTP_REQUEST_BODY_MODE, mode);
        return SessionParameter.HTTP_REQUEST_BODY_MODE_AUTO;
    }

    /**
     * Streams the body without spooling. Prefer a known-length
     * {@link StreamableOutput} ({@code Content-Length}); otherwise use chunked
     * {@code writeTo} like {@link DefaultHttpInvoker}.
     */
    protected PreparedRequestBody prepareStreamingRequestBody(final Output writer, final boolean gzip,
            final ContentType entityContentType) throws IOException {
        if (!gzip && writer instanceof StreamableOutput) {
            StreamableOutput streamable = (StreamableOutput) writer;
            long length = streamable.getContentLength();
            if (length >= 0L) {
                InputStream in = streamable.openStream();
                if (in == null) {
                    throw new IOException(
                            "StreamableOutput.openStream() returned null for declared content length " + length);
                }
                // Fail fast if the stream ends before the declared length instead of
                // sending a short body with a larger Content-Length (server hang risk).
                HttpEntity entity = new InputStreamEntity(new LengthCheckedInputStream(in, length), length,
                        entityContentType);
                return new PreparedRequestBody(entity, null, null);
            }
        }
        HttpEntity entity = new StreamingOutputEntity(writer, entityContentType, gzip);
        return new PreparedRequestBody(entity, null, null);
    }

    /**
     * Non-repeatable entity that invokes {@link Output#write(OutputStream)}
     * directly against the HTTP connection (chunked; Content-Length unknown).
     */
    protected static final class StreamingOutputEntity extends AbstractHttpEntity {
        private final Output writer;
        private final boolean gzip;

        StreamingOutputEntity(Output writer, ContentType contentType, boolean gzip) {
            // chunked=true: length unknown (especially after on-the-fly gzip)
            super(contentType, gzip ? "gzip" : null, true);
            this.writer = writer;
            this.gzip = gzip;
        }

        @Override
        public long getContentLength() {
            return -1L;
        }

        @Override
        public InputStream getContent() {
            throw new UnsupportedOperationException("Streaming output entity cannot be read as InputStream");
        }

        @Override
        public boolean isStreaming() {
            return true;
        }

        @Override
        public boolean isRepeatable() {
            return false;
        }

        @Override
        public void writeTo(OutputStream outStream) throws IOException {
            // Do not close outStream — HC5 owns the connection stream.
            OutputStream sink = outStream;
            GZIPOutputStream gzipStream = null;
            BufferedOutputStream buffered = null;
            try {
                if (gzip) {
                    gzipStream = new GZIPOutputStream(sink, 4096);
                    sink = gzipStream;
                }
                buffered = new BufferedOutputStream(sink, 64 * 1024);
                writer.write(buffered);
                buffered.flush();
                if (gzipStream != null) {
                    gzipStream.finish();
                }
            } catch (IOException ioe) {
                throw ioe;
            } catch (Exception e) {
                throw new IOException(e);
            }
        }

        @Override
        public void close() {
            // Connection stream is owned by HC5; nothing else to close.
        }
    }

    /**
     * Spools the request body to memory up to the configured limit, then to a
     * temporary file. Always produces a known Content-Length entity.
     */
    protected RequestBodySpool spoolRequestBody(Output writer, boolean gzip, BindingSession session)
            throws IOException {
        int memoryLimit = (int) Math.min(Integer.MAX_VALUE,
                getLong(session, SessionParameter.HTTP_REQUEST_MEMORY_LIMIT, DEFAULT_REQUEST_MEMORY_LIMIT));
        if (memoryLimit < 0) {
            memoryLimit = DEFAULT_REQUEST_MEMORY_LIMIT;
        }
        long maxSpoolSize = getLong(session, SessionParameter.HTTP_REQUEST_SPOOL_MAX_SIZE,
                DEFAULT_REQUEST_SPOOL_MAX_SIZE);
        if (maxSpoolSize <= 0) {
            maxSpoolSize = DEFAULT_REQUEST_SPOOL_MAX_SIZE;
        }
        long maxTotalBytes = getLong(session, SessionParameter.HTTP_REQUEST_SPOOL_MAX_TOTAL_BYTES,
                DEFAULT_REQUEST_SPOOL_MAX_TOTAL_BYTES);
        if (maxTotalBytes <= 0) {
            maxTotalBytes = DEFAULT_REQUEST_SPOOL_MAX_TOTAL_BYTES;
        }
        int maxConcurrent = (int) getLong(session, SessionParameter.HTTP_REQUEST_SPOOL_MAX_CONCURRENT,
                DEFAULT_REQUEST_SPOOL_MAX_CONCURRENT);
        if (maxConcurrent <= 0) {
            maxConcurrent = DEFAULT_REQUEST_SPOOL_MAX_CONCURRENT;
        }
        File tempDir = resolveTempDir(session);
        long acquireTimeout = resolveSpoolAcquireTimeout(session);

        RequestBodySpool spool = new RequestBodySpool(memoryLimit, maxSpoolSize, maxTotalBytes, maxConcurrent, tempDir,
                acquireTimeout);
        OutputStream out = spool;
        GZIPOutputStream gzipStream = null;
        try {
            spool.begin();
            if (gzip) {
                gzipStream = new GZIPOutputStream(out, 4096);
                out = gzipStream;
            }
            out = new BufferedOutputStream(out, 64 * 1024);
            try {
                writer.write(out);
                out.flush();
                if (gzipStream != null) {
                    gzipStream.finish();
                }
            } catch (IOException ioe) {
                throw ioe;
            } catch (Exception e) {
                throw new IOException(e);
            }
            spool.finish();
            return spool;
        } catch (IOException ioe) {
            spool.discard();
            throw ioe;
        } catch (RuntimeException re) {
            spool.discard();
            throw re;
        } catch (Error err) {
            // e.g. OutOfMemoryError while buffering: the lease must not leak
            // because limits are classloader-wide and never recover otherwise.
            spool.discard();
            throw err;
        }
    }

    /**
     * Same normalization as connection-lease wait: never block forever.
     */
    protected static long resolveSpoolAcquireTimeout(BindingSession session) {
        long acquireTimeout = getLong(session, SessionParameter.HTTP_CONNECTION_REQUEST_TIMEOUT,
                DEFAULT_CONNECTION_REQUEST_TIMEOUT_MS);
        if (acquireTimeout < 0) {
            long connectTimeout = getLong(session, SessionParameter.CONNECT_TIMEOUT, -1);
            acquireTimeout = connectTimeout >= 0 ? connectTimeout : DEFAULT_CONNECTION_REQUEST_TIMEOUT_MS;
        }
        return acquireTimeout;
    }

    protected static long getLong(BindingSession session, String key, long defaultValue) {
        Object value = session.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(value.toString().trim());
        } catch (NumberFormatException nfe) {
            return defaultValue;
        }
    }

    protected static File resolveTempDir(BindingSession session) throws IOException {
        Object configured = session.get(SessionParameter.HTTP_TEMP_DIR);
        if (configured == null || configured.toString().trim().isEmpty()) {
            return null;
        }
        File dir = new File(configured.toString().trim());
        if (!dir.isDirectory() && !dir.mkdirs()) {
            throw new CmisConnectionException(
                    "Cannot create HTTP request-body temp directory (check "
                            + SessionParameter.HTTP_TEMP_DIR + "): " + dir.getAbsolutePath());
        }
        return dir;
    }

    /**
     * Returns an input stream for the entity and releases the pooled connection
     * as soon as practical. Bodies up to {@link SessionParameter#HTTP_RESPONSE_BUFFER_LIMIT}
     * are copied into memory so callers cannot leak leases via partial reads.
     * Larger bodies stream and close the HTTP response when the stream is closed.
     */
    protected InputStream materializeEntity(HttpEntity entity, CloseableHttpResponse response, BindingSession session)
            throws IOException {
        if (entity == null) {
            response.close();
            return new ByteArrayInputStream(new byte[0]);
        }

        long contentLength = entity.getContentLength();
        int bufferLimit = session.get(SessionParameter.HTTP_RESPONSE_BUFFER_LIMIT, DEFAULT_RESPONSE_BUFFER_LIMIT);
        if (bufferLimit < 0) {
            bufferLimit = DEFAULT_RESPONSE_BUFFER_LIMIT;
        }

        // Large known bodies: stream to avoid huge heap spikes (caller must close).
        if (contentLength > bufferLimit) {
            return wrapWithResponseClose(entity.getContent(), response);
        }

        // Unknown length: stream (chunked large downloads). Small chunked AtomPub /
        // Browser payloads are still closed by parsers via wrapWithResponseClose.
        if (contentLength < 0) {
            return wrapWithResponseClose(entity.getContent(), response);
        }

        try {
            byte[] data = contentLength == 0 ? new byte[0] : EntityUtils.toByteArray(entity);
            return new ByteArrayInputStream(data);
        } finally {
            response.close();
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

        // Default off: avoid following redirects to unexpected hosts (SSRF-ish).
        boolean followRedirects = session.get(SessionParameter.HTTP_FOLLOW_REDIRECTS, false);
        builder.setRedirectsEnabled(followRedirects);
        if (followRedirects) {
            builder.setMaxRedirects(5);
            builder.setCircularRedirectsAllowed(false);
        }

        int connectTimeout = session.get(SessionParameter.CONNECT_TIMEOUT, -1);
        if (connectTimeout >= 0) {
            builder.setConnectTimeout(Timeout.ofMilliseconds(connectTimeout));
        }

        // Never wait forever for a pooled connection under load.
        int connectionRequestTimeout = session.get(SessionParameter.HTTP_CONNECTION_REQUEST_TIMEOUT, -1);
        if (connectionRequestTimeout < 0) {
            connectionRequestTimeout = connectTimeout >= 0 ? connectTimeout : DEFAULT_CONNECTION_REQUEST_TIMEOUT_MS;
        }
        builder.setConnectionRequestTimeout(Timeout.ofMilliseconds(connectionRequestTimeout));

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
            private boolean closed;

            @Override
            public void close() throws IOException {
                if (closed) {
                    return;
                }
                closed = true;
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

    /**
     * Result of {@link #prepareRequestBody}.
     */
    protected static final class PreparedRequestBody {
        final HttpEntity entity;
        final File tempFile;
        final RequestSpoolLimiter.SpoolLease spoolLease;

        PreparedRequestBody(HttpEntity entity, File tempFile, RequestSpoolLimiter.SpoolLease spoolLease) {
            this.entity = entity;
            this.tempFile = tempFile;
            this.spoolLease = spoolLease;
        }
    }

    /**
     * Threshold spool for request bodies: memory first, then a temp file.
     * Acquires a materialization lease before any bytes are buffered.
     */
    protected static final class RequestBodySpool extends OutputStream {
        private final int memoryLimit;
        private final long maxSpoolSize;
        private final long maxTotalBytes;
        private final int maxConcurrent;
        private final File tempDir;
        private final long acquireTimeoutMs;
        private TransferByteArrayOutputStream memory;
        private File tempFile;
        private OutputStream fileOut;
        private long size;
        private RequestSpoolLimiter.SpoolLease lease;
        private boolean leaseTransferred;
        private boolean finished;

        RequestBodySpool(int memoryLimit, long maxSpoolSize, long maxTotalBytes, int maxConcurrent, File tempDir,
                long acquireTimeoutMs) {
            this.memoryLimit = memoryLimit;
            this.maxSpoolSize = maxSpoolSize;
            this.maxTotalBytes = maxTotalBytes;
            this.maxConcurrent = maxConcurrent;
            this.tempDir = tempDir;
            this.acquireTimeoutMs = acquireTimeoutMs;
            this.memory = new TransferByteArrayOutputStream(Math.min(64 * 1024, Math.max(256, memoryLimit)));
        }

        /** Takes the classloader-wide materialization permit before buffering. */
        void begin() {
            lease = RequestSpoolLimiter.getInstance().acquire(maxConcurrent, maxTotalBytes, acquireTimeoutMs);
        }

        @Override
        public void write(int b) throws IOException {
            write(new byte[] { (byte) b }, 0, 1);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            if (len <= 0) {
                return;
            }
            if (lease == null) {
                throw new IOException("Request body materialization lease was not acquired");
            }
            ensureSpoolBudget(len);
            ensureCapacity(len);
            lease.addBytes(len);
            if (fileOut != null) {
                fileOut.write(b, off, len);
            } else {
                memory.write(b, off, len);
            }
            size += len;
        }

        private void ensureCapacity(int incoming) throws IOException {
            if (fileOut != null || memory == null) {
                return;
            }
            if ((long) memory.size() + incoming <= memoryLimit) {
                return;
            }

            try {
                if (tempDir != null) {
                    tempFile = Files.createTempFile(tempDir.toPath(), "opencmis-http-body-", ".bin").toFile();
                } else {
                    tempFile = Files.createTempFile("opencmis-http-body-", ".bin").toFile();
                }
                // deleteOnExit only on failed delete — not on create (avoids JVM heap growth).
                fileOut = new BufferedOutputStream(new FileOutputStream(tempFile), 64 * 1024);
                // Memory bytes were already charged via addBytes while writing.
                memory.writeTo(fileOut);
                fileOut.flush();
                if (FORCE_SPILL_FAILURE_FOR_TESTS.get()) {
                    throw new IOException("forced spill failure for tests");
                }
                memory = null;
            } catch (IOException | RuntimeException e) {
                IOUtils.closeQuietly(fileOut);
                fileOut = null;
                if (tempFile != null) {
                    if (!deleteTempFile()) {
                        // Keep tempFile so discard() can treat the orphan as undeleted.
                    } else {
                        tempFile = null;
                    }
                }
                throw e instanceof IOException ? (IOException) e : new IOException(e);
            }
        }

        private void ensureSpoolBudget(long additional) {
            if (maxSpoolSize > 0 && size + additional > maxSpoolSize) {
                throw new CmisConnectionException(
                        "HTTP request body materialization size exceeded (max " + maxSpoolSize
                                + " bytes; raise " + SessionParameter.HTTP_REQUEST_SPOOL_MAX_SIZE
                                + " or use request body mode auto/stream)");
            }
        }

        void finish() throws IOException {
            if (finished) {
                return;
            }
            finished = true;
            if (fileOut != null) {
                fileOut.flush();
                fileOut.close();
                fileOut = null;
            }
        }

        void discard() {
            IOUtils.closeQuietly(fileOut);
            fileOut = null;
            memory = null;
            boolean deleted = true;
            if (tempFile != null) {
                deleted = deleteTempFile();
                if (deleted) {
                    tempFile = null;
                }
            }
            if (lease != null && !leaseTransferred) {
                if (deleted) {
                    lease.release();
                } else {
                    lease.releasePermitKeepBytes(tempFile);
                }
                lease = null;
            }
        }

        /**
         * @return {@code true} if the temp file is gone (or was never present)
         */
        private boolean deleteTempFile() {
            return deleteTempFileWithRetries(tempFile);
        }

        File getTempFile() {
            return tempFile;
        }

        HttpEntity toEntity(ContentType contentType) {
            if (tempFile != null) {
                return new FileEntity(tempFile, contentType);
            }
            if (memory == null) {
                return new ByteArrayEntity(new byte[0], contentType);
            }
            TransferByteArrayOutputStream buffered = memory;
            memory = null;
            // Hand the internal buffer to the entity without copying.
            return new ByteArrayEntity(buffered.rawBuffer(), 0, buffered.size(), contentType);
        }

        long getSize() {
            return size;
        }

        RequestSpoolLimiter.SpoolLease transferLease() {
            if (lease != null && !leaseTransferred) {
                leaseTransferred = true;
                return lease;
            }
            return null;
        }
    }

    /** Test-only: force spill-to-disk to fail after the temp file is created. */
    static final AtomicBoolean FORCE_SPILL_FAILURE_FOR_TESTS = new AtomicBoolean();

    /** Test-only: force temp-file delete to fail. */
    static final AtomicBoolean FORCE_TEMP_DELETE_FAILURE_FOR_TESTS = new AtomicBoolean();

    /**
     * InputStream that fails when EOF arrives before {@code expectedLength}
     * bytes, and stops after that many bytes if the underlying stream is longer.
     */
    static final class LengthCheckedInputStream extends FilterInputStream {
        private final long expectedLength;
        private long remaining;

        LengthCheckedInputStream(InputStream in, long expectedLength) {
            super(in);
            this.expectedLength = expectedLength;
            this.remaining = expectedLength;
        }

        @Override
        public int read() throws IOException {
            if (remaining <= 0L) {
                return -1;
            }
            int b = super.read();
            if (b < 0) {
                throw shortStream();
            }
            remaining--;
            return b;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (remaining <= 0L) {
                return -1;
            }
            int toRead = (int) Math.min(len, remaining);
            int n = super.read(b, off, toRead);
            if (n < 0) {
                throw shortStream();
            }
            remaining -= n;
            return n;
        }

        @Override
        public long skip(long n) throws IOException {
            if (n <= 0L || remaining <= 0L) {
                return 0L;
            }
            long skipped = super.skip(Math.min(n, remaining));
            remaining -= skipped;
            return skipped;
        }

        private IOException shortStream() {
            return new IOException("Content stream ended after " + (expectedLength - remaining)
                    + " bytes but ContentStream.getLength()/StreamableOutput.getContentLength() declared "
                    + expectedLength + " bytes");
        }
    }

    /**
     * ByteArrayOutputStream that exposes its internal buffer so the request
     * entity can reference it directly (with {@link #size()} as the length)
     * instead of paying the {@code toByteArray()} copy.
     */
    private static final class TransferByteArrayOutputStream extends ByteArrayOutputStream {
        TransferByteArrayOutputStream(int size) {
            super(size);
        }

        /** Caller must stop writing once the buffer has been handed off. */
        synchronized byte[] rawBuffer() {
            return buf;
        }
    }
}
