/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011, 2012, FrostWire(TM). All rights reserved.
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

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.os.AsyncTask;

import com.bt.download.android.R;
import com.bt.download.android.core.FileDescriptor;
import com.bt.download.android.gui.Librarian;
import com.bt.download.android.gui.adapters.FileListAdapter;
import com.bt.download.android.gui.util.UIUtils;
import com.bt.download.android.gui.views.MenuAction;

/**
 * Action to set to Shared all selected, or to set to Unshared all selected files.
 * 
 * @author gubatron
 * @author aldenml
 *
 */
public class SetSharedStateFileGrainedMenuAction extends MenuAction {

    private final FileListAdapter adapter;
    private final List<FileDescriptor> fds;
    private final boolean shared;

    public SetSharedStateFileGrainedMenuAction(Context context, FileListAdapter adapter, List<FileDescriptor> fds, boolean shared) {
        super(context, (shared) ? R.drawable.contextmenu_icon_share : R.drawable.contextmenu_icon_unshare, context.getResources().getString((shared) ? R.string.share_selected_files : R.string.unshare_selected_files) + (fds.size() > 1 ? " (" + fds.size() + ")" : ""));
        this.fds = fds;
        this.adapter = adapter;
        this.shared = shared;
    }

    @Override
    protected void onClick(final Context context) {
        //toggle everybody in memory (fast)
        int size = fds.size();
        if (size == 0) {
            return;
        }
        for (int i = 0; i < size; i++) {
            FileDescriptor fd = fds.get(i);
            fd.shared = shared;
        }

        adapter.notifyDataSetChanged();

        final byte fileType = fds.get(0).fileType;

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                // we pass a copy of the list of FileDescriptor in order to avoid inconsistencies
                Librarian.instance().updateSharedStates(fileType, new ArrayList<FileDescriptor>(fds));
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                try {
                    if (shared) {
                        int numShared = Librarian.instance().getNumFiles(fileType, true);
                        if (numShared > 1) {
                            UIUtils.showLongMessage(context, context.getString(R.string.sharing_num_files, numShared, UIUtils.getFileTypeAsString(context.getResources(), fileType)));
                        }
                    }
                } catch (Throwable e) {
                    // ignore
                }
            }
        }.execute();
    }
}
