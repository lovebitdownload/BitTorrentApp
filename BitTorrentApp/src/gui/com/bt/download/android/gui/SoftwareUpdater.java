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

package com.bt.download.android.gui;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.bt.download.android.R;
import com.bt.download.android.core.ConfigurationManager;
import com.bt.download.android.core.Constants;
import com.bt.download.android.core.HttpFetcher;
import com.bt.download.android.core.SystemPaths;
import com.bt.download.android.gui.services.Engine;
import com.bt.download.android.gui.util.OSUtils;
import com.bt.download.android.gui.util.UIUtils;
import com.frostwire.util.StringUtils;
import com.frostwire.util.ByteUtils;
import com.frostwire.util.JsonUtils;
import com.frostwire.uxstats.UXStats;
import com.frostwire.uxstats.UXStatsConf;

/**
 * 
 * @author gubatron
 * @author aldenml
 * 
 */
public final class SoftwareUpdater {

    public interface ConfigurationUpdateListener {
        void onConfigurationUpdate();
    }

    private static final String TAG = "FW.SoftwareUpdater";

    private static final long UPDATE_MESSAGE_TIMEOUT = 30 * 60 * 1000; // 30 minutes

    private static final String UPDATE_ACTION_OTA = "ota";
    private static final String UPDATE_ACTION_MARKET = "market";

    private boolean oldVersion;
    private String latestVersion;
    private Update update;

    private long updateTimestamp;
    private AsyncTask<Void, Void, Boolean> updateTask;

    private final Set<ConfigurationUpdateListener> configurationUpdateListeners;

    private static SoftwareUpdater instance;

    public static SoftwareUpdater instance() {
        if (instance == null) {
            instance = new SoftwareUpdater();
        }
        return instance;
    }

    private SoftwareUpdater() {
        this.oldVersion = false;
        this.latestVersion = Constants.FROSTWIRE_VERSION_STRING;
        this.configurationUpdateListeners = new HashSet<ConfigurationUpdateListener>();
    }

    public boolean isOldVersion() {
        return oldVersion;
    }

    public String getLatestVersion() {
        return latestVersion;
    }

    public void checkForUpdate(final Context context) {
        long now = System.currentTimeMillis();

        if (now - updateTimestamp < UPDATE_MESSAGE_TIMEOUT) {
            return;
        }

        updateTimestamp = now;
        updateTask = new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... params) {
                try {
                    byte[] jsonBytes = new HttpFetcher(Constants.SERVER_UPDATE_URL).fetch();
                    update = JsonUtils.toObject(new String(jsonBytes), Update.class);

                    latestVersion = update.v;
                    String[] latestVersionArr = latestVersion.split("\\.");

                    // lv = latest version
                    byte[] lv = new byte[] { Byte.valueOf(latestVersionArr[0]), Byte.valueOf(latestVersionArr[1]), Byte.valueOf(latestVersionArr[2]) };

                    // mv = my version
                    byte[] mv = Constants.FROSTWIRE_VERSION;

                    oldVersion = isFrostWireOld(mv, lv);

                    updateConfiguration(update);

                    if (oldVersion) {
                        if (update.a == null) {
                            update.a = UPDATE_ACTION_OTA; // make it the old behavior
                        }

                        if (update.a.equals(UPDATE_ACTION_OTA)) {
                            // did we download the newest already?
                            if (downloadedLatestFrostWire(update.md5)) {
                                return true;
                            }
                            // didn't download it? go get it now
                            else {
                                new HttpFetcher(update.u).save(SystemPaths.getUpdateApk());

                                if (downloadedLatestFrostWire(update.md5)) {
                                    return true;
                                }
                            }
                        } else if (update.a.equals(UPDATE_ACTION_MARKET)) {
                            return update.m != null;
                        }
                    }

                } catch (Throwable e) {
                    Log.e(TAG, "Failed to check/retrieve/update the update information", e);
                }

                return false;
            }

