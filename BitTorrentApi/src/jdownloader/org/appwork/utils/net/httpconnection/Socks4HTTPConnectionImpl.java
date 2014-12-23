/**
 * Copyright (c) 2009 - 2012 AppWork UG(haftungsbeschränkt) <e-mail@appwork.org>
 * 
 * This file is part of org.appwork.utils.net.httpconnection
 * 
 * This software is licensed under the Artistic License 2.0,
 * see the LICENSE file or http://www.opensource.org/licenses/artistic-license-2.0.php
 * for details
 */
package org.appwork.utils.net.httpconnection;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;

/**
 * @author daniel
 * 
 */
public class Socks4HTTPConnectionImpl extends SocksHTTPconnection {

    public Socks4HTTPConnectionImpl(final URL url, final HTTPProxy proxy) {
        super(url, proxy);
    }

    @Override
    protected void authenticateProxyPlain() throws IOException {
        /* SOCKS4 has no authentication */
    }

    @Override
    protected Socket establishConnection() throws IOException {
        try {
            /* tcp/ip connection */
            this.socksoutputstream.write((byte) 1);
            /* send port */
            /* network byte order */
            this.socksoutputstream.write(this.httpPort >> 8 & 0xff);
            this.socksoutputstream.write(this.httpPort & 0xff);
            boolean ipResolvOK = false;
            /* send domain ip */
            try {
                final InetAddress addr = InetAddress.getByName(this.httpHost);
                if (addr == null) { throw new UnknownHostException("Could not resolv host"); }
                final byte[] addrIP = addr.getAddress();
                this.proxyRequest.append("->SEND tcp connect request by ip\r\n");
                this.socksoutputstream.write(addrIP);
                ipResolvOK = true;
            } catch (final UnknownHostException e) {
            }
            if (ipResolvOK == false) {
                this.proxyRequest.append("->SEND tcp connect request by domain\r\n");
                this.socksoutputstream.write((byte) 0);
                this.socksoutputstream.write((byte) 0);
                this.socksoutputstream.write((byte) 0);
                this.socksoutputstream.write((byte) 100);
            }
            /* user ID string */
            final String user = this.proxy.getUser() == null ? "" : this.proxy.getUser().trim();
            final byte[] username = user.getBytes("ISO-8859-1");
            if (username.length > 0) {
                this.socksoutputstream.write(username);
            }
            this.socksoutputstream.write((byte) 0);
            if (ipResolvOK == false) {
                /* send domain as string,socks4a */
                final byte[] domain = this.httpHost.getBytes("ISO-8859-1");
                this.socksoutputstream.write(domain);
                this.socksoutputstream.write((byte) 0);
            }
            this.socksoutputstream.flush();
            /* read response, 8 bytes */
            final byte[] resp = this.readResponse(8);
            switch (resp[1]) {
            case 0x5a:
                break;
            case 0x5b:
                throw new SocketException("request rejected or failed");
            case 0x5c:
                throw new SocketException("request failed because client is not running identd (or not reachable from the server)");
            case 0x5d:
                throw new ConnectException("request failed because client's identd could not confirm the user ID string in the request");
            default:
                throw new ProxyConnectException("Socks4HTTPConnection: could not establish connection, status=" + resp[1], this.proxy);
            }
            return this.sockssocket;
        } catch (final IOException e) {
            try {
                this.sockssocket.close();
            } catch (final Throwable e2) {
            }
            if (e instanceof HTTPProxyException) { throw e; }
            throw new ProxyConnectException(e, this.proxy);
        }
    }

    @Override
    protected AUTH sayHello() throws IOException {
        try {
            this.proxyRequest.append("->SOCKS4 Hello\r\n");
            /* socks4 */
            this.socksoutputstream.write((byte) 4);
            return AUTH.NONE;
        } catch (final IOException e) {
            try {
                this.sockssocket.close();
            } catch (final Throwable e2) {
            }
            if (e instanceof HTTPProxyException) { throw e; }
            throw new ProxyConnectException(e, this.proxy);
        }
    }

    @Override
    protected void validateProxy() throws IOException {
        if (this.proxy == null || !this.proxy.getType().equals(HTTPProxy.TYPE.SOCKS4)) { throw new IOException("Socks4HTTPConnection: invalid Socks4 Proxy!"); }
    }

}
