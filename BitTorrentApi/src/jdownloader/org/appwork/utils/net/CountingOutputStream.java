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
public class CountingOutputStream extends OutputStream implements CountingConnection {

    private OutputStream  os      = null;
    private volatile long written = 0;

    public CountingOutputStream(final OutputStream os) {
        this.os = os;
    }

    @Override
    public void close() throws IOException {
        this.os.close();
    }

    @Override
    public void flush() throws IOException {
        this.os.flush();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.appwork.utils.net.CountingConnection#transferedBytes()
     */
    @Override
    public long transferedBytes() {
        return this.written;
    }

    @Override
    public void write(final byte b[]) throws IOException {
        this.os.write(b);
        this.written += b.length;
    }

    @Override
    public void write(final byte b[], final int off, final int len) throws IOException {
        this.os.write(b, off, len);
        this.written += len;
    }

    @Override
    public void write(final int b) throws IOException {
        this.os.write(b);
        this.written++;
    }

}
