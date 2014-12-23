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

package com.bt.download.android.gui.services;

import com.bt.download.android.R;
import com.bt.download.android.core.ConfigurationManager;
import com.bt.download.android.core.Constants;
import com.bt.download.android.core.player.CoreMediaPlayer;
import com.bt.download.android.gui.Librarian;
import com.bt.download.android.gui.PeerManager;
import com.bt.download.android.gui.activities.MainActivity;
import com.bt.download.android.gui.transfers.TransferManager;
import com.frostwire.bittorrent.BTEngine;
import com.frostwire.logging.Logger;
import com.frostwire.util.ThreadPool;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Binder;
import android.os.IBinder;
import android.os.Process;
import android.util.Log;

import java.io.File;
import java.util.concurrent.ExecutorService;

/**
 * @author gubatron
 * @author aldenml
 *
 */
public class EngineService extends Service implements IEngineService {

    private static final Logger LOG = Logger.getLogger(EngineService.class);

    private static final String TAG = "FW.EngineService";

    private final static long[] VENEZUELAN_VIBE = buildVenezuelanVibe();

    private final IBinder binder;

    static final ExecutorService threadPool = ThreadPool.newThreadPool("Engine");

    // services in background

    private final CoreMediaPlayer mediaPlayer;

    private byte state;

    private OnSharedPreferenceChangeListener preferenceListener;

    public EngineService() {
        binder = new EngineServiceBinder();

        mediaPlayer = new ApolloMediaPlayer(this);

        registerPreferencesChangeListener();

        state = STATE_DISCONNECTED;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).cancelAll();

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        LOG.debug("EngineService onDestroy");

        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).cancelAll();
        stopServices(false);

        mediaPlayer.stop();
        mediaPlayer.shutdown();

        BTEngine.getInstance().stop();

        new Thread("shutdown-halt") {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    // ignore
                }
                Process.killProcess(Process.myPid());
            }
        }.start();
    }

    @Override
    public CoreMediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }

    @Override
    public byte getState() {
        return state;
    }

    @Override
    public boolean isStarted() {
        return getState() == STATE_STARTED;
    }

    @Override
    public boolean isStarting() {
        return getState() == STATE_STARTING;
    }

    @Override
    public boolean isStopped() {
        return getState() == STATE_STOPPED;
    }

    @Override
    public boolean isStopping() {
        return getState() == STATE_STOPPING;
    }

    @Override
    public boolean isDisconnected() {
        return getState() == STATE_DISCONNECTED;
    }

    @Override
    public synchronized void startServices() {
        // hard check for TOS
        //        if (!ConfigurationManager.instance().getBoolean(Constants.PREF_KEY_GUI_TOS_ACCEPTED)) {
        //            return;
        //        }

        if (!Librarian.instance().isExternalStorageMounted()) {
            return;
        }

        if (isStarted() || isStarting()) {
            return;
        }

        state = STATE_STARTING;

        Librarian.instance().invalidateCountCache();

        //TransferManager.instance().loadTorrents();

        BTEngine.getInstance().resume();

        PeerManager.instance().clear();

        if (ConfigurationManager.instance().getBoolean(Constants.PREF_KEY_NETWORK_USE_UPNP)) {
            PeerManager.instance().start();
        }

        state = STATE_STARTED;
        Log.v(TAG, "Engine started");
    }

    @Override
    public synchronized void stopServices(boolean disconnected) {
        if (isStopped() || isStopping() || isDisconnected()) {
            return;
        }

        state = STATE_STOPPING;

        BTEngine.getInstance().pause();

        PeerManager.instance().clear();

        try {
            PeerManager.instance().stop();
        } catch (Throwable e) {
            LOG.error("Error stopping peer manager", e);
        }

        state = disconnected ? STATE_DISCONNECTED : STATE_STOPPED;
        Log.v(TAG, "Engine stopped, state: " + state);
    }

    @Override
    public ExecutorService getThreadPool() {
        return threadPool;
    }

    @Override
    public void notifyDownloadFinished(String displayName, File file) {
        try {
            Context context = getApplicationContext();

            Intent i = new Intent(context, MainActivity.class);

            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            i.putExtra(Constants.EXTRA_DOWNLOAD_COMPLETE_NOTIFICATION, true);
            i.putExtra(Constants.EXTRA_DOWNLOAD_COMPLETE_PATH, file.getAbsolutePath());

            PendingIntent pi = PendingIntent.getActivity(context, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);

            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            Notification notification = new Notification(R.drawable.frostwire_notification, getString(R.string.download_finished), System.currentTimeMillis());
            notification.vibrate = ConfigurationManager.instance().vibrateOnFinishedDownload() ? VENEZUELAN_VIBE : null;
            notification.number = TransferManager.instance().getDownloadsToReview();
            notification.flags |= Notification.FLAG_AUTO_CANCEL;
            notification.setLatestEventInfo(context, getString(R.string.download_finished), displayName, pi);
            manager.notify(Constants.NOTIFICATION_DOWNLOAD_TRANSFER_FINISHED, notification);
        } catch (Throwable e) {
            Log.e(TAG, "Error creating notification for download finished", e);
        }
    }

    @Override
    public void shutdown() {
        stopForeground(true);
        stopSelf();
    }

    private void registerPreferencesChangeListener() {
        preferenceListener = new OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if (key.equals(Constants.PREF_KEY_GUI_NICKNAME)) {
                    PeerManager.instance().clear();
                }
            }
        };
        ConfigurationManager.instance().registerOnPreferenceChange(preferenceListener);
    }

    private static long[] buildVenezuelanVibe() {

        long shortVibration = 80;
        long mediumVibration = 100;
        long shortPause = 100;
        long mediumPause = 150;
        long longPause = 180;

        return new long[] { 0, shortVibration, longPause, shortVibration, shortPause, shortVibration, shortPause, shortVibration, mediumPause, mediumVibration };
    }

    public class EngineServiceBinder extends Binder {
        public EngineService getService() {
            return EngineService.this;
        }
    }
}
