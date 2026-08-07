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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.impl.IOUtils;

/**
 * {@link Output} that can be sent without first spooling the entire body.
 * <p>
 * When {@link #getContentLength()} is non-negative, Apache HttpClient 5 may use
 * an {@code InputStreamEntity} with a known {@code Content-Length}. Otherwise
 * the invoker falls back to {@link #write(OutputStream)} (possibly chunked).
 */
public interface StreamableOutput extends Output {

    /**
     * Exact body length in bytes, or {@code -1} if unknown.
     */
    long getContentLength();

    /**
     * Opens the body stream for a one-shot send. Invoked only when the streaming
     * path with a known length is selected; the HTTP stack closes the stream.
     */
    InputStream openStream() throws IOException;

    /**
     * Builds a {@link StreamableOutput} from a {@link ContentStream} when the
     * length is known; otherwise returns a plain copy {@link Output}.
     */
    static Output fromContentStream(final ContentStream contentStream) {
        if (contentStream == null || contentStream.getStream() == null) {
            throw new IllegalArgumentException("Content stream must be set");
        }
        final InputStream stream = contentStream.getStream();
        final long length = contentStream.getLength();
        if (length >= 0L) {
            return new StreamableOutput() {
                @Override
                public long getContentLength() {
                    return length;
                }

                @Override
                public InputStream openStream() {
                    return stream;
                }

                @Override
                public void write(OutputStream out) throws IOException {
                    IOUtils.copy(stream, out);
                }
            };
        }
        return new Output() {
            @Override
            public void write(OutputStream out) throws IOException {
                IOUtils.copy(stream, out);
            }
        };
    }
}
