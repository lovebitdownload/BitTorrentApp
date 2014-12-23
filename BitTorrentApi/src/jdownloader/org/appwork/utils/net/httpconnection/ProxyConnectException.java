package org.appwork.utils.net.httpconnection;

public class ProxyConnectException extends HTTPProxyException {

    /**
	 * 
	 */
    private static final long serialVersionUID = 189884014110822090L;

    public ProxyConnectException(final HTTPProxy proxy) {
        super();
        this.proxy = proxy;
    }

    public ProxyConnectException(final String message, final HTTPProxy proxy) {
        super(message);
        this.proxy = proxy;
    }

    public ProxyConnectException(final Throwable e, final HTTPProxy proxy) {
        super(e);
        this.proxy = proxy;
    }
}
