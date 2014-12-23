package org.appwork.utils.net.httpconnection;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
//import org.appwork.utils.locale._AWU;
import org.appwork.utils.logging.Log;

public class HTTPProxy {

    public static enum TYPE {
        NONE,
        DIRECT,
        SOCKS4,
        SOCKS5,
        HTTP
    }

    public static final HTTPProxy NONE = new HTTPProxy(TYPE.NONE) {

                                           @Override
                                           public void setConnectMethodPrefered(final boolean value) {
                                           }

                                           @Override
                                           public void setLocalIP(final InetAddress localIP) {
                                           }

                                           @Override
                                           public void setPass(final String pass) {
                                           }

                                           @Override
                                           public void setPort(final int port) {
                                           }

                                           @Override
                                           public void setType(final TYPE type) {
                                           }

                                           @Override
                                           public void setUser(final String user) {
                                           }

                                       };

    public static List<HTTPProxy> getFromSystemProperties() {
        final java.util.List<HTTPProxy> ret = new ArrayList<HTTPProxy>();
        try {
            {
                /* try to parse http proxy from system properties */
                final String host = System.getProperties().getProperty("http.proxyHost");
                if (!StringUtils.isEmpty(host)) {
                    int port = 80;
                    final String ports = System.getProperty("http.proxyPort");
                    if (!StringUtils.isEmpty(ports)) {
                        port = Integer.parseInt(ports);
                    }
                    final HTTPProxy pr = new HTTPProxy(HTTPProxy.TYPE.HTTP, host, port);
                    final String user = System.getProperty("http.proxyUser");
                    final String pass = System.getProperty("http.proxyPassword");
                    if (!StringUtils.isEmpty(user)) {
                        pr.setUser(user);
                    }
                    if (!StringUtils.isEmpty(pass)) {
                        pr.setPass(pass);
                    }
                    ret.add(pr);
                }
            }
            {
                /* try to parse socks5 proxy from system properties */
                final String host = System.getProperties().getProperty("socksProxyHost");
                if (!StringUtils.isEmpty(host)) {
                    int port = 1080;
                    final String ports = System.getProperty("socksProxyPort");
                    if (!StringUtils.isEmpty(ports)) {
                        port = Integer.parseInt(ports);
                    }
                    final HTTPProxy pr = new HTTPProxy(HTTPProxy.TYPE.SOCKS5, host, port);
                    ret.add(pr);
                }
            }
        } catch (final Throwable e) {
            Log.exception(e);
        }
        return ret;
    }

    public static HTTPProxy getHTTPProxy(final HTTPProxyStorable storable) {
        if (storable == null || storable.getType() == null) { return null; }
        HTTPProxy ret = null;
        switch (storable.getType()) {
        case NONE:
            return HTTPProxy.NONE;
        case DIRECT:
            ret = new HTTPProxy(TYPE.DIRECT);
            if (ret.getHost() != null) {
                try {
                    final InetAddress ip = InetAddress.getByName(ret.getHost());
                    ret.setLocalIP(ip);
                } catch (final Throwable e) {
                    e.printStackTrace();
                    return null;
                }
            } else {
                return null;
            }
            break;
        case HTTP:
            ret = new HTTPProxy(TYPE.HTTP);
            ret.host = storable.getAddress();
            break;
        case SOCKS4:
            ret = new HTTPProxy(TYPE.SOCKS4);
            ret.host = storable.getAddress();
        case SOCKS5:
            ret = new HTTPProxy(TYPE.SOCKS5);
            ret.host = storable.getAddress();
            break;
        }
        ret.setPass(storable.getPassword());
        ret.setUser(storable.getUsername());
        ret.setPort(storable.getPort());
        return ret;
    }

