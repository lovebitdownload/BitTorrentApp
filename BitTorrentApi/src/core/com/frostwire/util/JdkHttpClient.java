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

import com.frostwire.logging.Logger;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * A pure java based HTTP client with resume capabilities.
 *
 * @author gubatron
 * @author aldenml
 */
final class JdkHttpClient implements HttpClient {

    private static final Logger LOG = Logger.getLogger(JdkHttpClient.class);

    private static final int DEFAULT_TIMEOUT = 10000;
    private static final String DEFAULT_USER_AGENT = UserAgentGenerator.getUserAgent();
    private HttpClientListener listener;

    private boolean canceled;

    @Override
    public int head(String url, int connectTimeoutInMillis) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(connectTimeoutInMillis);
        connection.setReadTimeout(connectTimeoutInMillis);
        connection.setRequestMethod("HEAD");
        return connection.getResponseCode();
    }


    public String get(String url) throws IOException {
        return get(url, DEFAULT_TIMEOUT, DEFAULT_USER_AGENT);
    }

    public String get(String url, int timeout) throws IOException {
        return get(url, timeout, DEFAULT_USER_AGENT);
    }

    public String get(String url, int timeout, String userAgent) throws IOException {
        return get(url, timeout, userAgent, null, null);
    }

    public String get(String url, int timeout, String userAgent, String referrer, String cookie) throws IOException {
        return get(url, timeout, userAgent, referrer, cookie, null);
    }

    @Override
    public String get(String url, int timeout, String userAgent, String referrer, String cookie, Map<String, String> customHeaders) throws IOException {
        String result = null;

        ByteArrayOutputStream baos = null;

        try {
            baos = new ByteArrayOutputStream();
            get(url, baos, timeout, userAgent, referrer, cookie, -1, -1, customHeaders);

            result = new String(baos.toByteArray(), "UTF-8");
        } catch (java.net.SocketTimeoutException timeoutException) {
            throw timeoutException;
        } catch (IOException e) {
            throw e;
        } finally {
            closeQuietly(baos);
        }

        return result;
    }

    public byte[] getBytes(String url, int timeout, String userAgent, String referrer) {
        byte[] result = null;

        ByteArrayOutputStream baos = null;

        try {
            baos = new ByteArrayOutputStream();
            get(url, baos, timeout, userAgent, referrer, null, -1);

            result = baos.toByteArray();
        } catch (Throwable e) {
            LOG.error("Error getting bytes from http body response: " + e.getMessage(), e);
        } finally {
            closeQuietly(baos);
        }

        return result;
    }

    @Override
    public byte[] getBytes(String url, int timeout, String referrer) {
        return getBytes(url, timeout, DEFAULT_USER_AGENT, referrer);
    }

    @Override
    public byte[] getBytes(String url, int timeout) {
        return getBytes(url, timeout, null);
    }

    @Override
    public byte[] getBytes(String url) {
        return getBytes(url, DEFAULT_TIMEOUT);
    }

    public void save(String url, File file) throws IOException {
        save(url, file, false, DEFAULT_TIMEOUT, DEFAULT_USER_AGENT);
    }

    public void save(String url, File file, boolean resume) throws IOException {
        save(url, file, resume, DEFAULT_TIMEOUT, DEFAULT_USER_AGENT);
    }

    public void save(String url, File file, boolean resume, int timeout, String userAgent) throws IOException {
        save(url, file, resume, timeout, userAgent, null);
    }

    public void save(String url, File file, boolean resume, int timeout, String userAgent, String referrer) throws IOException {
        FileOutputStream fos = null;
        int rangeStart = 0;

        try {
            if (resume && file.exists()) {
                fos = new FileOutputStream(file, true);
                rangeStart = (int) file.length();
            } else {
                fos = new FileOutputStream(file, false);
                rangeStart = -1;
            }

            get(url, fos, timeout, userAgent, null, referrer, rangeStart);
        } finally {
            closeQuietly(fos);
        }
    }

    @Override
    public String post(String url, int timeout, String userAgent, String content, String postContentType, boolean gzip) throws IOException {
        String result = null;
        canceled = false;
        final URL u = new URL(url);
        final HttpURLConnection conn = (HttpURLConnection) u.openConnection();
        conn.setDoOutput(true);

        conn.setConnectTimeout(timeout);
        conn.setReadTimeout(timeout);
        conn.setRequestProperty("User-Agent", userAgent);
        conn.setInstanceFollowRedirects(false);

        if (conn instanceof HttpsURLConnection) {
            setHostnameVerifier((HttpsURLConnection) conn);
        }

        byte[] data = content.getBytes("UTF-8");

        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", postContentType);
        conn.setRequestProperty("charset", "utf-8");
        conn.setUseCaches(false);

        ByteArrayInputStream in = new ByteArrayInputStream(data);

        try {
            OutputStream out = null;
            if (gzip) {
                out = new GZIPOutputStream(conn.getOutputStream());
            } else {
                out = conn.getOutputStream();
            }

            byte[] b = new byte[4096];
            int n = 0;
            while (!canceled && (n = in.read(b, 0, b.length)) != -1) {
                if (!canceled) {
                    out.write(b, 0, n);
                    out.flush();
                    onData(b, 0, n);
                }
            }

            closeQuietly(out);

            conn.connect();
            int httpResponseCode = getResponseCode(conn);

            if (httpResponseCode != HttpURLConnection.HTTP_OK && httpResponseCode != HttpURLConnection.HTTP_PARTIAL) {
                throw new ResponseCodeNotSupportedException(httpResponseCode);
            }

            if (canceled) {
                onCancel();
            } else {
                BufferedInputStream bis = new BufferedInputStream(conn.getInputStream(), 4096);
                ByteArrayBuffer baf = new ByteArrayBuffer(1024);
                byte[] buffer = new byte[64];
                int read = 0;
                while (true) {
                    read = bis.read(buffer);
                    if (read == -1) {
                        break;
                    }
                    baf.append(buffer, 0, read);
                }
                result = new String(baf.toByteArray());
                onComplete();
            }
        } catch (Exception e) {
            onError(e);
        } finally {
            closeQuietly(in);
            closeQuietly(conn);
        }
        return result;
    }


    @Override
    public String post(String url, int timeout, String userAgent, String content, boolean gzip) throws IOException {
        return post(url, timeout, userAgent, content, "text/plain", gzip);
    }

    /**
     * Post a form Content-type: application/x-www-form-urlencoded
     */
    @Override
    public String post(String url, int timeout, String userAgent, Map<String, String> formData) {
        String result = null;

        ByteArrayOutputStream baos = null;

        try {
            baos = new ByteArrayOutputStream();
            post(url, baos, timeout, userAgent, formData);
            result = new String(baos.toByteArray(), "UTF-8");
        } catch (Throwable e) {
            LOG.error("Error posting data via http: " + e.getMessage(), e);
        } finally {
            closeQuietly(baos);
        }

        return result;
    }

    private String buildRange(int rangeStart, int rangeLength) {
        String prefix = "bytes=" + rangeStart + "-";
        return prefix + ((rangeLength > -1) ? (rangeStart + rangeLength) : "");
    }

    private void get(String url, OutputStream out, int timeout, String userAgent, String referrer, String cookie, int rangeStart) throws IOException {
        get(url, out, timeout, userAgent, referrer, cookie, rangeStart, -1, null);
    }

    private void get(String url, OutputStream out, int timeout, String userAgent, String referrer, String cookie, int rangeStart, int rangeLength, final Map<String, String> customHeaders) throws IOException {
        canceled = false;
        final URL u = new URL(url);
        final URLConnection conn = u.openConnection();

        conn.setConnectTimeout(timeout);
        conn.setReadTimeout(timeout);
        conn.setRequestProperty("User-Agent", userAgent);

        if (referrer != null) {
            conn.setRequestProperty("Referer", referrer);
        }

        if (cookie != null) {
            conn.setRequestProperty("Cookie", cookie);
        }

        if (conn instanceof HttpURLConnection) {
            ((HttpURLConnection) conn).setInstanceFollowRedirects(true);
        }

        if (conn instanceof HttpsURLConnection) {
            setHostnameVerifier((HttpsURLConnection) conn);
        }

        if (rangeStart > 0) {
            conn.setRequestProperty("Range", buildRange(rangeStart, rangeLength));
        }

        if (customHeaders != null && customHeaders.size() > 0) {
            //put down here so it can overwrite any of the previous headers.
            setCustomHeaders(conn, customHeaders);
        }

        InputStream in = conn.getInputStream();
        if ("gzip".equals(conn.getContentEncoding())) {
            in = new GZIPInputStream(in);
        }

        int httpResponseCode = getResponseCode(conn);

        if (httpResponseCode != HttpURLConnection.HTTP_OK &&
                httpResponseCode != HttpURLConnection.HTTP_PARTIAL &&
                httpResponseCode != HttpURLConnection.HTTP_MOVED_TEMP &&
                httpResponseCode != HttpURLConnection.HTTP_MOVED_PERM) {
            throw new ResponseCodeNotSupportedException(httpResponseCode);
        }

        onHeaders(conn.getHeaderFields());
        checkRangeSupport(rangeStart, conn);

        try {
            byte[] b = new byte[4096];
            int n = 0;
            while (!canceled && (n = in.read(b, 0, b.length)) != -1) {
                if (!canceled) {
                    out.write(b, 0, n);
                    onData(b, 0, n);
                }
            }

            closeQuietly(out);

            if (canceled) {
                onCancel();
            } else {
                onComplete();
            }
        } catch (Exception e) {
            onError(e);
        } finally {
            closeQuietly(in);
            closeQuietly(conn);
        }
    }

    @Override
    public void post(String url, int timeout, String userAgent, ProgressFileEntity fileEntity) throws Throwable {
        canceled = false;
        final URL u = new URL(url);
        final HttpURLConnection conn = (HttpURLConnection) u.openConnection();
        conn.setDoOutput(true);

        conn.setConnectTimeout(timeout);
        conn.setReadTimeout(timeout);
        conn.setRequestProperty("User-Agent", userAgent);
        conn.setInstanceFollowRedirects(false);

        if (conn instanceof HttpsURLConnection) {
            setHostnameVerifier((HttpsURLConnection) conn);
        }

        InputStream in = null;
        try {
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", fileEntity.getContentType());
            conn.setRequestProperty("charset", "utf-8");
            conn.setUseCaches(false);

            OutputStream postOut = conn.getOutputStream();
            fileEntity.writeTo(postOut);
            closeQuietly(postOut);
            conn.connect();

            in = conn.getInputStream();
            int httpResponseCode = getResponseCode(conn);

            if (httpResponseCode != HttpURLConnection.HTTP_OK && httpResponseCode != HttpURLConnection.HTTP_PARTIAL) {
                throw new ResponseCodeNotSupportedException(httpResponseCode);
            }

            if (canceled) {
                onCancel();
            } else {
                onComplete();
            }
        } catch (Exception e) {
            onError(e);
        } finally {
            closeQuietly(in);
            closeQuietly(conn);
        }
    }


    private void post(String url, OutputStream out, int timeout, String userAgent, Map<String, String> formData) throws IOException {
        canceled = false;
        final URL u = new URL(url);
        final HttpURLConnection conn = (HttpURLConnection) u.openConnection();
        conn.setDoOutput(true);

        conn.setConnectTimeout(timeout);
        conn.setReadTimeout(timeout);
        conn.setRequestProperty("User-Agent", userAgent);
        conn.setInstanceFollowRedirects(false);

        if (conn instanceof HttpsURLConnection) {
            setHostnameVerifier((HttpsURLConnection) conn);
        }

        StringBuilder sb = new StringBuilder();
        if (formData != null && formData.size() > 0) {
            for (Entry<String, String> kv : formData.entrySet()) {
                sb.append("&");
                sb.append(kv.getKey());
                sb.append("=");
                sb.append(kv.getValue());
            }
            sb.deleteCharAt(0);
        }

        byte[] data = sb.toString().getBytes("UTF-8");

        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setRequestProperty("charset", "utf-8");
        conn.setUseCaches(false);

        InputStream in = new ByteArrayInputStream(data);

        try {
            OutputStream postOut = conn.getOutputStream();

            byte[] b = new byte[4096];
            int n = 0;
            while (!canceled && (n = in.read(b, 0, b.length)) != -1) {
                if (!canceled) {
                    postOut.write(b, 0, n);
                    postOut.flush();
                    onData(b, 0, n);
                }
            }

            closeQuietly(postOut);
            closeQuietly(in);

            conn.connect();

            in = conn.getInputStream();
            int httpResponseCode = getResponseCode(conn);

            if (httpResponseCode != HttpURLConnection.HTTP_OK && httpResponseCode != HttpURLConnection.HTTP_PARTIAL) {
                throw new ResponseCodeNotSupportedException(httpResponseCode);
            }

            b = new byte[4096];
            n = 0;
            while (!canceled && (n = in.read(b, 0, b.length)) != -1) {
                if (!canceled) {
                    out.write(b, 0, n);
                    onData(b, 0, n);
                }
            }

            closeQuietly(out);

            if (canceled) {
                onCancel();
            } else {
                onComplete();
            }
        } catch (Exception e) {
            onError(e);
        } finally {
            closeQuietly(in);
            closeQuietly(conn);
        }
    }

    private void setCustomHeaders(URLConnection conn, Map<String, String> headers) {
        for (Entry<String, String> e : headers.entrySet()) {
            conn.setRequestProperty(e.getKey(), e.getValue());
        }
    }

    private void setHostnameVerifier(HttpsURLConnection conn) {
        conn.setHostnameVerifier(new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        });
    }

    private int getResponseCode(URLConnection conn) {
        try {
            return ((HttpURLConnection) conn).getResponseCode();
        } catch (Throwable e) {
            e.printStackTrace();
            LOG.error("can't get response code ", e);
            return -1;
        }
    }

    private void checkRangeSupport(int rangeStart, URLConnection conn) throws HttpRangeOutOfBoundsException, RangeNotSupportedException {

        boolean hasContentRange = conn.getHeaderField("Content-Range") != null;
        boolean hasAcceptRanges = conn.getHeaderField("Accept-Ranges") != null && conn.getHeaderField("Accept-Ranges").equals("bytes");

        if (rangeStart > 0 && !hasContentRange && !hasAcceptRanges) {
            RangeNotSupportedException rangeNotSupportedException = new RangeNotSupportedException("Server does not support bytes range request");
            onError(rangeNotSupportedException);
            throw rangeNotSupportedException;
        }
    }

    private void onHeaders(Map<String, List<String>> headerFields) {
        if (getListener() != null) {
            try {
                getListener().onHeaders(this, headerFields);
            } catch (Exception e) {
                LOG.warn(e.getMessage(), e);
            }
        }
    }

    private void onCancel() {
        if (getListener() != null) {
            try {
                getListener().onCancel(this);
            } catch (Exception e) {
                LOG.warn(e.getMessage(), e);
            }
        }
    }

    private void onData(byte[] b, int i, int n) {
        if (getListener() != null) {
            try {
                getListener().onData(this, b, 0, n);
            } catch (Exception e) {
                LOG.warn(e.getMessage(), e);
            }
        }
    }

    protected void onError(Exception e) {
        if (getListener() != null) {
            try {
                getListener().onError(this, e);
            } catch (Exception e2) {
                LOG.warn(e2.getMessage());
            }
        }  else {
            e.printStackTrace();
        }
    }

    protected void onComplete() {
        if (getListener() != null) {
            try {
                getListener().onComplete(this);
            } catch (Exception e) {
                LOG.warn(e.getMessage(), e);
            }
        }
    }

    private static void closeQuietly(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (IOException ioe) {
            // ignore
        }
    }

    private void closeQuietly(URLConnection conn) {
        if (conn instanceof HttpURLConnection) {
            try {
                ((HttpURLConnection) conn).disconnect();
            } catch (Throwable e) {
                LOG.debug("Error closing http connection", e);
            }
        }
    }

    @Override
    public void setListener(HttpClientListener listener) {
        this.listener = listener;
    }

    @Override
    public HttpClientListener getListener() {
        return listener;
    }

    @Override
    public void cancel() {
        canceled = true;
    }

    @Override
    public boolean isCanceled() {
        return canceled;
    }
}
