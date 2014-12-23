package org.appwork.utils.net.httpconnection;

import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.nio.ByteBuffer;

import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.appwork.utils.Regex;
import org.appwork.utils.encoding.Base64;

public class HTTPProxyHTTPConnectionImpl extends HTTPConnectionImpl {
    private int               httpPort;
    private String            httpHost;
    private StringBuilder     proxyRequest;
    private InetSocketAddress proxyInetSocketAddress = null;

    private boolean           preferConnectMethod    = true;

    public HTTPProxyHTTPConnectionImpl(final URL url, final HTTPProxy p) {
        super(url, p);
        this.preferConnectMethod = p.isConnectMethodPrefered();
    }

    /*
     * SSL over HTTP Proxy, see
     * http://muffin.doit.org/docs/rfc/tunneling_ssl.html
     */
    @Override
    public void connect() throws IOException {
        if (this.isConnected()) { return;/* oder fehler */
        }
        try {
            if (this.proxy == null || !this.proxy.getType().equals(HTTPProxy.TYPE.HTTP)) { throw new IOException("HTTPProxyHTTPConnection: invalid HTTP Proxy!"); }
            if (this.proxy.getPass() != null && this.proxy.getPass().length() > 0 || this.proxy.getUser() != null && this.proxy.getUser().length() > 0) {
                /* add proxy auth in case username/pw are set */
                final String user = this.proxy.getUser() == null ? "" : this.proxy.getUser();
                final String pass = this.proxy.getPass() == null ? "" : this.proxy.getPass();
                this.requestProperties.put("Proxy-Authorization", "Basic " + new String(Base64.encodeToByte((user + ":" + pass).getBytes(), false)));
            }
            final InetAddress hosts[] = this.resolvHostIP(this.proxy.getHost());
            IOException ee = null;
            long startTime = System.currentTimeMillis();
            for (final InetAddress host : hosts) {
                this.httpSocket = new Socket();
                this.httpSocket.setSoTimeout(this.readTimeout);
                try {
                    /* create and connect to socks5 proxy */
                    startTime = System.currentTimeMillis();
                    this.httpSocket.connect(this.proxyInetSocketAddress = new InetSocketAddress(host, this.proxy.getPort()), this.connectTimeout);
                    /* connection is okay */
                    ee = null;
                    break;
                } catch (final IOException e) {
                    /* connection failed, try next available ip */
                    this.proxyInetSocketAddress = null;
                    try {
                        this.httpSocket.close();
                    } catch (final Throwable e2) {
                    }
                    ee = e;
                }
            }
            if (ee != null) { throw new ProxyConnectException(ee, this.proxy); }
            this.requestTime = System.currentTimeMillis() - startTime;
            if (this.httpURL.getProtocol().startsWith("https") || this.isConnectMethodPrefered()) {
                /* ssl via CONNECT method or because we prefer CONNECT */
                /* build CONNECT request */
                this.proxyRequest = new StringBuilder();
                this.proxyRequest.append("CONNECT ");
                this.proxyRequest.append(this.httpURL.getHost() + ":" + (this.httpURL.getPort() != -1 ? this.httpURL.getPort() : this.httpURL.getDefaultPort()));
                this.proxyRequest.append(" HTTP/1.1\r\n");
                if (this.requestProperties.get("User-Agent") != null) {
                    this.proxyRequest.append("User-Agent: " + this.requestProperties.get("User-Agent") + "\r\n");
                }
                if (this.requestProperties.get("Host") != null) {
                    /* use existing host header */
                    this.proxyRequest.append("Host: " + this.requestProperties.get("Host") + "\r\n");
                } else {
                    /* add host from url as fallback */
                    this.proxyRequest.append("Host: " + this.httpURL.getHost() + "\r\n");
                }
                if (this.requestProperties.get("Proxy-Authorization") != null) {
                    this.proxyRequest.append("Proxy-Authorization: " + this.requestProperties.get("Proxy-Authorization") + "\r\n");
                }
                this.proxyRequest.append("\r\n");
                /* send CONNECT to proxy */
                this.httpSocket.getOutputStream().write(this.proxyRequest.toString().getBytes("UTF-8"));
                this.httpSocket.getOutputStream().flush();
                /* parse CONNECT response */
                ByteBuffer header = HTTPConnectionUtils.readheader(this.httpSocket.getInputStream(), true);
                byte[] bytes = new byte[header.limit()];
                header.get(bytes);
                final String proxyResponseStatus = new String(bytes, "ISO-8859-1").trim();
                this.proxyRequest.append(proxyResponseStatus + "\r\n");
                String proxyCode = null;
                if (proxyResponseStatus.startsWith("HTTP")) {
                    /* parse response code */
                    proxyCode = new Regex(proxyResponseStatus, "HTTP.*? (\\d+)").getMatch(0);
                }
                if (!"200".equals(proxyCode)) {
                    /* something went wrong */
                    try {
                        this.httpSocket.close();
                    } catch (final Throwable nothing) {
                    }
                    if ("407".equals(proxyCode)) {
                        /* auth invalid/missing */
                        throw new ProxyAuthException(this.proxy);
                    }

                    throw new ProxyConnectException(this.proxy);
                }
                /* read rest of CONNECT headers */
                /*
                 * Again, the response follows the HTTP/1.0 protocol, so the
                 * response line starts with the protocol version specifier, and
                 * the response line is followed by zero or more response
                 * headers, followed by an empty line. The line separator is CR
                 * LF pair, or a single LF.
                 */
                while (true) {
                    /*
                     * read line by line until we reach the single empty line as
                     * seperator
                     */
                    header = HTTPConnectionUtils.readheader(this.httpSocket.getInputStream(), true);
                    if (header.limit() <= 2) {
                        /* empty line, <=2, as it may contains \r and/or \n */
                        break;
                    }
                    bytes = new byte[header.limit()];
                    header.get(bytes);
                    final String temp = new String(bytes, "UTF-8").trim();
                    this.proxyRequest.append(temp + "\r\n");
                }
                this.httpPort = this.httpURL.getPort();
                this.httpHost = this.httpURL.getHost();
                if (this.httpPort == -1) {
                    this.httpPort = this.httpURL.getDefaultPort();
                }
                if (this.httpURL.getProtocol().startsWith("https")) {
                    SSLSocket sslSocket = null;
                    try {
                        final SSLSocketFactory socketFactory = TrustALLSSLFactory.getSSLFactoryTrustALL();
                        sslSocket = (SSLSocket) socketFactory.createSocket(this.httpSocket, this.httpHost, this.httpPort, true);
                        sslSocket.startHandshake();
                    } catch (final SSLHandshakeException e) {
                        try {
                            sslSocket.close();
                        } catch (final Throwable e3) {
                        }
                        try {
                            this.httpSocket.close();
                        } catch (final Throwable e2) {
                        }
                        throw new IOException("HTTPProxyHTTPConnection: " + e.toString());
                    }
                    this.httpSocket = sslSocket;
                }
                /* httpPath needs to be like normal http request, eg /index.html */
                this.httpPath = new org.appwork.utils.Regex(this.httpURL.toString(), "https?://.*?(/.+)").getMatch(0);
                if (this.httpPath == null) {
                    this.httpPath = "/";
                }
            } else {
                /* direct connect via proxy */
                /*
                 * httpPath needs to include complete path here, eg
                 * http://google.de/
                 */
                this.proxyRequest = new StringBuilder("DIRECT\r\n");
                this.httpPath = this.httpURL.toString();
            }
            /* now send Request */
            this.sendRequest();
        } catch (final IOException e) {
            try {
                this.disconnect();
            } catch (final Throwable e2) {
            }
            if (e instanceof HTTPProxyException) { throw e; }
            throw new ProxyConnectException(e, this.proxy);
        }
    }

