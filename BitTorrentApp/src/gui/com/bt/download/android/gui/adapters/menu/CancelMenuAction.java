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

import android.content.Context;
import android.content.DialogInterface;

import com.bt.download.android.R;
import com.bt.download.android.gui.transfers.*;
import com.bt.download.android.gui.util.UIUtils;
import com.bt.download.android.gui.views.MenuAction;
import com.frostwire.uxstats.UXAction;
import com.frostwire.uxstats.UXStats;

/**
 * @author gubatron
 * @author aldenml
 *
 */
public class CancelMenuAction extends MenuAction {

    private final Transfer transfer;
    private final boolean deleteData;

    public CancelMenuAction(Context context, Transfer transfer, boolean deleteData) {
        super(context, R.drawable.contextmenu_icon_stop_transfer, (deleteData) ? R.string.cancel_delete_menu_action : (transfer.isComplete()) ? R.string.clear_complete : R.string.cancel_menu_action);
        this.transfer = transfer;
        this.deleteData = deleteData;
    }

    @Override
    protected void onClick(Context context) {
        int yes_no_cancel_transfer_id = R.string.yes_no_cancel_transfer_question;
        if (transfer instanceof HttpDownload || transfer instanceof YouTubeDownload || transfer instanceof SoundcloudDownload) {
            yes_no_cancel_transfer_id = R.string.yes_no_cancel_transfer_question_cloud;
        }

        UIUtils.showYesNoDialog(context, (deleteData) ? R.string.yes_no_cancel_delete_transfer_question : yes_no_cancel_transfer_id, R.string.cancel_transfer, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                if (transfer instanceof DownloadTransfer) {
                    ((DownloadTransfer) transfer).cancel(deleteData);
                } else {
                    transfer.cancel();
                }
                UXStats.instance().log(UXAction.DOWNLOAD_REMOVE);
            }
        });
    }
}