            @Override
            protected void onPostExecute(Boolean result) {
                if (result && !isCancelled()) {
                    notifyUpdate(context);
                }

                //nav menu always needs to be updated after we read the config.
                notifyConfigurationUpdateListeners();
            }
        };

        updateTask.execute();
    }

    public void addConfigurationUpdateListener(ConfigurationUpdateListener listener) {
        try {
            configurationUpdateListeners.add(listener);
        } catch (Throwable t) {

        }
    }

    private void notifyUpdate(final Context context) {
        try {
            if (update.a == null) {
                update.a = UPDATE_ACTION_OTA; // make it the old behavior
            }

            if (update.a.equals(UPDATE_ACTION_OTA)) {
                if (!SystemPaths.getUpdateApk().exists()) {
                    return;
                }

                String message = StringUtils.getLocaleString(update.updateMessages, context.getString(R.string.update_message));

                UIUtils.showYesNoDialog(context, R.drawable.app_icon, message, R.string.update_title, new OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Engine.instance().stopServices(false);
                        UIUtils.openFile(context, SystemPaths.getUpdateApk().getAbsolutePath(), Constants.MIME_TYPE_ANDROID_PACKAGE_ARCHIVE);
                    }
                });
            } else if (update.a.equals(UPDATE_ACTION_MARKET)) {

                String message = StringUtils.getLocaleString(update.marketMessages, context.getString(R.string.update_message));

                UIUtils.showYesNoDialog(context, R.drawable.app_icon, message, R.string.update_title, new OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse(update.m));
                        context.startActivity(intent);
                    }
                });
            }
        } catch (Throwable e) {
            Log.e(TAG, "Failed to notify update", e);
        }
    }

    /**
     * 
     * @param md5
     *            - Expected MD5 hash as a string.
     * @return
     */
    private boolean downloadedLatestFrostWire(String md5) {
        if (!SystemPaths.getUpdateApk().exists()) {
            return false;
        }
        return checkMD5(SystemPaths.getUpdateApk(), md5);
    }

    /**
     * mv = my version
     * lv = latest version
     * 
     * returns true if mv is older.
     */
    private boolean isFrostWireOld(byte[] mv, byte[] lv) {
        if (mv[0] < lv[0]) {
            return true;
        }

        if (mv[0] == lv[0] && mv[1] < lv[1]) {
            return true;
        }

        if (mv[0] == lv[0] && mv[1] == lv[1] && mv[2] < lv[2]) {
            return true;
        }

        return false;
    }

    private static String getMD5(File f) {
        try {
            MessageDigest m = MessageDigest.getInstance("MD5");

            // We read the file in buffers so we don't
            // eat all the memory in case we have a huge plugin.
            byte[] buf = new byte[65536];
            int num_read;

            InputStream in = new BufferedInputStream(new FileInputStream(f));

            while ((num_read = in.read(buf)) != -1) {
                m.update(buf, 0, num_read);
            }

            in.close();

            String result = new BigInteger(1, m.digest()).toString(16);

            // pad with zeros if until it's 32 chars long.
            if (result.length() < 32) {
                int paddingSize = 32 - result.length();
                for (int i = 0; i < paddingSize; i++) {
                    result = "0" + result;
                }
            }

            return result;
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean checkMD5(File f, String expectedMD5) {
        if (expectedMD5 == null) {
            return false;
        }

        if (expectedMD5.length() != 32) {
            return false;
        }

        String checkedMD5 = getMD5(f);
        if (checkedMD5 == null) {
            return false;
        }

        return checkedMD5.trim().equalsIgnoreCase(expectedMD5.trim());
    }

    private void updateConfiguration(Update update) {
        if (update.config == null) {
            return;
        }

        ConfigurationManager.instance().setBoolean(Constants.PREF_KEY_GUI_SUPPORT_FROSTWIRE_THRESHOLD, ByteUtils.randomInt(0, 100) < update.config.supportThreshold);

        if (update.config.activeSearchEngines != null && update.config.activeSearchEngines.keySet() != null) {
            for (String name : update.config.activeSearchEngines.keySet()) {
                SearchEngine engine = SearchEngine.forName(name);
                if (engine != null) {
                    engine.setActive(update.config.activeSearchEngines.get(name));
                }
            }
        }

        ConfigurationManager.instance().setBoolean(Constants.PREF_KEY_GUI_SHOW_TV_MENU_ITEM, update.config.tv);
        ConfigurationManager.instance().setBoolean(Constants.PREF_KEY_GUI_INITIALIZE_OFFERCAST_LOCKSCREEN, update.config.offercastLockScreen);
        ConfigurationManager.instance().setBoolean(Constants.PREF_KEY_GUI_INITIALIZE_APPIA, update.config.appia);
        ConfigurationManager.instance().setBoolean(Constants.PREF_KEY_GUI_USE_APPIA_SEARCH, update.config.appiaSearch);

        if (update.config.uxEnabled && ConfigurationManager.instance().getBoolean(Constants.PREF_KEY_UXSTATS_ENABLED)) {
            String url = "http://ux.frostwire.com/aux";
            String os = OSUtils.getOSVersionString();
            String fwversion = Constants.FROSTWIRE_VERSION_STRING;
            String fwbuild = Constants.FROSTWIRE_BUILD;
            int period = update.config.uxPeriod;
            int minEntries = update.config.uxMinEntries;
            int maxEntries = update.config.uxMaxEntries;

            UXStatsConf context = new UXStatsConf(url, os, fwversion, fwbuild, period, minEntries, maxEntries);
            UXStats.instance().setContext(context);
        }
    }

    private void notifyConfigurationUpdateListeners() {
        for (ConfigurationUpdateListener listener : configurationUpdateListeners) {
            try {
                listener.onConfigurationUpdate();
            } catch (Throwable t) {

            }
        }
    }

    private static class Update {
        public String v;
        public String u;
        public String md5;

        /**
         * Address from the market
         */
        public String m;

        /**
         * Action for the update message
         * - "ota" is download from 'u' (a regular http)
         * - "market" go to market page 
         */
        public String a;

        public Map<String, String> updateMessages;
        public Map<String, String> marketMessages;
        public Config config;
    }

    private static class Config {
        public int supportThreshold = 100;
        public Map<String, Boolean> activeSearchEngines;
        public boolean tv = true;
        public boolean appia = true;
        public boolean appiaSearch = true;
        public boolean offercastLockScreen = true;

        // ux stats
        public boolean uxEnabled = false;
        public int uxPeriod = 3600;
        public int uxMinEntries = 10;
        public int uxMaxEntries = 10000;
    }

    public void removeConfigurationUpdateListener(Object slideMenuFragment) {
        if (configurationUpdateListeners.size() > 0) {
            configurationUpdateListeners.remove(slideMenuFragment);
        }
    }
}
