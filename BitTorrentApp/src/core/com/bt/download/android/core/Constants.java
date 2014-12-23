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

package com.bt.download.android.core;

import com.frostwire.core.CommonConstants;

/**
 * Static class containing all constants in one place.
 * 
 * @author gubatron
 * @author aldenml
 * 
 */
public final class Constants {

    private Constants() {
    }

    public static final byte[] FROSTWIRE_VERSION = { (byte) 1, (byte) 4, (byte) 1 };

    public static final String FROSTWIRE_VERSION_STRING = FROSTWIRE_VERSION[0] + "." + FROSTWIRE_VERSION[1] + "." + FROSTWIRE_VERSION[2];

    public static final String TORRENTCOW_VERSION = "1.1.0";

    /** should manually match the manifest, here for convenience so we can ask for it from static contexts without
     * needing to pass the Android app context to obtain the PackageManager instance.  */
    public static final String FROSTWIRE_BUILD = "154";

    public static final boolean IS_AMAZON_DISTRIBUTION = false;

    public static final boolean IS_FREE_DISTRIBUTION = !IS_AMAZON_DISTRIBUTION;

    //for ads
    public static final String ADS_APP_ID = "ID";
    public static final String ADS_SECRET_KEY = "ADS_SECRET_KEY";
    public static final String TAG_LIST = "TAG_LIST";
    public static final String TAG_INTERSTITIAL_WIDGET = "TAG_INTERSTITIAL_WIDGET";
    public static final String TAG_BANNER = "TAG_BANNER";

    /**
     * 65280 - Default LAN bound port
     */
    public static final int GENERIC_LISTENING_PORT = 0xff00;

    // preference keys
    public static final String PREF_KEY_CORE_UUID = "frostwire.prefs.core.uuid";
    public static final String PREF_KEY_CORE_LAST_SEEN_VERSION = "frostwire.prefs.core.last_seen_version";

    public static final String PREF_KEY_NETWORK_USE_UPNP = "froswire.prefs.network.use_upnp";
    public static final String PREF_KEY_NETWORK_USE_MOBILE_DATA = "frostwire.prefs.network.use_mobile_data";
    public static final String PREF_KEY_NETWORK_USE_RANDOM_LISTENING_PORT = "frostwire.prefs.network.use_random_listening_port";
    public static final String PREF_KEY_NETWORK_MAX_CONCURRENT_UPLOADS = "frostwire.prefs.network.max_concurrent_uploads";
    public static final String PREF_KEY_NETWORK_PINGS_INTERVAL = "frostwire.prefs.network.pings_interval";

    public static final String PREF_KEY_TRANSFER_SHARE_FINISHED_DOWNLOADS = "frostwire.prefs.transfer.share_finished_downloads";

    public static final String PREF_KEY_SEARCH_COUNT_DOWNLOAD_FOR_TORRENT_DEEP_SCAN = "frostwire.prefs.search.count_download_for_torrent_deep_scan";
    public static final String PREF_KEY_SEARCH_COUNT_ROUNDS_FOR_TORRENT_DEEP_SCAN = "frostwire.prefs.search.count_rounds_for_torrent_deep_scan";
    public static final String PREF_KEY_SEARCH_INTERVAL_MS_FOR_TORRENT_DEEP_SCAN = "frostwire.prefs.search.interval_ms_for_torrent_deep_scan";
    public static final String PREF_KEY_SEARCH_MIN_SEEDS_FOR_TORRENT_DEEP_SCAN = "frostwire.prefs.search.min_seeds_for_torrent_deep_scan";
    public static final String PREF_KEY_SEARCH_MIN_SEEDS_FOR_TORRENT_RESULT = "frostwire.prefs.search.min_seeds_for_torrent_result";
    public static final String PREF_KEY_SEARCH_MAX_TORRENT_FILES_TO_INDEX = "frostwire.prefs.search.max_torrent_files_to_index";
    public static final String PREF_KEY_SEARCH_FULLTEXT_SEARCH_RESULTS_LIMIT = "frostwire.prefs.search.fulltext_search_results_limit";

