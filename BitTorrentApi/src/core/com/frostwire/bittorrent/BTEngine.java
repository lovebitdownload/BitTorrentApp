package com.frostwire.bittorrent;

import com.frostwire.jlibtorrent.*;
import com.frostwire.jlibtorrent.alerts.Alert;
import com.frostwire.jlibtorrent.alerts.AlertType;
import com.frostwire.jlibtorrent.alerts.TorrentAlert;
import com.frostwire.jlibtorrent.swig.entry;
import com.frostwire.logging.Logger;
import com.frostwire.search.torrent.TorrentCrawledSearchResult;
import com.frostwire.util.OSUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import static com.frostwire.jlibtorrent.alerts.AlertType.*;

/**
 * @author gubatron
 * @author aldenml
 */
public final class BTEngine {

    private static final int[] INNER_LISTENER_TYPES = new int[]{TORRENT_ADDED.getSwig(),
            PIECE_FINISHED.getSwig(),
            PORTMAP.getSwig(),
            PORTMAP_ERROR.getSwig()};

    private static final Logger LOG = Logger.getLogger(BTEngine.class);

    private static final String TORRENT_ORIG_PATH_KEY = "torrent_orig_path";

    public static BTContext ctx;

    private final ReentrantLock sync;
    private final InnerListener innerListener;

    private Session session;
    private Downloader downloader;
    private SessionSettings defaultSettings;

    private boolean firewalled;
    private BTEngineListener listener;

    private BTEngine() {
        this.sync = new ReentrantLock();
        this.innerListener = new InnerListener();
    }

    private static class Loader {
        static BTEngine INSTANCE = new BTEngine();
    }

    public static BTEngine getInstance() {
        if (ctx == null) {
            throw new IllegalStateException("Context can't be null");
        }
        return Loader.INSTANCE;
    }

    public Session getSession() {
        return session;
    }

    private SessionSettings getSettings() {
        if (session == null) {
            return null;
        }

        return session.getSettings();
    }

    public BTEngineListener getListener() {
        return listener;
    }

    public void setListener(BTEngineListener listener) {
        this.listener = listener;
    }

    public boolean isFirewalled() {
        return firewalled;
    }

    public long getDownloadRate() {
        if (session == null) {
            return 0;
        }

        return session.getStatus().getPayloadDownloadRate();
    }

    public long getUploadRate() {
        if (session == null) {
            return 0;
        }

        return session.getStatus().getPayloadUploadRate();
    }

    public long getTotalDownload() {
        if (session == null) {
            return 0;
        }

        return session.getStatus().getTotalDownload();
    }

    public long getTotalUpload() {
        if (session == null) {
            return 0;
        }

        return session.getStatus().getTotalUpload();
    }

    public int getDownloadRateLimit() {
        if (session == null) {
            return 0;
        }

        return session.getSettings().getDownloadRateLimit();
    }

    public int getUploadRateLimit() {
        if (session == null) {
            return 0;
        }

        return session.getSettings().getDownloadRateLimit();
    }

    public boolean isStarted() {
        return session != null;
    }

    public boolean isPaused() {
        return session != null && session.isPaused();
    }

    public void start() {
        sync.lock();

        try {
            if (session != null) {
                return;
            }

            Pair<Integer, Integer> prange = new Pair<Integer, Integer>(ctx.port0, ctx.port1);
            session = new Session(prange, ctx.iface);

            downloader = new Downloader(session);
            defaultSettings = session.getSettings();

            loadSettings();
            session.addListener(innerListener);

            fireStarted();

        } finally {
            sync.unlock();
        }
    }

    /**
     * Abort and destroy the internal libtorrent session.
     */
    public void stop() {
        sync.lock();

        try {
            if (session == null) {
                return;
            }

            session.removeListener(innerListener);
            saveSettings();

            downloader = null;
            defaultSettings = null;

            session.abort();
            session = null;

            fireStopped();

        } finally {
            sync.unlock();
        }
    }

    public void restart() {
        sync.lock();

        try {

            stop();
            Thread.sleep(1000); // allow some time to release native resources
            start();

        } catch (InterruptedException e) {
            // ignore
        } finally {
            sync.unlock();
        }
    }

    public void pause() {
        if (session != null && !session.isPaused()) {
            session.pause();
        }
    }

    public void resume() {
        if (session != null) {
            session.resume();
        }
    }

