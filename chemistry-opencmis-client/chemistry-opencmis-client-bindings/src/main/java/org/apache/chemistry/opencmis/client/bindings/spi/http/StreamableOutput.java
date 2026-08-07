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

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.impl.IOUtils;

/**
 * {@link Output} that can be sent without first spooling the entire body.
 * <p>
 * When {@link #getContentLength()} is non-negative and client compression is
 * off, Apache HttpClient 5 uses an {@code InputStreamEntity} with
 * {@code Content-Length}. The stream must deliver exactly that many bytes
 * (shorter streams fail the request; longer streams are truncated). Otherwise
 * the invoker falls back to {@link #write(OutputStream)} (chunked, or gzipped
 * on the fly).
 */
public interface StreamableOutput extends Output {

    /**
     * Exact body length in bytes, or {@code -1} if unknown.
     */
    long getContentLength();

    /**
     * Opens the body stream for send. Invoked on the known-length streaming
     * path. Implementations built by {@link #fromContentStream} from a
     * mark-supported stream (e.g. {@link java.io.ByteArrayInputStream}) may be
     * opened more than once; other streams are one-shot and are closed by the
     * HTTP stack after send.
     */
    InputStream openStream() throws IOException;

    /**
     * Builds a {@link StreamableOutput} from a {@link ContentStream} when
     * {@link ContentStream#getLength()} is non-negative; otherwise returns a
     * plain copy {@link Output} (chunked {@code writeTo}).
     * <p>
     * When the underlying stream supports mark/reset (typical for
     * {@code ByteArrayInputStream} used by tests and many clients), the returned
     * output is reusable across multiple HTTP sends — matching historical
     * OpenCMIS behaviour where the same {@link ContentStream} instance was
     * passed to successive {@code setContentStream} calls.
     */
    static Output fromContentStream(final ContentStream contentStream) {
        if (contentStream == null || contentStream.getStream() == null) {
            throw new IllegalArgumentException("ContentStream and its InputStream must be set");
        }
        final InputStream stream = contentStream.getStream();
        final long length = contentStream.getLength();
        if (length < 0L) {
            return new Output() {
                @Override
                public void write(OutputStream out) throws IOException {
                    IOUtils.copy(stream, out);
                }
            };
        }

        final boolean reusable = stream.markSupported();
        if (reusable) {
            // Clients (and TCK ChangeTokenTest) often call setContentStream twice with
            // the same ContentStream. The first send leaves a ByteArrayInputStream at
            // EOF; reset to the prior mark (0 for BAIS) before re-marking.
            try {
                stream.reset();
            } catch (IOException ignored) {
                // No prior mark — continue from the current position.
            }
            // Read limit covers the declared body; ByteArrayInputStream ignores it.
            int readLimit = length > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) Math.max(length, 0L);
            stream.mark(readLimit);
        }

        return new StreamableOutput() {
            @Override
            public long getContentLength() {
                return length;
            }

            @Override
            public InputStream openStream() throws IOException {
                if (reusable) {
                    stream.reset();
                    // HC5 closes the entity stream after send; keep the source open.
                    return new NonClosingInputStream(stream);
                }
                return stream;
            }

            @Override
            public void write(OutputStream out) throws IOException {
                if (reusable) {
                    stream.reset();
                }
                IOUtils.copy(stream, out);
            }
        };
    }

    /**
     * Delegates reads but ignores {@link #close()} so a mark-supported source
     * can be reset and sent again.
     */
    final class NonClosingInputStream extends FilterInputStream {
        NonClosingInputStream(InputStream in) {
            super(in);
        }

        @Override
        public void close() {
            // Intentionally empty.
        }
    }
}
