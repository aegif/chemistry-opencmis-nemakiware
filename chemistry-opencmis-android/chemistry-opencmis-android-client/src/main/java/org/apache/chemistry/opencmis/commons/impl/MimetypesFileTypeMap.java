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
package org.apache.chemistry.opencmis.commons.impl;

import java.io.File;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Android-side fallback for MIME type detection.
 */
public final class MimetypesFileTypeMap {

    private static final String OCTET_STREAM = "application/octet-stream";
    private static final Map<String, String> EXTENSION_MIME_TYPES = new HashMap<String, String>();

    static {
        EXTENSION_MIME_TYPES.put("json", "application/json");
        EXTENSION_MIME_TYPES.put("pdf", "application/pdf");
        EXTENSION_MIME_TYPES.put("xml", "text/xml");
        EXTENSION_MIME_TYPES.put("txt", "text/plain");
        EXTENSION_MIME_TYPES.put("html", "text/html");
        EXTENSION_MIME_TYPES.put("csv", "text/csv");
        EXTENSION_MIME_TYPES.put("md", "text/markdown");
        EXTENSION_MIME_TYPES.put("doc", "application/msword");
        EXTENSION_MIME_TYPES.put("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        EXTENSION_MIME_TYPES.put("xls", "application/vnd.ms-excel");
        EXTENSION_MIME_TYPES.put("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        EXTENSION_MIME_TYPES.put("ppt", "application/vnd.ms-powerpoint");
        EXTENSION_MIME_TYPES.put("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation");
        EXTENSION_MIME_TYPES.put("jpg", "image/jpeg");
        EXTENSION_MIME_TYPES.put("jpeg", "image/jpeg");
        EXTENSION_MIME_TYPES.put("png", "image/png");
        EXTENSION_MIME_TYPES.put("gif", "image/gif");
        EXTENSION_MIME_TYPES.put("svg", "image/svg+xml");
        EXTENSION_MIME_TYPES.put("webp", "image/webp");
        EXTENSION_MIME_TYPES.put("mp3", "audio/mpeg");
        EXTENSION_MIME_TYPES.put("mp4", "video/mp4");
        EXTENSION_MIME_TYPES.put("zip", "application/zip");
    }

    public String getContentType(File file) {
        if (file == null) {
            return OCTET_STREAM;
        }

        return getContentType(file.getName());
    }

    public String getContentType(String filename) {
        if (filename == null) {
            return OCTET_STREAM;
        }

        String detectedType = URLConnection.guessContentTypeFromName(filename);
        if (detectedType != null && !detectedType.trim().isEmpty()) {
            return detectedType;
        }

        int extensionIndex = filename.lastIndexOf('.');
        if (extensionIndex < 0 || extensionIndex + 1 >= filename.length()) {
            return OCTET_STREAM;
        }

        String extension = filename.substring(extensionIndex + 1).toLowerCase(Locale.ENGLISH);
        String mappedType = EXTENSION_MIME_TYPES.get(extension);
        return mappedType != null ? mappedType : OCTET_STREAM;
    }
}