    public void loadSettings() {
        if (session == null) {
            return;
        }

        try {
            File f = settingsFile();
            if (f.exists()) {
                byte[] data = FileUtils.readFileToByteArray(f);
                session.loadState(data);
            } else {
                revertToDefaultConfiguration();
            }
        } catch (Throwable e) {
            LOG.error("Error loading session state", e);
        }
    }

    public void saveSettings() {
        if (session == null) {
            return;
        }

        try {
            byte[] data = session.saveState();
            FileUtils.writeByteArrayToFile(settingsFile(), data);
        } catch (Throwable e) {
            LOG.error("Error saving session state", e);
        }
    }

    private void saveSettings(SessionSettings s) {
        if (session == null) {
            return;
        }

        session.setSettings(s);
        saveSettings();
    }

    public void revertToDefaultConfiguration() {
        if (session == null) {
            return;
        }

        defaultSettings.broadcastLSD(true);

        session.setSettings(defaultSettings);

        SessionSettings s = session.getSettings(); // working with a copy?

        if (ctx.optimizeMemory) {

            int maxQueuedDiskBytes = s.getMaxQueuedDiskBytes();
            s.setMaxQueuedDiskBytes(maxQueuedDiskBytes / 2);
            int sendBufferWatermark = s.getSendBufferWatermark();
            s.setSendBufferWatermark(sendBufferWatermark / 2);
            s.setCacheSize(256);
            s.setActiveDownloads(4);
            s.setActiveSeeds(4);
            s.setMaxPeerlistSize(200);
            s.setUtpDynamicSockBuf(false);
            s.setGuidedReadCache(true);
            s.setTickInterval(1000);
            s.setInactivityTimeout(60);
            s.optimizeHashingForSpeed(false);
            s.setSeedingOutgoingConnections(false);
            s.setConnectionsLimit(200);

        } else {

            s.setActiveDownloads(10);
            s.setActiveSeeds(10);
        }

        session.setSettings(s);

        saveSettings();
    }

    public void download(File torrent, File saveDir) {
        download(torrent, saveDir, null);
    }

    public void download(File torrent, File saveDir, boolean[] selection) {
        if (session == null) {
            return;
        }

        saveDir = setupSaveDir(saveDir);
        if (saveDir == null) {
            return;
        }

        TorrentInfo ti = new TorrentInfo(torrent);

        Priority[] priorities = null;

        TorrentHandle th = downloader.find(ti.getInfoHash());
        boolean exists = th != null;

        if (selection != null) {
            if (th != null) {
                priorities = th.getFilePriorities();
            } else {
                priorities = Priority.array(Priority.IGNORE, ti.getNumFiles());
            }

            for (int i = 0; i < selection.length; i++) {
                if (selection[i]) {
                    priorities[i] = Priority.NORMAL;
                }
            }
        }

        downloader.download(ti, saveDir, priorities, null);

        if (!exists) {
            saveResumeTorrent(torrent);
        }
    }

    public void download(TorrentInfo ti, File saveDir, boolean[] selection) {
        if (session == null) {
            return;
        }

        saveDir = setupSaveDir(saveDir);
        if (saveDir == null) {
            return;
        }

        Priority[] priorities = null;

        TorrentHandle th = downloader.find(ti.getInfoHash());
        boolean exists = th != null;

        if (selection != null) {
            if (th != null) {
                priorities = th.getFilePriorities();
            } else {
                priorities = Priority.array(Priority.IGNORE, ti.getNumFiles());
            }

            for (int i = 0; i < selection.length; i++) {
                if (selection[i]) {
                    priorities[i] = Priority.NORMAL;
                }
            }
        }

        downloader.download(ti, saveDir, priorities, null);

        if (!exists) {
            File torrent = saveTorrent(ti);
            saveResumeTorrent(torrent);
        }
    }

    public void download(TorrentCrawledSearchResult sr, File saveDir) {
        if (session == null) {
            return;
        }

        saveDir = setupSaveDir(saveDir);
        if (saveDir == null) {
            return;
        }

        TorrentInfo ti = sr.getTorrentInfo();
        int fileIndex = sr.getFileIndex();

        TorrentHandle th = downloader.find(ti.getInfoHash());
        boolean exists = th != null;

        if (th != null) {
            Priority[] priorities = th.getFilePriorities();
            if (priorities[fileIndex] == Priority.IGNORE) {
                priorities[fileIndex] = Priority.NORMAL;
                downloader.download(ti, saveDir, priorities, null);
            }
        } else {
            Priority[] priorities = Priority.array(Priority.IGNORE, ti.getNumFiles());
            priorities[fileIndex] = Priority.NORMAL;
            downloader.download(ti, saveDir, priorities, null);
        }

        if (!exists) {
            File torrent = saveTorrent(ti);
            saveResumeTorrent(torrent);
        }
    }

