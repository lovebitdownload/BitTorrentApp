/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011-2014, FrostWire(R). All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.frostwire.util;

/**
 * 
 * @author gubatron
 * @author aldenml
 *
 */
public final class Condition {

    private Condition() {
    }

    /**
     * Useful to shorten long "or" boolean expressions.
     * @param needle
     * @param args
     * @return true if needle is in any of the args.
     */
    public static <T> boolean in(T needle, T... args) {
        if (args == null) {
            throw new IllegalArgumentException("args on in operation can't be null");
        }

        for (T t : args) {
            if (t != null && t.equals(needle)) {
                return true;
            }
        }

        return false;
    }
}