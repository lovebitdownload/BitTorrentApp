/**
 * Copyright (c) 2009 - 2011 AppWork UG(haftungsbeschränkt) <e-mail@appwork.org>
 * 
 * This file is part of org.appwork.utils.net
 * 
 * This software is licensed under the Artistic License 2.0,
 * see the LICENSE file or http://www.opensource.org/licenses/artistic-license-2.0.php
 * for details
 */
package org.appwork.utils.net;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author daniel
 * 
 *         Base64InputStream, based on RFC R2045, section 6.8
 */
public class Base64InputStream extends FilterInputStream {

    private static final char[] BASE64           = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray();
    private static final byte[] BASE64MAP;

    static {
        /* build Base64 mapping */
        BASE64MAP = new byte[256];
        for (int i = 0; i < 255; i++) {
            Base64InputStream.BASE64MAP[i] = -1;
        }
        for (int i = 0; i < Base64InputStream.BASE64.length; i++) {
            Base64InputStream.BASE64MAP[Base64InputStream.BASE64[i]] = (byte) i;
        }
    }

    private final byte[]        base64Encoded    = new byte[4];
    private final byte[]        base64Decoded    = new byte[3];
    private int                 decodedAvailable = 0;

    private static final int    LF               = 10;
    private static final int    CR               = 13;
    private static final byte   PADDING          = (byte) '=';

    /**
     * @param in
     */
    public Base64InputStream(final InputStream in) {
        super(in);
    }

    @Override
    public int available() throws IOException {
        return this.in.available() * 3 / 4 + this.decodedAvailable;
    }

    private void decodeBase64() throws IOException {
        int next = -1;
        int encodedPosition = 0;
        this.decodedAvailable = 0;
        while (encodedPosition < 4 && (next = this.in.read()) >= 0) {
            /* fill base64Encoded buffer */
            if (next == Base64InputStream.LF || next == Base64InputStream.CR) {
                /* ignore CR and LF */
                continue;
            }
            this.base64Encoded[encodedPosition++] = (byte) next;
        }
        if (next == -1 && encodedPosition != 0) { throw new IOException("Base64 encoding error"); }
        if (next == -1 && encodedPosition == 0) { return; }
        byte byte_part_1 = Base64InputStream.BASE64MAP[this.base64Encoded[0] & 0xff];
        byte byte_part_2 = Base64InputStream.BASE64MAP[this.base64Encoded[1] & 0xff];
        /* reconstruct first decoded byte */
        this.base64Decoded[this.decodedAvailable++] = (byte) (byte_part_1 << 2 & 0xfc | byte_part_2 >>> 4 & 0x3);
        if (this.base64Encoded[2] != Base64InputStream.PADDING) {
            /* still more base64 data available */
            byte_part_1 = byte_part_2;
            byte_part_2 = Base64InputStream.BASE64MAP[this.base64Encoded[2] & 0xff];
            /* reconstruct next decoded byte */
            this.base64Decoded[this.decodedAvailable++] = (byte) (byte_part_1 << 4 & 0xf0 | byte_part_2 >>> 2 & 0xf);
            if (this.base64Encoded[3] != Base64InputStream.PADDING) {
                /* still more base64 data available */
                byte_part_1 = byte_part_2;
                byte_part_2 = Base64InputStream.BASE64MAP[this.base64Encoded[3] & 0xff];
                this.base64Decoded[this.decodedAvailable++] = (byte) (byte_part_1 << 6 & 0xc0 | byte_part_2 & 0x3f);
            }
        }
    }

    @Override
    public synchronized void mark(final int readlimit) {
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    @Override
    public int read() throws IOException {
        final int next = this.returnDecodedBase64();       
        if (next >= 0) {
            /* we still have decoded data left to return */
            return next;
        }
        /* we have to decode some more data again */
        this.decodeBase64();
        /* return what is possible to return or EOF */
        return this.returnDecodedBase64();
    }

    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
        /* taken from InputStream */
        if (b == null) {
            throw new NullPointerException();
        } else if (off < 0 || len < 0 || len > b.length - off) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) { return 0; }

        int c = this.read();
        if (c == -1) { return -1; }
        int index = 0;
        b[off + index++] = (byte) c;
        for (; index < len; index++) {
            c = this.read();
            if (c == -1) {
                break;
            }
            b[off + index] = (byte) c;
        }
        return index;
    }

    @Override
    public synchronized void reset() throws IOException {
        throw new IOException("mark/reset not supported");
    }

    private int returnDecodedBase64() {
        int next = -1;
        if (this.decodedAvailable > 0) {
            this.decodedAvailable--;
            /*need to mask the byte*/
            next = (int) ((byte) this.base64Decoded[0] & 0xff);
            this.base64Decoded[0] = this.base64Decoded[1];
            this.base64Decoded[1] = this.base64Decoded[2];
        }
        return next;
    }

}
