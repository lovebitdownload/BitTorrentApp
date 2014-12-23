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

/**
 * This class helps you to handle binary flags
 * 
 * @author $Author: unknown$
 */
public class BinaryLogic {
    /**
     * Returns true if all flagBitmask are contained in bitmask<br>
     * example:<br>
     * <code>
     * flags: 0001, 1000, 0100<br>
     * status: 1101<br>
     * returns: true
     * </code>
     * 
     * @param bitmask
     * @param flagBitmask
     * @return
     */
    public static boolean containsAll(int bitmask, int... flagBitmask) {
        for (int i : flagBitmask) {
            if ((bitmask & i) == 0) return false;
        }
        return true;
    }

    /**
     * Returns true if bitmask contains non of the flagBitmask<br>
     * example:<br>
     * <code>
     * bitmask: 1001<br>
     * flagBitmask: 0100, 0010<br>
     * returns: true
     * </code>
     * 
     * @param bitmask
     * @param flagBitmask
     * @return
     */
    public static boolean containsNone(int bitmask, int... flagBitmask) {
        for (int i : flagBitmask) {
            if ((bitmask & i) != 0) return false;
        }
        return true;
    }

    /**
     * Returns true if bitmask contains at least one of the flagBitmask
     * 
     * @param bitmask
     * @param flagBitmask
     * @see #containsAll(int, int...)
     * @return
     */
    public static boolean containsSome(int bitmask, int... flagBitmask) {
        for (int i : flagBitmask) {
            if ((bitmask & i) != 0) return true;
        }
        return false;
    }

}
