/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011-2013, FrostWire(R). All rights reserved.
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

package com.bt.download.android.gui.adapters.menu;

import android.content.Context;
import android.provider.MediaStore.Audio;
import android.provider.Settings;

import com.bt.download.android.R;
import com.bt.download.android.core.FileDescriptor;
import com.bt.download.android.core.Constants;
import com.bt.download.android.gui.views.MenuAction;

/**
 * @author gubatron
 * @author aldenml
 *
 */
public class SetAsRingtoneMenuAction extends MenuAction {

    private final FileDescriptor fd;

    public SetAsRingtoneMenuAction(Context context, FileDescriptor fd) {
        super(context, R.drawable.contextmenu_icon_ringtone, R.string.set_as_ringtone);

        this.fd = fd;
    }

    @Override
    protected void onClick(Context context) {
        String uri = null;

        if (fd.fileType == Constants.FILE_TYPE_RINGTONES) {
            uri = Audio.Media.INTERNAL_CONTENT_URI.toString() + "/" + fd.id;
        } else if (fd.fileType == Constants.FILE_TYPE_AUDIO) {
            uri = Audio.Media.EXTERNAL_CONTENT_URI.toString() + "/" + fd.id;
        }

        if (uri != null) {
            Settings.System.putString(context.getContentResolver(), Settings.System.RINGTONE, uri);
        }
    }
}