    public static final String PREF_KEY_SEARCH_USE_EXTRATORRENT = "frostwire.prefs.search.use_extratorrent";
    public static final String PREF_KEY_SEARCH_USE_MININOVA = "frostwire.prefs.search.use_mininova";
    public static final String PREF_KEY_SEARCH_USE_VERTOR = "frostwire.prefs.search.use_vertor";
    public static final String PREF_KEY_SEARCH_USE_YOUTUBE = "frostwire.prefs.search.use_youtube";
    public static final String PREF_KEY_SEARCH_USE_SOUNDCLOUD = "frostwire.prefs.search.use_soundcloud";
    public static final String PREF_KEY_SEARCH_USE_ARCHIVEORG = "frostwire.prefs.search.use_archiveorg";
    public static final String PREF_KEY_SEARCH_USE_FROSTCLICK = "frostwire.prefs.search.use_frostclick";
    public static final String PREF_KEY_SEARCH_USE_BITSNOOP = "frostwire.prefs.search.use_bitsnoop";
    public static final String PREF_KEY_SEARCH_USE_TORLOCK = "frostwire.prefs.search.use_torlock";
    public static final String PREF_KEY_SEARCH_USE_EZTV = "frostwire.prefs.search.use_eztv";
    public static final String PREF_KEY_SEARCH_USE_APPIA = "frostwire.prefs.search.use_appia";
    public static final String PREF_KEY_SEARCH_USE_TPB = "frostwire.prefs.search.use_tpb";
    public static final String PREF_KEY_SEARCH_USE_MONOVA = "frostwire.prefs.search.use_monova";
    public static final String PREF_KEY_SEARCH_USE_YIFY = "frostwire.prefs.search.use_yify";
    public static final String PREF_KEY_SEARCH_USE_TORRENTSFM = "frostwire.prefs.search.use_torrentsfm";

    public static final String PREF_KEY_SEARCH_PREFERENCE_CATEGORY = "frostwire.prefs.search.preference_category";

    public static final String PREF_KEY_GUI_NICKNAME = "frostwire.prefs.gui.nickname";
    public static final String PREF_KEY_GUI_VIBRATE_ON_FINISHED_DOWNLOAD = "frostwire.prefs.gui.vibrate_on_finished_download";
    public static final String PREF_KEY_GUI_SHOW_SHARE_INDICATION = "frostwire.prefs.gui.show_share_indication";
    public static final String PREF_KEY_GUI_LAST_MEDIA_TYPE_FILTER = "frostwire.prefs.gui.last_media_type_filter";
    public static final String PREF_KEY_GUI_TOS_ACCEPTED = "frostwire.prefs.gui.tos_accepted";
    public static final String PREF_KEY_GUI_INITIAL_SETTINGS_COMPLETE = "frostwire.prefs.gui.initial_settings_complete";
    public static final String PREF_KEY_GUI_SHOW_TRANSFERS_ON_DOWNLOAD_START = "frostwire.prefs.gui.show_transfers_on_download_start";
    public static final String PREF_KEY_GUI_SHOW_NEW_TRANSFER_DIALOG = "frostwire.prefs.gui.show_new_transfer_dialog";
    public static final String PREF_KEY_GUI_SUPPORT_FROSTWIRE = "frostwire.prefs.gui.support_frostwire";
    public static final String PREF_KEY_GUI_SUPPORT_FROSTWIRE_THRESHOLD = "frostwire.prefs.gui.support_frostwire_threshold";
    public static final String PREF_KEY_GUI_SHOW_TV_MENU_ITEM = "frostwire.prefs.gui.show_tv_menu_item";
    public static final String PREF_KEY_GUI_INITIALIZE_OFFERCAST_LOCKSCREEN = "frostwire.prefs.gui.initialize_offercast_lockscreen";
    public static final String PREF_KEY_GUI_INITIALIZE_APPIA = "frostwire.prefs.gui.initialize_appia";
    public static final String PREF_KEY_GUI_USE_APPIA_SEARCH = "frostwire.prefs.gui.use_appia_search";

    public static final String PREF_KEY_TORRENT_MAX_DOWNLOAD_SPEED = "frostwire.prefs.torrent.max_download_speed";
    public static final String PREF_KEY_TORRENT_MAX_UPLOAD_SPEED = "frostwire.prefs.torrent.max_upload_speed";
    public static final String PREF_KEY_TORRENT_MAX_DOWNLOADS = "frostwire.prefs.torrent.max_downloads";
    public static final String PREF_KEY_TORRENT_MAX_UPLOADS = "frostwire.prefs.torrent.max_uploads";
    public static final String PREF_KEY_TORRENT_MAX_TOTAL_CONNECTIONS = "frostwire.prefs.torrent.max_total_connections";
    public static final String PREF_KEY_TORRENT_MAX_PEERS = "frostwire.prefs.torrent.max_peers";
    public static final String PREF_KEY_TORRENT_SEED_FINISHED_TORRENTS = "frostwire.prefs.torrent.seed_finished_torrents";
    public static final String PREF_KEY_TORRENT_SEED_FINISHED_TORRENTS_WIFI_ONLY = "frostwire.prefs.torrent.seed_finished_torrents_wifi_only";

    public static final String PREF_KEY_STORAGE_PATH = "frostwire.prefs.storage.path";

    public static final String PREF_KEY_UXSTATS_ENABLED = "frostwire.prefs.uxstats.enabled";

