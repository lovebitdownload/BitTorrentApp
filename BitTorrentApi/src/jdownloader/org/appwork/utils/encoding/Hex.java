/**
 * Copyright (c) 2009 - 2011 AppWork UG(haftungsbeschränkt) <e-mail@appwork.org>
 * 
 * This file is part of org.appwork.utils.encoding
 * 
 * This software is licensed under the Artistic License 2.0,
 * see the LICENSE file or http://www.opensource.org/licenses/artistic-license-2.0.php
 * for details
 */
package org.appwork.utils.encoding;

import java.io.UnsupportedEncodingException;

/**
 * @author daniel
 * 
 */
public class Hex {

    public static byte[] hex2ByteArray(final String hexString) {
        if (hexString == null) { return null; }

        final int length = hexString.length();
        final byte[] buffer = new byte[(length + 1) / 2];
        boolean evenByte = true;
        byte nextByte = 0;
        int bufferOffset = 0;

        if (length % 2 == 1) {
            evenByte = false;
        }

        for (int i = 0; i < length; i++) {
            final char c = hexString.charAt(i);
            int nibble;

            if (c >= '0' && c <= '9') {
                nibble = c - '0';
            } else if (c >= 'A' && c <= 'F') {
                nibble = c - 'A' + 0x0A;
            } else if (c >= 'a' && c <= 'f') {
                nibble = c - 'a' + 0x0A;
            } else {
                throw new NumberFormatException("Invalid hex digit '" + c + "'.");
            }

            if (evenByte) {
                nextByte = (byte) (nibble << 4);
            } else {
                nextByte += (byte) nibble;
                buffer[bufferOffset++] = nextByte;
            }
            evenByte = !evenByte;
        }
        return buffer;
    }

    public static String hex2String(final String hexString) {
        if (hexString == null) { return null; }
        try {
            return new String(Hex.hex2ByteArray(hexString), "UTF-8");
        } catch (final UnsupportedEncodingException e) {
            return new String(Hex.hex2ByteArray(hexString));
        }
    }

    private Hex() {
    }
}
