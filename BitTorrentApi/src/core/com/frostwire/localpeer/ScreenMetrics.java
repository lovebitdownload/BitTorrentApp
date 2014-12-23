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

package com.frostwire.localpeer;

/**
 * A structure describing general information about the screen,
 * such as its size and density.
 * 
 * @author gubatron
 * @author aldenml
 * 
 */
public class ScreenMetrics {

    /**
     * The screen density expressed as dots-per-inch.
     */
    public int densityDpi;

    /**
     * The absolute height of the display in pixels.
     */
    public int heightPixels;

    /**
     * The absolute width of the display in pixels.
     */
    public int widthPixels;

    /**
     * The exact physical pixels per inch of the screen in the X dimension.
     */
    public float xdpi;

    /**
     * The exact physical pixels per inch of the screen in the Y dimension.
     */
    public float ydpi;
}
