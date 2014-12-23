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

package com.frostwire.bittorrent;

import com.frostwire.jlibtorrent.*;
import com.frostwire.jlibtorrent.alerts.*;
import com.frostwire.jlibtorrent.swig.entry;
import com.frostwire.jlibtorrent.swig.string_entry_map;
import com.frostwire.jlibtorrent.swig.string_vector;
import com.frostwire.logging.Logger;
import com.frostwire.transfers.BittorrentDownload;
import com.frostwire.transfers.TransferItem;
import com.frostwire.transfers.TransferState;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.util.*;

/**
 * @author gubatron
 * @author aldenml
 */
public final class BTDownload extends TorrentAlertAdapter implements BittorrentDownload {

    private static final Logger LOG = Logger.getLogger(BTDownload.class);

    private static final long SAVE_RESUME_RESOLUTION_MILLIS = 10000;

    private static final int[] ALERT_TYPES = {AlertType.TORRENT_PRIORITIZE.getSwig(),
            AlertType.TORRENT_FINISHED.getSwig(),
            AlertType.TORRENT_REMOVED.getSwig(), AlertType.SAVE_RESUME_DATA.getSwig()};

    public static final String WAS_PAUSED_EXTRA_KEY = "was_paused";

    private final BTEngine engine;
    private final TorrentHandle th;
    private final File savePath;
    private final Date created;

    private final Map<String, String> extra;

    private BTDownloadListener listener;

    private Set<File> incompleteFilesToRemove;

    private long lastSaveResumeTime;

    public BTDownload(BTEngine engine, TorrentHandle th) {
        super(th);
        this.engine = engine;
        this.th = th;
        this.savePath = new File(th.getSavePath());
        this.created = new Date(th.getStatus().getAddedTime());

        this.extra = createExtra();

        engine.getSession().addListener(this);
    }

    public Map<String, String> getExtra() {
        return extra;
    }

    @Override
    public String getName() {
        return th.getName();
    }

    @Override
    public String getDisplayName() {
        Priority[] priorities = th.getFilePriorities();

        int count = 0;
        int index = 0;
        for (int i = 0; i < priorities.length; i++) {
            if (!Priority.IGNORE.equals(priorities[i])) {
                count++;
                index = i;
            }
        }

        return count != 1 ? th.getName() : FilenameUtils.getName(th.getTorrentInfo().getFileAt(index).getPath());
    }

    public long getSize() {
        TorrentInfo ti = th.getTorrentInfo();
        return ti != null ? ti.getTotalSize() : 0;
    }

    public boolean isPaused() {
        return th.getStatus().isPaused();
    }

    public boolean isSeeding() {
        return th.getStatus().isSeeding();
    }

    public boolean isFinished() {
        return th.getStatus().isFinished();
    }

    @Override
    public boolean isDownloading() {
        return getDownloadSpeed() > 0;
    }

    @Override
    public boolean isUploading() {
        return getUploadSpeed() > 0;
    }

    public TransferState getState() {
        if (!engine.isStarted()) {
            return TransferState.STOPPED;
        }

        if (engine.isPaused()) {
            return TransferState.PAUSED;
        }

        if (!th.isValid()) {
            return TransferState.ERROR;
        }

        final TorrentStatus status = th.getStatus();

        if (status.isPaused()) {
            return TransferState.PAUSED;
        }

        if (status.isFinished()) { // see the docs of isFinished
            return TransferState.SEEDING;
        }

        final TorrentStatus.State state = status.getState();

        switch (state) {
            case QUEUED_FOR_CHECKING:
                return TransferState.QUEUED_FOR_CHECKING;
            case CHECKING_FILES:
                return TransferState.CHECKING;
            case DOWNLOADING_METADATA:
                return TransferState.DOWNLOADING_METADATA;
            case DOWNLOADING:
                return TransferState.DOWNLOADING;
            case FINISHED:
                return TransferState.FINISHED;
            case SEEDING:
                return TransferState.SEEDING;
            case ALLOCATING:
                return TransferState.ALLOCATING;
            case CHECKING_RESUME_DATA:
                return TransferState.CHECKING;
            case UNKNOWN:
                return TransferState.UNKNOWN;
            default:
                return TransferState.UNKNOWN;
        }
    }

    @Override
    public File getSavePath() {
        return savePath;
    }

    @Override
    public int getProgress() {
        float fp = th.getStatus().getProgress();

        if (Float.compare(fp, 1f) == 0) {
            return 100;
        }

        int p = (int) (th.getStatus().getProgress() * 100);
        return Math.min(p, 100);
    }

