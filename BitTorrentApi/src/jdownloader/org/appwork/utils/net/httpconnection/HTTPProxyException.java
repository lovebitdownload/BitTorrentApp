/**
 * Copyright (c) 2009 - 2011 AppWork UG(haftungsbeschränkt) <e-mail@appwork.org>
 * 
 * This file is part of org.appwork.utils.net.httpconnection
 * 
 * This software is licensed under the Artistic License 2.0,
 * see the LICENSE file or http://www.opensource.org/licenses/artistic-license-2.0.php
 * for details
 */
package org.appwork.utils.net.httpconnection;

import java.io.IOException;

/**
 * @author daniel
 * 
 */
abstract class HTTPProxyException extends IOException {

    protected HTTPProxy       proxy            = null;

    private static final long serialVersionUID = -7826780596815416403L;

    public HTTPProxyException() {
        super();
    }

    public HTTPProxyException(final String message) {
        super(message);
    }

    public HTTPProxyException(final Throwable cause) {
        super(cause.toString());
    }

    public HTTPProxy getProxy() {
        return this.proxy;
    }

}
