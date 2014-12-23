/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011-2014,, FrostWire(R). All rights reserved.
 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.frostwire.util;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;


/**
 * A pure java based HTTP client with resume capabilities.
 *
 * @author gubatron
 * @author aldenml
 */
public interface HttpClient {

    public void setListener(HttpClientListener listener);

    public HttpClientListener getListener();

    /**
     * Returns the HTTP response code
     */
    public int head(String url, int connectTimeoutInMillis) throws IOException;

    public String get(String url) throws IOException;

    public String get(String url, int timeout) throws IOException;

    public String get(String url, int timeout, String userAgent) throws IOException;

    public String get(String url, int timeout, String userAgent, String referrer, String cookie) throws IOException;

    public String get(String url, int timeout, String userAgent, String referrer, String cookie, Map<String, String> customHeaders) throws IOException;

    public byte[] getBytes(String url, int timeout, String userAgent, String referrer);

    public byte[] getBytes(String url, int timeout, String referrer);

    public byte[] getBytes(String url, int timeout);

    public byte[] getBytes(String url);

    public void save(String url, File file) throws IOException;

    public void save(String url, File file, boolean resume) throws IOException;

    public void save(String url, File file, boolean resume, int timeout, String userAgent) throws IOException;

    public String post(String url, int timeout, String userAgent, Map<String, String> formData);

    public String post(String url, int timeout, String userAgent, String content, boolean gzip) throws IOException;

    public String post(String url, int timeout, String userAgent, String content, String postContentType, boolean gzip) throws IOException;

    public void post(String url, int timeout, String userAgent, ProgressFileEntity fileEntity) throws Throwable;

    public void cancel();

    public boolean isCanceled();

    public interface HttpClientListener {

        public void onError(HttpClient client, Throwable e);

        public void onData(HttpClient client, byte[] buffer, int offset, int length);

        public void onComplete(HttpClient client);

        public void onCancel(HttpClient client);

        public void onHeaders(HttpClient httpClient, Map<String, List<String>> headerFields);
    }

    public abstract class HttpClientListenerAdapter implements HttpClientListener {

        public void onError(HttpClient client, Throwable e) {
        }

        public void onData(HttpClient client, byte[] buffer, int offset, int length) {
        }

        public void onComplete(HttpClient client) {
        }

        public void onCancel(HttpClient client) {
        }

        public void onHeaders(HttpClient httpClient, Map<String, List<String>> headerFields) {
        }
    }

    public static class HttpRangeException extends IOException {

        private static final long serialVersionUID = 1891038288667531894L;

        public HttpRangeException(String message) {
            super(message);
        }
    }

    public static final class RangeNotSupportedException extends HttpRangeException {

        private static final long serialVersionUID = -3356618211960630147L;

        public RangeNotSupportedException(String message) {
            super(message);
        }
    }

    public static final class HttpRangeOutOfBoundsException extends HttpRangeException {

        private static final long serialVersionUID = -335661829606230147L;

        public HttpRangeOutOfBoundsException(int rangeStart, long expectedFileSize) {
            super("HttpRange Out of Bounds error: start=" + rangeStart + " expected file size=" + expectedFileSize);
        }

    }

    public static final class ResponseCodeNotSupportedException extends IOException {
        private final int responseCode;

        public ResponseCodeNotSupportedException(int code) {
            responseCode = code;
        }

        public int getResponseCode() {
            return responseCode;
        }
    }
}
