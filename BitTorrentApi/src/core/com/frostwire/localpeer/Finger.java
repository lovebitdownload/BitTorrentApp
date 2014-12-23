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
 * 
 * @author gubatron
 * @author aldenml
 *
 */
public final class Finger {

    // general data

    public String uuid;

    public String nickname;

    public String frostwireVersion;

    public int totalShared;

    // device data

    public String deviceVersion;

    public String deviceModel;

    public String deviceProduct;

    public String deviceName;

    public String deviceManufacturer;

    public String deviceBrand;

    public ScreenMetrics deviceScreen;

    // shared data

    public int numSharedAudioFiles;

    public int numSharedVideoFiles;

    public int numSharedPictureFiles;

    public int numSharedDocumentFiles;

    public int numSharedApplicationFiles;

    public int numSharedRingtoneFiles;

    // total data

    public int numTotalAudioFiles;

    public int numTotalVideoFiles;

    public int numTotalPictureFiles;

    public int numTotalDocumentFiles;

    public int numTotalApplicationFiles;

    public int numTotalRingtoneFiles;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("(");
        sb.append(nickname + ", " + totalShared + ", " + (deviceScreen != null ? " sc:" + deviceScreen.widthPixels + "x" + deviceScreen.heightPixels : ""));
        sb.append("[");
        sb.append("aud:" + numSharedAudioFiles + "/" + numTotalAudioFiles + ", ");
        sb.append("vid:" + numSharedVideoFiles + "/" + numTotalVideoFiles + ", ");
        sb.append("pic:" + numSharedPictureFiles + "/" + numTotalPictureFiles + ", ");
        sb.append("doc:" + numSharedDocumentFiles + "/" + numTotalDocumentFiles + ", ");
        sb.append("app:" + numSharedApplicationFiles + "/" + numTotalApplicationFiles + ", ");
        sb.append("rng:" + numSharedRingtoneFiles + "/" + numTotalRingtoneFiles);
        sb.append("]");
        sb.append(")");

        return sb.toString();
    }
}