    @Override
    public boolean isComplete() {
        return getProgress() == 100;
    }

    public long getBytesReceived() {
        return th.getStatus().getTotalDownload();
    }

    public long getTotalBytesReceived() {
        return th.getStatus().getAllTimeDownload();
    }

    public long getBytesSent() {
        return th.getStatus().getTotalUpload();
    }

    public long getTotalBytesSent() {
        return th.getStatus().getAllTimeUpload();
    }

    public long getDownloadSpeed() {
        return (isFinished() || isPaused() || isSeeding()) ? 0 : th.getStatus().getDownloadPayloadRate();
    }

    public long getUploadSpeed() {
        return th.getStatus().getUploadPayloadRate();
    }

    public int getConnectedPeers() {
        return th.getStatus().getNumPeers();
    }

    public int getTotalPeers() {
        return th.getStatus().getListPeers();
    }

    public int getConnectedSeeds() {
        return th.getStatus().getNumSeeds();
    }

    public int getTotalSeeds() {
        return th.getStatus().getListSeeds();
    }

    public String getInfoHash() {
        return th.getInfoHash().toString();
    }

    @Override
    public Date getCreated() {
        return created;
    }

    public long getETA() {
        TorrentInfo ti = th.getTorrentInfo();
        if (ti == null) {
            return 0;
        }

        TorrentStatus status = th.getStatus();
        long left = ti.getTotalSize() - status.getTotalDone();
        long rate = status.getDownloadPayloadRate();

        if (left <= 0) {
            return 0;
        }

        if (rate <= 0) {
            return -1;
        }

        return left / rate;
    }

    public void pause() {
        extra.put(WAS_PAUSED_EXTRA_KEY, Boolean.TRUE.toString());

        th.setAutoManaged(false);
        th.pause();
        th.saveResumeData();
    }

    public void resume() {
        extra.put(WAS_PAUSED_EXTRA_KEY, Boolean.FALSE.toString());

        th.setAutoManaged(true);
        th.resume();
        th.saveResumeData();
    }

    public void remove() {
        remove(false, false);
    }

    @Override
    public void remove(boolean deleteData) {
        remove(false, deleteData);
    }

    public void remove(boolean deleteTorrent, boolean deleteData) {
        String infoHash = this.getInfoHash();

        Session s = engine.getSession();

        incompleteFilesToRemove = getIncompleteFiles(true);

        if (th.isValid()) {
            if (deleteData) {
                s.removeTorrent(th, Session.Options.DELETE_FILES);
            } else {
                s.removeTorrent(th);
            }
        }

        if (deleteTorrent) {
            File torrent = engine.readTorrentPath(infoHash);
            if (torrent != null && torrent.exists()) {
                torrent.delete();
            }
        }

        engine.resumeDataFile(infoHash).delete();
        engine.resumeTorrentFile(infoHash).delete();
    }

    public BTDownloadListener getListener() {
        return listener;
    }

    public void setListener(BTDownloadListener listener) {
        this.listener = listener;
    }

    @Override
    public int[] types() {
        return ALERT_TYPES;
    }

    @Override
    public void torrentPrioritize(TorrentPrioritizeAlert alert) {
        if (listener != null) {
            try {
                listener.update(this);
            } catch (Throwable e) {
                LOG.error("Error calling listener", e);
            }
        }
        resume();
    }

    @Override
    public void torrentFinished(TorrentFinishedAlert alert) {
        if (listener != null) {
            try {
                listener.finished(this);
            } catch (Throwable e) {
                LOG.error("Error calling listener", e);
            }
        }
    }

    @Override
    public void torrentRemoved(TorrentRemovedAlert alert) {
        engine.getSession().removeListener(this);
        fireRemoved(incompleteFilesToRemove);
    }

    @Override
    public void saveResumeData(SaveResumeDataAlert alert) {
        long now = System.currentTimeMillis();
        if ((now - lastSaveResumeTime) >= SAVE_RESUME_RESOLUTION_MILLIS) {
            lastSaveResumeTime = now;
        } else {
            // skip, too fast, see SAVE_RESUME_RESOLUTION_MILLIS
            return;
        }

        try {
            TorrentHandle th = alert.getHandle();
            if (th.isValid()) {
                String infoHash = th.getInfoHash().toString();
                File file = engine.resumeDataFile(infoHash);

                Entry e = alert.getResumeData();
                e.getSwig().dict().set("extra_data", Entry.fromMap(extra).getSwig());

                FileUtils.writeByteArrayToFile(file, e.bencode());
            }
        } catch (Throwable e) {
            LOG.warn("Error saving resume data", e);
        }
    }

