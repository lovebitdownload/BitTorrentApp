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

package com.bt.download.android.gui.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;

import com.bt.download.android.R;
import com.bt.download.android.core.ConfigurationManager;
import com.bt.download.android.core.Constants;
import com.bt.download.android.gui.views.AbstractDialog;
import com.bt.download.android.gui.views.ClickAdapter;

/**
 * @author gubatron
 * @author aldenml
 *
 */
public class ShareIndicationDialog extends AbstractDialog {

    public static final String TAG = "share_indication_dialog";

    private static final String CHECK_SHOW_STATE_KEY = "check_show_state";

    private CheckBox checkShow;
    private Button buttonDone;

    public ShareIndicationDialog() {
        super(TAG, R.layout.dialog_share_indication);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null) {
            checkShow.setChecked(savedInstanceState.getBoolean(CHECK_SHOW_STATE_KEY, ConfigurationManager.instance().getBoolean(Constants.PREF_KEY_GUI_SHOW_SHARE_INDICATION)));
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(CHECK_SHOW_STATE_KEY, checkShow.isChecked());
    }

    @Override
    protected void setContentView(Dialog dlg, int layoutResId) {
        dlg.requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.setContentView(dlg, layoutResId);
    }

    @Override
    protected void initComponents(Dialog dlg, Bundle savedInstanceState) {
        checkShow = findView(dlg, R.id.dialog_share_indicator_check_show);
        checkShow.setChecked(ConfigurationManager.instance().getBoolean(Constants.PREF_KEY_GUI_SHOW_SHARE_INDICATION));

        buttonDone = findView(dlg, R.id.dialog_share_indicator_button_done);
        buttonDone.setOnClickListener(new DoneListener(this));
    }

    private static final class DoneListener extends ClickAdapter<ShareIndicationDialog> {

        public DoneListener(ShareIndicationDialog dlg) {
            super(dlg);
        }

        @Override
        public void onClick(ShareIndicationDialog dlg, View v) {
            ConfigurationManager.instance().setBoolean(Constants.PREF_KEY_GUI_SHOW_SHARE_INDICATION, dlg.checkShow.isChecked());
            dlg.dismiss();
        }
    }
}