    public byte[] fetchMagnet(String uri, long timeout) {
        if (session == null) {
            return null;
        }

        return downloader.fetchMagnet(uri, timeout);
    }

    public void restoreDownloads() {
        if (session == null) {
            return;
        }

        if (ctx.homeDir == null || !ctx.homeDir.exists()) {
            LOG.warn("Wrong setup with BTEngine home dir");
            return;
        }

        File[] torrents = ctx.homeDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return FilenameUtils.getExtension(name).equals("torrent");
            }
        });

        for (File t : torrents) {
            try {
                String infoHash = FilenameUtils.getBaseName(t.getName());
                File resumeFile = resumeDataFile(infoHash);

                session.asyncAddTorrent(t, null, resumeFile);
            } catch (Throwable e) {
                LOG.error("Error restoring torrent download: " + t, e);
            }
        }

        migrateVuzeDownloads();
    }

    File settingsFile() {
        return new File(ctx.homeDir, "settings.dat");
    }

    File resumeTorrentFile(String infoHash) {
        return new File(ctx.homeDir, infoHash + ".torrent");
    }

    File resumeDataFile(String infoHash) {
        return new File(ctx.homeDir, infoHash + ".resume");
    }

    File readTorrentPath(String infoHash) {
        File torrent = null;

        try {
            byte[] arr = FileUtils.readFileToByteArray(resumeTorrentFile(infoHash));
            entry e = entry.bdecode(Vectors.bytes2char_vector(arr));
            torrent = new File(e.dict().get(TORRENT_ORIG_PATH_KEY).string());
        } catch (Throwable e) {
            // can't recover original torrent path
        }

        return torrent;
    }

    private File saveTorrent(TorrentInfo ti) {
        File torrentFile;

        try {
            String name = ti.getName();
            if (name == null || name.length() == 0) {
                name = ti.getInfoHash().toString();
            }
            name = OSUtils.escapeFilename(name);

            torrentFile = new File(ctx.torrentsDir, name + ".torrent");
            byte[] arr = ti.toEntry().bencode();

            FileUtils.writeByteArrayToFile(torrentFile, arr);
        } catch (Throwable e) {
            torrentFile = null;
            LOG.warn("Error saving torrent info to file", e);
        }

        return torrentFile;
    }

    private void saveResumeTorrent(File torrent) {
        try {
            TorrentInfo ti = new TorrentInfo(torrent);
            entry e = ti.toEntry().getSwig();
            e.dict().set(TORRENT_ORIG_PATH_KEY, new entry(torrent.getAbsolutePath()));
            byte[] arr = Vectors.char_vector2bytes(e.bencode());
            FileUtils.writeByteArrayToFile(resumeTorrentFile(ti.getInfoHash().toString()), arr);
        } catch (Throwable e) {
            LOG.warn("Error saving resume torrent", e);
        }
    }

    private void doResumeData(TorrentAlert<?> alert) {
        TorrentHandle th = alert.getHandle();
        if (th.isValid() && th.needSaveResumeData()) {
            th.saveResumeData();
        }
    }

    private void fireStarted() {
        if (listener != null) {
            listener.started(this);
        }
    }

    private void fireStopped() {
        if (listener != null) {
            listener.stopped(this);
        }
    }

    private void fireDownloadAdded(BTDownload dl) {
        if (listener != null) {
            listener.downloadAdded(this, dl);
        }
    }

    private void migrateVuzeDownloads() {
        try {
            File dir = new File(ctx.homeDir.getParent(), "azureus");
            File file = new File(dir, "downloads.config");

            if (file.exists()) {
                Entry configEntry = Entry.bdecode(file);
                List<Entry> downloads = configEntry.dictionary().get("downloads").list();

                for (Entry d : downloads) {
                    try {
                        Map<String, Entry> map = d.dictionary();
                        File saveDir = new File(map.get("save_dir").string());
                        File torrent = new File(map.get("torrent").string());
                        ArrayList<Entry> filePriorities = map.get("file_priorities").list();

                        Priority[] priorities = Priority.array(Priority.IGNORE, filePriorities.size());
                        for (int i = 0; i < filePriorities.size(); i++) {
                            long p = filePriorities.get(i).integer();
                            if (p != 0) {
                                priorities[i] = Priority.NORMAL;
                            }
                        }

                        if (torrent.exists() && saveDir.exists()) {
                            LOG.info("Restored old vuze download: " + torrent);
                            downloader.download(new TorrentInfo(torrent), saveDir, priorities, null);
                            saveResumeTorrent(torrent);
                        }
                    } catch (Throwable e) {
                        LOG.error("Error restoring vuze torrent download", e);
                    }
                }

                file.delete();
            }
        } catch (Throwable e) {
            LOG.error("Error migrating old vuze downloads", e);
        }
    }

    private File setupSaveDir(File saveDir) {
        File result = null;

        if (saveDir == null) {
            if (ctx.dataDir != null) {
                result = ctx.dataDir;
            } else {
                LOG.warn("Unable to setup save dir path, review your logic, both saveDir and ctx.dataDir are null.");
            }
        } else {
            result = saveDir;
        }

        if (result != null && !result.isDirectory() && !result.mkdirs()) {
            result = null;
            LOG.warn("Failed to create save dir to download");
        }

        if (result != null && !result.canWrite()) {
            result = null;
            LOG.warn("Failed to setup save dir with write access");
        }

        return result;
    }

    private final class InnerListener implements AlertListener {
        @Override
        public int[] types() {
            return INNER_LISTENER_TYPES;
        }

        @Override
        public void alert(Alert<?> alert) {
            //LOG.info(a.message());

            AlertType type = alert.getType();

            switch (type) {
                case TORRENT_ADDED:
                    fireDownloadAdded(new BTDownload(BTEngine.this, ((TorrentAlert<?>) alert).getHandle()));
                    doResumeData((TorrentAlert<?>) alert);
                    break;
                case PIECE_FINISHED:
                    doResumeData((TorrentAlert<?>) alert);
                    break;
                case PORTMAP:
                    firewalled = false;
                    break;
                case PORTMAP_ERROR:
                    firewalled = true;
                    break;
            }
        }
    }

    //--------------------------------------------------
    // Settings methods
    //--------------------------------------------------

    public int getDownloadSpeedLimit() {
        if (session == null) {
            return 0;
        }

        return getSettings().getDownloadRateLimit();
    }

    public void setDownloadSpeedLimit(int limit) {
        if (session == null) {
            return;
        }

        SessionSettings s = getSettings();
        s.setDownloadRateLimit(limit);
        saveSettings(s);
    }

    public int getUploadSpeedLimit() {
        if (session == null) {
            return 0;
        }

        return getSettings().getUploadRateLimit();
    }

    public void setUploadSpeedLimit(int limit) {
        if (session == null) {
            return;
        }

        SessionSettings s = getSettings();
        s.setUploadRateLimit(limit);
        saveSettings(s);
    }

    public int getMaxActiveDownloads() {
        if (session == null) {
            return 0;
        }

        return getSettings().getActiveDownloads();
    }

    public void setMaxActiveDownloads(int limit) {
        if (session == null) {
            return;
        }

        SessionSettings s = getSettings();
        s.setActiveDownloads(limit);
        saveSettings(s);
    }

    public int getMaxActiveSeeds() {
        if (session == null) {
            return 0;
        }

        return getSettings().getActiveSeeds();
    }

    public void setMaxActiveSeeds(int limit) {
        if (session == null) {
            return;
        }

        SessionSettings s = getSettings();
        s.setActiveSeeds(limit);
        saveSettings(s);
    }

    public int getMaxConnections() {
        if (session == null) {
            return 0;
        }

        return getSettings().getConnectionsLimit();
    }

    public void setMaxConnections(int limit) {
        if (session == null) {
            return;
        }

        SessionSettings s = getSettings();
        s.setConnectionsLimit(limit);
        saveSettings(s);
    }

    public int getMaxPeers() {
        if (session == null) {
            return 0;
        }

        return getSettings().getMaxPeerlistSize();
    }

    public void setMaxPeers(int limit) {
        if (session == null) {
            return;
        }

        SessionSettings s = getSettings();
        s.setMaxPeerlistSize(limit);
        saveSettings(s);
    }
}
