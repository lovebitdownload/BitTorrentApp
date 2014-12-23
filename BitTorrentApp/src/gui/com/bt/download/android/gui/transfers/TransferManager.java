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

package com.bt.download.android.gui.transfers;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;

import com.bt.download.android.R;
import com.bt.download.android.core.ConfigurationManager;
import com.bt.download.android.core.Constants;
import com.bt.download.android.core.FileDescriptor;
import com.bt.download.android.gui.NetworkManager;
import com.bt.download.android.gui.Peer;
import com.bt.download.android.gui.services.Engine;
import com.bt.download.android.gui.util.UIUtils;
import com.frostwire.bittorrent.BTDownload;
import com.frostwire.bittorrent.BTEngine;
import com.frostwire.bittorrent.BTEngineAdapter;
import com.frostwire.logging.Logger;
import com.frostwire.search.HttpSearchResult;
import com.frostwire.search.SearchResult;
import com.frostwire.search.soundcloud.SoundcloudSearchResult;
import com.frostwire.search.torrent.TorrentCrawledSearchResult;
import com.frostwire.search.torrent.TorrentSearchResult;
import com.frostwire.search.youtube.YouTubeCrawledSearchResult;
import com.frostwire.util.StringUtils;
import com.frostwire.uxstats.UXAction;
import com.frostwire.uxstats.UXStats;

/**
 * @author gubatron
 * @author aldenml
 *
 */
public final class TransferManager {

    private static final Logger LOG = Logger.getLogger(TransferManager.class);

    private final List<DownloadTransfer> downloads;
    private final List<UploadTransfer> uploads;
    private final List<BittorrentDownload> bittorrentDownloads;

    private int downloadsToReview;

    private final Object alreadyDownloadingMonitor = new Object();

    private volatile static TransferManager instance;

    private OnSharedPreferenceChangeListener preferenceListener;

    public static TransferManager instance() {
        if (instance == null) {
            instance = new TransferManager();
        }
        return instance;
    }

    private TransferManager() {
        registerPreferencesChangeListener();

        this.downloads = new CopyOnWriteArrayList<DownloadTransfer>();
        this.uploads = new CopyOnWriteArrayList<UploadTransfer>();
        this.bittorrentDownloads = new CopyOnWriteArrayList<BittorrentDownload>();

        this.downloadsToReview = 0;

        loadTorrents();
    }

    public List<Transfer> getTransfers() {
        List<Transfer> transfers = new ArrayList<Transfer>();

        if (downloads != null) {
            transfers.addAll(downloads);
        }

        if (uploads != null) {
            transfers.addAll(uploads);
        }

        if (bittorrentDownloads != null) {
            transfers.addAll(bittorrentDownloads);
        }

        return transfers;
    }