    public static final String ACTION_OPEN_TORRENT_URL = "android.intent.action.VIEW";
    public static final String ACTION_SHOW_TRANSFERS = "com.bt.download.android.ACTION_SHOW_TRANSFERS";
    public static final String ACTION_MEDIA_PLAYER_PLAY = "com.bt.download.android.ACTION_MEDIA_PLAYER_PLAY";
    public static final String ACTION_MEDIA_PLAYER_STOPPED = "com.bt.download.android.ACTION_MEDIA_PLAYER_STOPPED";
    public static final String ACTION_MEDIA_PLAYER_PAUSED = "com.bt.download.android.ACTION_MEDIA_PLAYER_PAUSED";
    public static final String ACTION_REFRESH_FINGER = "com.bt.download.android.ACTION_REFRESH_FINGER";
    public static final String ACTION_DESKTOP_UPLOAD_REQUEST = "com.bt.download.android.ACTION_DESKTOP_UPLOAD_REQUEST";
    public static final String ACTION_SETTINGS_SELECT_STORAGE = "com.bt.download.android.ACTION_SETTINGS_SELECT_STORAGE";
    public static final String ACTION_NOTIFY_SDCARD_MOUNTED = "com.bt.download.android.ACTION_NOTIFY_SDCARD_MOUNTED";
    public static final String EXTRA_DOWNLOAD_COMPLETE_NOTIFICATION = "com.bt.download.android.EXTRA_DOWNLOAD_COMPLETE_NOTIFICATION";
    public static final String EXTRA_DOWNLOAD_COMPLETE_PATH = "com.bt.download.android.EXTRA_DOWNLOAD_COMPLETE_PATH";
    public static final String EXTRA_PEER_UUID = "com.bt.download.android.EXTRA_PEER_UUID";
    public static final String EXTRA_DESKTOP_UPLOAD_REQUEST_TOKEN = "com.bt.download.android.EXTRA_DESKTOP_UPLOAD_REQUEST_TOKEN";

    public static final String BROWSE_PEER_FRAGMENT_LISTVIEW_FIRST_VISIBLE_POSITION = "com.bt.download.android.BROWSE_PEER_FRAGMENT_LISTVIEW_FIRST_VISIBLE_POSITION.";

    public static final int NOTIFICATION_MEDIA_PLAYING_ID = 1000;
    public static final int NOTIFICATION_DOWNLOAD_TRANSFER_FINISHED = 1001;
    public static final int NOTIFICATION_MEDIA_PAUSED_ID = 1002;

    // generic file types
    public static final byte FILE_TYPE_AUDIO = CommonConstants.FILE_TYPE_AUDIO;
    public static final byte FILE_TYPE_PICTURES = CommonConstants.FILE_TYPE_PICTURES;
    public static final byte FILE_TYPE_VIDEOS = CommonConstants.FILE_TYPE_VIDEOS;
    public static final byte FILE_TYPE_DOCUMENTS = CommonConstants.FILE_TYPE_DOCUMENTS;
    public static final byte FILE_TYPE_APPLICATIONS = CommonConstants.FILE_TYPE_APPLICATIONS;
    public static final byte FILE_TYPE_RINGTONES = CommonConstants.FILE_TYPE_RINGTONES;
    public static final byte FILE_TYPE_TORRENTS = CommonConstants.FILE_TYPE_TORRENTS;

    public static final String MIME_TYPE_ANDROID_PACKAGE_ARCHIVE = CommonConstants.MIME_TYPE_ANDROID_PACKAGE_ARCHIVE;

    /**
     * URL where FrostWIre checks for software updates
     */
    public static final String SERVER_UPDATE_URL = "http://update.frostwire.com/android";

    public static final String SERVER_PROMOTIONS_URL = "http://update.frostwire.com/o.php";

    public static final long LIBRARIAN_FILE_COUNT_CACHE_TIMEOUT = 2 * 60 * 1000; // 2 minutes

    public static final int MAX_NUM_DOWNLOAD_CHECKED = 5;

    public static final int MAX_INDEXED_TORRENT_SUB_FILES = 4000;

    public static final int MAX_PEER_HTTP_DOWNLOAD_RETRIES = 3;

    public static final int DEVICE_MAJOR_TYPE_DESKTOP = CommonConstants.DEVICE_MAJOR_TYPE_DESKTOP;
    public static final int DEVICE_MAJOR_TYPE_PHONE = CommonConstants.DEVICE_MAJOR_TYPE_PHONE;
    public static final int DEVICE_MAJOR_TYPE_TABLET = CommonConstants.DEVICE_MAJOR_TYPE_TABLET;

    public static final String BITCOIN_DONATION_URI = "bitcoin:19NzEEocAWydbkm3xEEVu43Ho2JFEYf5Vr?amount=0.0104";
}
