/**
 * Copyright (c) 2009 - 2010 AppWork UG(haftungsbeschränkt) <e-mail@appwork.org>
 * 
 * This file is part of org.appwork.utils
 * 
 * This software is licensed under the Artistic License 2.0,
 * see the LICENSE file or http://www.opensource.org/licenses/artistic-license-2.0.php
 * for details
 */
package org.appwork.utils;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

/**
 * @author $Author: unknown$
 * 
 */
public class Exceptions {
    /**
     * prints the StrackTrace into given StringBuffer
     * 
     */
    public static String getStackTrace(final StringBuilder sb, final Throwable thrown) {
        final Writer sw = new Writer() {
            final int startPos;
            {
                this.lock = sb;
                this.startPos = sb.length();
            }

            @Override
            public Writer append(final char c) throws IOException {
                this.write(c);
                return this;
            }

            @Override
            public Writer append(final CharSequence csq) throws IOException {
                if (csq == null) {
                    this.write("null");
                } else {
                    this.write(csq.toString());
                }
                return this;
            }

            @Override
            public Writer append(final CharSequence csq, final int start, final int end) throws IOException {
                final CharSequence cs = csq == null ? "null" : csq;
                this.write(cs.subSequence(start, end).toString());
                return this;
            }

            @Override
            public void close() throws IOException {
            }

            @Override
            public void flush() throws IOException {
            }

            @Override
            public String toString() {
                return sb.substring(this.startPos);
            }

            @Override
            public void write(final char[] cbuf) throws IOException {
                sb.append(cbuf);
            }

            @Override
            public void write(final char[] cbuf, final int off, final int len) throws IOException {
                if (off < 0 || off > cbuf.length || len < 0 || off + len > cbuf.length || off + len < 0) {
                    throw new IndexOutOfBoundsException();
                } else if (len == 0) { return; }
                sb.append(cbuf, off, len);
            }

            @Override
            public void write(final int c) throws IOException {
                sb.append(c);
            }

            @Override
            public void write(final String str) throws IOException {
                sb.append(str);
            }

            @Override
            public void write(final String str, final int off, final int len) {
                sb.append(str.substring(off, off + len));
            }

        };
        final PrintWriter pw = new PrintWriter(sw);
        thrown.printStackTrace(pw);
        pw.close();
        return "";
    }

    /**
     * returns the Exceptions Stacktrace as String
     * 
     * @param thrown
     * @return
     */
    public static String getStackTrace(final Throwable thrown) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);
        thrown.printStackTrace(pw);
        pw.close();
        return sw.toString();
    }

}