    private static String[] getInfo(final String host, final String port) {
        final String[] info = new String[2];
        if (host == null) { return info; }
        final String tmphost = host.replaceFirst("http://", "").replaceFirst("https://", "");
        String tmpport = new org.appwork.utils.Regex(host, ".*?:(\\d+)").getMatch(0);
        if (tmpport != null) {
            info[1] = "" + tmpport;
        } else {
            if (port != null) {
                tmpport = new Regex(port, "(\\d+)").getMatch(0);
            }
            if (tmpport != null) {
                info[1] = "" + tmpport;
            } else {
                Log.L.severe("No proxyport defined, using default 8080");
                info[1] = "8080";
            }
        }
        info[0] = new Regex(tmphost, "(.*?)(:|/|$)").getMatch(0);
        return info;
    }

    public static HTTPProxyStorable getStorable(final HTTPProxy proxy) {
        if (proxy == null || proxy.getType() == null) { return null; }
        final HTTPProxyStorable ret = new HTTPProxyStorable();
        switch (proxy.getType()) {
        case NONE:
            ret.setType(HTTPProxyStorable.TYPE.NONE);
            ret.setAddress(null);
            break;
        case DIRECT:
            ret.setType(HTTPProxyStorable.TYPE.DIRECT);
            if (proxy.getLocalIP() != null) {
                final String ip = proxy.getLocalIP().getHostAddress();
                ret.setAddress(ip);
            } else {
                ret.setAddress(null);
            }
            break;
        case HTTP:
            ret.setType(HTTPProxyStorable.TYPE.HTTP);
            ret.setAddress(proxy.getHost());
            break;
        case SOCKS4:
            ret.setType(HTTPProxyStorable.TYPE.SOCKS4);
            ret.setAddress(proxy.getHost());
            break;
        case SOCKS5:
            ret.setType(HTTPProxyStorable.TYPE.SOCKS5);
            ret.setAddress(proxy.getHost());
            break;
        }
        ret.setPort(proxy.getPort());
        ret.setPassword(proxy.getPass());
        ret.setUsername(proxy.getUser());
        return ret;
    }

    public static HTTPProxy parseHTTPProxy(final String s) {
        if (StringUtils.isEmpty(s)) { return null; }
        final String type = new Regex(s, "(https?|socks5|socks4|direct)://").getMatch(0);
        final String auth = new Regex(s, "://(.*?)@").getMatch(0);
        final String host = new Regex(s, "://(.*?@)?(.*?)(/|$)").getMatch(1);
        HTTPProxy ret = null;
        if ("http".equalsIgnoreCase(type) || "https".equalsIgnoreCase(type)) {
            ret = new HTTPProxy(TYPE.HTTP);
            ret.port = 8080;
        } else if ("socks5".equalsIgnoreCase(type)) {
            ret = new HTTPProxy(TYPE.SOCKS5);
            ret.port = 1080;
        } else if ("socks4".equalsIgnoreCase(type)) {
            ret = new HTTPProxy(TYPE.SOCKS4);
            ret.port = 1080;
        } else if ("direct".equalsIgnoreCase(type)) {
            ret = new HTTPProxy(TYPE.DIRECT);
            final String hostname = new Regex(host, "(.*?)(:|$)").getMatch(0);
            if (!StringUtils.isEmpty(hostname)) {
                try {
                    final InetAddress ip = InetAddress.getByName(hostname);
                    ret.localIP = ip;
                    return ret;
                } catch (final Throwable e) {
                    e.printStackTrace();
                    return null;
                }
            }
        }
        if (ret != null) {
            final String hostname = new Regex(host, "(.*?)(:|$)").getMatch(0);
            final String port = new Regex(host, ".*?:(\\d+)").getMatch(0);
            if (!StringUtils.isEmpty(hostname)) {
                ret.host = hostname;
            }
            if (!StringUtils.isEmpty(port) && ret != null) {
                ret.port = Integer.parseInt(port);
            }
            final String username = new Regex(auth, "(.*?)(:|$)").getMatch(0);
            final String password = new Regex(auth, ".*?:(.+)").getMatch(0);
            if (!StringUtils.isEmpty(username)) {
                ret.user = username;
            }
            if (!StringUtils.isEmpty(password)) {
                ret.pass = password;
            }
            if (!StringUtils.isEmpty(ret.host)) { return ret; }
        }
        return null;
    }

    private InetAddress localIP          = null;

    private String      user             = null;

    private String      pass             = null;
    private int         port             = 80;