    private boolean alreadyDownloading(String detailsUrl) {
        synchronized (alreadyDownloadingMonitor) {
            for (DownloadTransfer dt : downloads) {
                if (dt.isDownloading()) {
                    if (dt.getDetailsUrl() != null && dt.getDetailsUrl().equals(detailsUrl)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isDownloadingTorrentByUri(String uri) {
        synchronized (alreadyDownloadingMonitor) {
            for (DownloadTransfer dt : downloads) {
                if (dt instanceof TorrentFetcherDownload) {
                    String torrentUri = ((TorrentFetcherDownload) dt).getTorrentUri();
                    if (torrentUri != null && torrentUri.equals(uri)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    public DownloadTransfer download(SearchResult sr) {
        DownloadTransfer transfer = null;
        
        if (alreadyDownloading(sr.getDetailsUrl())) {
            transfer = new ExistingDownload();
        }

        if (sr instanceof TorrentSearchResult) {
            transfer = newBittorrentDownload((TorrentSearchResult) sr);
        } else if (sr instanceof HttpSlideSearchResult) {
            transfer = newHttpDownload((HttpSlideSearchResult) sr);
        } else if (sr instanceof YouTubeCrawledSearchResult) {
            transfer = newYouTubeDownload((YouTubeCrawledSearchResult) sr);
        } else if (sr instanceof SoundcloudSearchResult) {
            transfer = newSoundcloudDownload((SoundcloudSearchResult) sr);
        } else if (sr instanceof HttpSearchResult) {
            transfer = newHttpDownload((HttpSearchResult) sr);
        }

        return transfer;
    }

    public DownloadTransfer download(Peer peer, FileDescriptor fd) {
        PeerHttpDownload download = new PeerHttpDownload(this, peer, fd);

        if (alreadyDownloading(download.getDetailsUrl())) {
            return new ExistingDownload();
        }

        downloads.add(download);
        download.start();

        UXStats.instance().log(UXAction.WIFI_SHARING_DOWNLOAD);

        return download;
    }

    public PeerHttpUpload upload(FileDescriptor fd) {
        PeerHttpUpload upload = new PeerHttpUpload(this, fd);
        uploads.add(upload);
        return upload;
    }

    public void clearComplete() {
        List<Transfer> transfers = getTransfers();

        for (Transfer transfer : transfers) {
            if (transfer != null && transfer.isComplete()) {
                if (transfer instanceof BittorrentDownload) {
                    BittorrentDownload bd = (BittorrentDownload) transfer;
                    if (bd != null && bd.isResumable()) {
                        bd.cancel();
                    }
                } else {
                    transfer.cancel();
                }
            }
        }
    }

    public int getActiveDownloads() {
        int count = 0;

        for (BittorrentDownload d : bittorrentDownloads) {
            if (!d.isComplete() && d.isDownloading()) {
                count++;
            }
        }

        for (DownloadTransfer d : downloads) {
            if (!d.isComplete() && d.isDownloading()) {
                count++;
            }
        }

        return count;
    }

    public int getActiveUploads() {
        int count = 0;

        for (BittorrentDownload d : bittorrentDownloads) {
            if (!d.isComplete() && d.isSeeding()) {
                count++;
            }
        }

        for (UploadTransfer u : uploads) {
            if (!u.isComplete() && u.isUploading()) {
                count++;
            }
        }

        return count;
    }

    public long getDownloadsBandwidth() {
        long torrentDownloadsBandwidth = BTEngine.getInstance().getDownloadRate();

        long peerDownloadsBandwidth = 0;
        for (DownloadTransfer d : downloads) {
            peerDownloadsBandwidth += d.getDownloadSpeed() / 1000;
        }

        return torrentDownloadsBandwidth + peerDownloadsBandwidth;
    }

    public double getUploadsBandwidth() {
        long torrentUploadsBandwidth = BTEngine.getInstance().getUploadRate();

        long peerUploadsBandwidth = 0;
        for (UploadTransfer u : uploads) {
            peerUploadsBandwidth += u.getUploadSpeed() / 1000;
        }

        return torrentUploadsBandwidth + peerUploadsBandwidth;
    }

    public int getDownloadsToReview() {
        return downloadsToReview;
    }

    public void incrementDownloadsToReview() {
        downloadsToReview++;
    }

    public void clearDownloadsToReview() {
        downloadsToReview = 0;
    }

    public void stopSeedingTorrents() {
        for (BittorrentDownload d : bittorrentDownloads) {
            if (d.isSeeding() || d.isComplete()) {
                d.pause();
            }
        }
    }

    public void loadTorrents() {
        bittorrentDownloads.clear();

        BTEngine engine = BTEngine.getInstance();

        engine.setListener(new BTEngineAdapter() {
            @Override
            public void downloadAdded(BTEngine engine, BTDownload dl) {
                String name = dl.getName();
                if (name != null && name.contains("fetchMagnet - ")) {
                    return;
                }

                bittorrentDownloads.add(new UIBittorrentDownload(TransferManager.this, dl));
            }
        });

        engine.restoreDownloads();
    }

    boolean remove(Transfer transfer) {
        if (transfer instanceof BittorrentDownload) {
            return bittorrentDownloads.remove(transfer);
        } else if (transfer instanceof DownloadTransfer) {
            return downloads.remove(transfer);
        } else if (transfer instanceof UploadTransfer) {
            return uploads.remove(transfer);
        }

        return false;
    }

    public void pauseTorrents() {
        for (BittorrentDownload d : bittorrentDownloads) {
            d.pause();
        }
    }

    public BittorrentDownload downloadTorrent(String uri) {
        String url = uri.trim();
        try {
            if (url.contains("urn%3Abtih%3A")) {
                //fixes issue #129: over-encoded url coming from intent
                url = url.replace("urn%3Abtih%3A", "urn:btih:");
            }

            URI u = URI.create(url);

            BittorrentDownload download = null;

            if (u.getScheme().equalsIgnoreCase("file")) {
                BTEngine.getInstance().download(new File(u.getPath()), null);
            } else if (u.getScheme().equalsIgnoreCase("http") || u.getScheme().equalsIgnoreCase("magnet")) {
                if (!isDownloadingTorrentByUri(url)) {
                    download = new TorrentFetcherDownload(this, new TorrentUrlInfo(u.toString()));
                    bittorrentDownloads.add(download);
                }
            } else {
                download = new InvalidBittorrentDownload(R.string.torrent_scheme_download_not_supported);
            }

            return download;
        } catch (Throwable e) {
            LOG.warn("Error creating download from uri: " + url);
            return new InvalidBittorrentDownload(R.string.empty_string);
        }
    }

    private static BittorrentDownload createBittorrentDownload(TransferManager manager, TorrentSearchResult sr) {
        if (sr instanceof  TorrentCrawledSearchResult) {
            BTEngine.getInstance().download((TorrentCrawledSearchResult) sr, null);
        } else if (sr.getTorrentUrl() != null) {
            return new TorrentFetcherDownload(manager, new TorrentSearchResultInfo(sr));
        }

        return null;
    }

    private BittorrentDownload newBittorrentDownload(TorrentSearchResult sr) {
        try {
            createBittorrentDownload(this, sr);

            return null;
        } catch (Throwable e) {
            LOG.warn("Error creating download from search result: " + sr);
            return new InvalidBittorrentDownload(R.string.empty_string);
        }
    }

    private HttpDownload newHttpDownload(HttpSlideSearchResult sr) {
        HttpDownload download = new HttpDownload(this, sr.getDownloadLink());

        downloads.add(download);
        download.start();

        return download;
    }

    private DownloadTransfer newYouTubeDownload(YouTubeCrawledSearchResult sr) {
        YouTubeDownload download = new YouTubeDownload(this, sr);

        downloads.add(download);
        download.start();

        return download;
    }

    private DownloadTransfer newSoundcloudDownload(SoundcloudSearchResult sr) {
        SoundcloudDownload download = new SoundcloudDownload(this, sr);

        downloads.add(download);
        download.start();

        return download;
    }

    private DownloadTransfer newHttpDownload(HttpSearchResult sr) {
        HttpDownload download = new HttpDownload(this, new HttpSearchResultDownloadLink(sr));

        downloads.add(download);
        download.start();

        return download;
    }
    
    private boolean isBittorrentDownload(DownloadTransfer transfer) {
        return transfer instanceof UIBittorrentDownload || transfer instanceof TorrentFetcherDownload;
    }

    public boolean isBittorrentDownloadAndMobileDataSavingsOn(DownloadTransfer transfer) {
        return isBittorrentDownload(transfer) && 
                NetworkManager.instance().isDataMobileUp() && 
                !ConfigurationManager.instance().getBoolean(Constants.PREF_KEY_NETWORK_USE_MOBILE_DATA);
    }
    
    public boolean isBittorrentDownloadAndMobileDataSavingsOff(DownloadTransfer transfer) {
        return isBittorrentDownload(transfer) && 
               NetworkManager.instance().isDataMobileUp() && 
               ConfigurationManager.instance().getBoolean(Constants.PREF_KEY_NETWORK_USE_MOBILE_DATA);
    }
    
    public boolean isBittorrentDisconnected(){
       return Engine.instance().isStopped() || Engine.instance().isStopping() || Engine.instance().isDisconnected();
    }
    
    public void resumeResumableTransfers() {
        List<Transfer> transfers = getTransfers();

        for (Transfer t : transfers) {
            if (t instanceof BittorrentDownload) {
                BittorrentDownload bt = (BittorrentDownload) t;
                if (bt.isResumable()) {
                    bt.resume();
                }
            } 
        }        
    }

    /** Stops all HttpDownloads (Cloud and Wi-Fi) */
    public void stopHttpTransfers() {
        List<Transfer> transfers = new ArrayList<Transfer>();
        transfers.addAll(downloads);
        transfers.addAll(uploads);

        for (Transfer t : transfers) {
            if (t instanceof DownloadTransfer) {
                DownloadTransfer d = (DownloadTransfer) t;
                if (!d.isComplete() && d.isDownloading()) {
                    d.cancel();
                }
            } else if (t instanceof UploadTransfer) {
                UploadTransfer u = (UploadTransfer) t;

                if (!u.isComplete() && u.isUploading()) {
                    u.cancel();
                }
            }
        }
    }

    private void registerPreferencesChangeListener() {
        preferenceListener = new OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                BTEngine e = BTEngine.getInstance();

                if (key.equals(Constants.PREF_KEY_TORRENT_MAX_DOWNLOAD_SPEED)) {
                    e.setDownloadSpeedLimit((int) ConfigurationManager.instance().getLong(key));
                } else if (key.equals(Constants.PREF_KEY_TORRENT_MAX_UPLOAD_SPEED)) {
                    e.setUploadSpeedLimit((int) ConfigurationManager.instance().getLong(key));
                } else if (key.equals(Constants.PREF_KEY_TORRENT_MAX_DOWNLOADS)) {
                    e.setMaxActiveDownloads((int) ConfigurationManager.instance().getLong(key));
                } else if (key.equals(Constants.PREF_KEY_TORRENT_MAX_UPLOADS)) {
                    e.setMaxActiveSeeds((int) ConfigurationManager.instance().getLong(key));
                } else if (key.equals(Constants.PREF_KEY_TORRENT_MAX_TOTAL_CONNECTIONS)) {
                    e.setMaxConnections((int) ConfigurationManager.instance().getLong(key));
                } else if (key.equals(Constants.PREF_KEY_TORRENT_MAX_PEERS)) {
                    e.setMaxPeers((int) ConfigurationManager.instance().getLong(key));
                }
            }
        };
        ConfigurationManager.instance().registerOnPreferenceChange(preferenceListener);
    }
}
