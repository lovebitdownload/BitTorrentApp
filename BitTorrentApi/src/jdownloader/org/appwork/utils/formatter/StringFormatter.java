/**
 * Copyright (c) 2009 - 2010 AppWork UG(haftungsbeschränkt) <e-mail@appwork.org>
 * 
 * This file is part of org.appwork.utils.formatter
 * 
 * This software is licensed under the Artistic License 2.0,
 * see the LICENSE file or http://www.opensource.org/licenses/artistic-license-2.0.php
 * for details
 */
package org.appwork.utils.formatter;

public class StringFormatter {
    /**
     * Fills string pre with char sequence filler until a otal stringlength of
     * num(or more) is reached
     * 
     * @param pre
     * @param num
     * @param filler
     * @return
     */
    public static String fillStart(String pre, int num, String filler) {

        while (pre.length() < num) {
            pre = filler + pre;
        }
        return pre;
    }

    /**
     * Filters all chars in source which are not present in filter
     * 
     * @param source
     * @param filter
     * @return source -filter
     */
    public static String filterString(String source, String filter) {
        if (source == null || filter == null) { return ""; }

        byte[] org = source.getBytes();
        byte[] mask = filter.getBytes();
        byte[] ret = new byte[org.length];
        int count = 0;
        int i;
        for (i = 0; i < org.length; i++) {
            byte letter = org[i];
            for (byte element : mask) {
                if (letter == element) {
                    ret[count] = letter;
                    count++;
                    break;
                }
            }
        }
        return new String(ret).trim();
    }

    public static String fillString(String binaryString, String pre, String post, int length) {
        while (binaryString.length() < length) {
            if (binaryString.length() < length) {
                binaryString = pre + binaryString;
            }
            if (binaryString.length() < length) {
                binaryString = binaryString + post;
            }
        }
        return binaryString;
    }

}
