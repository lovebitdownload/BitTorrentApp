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

package com.bt.download.android.gui.activities.internal;

import com.bt.download.android.R;
import com.bt.download.android.core.ConfigurationManager;
import com.bt.download.android.core.Constants;
import com.bt.download.android.core.FileDescriptor;
import com.bt.download.android.gui.Librarian;
import com.bt.download.android.gui.activities.AudioPlayerActivity;
import com.bt.download.android.gui.activities.MainActivity;
import com.bt.download.android.gui.activities.PreferencesActivity;
import com.bt.download.android.gui.dialogs.ShareIndicationDialog;
import com.bt.download.android.gui.fragments.BrowsePeerFragment;
import com.bt.download.android.gui.fragments.TransfersFragment;
import com.bt.download.android.gui.fragments.TransfersFragment.TransferStatus;
import com.bt.download.android.gui.services.Engine;
import com.bt.download.android.gui.util.UIUtils;
import com.frostwire.logging.Logger;

import android.app.Fragment;
import android.content.Intent;
import android.net.Uri;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * 
 * @author gubatron
 * @author aldenml
 *
 */
public final class MainController {

    private static final Logger LOG = Logger.getLogger(MainController.class);

    private final MainActivity activity;

    public MainController(MainActivity activity) {
        this.activity = activity;
    }

    public MainActivity getActivity() {
        return activity;
    }

    public void closeSlideMenu() {
        activity.closeSlideMenu();
    }

    public void switchFragment(int itemId) {
        Fragment fragment = activity.getFragmentByMenuId(itemId);
        if (fragment != null) {
            activity.switchContent(fragment);
        }
    }

    public void showPreferences() {
        Intent i = new Intent(activity, PreferencesActivity.class);
        activity.startActivity(i);
    }

    /**
     * Will try to launch the app, if it cannot find the launch intent, it'll take the user to the Android market.
     */
//    public void launchFrostWireTV() {
//        Intent intent = null;
//        try {
//            intent = activity.getApplicationContext().getPackageManager().getLaunchIntentForPackage("com.bt.download.android.tv");
//
//            //on the nexus it wasn't throwing the NameNotFoundException, it was just returning null
//            if (intent == null) {
//                throw new NullPointerException();
//            }
//        } catch (Throwable t) {
//            intent = new Intent();
//            intent.setData(Uri.parse("market://details?id=com.bt.download.android.tv"));
//        }
//        activity.startActivity(intent);
//    }

    public void showTransfers(TransferStatus status) {
        if (!(activity.getCurrentFragment() instanceof TransfersFragment)) {
            TransfersFragment fragment = (TransfersFragment) activity.getFragmentByMenuId(R.id.menu_main_transfers);
            fragment.selectStatusTab(status);
            switchFragment(R.id.menu_main_transfers);
        }
    }

    public void shutdown() {
        Intent i = new Intent(activity, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        i.putExtra("shutdown-" + ConfigurationManager.instance().getUUIDString(), true);
        activity.startActivity(i);
    }

    public void showMyFiles() {
        if (!(activity.getCurrentFragment() instanceof BrowsePeerFragment)) {
            switchFragment(R.id.menu_main_library);
        }
        if (ConfigurationManager.instance().getBoolean(Constants.PREF_KEY_GUI_SHOW_SHARE_INDICATION)) {
            ShareIndicationDialog dlg = new ShareIndicationDialog();
            dlg.show(activity.getFragmentManager());
        }
    }

//    public void startWizardActivity() {
//        Intent i = new Intent(activity, WizardActivity.class);
//        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
//        activity.startActivity(i);
//    }

    public void launchPlayerActivity() {
        if (Engine.instance().getMediaPlayer().getCurrentFD() != null) {
            Intent i = new Intent(activity, AudioPlayerActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(i);
        }
    }

    public void handleSendAction(Intent intent) {
        String action = intent.getAction();
        if (action.equals(Intent.ACTION_SEND)) {
            handleSendSingleFile(intent);
        } else if (action.equals(Intent.ACTION_SEND_MULTIPLE)) {
            handleSendMultipleFiles(intent);
        }
    }

    private void handleSendMultipleFiles(Intent intent) {
        ArrayList<Uri> fileUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
        if (fileUris != null) {
            for (Uri uri : fileUris) {
                shareFileByUri(uri);
            }
            UIUtils.showLongMessage(activity, activity.getString(R.string.n_files_shared, fileUris.size()));
        }
    }

    private void handleSendSingleFile(Intent intent) {
        Uri uri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if (uri == null) {
            return;
        }

        try {
            shareFileByUri(uri);
            UIUtils.showLongMessage(activity, R.string.one_file_shared);
        } catch (Throwable t) {
            UIUtils.showLongMessage(activity, R.string.couldnt_share_file);
        }
    }

    private void shareFileByUri(Uri uri) {
        if (uri == null) {
            return;
        }

        FileDescriptor fileDescriptor = Librarian.instance().getFileDescriptor(uri);

        if (fileDescriptor != null) {
            fileDescriptor.shared = true;
            Librarian.instance().updateSharedStates(fileDescriptor.fileType, Arrays.asList(fileDescriptor));
        }
    }
}