    protected String    host             = null;

    private TYPE        type             = TYPE.DIRECT;

    private boolean     useConnectMethod = false;

    protected HTTPProxy() {
    }

    public HTTPProxy(final InetAddress direct) {
        this.type = TYPE.DIRECT;
        this.localIP = direct;
    }

    public HTTPProxy(final TYPE type) {
        this.type = type;
    }

    public HTTPProxy(final TYPE type, final String host, final int port) {
        this.port = port;
        this.type = type;
        this.host = HTTPProxy.getInfo(host, "" + port)[0];
    }

    protected void cloneProxy(final HTTPProxy proxy) {
        if (proxy == null) { return; }
        this.user = proxy.user;
        this.host = proxy.host;
        this.localIP = proxy.localIP;
        this.pass = proxy.pass;
        this.port = proxy.port;
        this.type = proxy.type;
        this.localIP = proxy.localIP;
        this.useConnectMethod = proxy.useConnectMethod;
    }

    public String getHost() {
        return this.host;
    }

    /**
     * @return the localIP
     */
    public InetAddress getLocalIP() {
        return this.localIP;
    }

    public String getPass() {
        return this.pass;
    }

    public int getPort() {
        return this.port;
    }

    public TYPE getType() {
        return this.type;
    }

    public String getUser() {
        return this.user;
    }

    /**
     * this proxy is DIRECT = using a local bound IP
     * 
     * @return
     */
    public boolean isDirect() {
        return this.type == TYPE.DIRECT;
    }

    public boolean isConnectMethodPrefered() {
        return this.useConnectMethod;
    }

    public boolean isLocal() {
        return this.isDirect() || this.isNone();
    }

    /**
     * this proxy is NONE = uses default gateway
     * 
     * @return
     */
    public boolean isNone() {
        return this.type == TYPE.NONE;
    }

    /**
     * this proxy is REMOTE = using http,socks proxy
     * 
     * @return
     */
    public boolean isRemote() {
        return !this.isDirect() && !this.isNone();
    }

    public boolean sameProxy(final HTTPProxy proxy) {
        if (proxy == null) { return false; }
        if (this == proxy) { return true; }
        if (!proxy.getType().equals(this.type)) { return false; }
        if (proxy.getType().equals(TYPE.DIRECT)) {
            /* Direct Proxies only differ in IP */
            if (!proxy.getLocalIP().equals(this.localIP)) { return false; }
            return true;
        } else {
            if (!proxy.getHost().equalsIgnoreCase(this.host)) { return false; }
        }
        if (!StringUtils.equals(proxy.getPass(), this.pass)) { return false; }
        if (!StringUtils.equals(proxy.getUser(), this.user)) { return false; }
        if (proxy.getPort() != this.port) { return false; }
        return true;
    }

    public void setHost(final String host) {
        this.host = host;
    }

    /**
     * @param localIP
     *            the localIP to set
     */
    public void setLocalIP(final InetAddress localIP) {
        this.localIP = localIP;
    }

    public void setPass(final String pass) {
        this.pass = pass;
    }

    public void setPort(final int port) {
        this.port = port;
    }

    public void setType(final TYPE type) {
        this.type = type;
    }

    public void setUser(final String user) {
        this.user = user;
    }

    public void setConnectMethodPrefered(final boolean value) {
        this.useConnectMethod = value;
    }

    @Override
    public String toString() {
//        if (this.type == TYPE.NONE) {
//            return _AWU.T.proxy_none();
//        } else if (this.type == TYPE.DIRECT) {
//            return _AWU.T.proxy_direct(this.localIP.getHostAddress());
//        } else if (this.type == TYPE.HTTP) {
//            return _AWU.T.proxy_http(this.getHost(), this.getPort());
//        } else if (this.type == TYPE.SOCKS5) {
//            return _AWU.T.proxy_socks5(this.getHost(), this.getPort());
//        } else if (this.type == TYPE.SOCKS4) {
//            return _AWU.T.proxy_socks4(this.getHost(), this.getPort());
//        } else {
            return "UNKNOWN";
//        }
    }
}
