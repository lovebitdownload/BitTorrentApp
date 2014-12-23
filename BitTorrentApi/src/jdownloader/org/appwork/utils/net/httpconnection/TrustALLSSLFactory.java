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
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * @author daniel
 * 
 */
public class TrustALLSSLFactory {
    private static TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {

                                                    @Override
                                                    public void checkClientTrusted(final java.security.cert.X509Certificate[] chain, final String authType) throws CertificateException {
                                                    }

                                                    @Override
                                                    public void checkServerTrusted(final java.security.cert.X509Certificate[] chain, final String authType) throws CertificateException {
                                                    }

                                                    @Override
                                                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                                                        /*
                                                         * returning null here
                                                         * can cause a NPE in
                                                         * some java versions!
                                                         */
                                                        return new java.security.cert.X509Certificate[0];
                                                    }
                                                } };

    public static SSLSocketFactory getSSLFactoryTrustALL() throws IOException {
        try {
            final SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, TrustALLSSLFactory.trustAllCerts, new java.security.SecureRandom());
            return sc.getSocketFactory();
        } catch (final NoSuchAlgorithmException e) {
            throw new IOException(e.toString());
        } catch (final KeyManagementException e) {
            throw new IOException(e.toString());
        }
    }

}