    public boolean isPartial() {
        Priority[] priorities = th.getFilePriorities();

        for (Priority p : priorities) {
            if (Priority.IGNORE.equals(p)) {
                return true;
            }
        }

        return false;
    }

    public String makeMagnetUri() {
        return th.makeMagnetUri();
    }

    public int getDownloadRateLimit() {
        return th.getDownloadLimit();
    }

    public void setDownloadRateLimit(int limit) {
        th.setDownloadLimit(limit);
        th.saveResumeData();
    }

    public int getUploadRateLimit() {
        return th.getUploadLimit();
    }

    public void setUploadRateLimit(int limit) {
        th.setUploadLimit(limit);
        th.saveResumeData();
    }

    public void requestTrackerAnnounce() {
        th.forceReannounce();
    }

    public void requestTrackerScrape() {
        th.scrapeTracker();
    }

    public Set<String> getTrackers() {
        List<AnnounceEntry> trackers = th.getTrackers();

        Set<String> urls = new HashSet<String>(trackers.size());

        for (AnnounceEntry e : trackers) {
            urls.add(e.getUrl());
        }

        return urls;
    }

    public void setTrackers(Set<String> trackers) {
        List<AnnounceEntry> list = new ArrayList<AnnounceEntry>(trackers.size());

        for (String url : trackers) {
            list.add(new AnnounceEntry(url));
        }

        th.replaceTrackers(list);
        th.saveResumeData();
    }

    @Override
    public List<TransferItem> getItems() {
        List<TransferItem> items = Collections.emptyList();

        if (th.isValid()) {
            TorrentInfo ti = th.getTorrentInfo();
            if (ti != null && ti.isValid()) {
                int numFiles = ti.getNumFiles();

                items = new ArrayList<TransferItem>(numFiles);

                for (int i = 0; i < numFiles; i++) {
                    FileEntry fe = ti.getFileAt(i);
                    items.add(new BTDownloadItem(th, i, fe));
                }
            }
        }

        return items;
    }

    public File getTorrentFile() {
        return engine.readTorrentPath(this.getInfoHash());
    }

    public Set<File> getIncompleteFiles() {
        return getIncompleteFiles(false);
    }

    private Set<File> getIncompleteFiles(boolean accurate) {
        Set<File> s = new HashSet<File>();

        try {
            if (!th.isValid()) {
                return s;
            }

            long[] progress = accurate ? th.getFileProgress() : th.getFileProgress(TorrentHandle.FileProgressFlags.PIECE_GRANULARITY);

            TorrentInfo ti = th.getTorrentInfo();
            String prefix = savePath.getAbsolutePath();

            for (int i = 0; i < progress.length; i++) {
                FileEntry fe = ti.getFileAt(i);
                if (progress[i] < fe.getSize()) {
                    s.add(new File(prefix, fe.getPath()));
                }
            }
        } catch (Throwable e) {
            LOG.error("Error calculating the incomplete files set", e);
        }

        return s;
    }

    private Map<String, String> createExtra() {
        Map<String, String> map = new HashMap<String, String>();

        try {
            String infoHash = getInfoHash();
            File file = engine.resumeDataFile(infoHash);

            if (file.exists()) {
                byte[] arr = FileUtils.readFileToByteArray(file);
                entry e = entry.bdecode(Vectors.bytes2char_vector(arr));
                string_entry_map d = e.dict();

                if (d.has_key("extra_data")) {
                    readExtra(d.get("extra_data").dict(), map);
                }
            }

        } catch (Throwable e) {
            LOG.error("Error reading extra data from resume file", e);
        }

        return map;
    }

    private void readExtra(string_entry_map dict, Map<String, String> map) {
        string_vector keys = dict.keys();
        int size = (int) keys.size();
        for (int i = 0; i < size; i++) {
            String k = keys.get(i);
            entry e = dict.get(k);
            if (e.type() == entry.data_type.string_t) {
                map.put(k, e.string());
            }
        }
    }

    public boolean wasPaused() {
        boolean flag = false;
        if (extra.containsKey(WAS_PAUSED_EXTRA_KEY)) {
            try {
                flag = Boolean.parseBoolean(extra.get(WAS_PAUSED_EXTRA_KEY));
            } catch (Throwable e) {
                // ignore
            }
        }

        return flag;
    }

    private void fireRemoved(Set<File> incompleteFiles) {
        if (listener != null) {
            try {
                listener.removed(this, incompleteFiles);
            } catch (Throwable e) {
                LOG.error("Error calling listener", e);
            }
        }
    }
}
