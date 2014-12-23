/**
 * Copyright (c) 2009 - 2010 AppWork UG(haftungsbeschränkt) <e-mail@appwork.org>
 * 
 * This file is part of org.appwork.utils.net
 * 
 * This software is licensed under the Artistic License 2.0,
 * see the LICENSE file or http://www.opensource.org/licenses/artistic-license-2.0.php
 * for details
 */
package org.appwork.utils.net;

import java.io.IOException;
import java.io.OutputStream;

/**
 * @author daniel
 * 
 */
public class NullOutputStream extends OutputStream {

    @Override
    public void write(int b) throws IOException {
    }

    @Override
    public void write(byte b[]) throws IOException {
    }

    @Override
    public void write(byte b[], int off, int len) throws IOException {
    }

}