    @Override
    public InputStream getInputStream() throws IOException {
        this.connect();
        this.connectInputStream();
        if (this.getResponseCode() == 407) {
            /* auth invalid/missing */
            throw new ProxyAuthException(this.proxy);
        }
        if (this.getResponseCode() == 504) { throw new ConnectException(this.getResponseCode() + " " + this.getResponseMessage()); }
        return super.getInputStream();
    }

    @Override
    protected String getRequestInfo() {
        if (this.proxyRequest != null) {
            final StringBuilder sb = new StringBuilder();
            sb.append("-->HTTPProxy:").append(this.proxy.getHost() + ":" + this.proxy.getPort()).append("\r\n");
            if (this.proxyInetSocketAddress != null && this.proxyInetSocketAddress.getAddress() != null) {
                sb.append("-->HTTPProxyIP:").append(this.proxyInetSocketAddress.getAddress().getHostAddress()).append("\r\n");
            }
            sb.append("----------------CONNECTRequest(HTTP)------------\r\n");
            sb.append(this.proxyRequest.toString());
            sb.append("------------------------------------------------\r\n");
            sb.append(super.getRequestInfo());
            return sb.toString();
        }
        return super.getRequestInfo();
    }

    public boolean isConnectMethodPrefered() {
        return this.preferConnectMethod;
    }

    public void setPreferConnectMethod(final boolean preferConnectMethod) {
        this.preferConnectMethod = preferConnectMethod;
    }
}
