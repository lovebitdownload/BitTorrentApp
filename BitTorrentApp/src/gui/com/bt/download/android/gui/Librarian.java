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

package com.bt.download.android.gui;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

import com.bt.download.android.core.*;
import com.bt.download.android.gui.transfers.Transfers;
import org.apache.commons.io.FilenameUtils;
import org.xmlpull.v1.XmlPullParser;

import android.app.Application;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.res.XmlResourceParser;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Process;
import android.provider.BaseColumns;
import android.provider.MediaStore.MediaColumns;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Pair;
import android.view.WindowManager;

import com.bt.download.android.core.player.EphemeralPlaylist;
import com.bt.download.android.core.player.PlaylistItem;
import com.bt.download.android.core.providers.TableFetcher;
import com.bt.download.android.core.providers.TableFetchers;
import com.bt.download.android.core.providers.UniversalStore;
import com.bt.download.android.core.providers.UniversalStore.Applications;
import com.bt.download.android.core.providers.UniversalStore.Applications.ApplicationsColumns;
import com.bt.download.android.core.providers.UniversalStore.Sharing;
import com.bt.download.android.core.providers.UniversalStore.Sharing.SharingColumns;
import com.bt.download.android.gui.util.Apk;
import com.frostwire.util.StringUtils;
import com.frostwire.localpeer.Finger;
import com.frostwire.localpeer.ScreenMetrics;
import com.frostwire.util.DirectoryUtils;

/**
 * The Librarian is in charge of:
 * -> Keeping track of what files we're sharing or not.
 * -> Indexing the files we're sharing.
 * -> Searching for files we're sharing.
 * 
 * @author gubatron
 * @author aldenml
 * 
 */
public final class Librarian {

    private static final String TAG = "FW.Librarian";

    private final Application context;
    private final FileCountCache[] cache; // it is an array for performance reasons

    private static Librarian instance;

    public synchronized static void create(Application context) {
        if (instance != null) {
            return;
        }
        instance = new Librarian(context);
    }

    public static Librarian instance() {
        if (instance == null) {
            throw new CoreRuntimeException("Librarian not created");
        }
        return instance;
    }

    private Librarian(Application context) {
        this.context = context;
        this.cache = new FileCountCache[] { new FileCountCache(), new FileCountCache(), new FileCountCache(), new FileCountCache(), new FileCountCache(), new FileCountCache() };
    }

    public List<FileDescriptor> getFiles(byte fileType, int offset, int pageSize, boolean sharedOnly) {
        return getFiles(offset, pageSize, TableFetchers.getFetcher(fileType), sharedOnly);
    }

    public List<FileDescriptor> getFiles(byte fileType, String where, String[] whereArgs) {
        return getFiles(0, Integer.MAX_VALUE, TableFetchers.getFetcher(fileType), where, whereArgs, false);
    }

    /**
     * Returns the total number of shared files by this peer. note: each of the
     * number of shared files (by type) is stored on _cacheNumFiles, this
     * function returns the sum of this cache.
     * 
     * @return
     */
    public int getNumFiles() {
        int result = 0;

        for (byte i = 0; i < 6; i++) {
            //update numbers if you have to.
            if (!cache[i].cacheValid(true)) {
                cache[i].updateShared(getNumFiles(i, true));
            }

            result += cache[i].shared;
        }

        return result < 0 ? 0 : result;
    }

    /**
     * 
     * @param fileType
     * @param onlyShared - If false, forces getting all files, shared or unshared. 
     * @return
     */
    public int getNumFiles(byte fileType, boolean onlyShared) {
        TableFetcher fetcher = TableFetchers.getFetcher(fileType);

        if (cache[fileType].cacheValid(onlyShared)) {
            return cache[fileType].getCount(onlyShared);
        }

        Cursor c = null;

        int result = 0;
        int numFiles = 0;

        try {
            ContentResolver cr = context.getContentResolver();
            c = cr.query(fetcher.getContentUri(), new String[] { BaseColumns._ID }, null, null, null);
            numFiles = c != null ? c.getCount() : 0;
        } catch (Exception e) {
            Log.e(TAG, "Failed to get num of files", e);
        } finally {
            if (c != null) {
                c.close();
            }
        }

        result = onlyShared ? (getSharedFiles(fileType).size()) : numFiles;

        updateCacheNumFiles(fileType, result, onlyShared);

        return result;
    }

    public FileDescriptor getFileDescriptor(byte fileType, int fileId) {
        return getFileDescriptor(fileType, fileId, true);
    }

    public FileDescriptor getFileDescriptor(byte fileType, int fileId, boolean sharedOnly) {
        List<FileDescriptor> fds = getFiles(0, 1, TableFetchers.getFetcher(fileType), BaseColumns._ID + "=?", new String[] { String.valueOf(fileId) }, sharedOnly);
        if (fds.size() > 0) {
            return fds.get(0);
        } else {
            return null;
        }
    }

    public String renameFile(FileDescriptor fd, String newFileName) {
        try {

            String filePath = fd.filePath;

            File oldFile = new File(filePath);

            String ext = FilenameUtils.getExtension(filePath);

            File newFile = new File(oldFile.getParentFile(), newFileName + '.' + ext);

            ContentResolver cr = context.getContentResolver();

            ContentValues values = new ContentValues();

            values.put(MediaColumns.DATA, newFile.getAbsolutePath());
            values.put(MediaColumns.DISPLAY_NAME, FilenameUtils.getBaseName(newFileName));
            values.put(MediaColumns.TITLE, FilenameUtils.getBaseName(newFileName));

            TableFetcher fetcher = TableFetchers.getFetcher(fd.fileType);

            cr.update(fetcher.getContentUri(), values, BaseColumns._ID + "=?", new String[] { String.valueOf(fd.id) });

            oldFile.renameTo(newFile);

            return newFile.getAbsolutePath();

        } catch (Throwable e) {
            Log.e(TAG, "Failed to rename file: " + fd, e);
        }

        return null;
    }

    public void updateSharedStates(byte fileType, List<FileDescriptor> fds) {
        if (fds == null || fds.size() == 0) {
            return;
        }

        try {
            ContentResolver cr = context.getContentResolver();

            Set<Integer> sharedFiles = getSharedFiles(fds.get(0).fileType);

            int size = fds.size();

            // we know this is a slow process, we can improve it later
            for (int i = 0; i < size; i++) {

                FileDescriptor fileDescriptor = fds.get(i);

                ContentValues contentValues = new ContentValues();
                contentValues.put(SharingColumns.SHARED, fileDescriptor.shared ? 1 : 0);

                // Is this a NEW Shared File?
                if (!sharedFiles.contains(fileDescriptor.id) && fileDescriptor.shared) {
                    // insert in table as unshared.
                    contentValues.put(SharingColumns.FILE_ID, fileDescriptor.id);
                    contentValues.put(SharingColumns.FILE_TYPE, fileType);
                    cr.insert(Sharing.Media.CONTENT_URI, contentValues);
                } else {
                    // everything else is an update
                    cr.update(Sharing.Media.CONTENT_URI, contentValues, SharingColumns.FILE_ID + "=? AND " + SharingColumns.FILE_TYPE + "=?", new String[] { String.valueOf(fileDescriptor.id), String.valueOf(fileType) });
                }
            }

            invalidateCountCache(fileType);

        } catch (Throwable e) {
            Log.e(TAG, "Failed to update share states", e);
        }
    }

    public void deleteFiles(byte fileType, Collection<FileDescriptor> fds) {
        List<Integer> ids = new ArrayList<Integer>(fds.size());

        for (FileDescriptor fd : fds) {
            if (new File(fd.filePath).delete()) {
                ids.add(fd.id);
                deleteSharedState(fd.fileType, fd.id);
            }
        }

        try {
            ContentResolver cr = context.getContentResolver();
            TableFetcher fetcher = TableFetchers.getFetcher(fileType);
            cr.delete(fetcher.getContentUri(), MediaColumns._ID + " IN " + StringUtils.buildSet(ids), null);
        } catch (Throwable e) {
            Log.e(TAG, "Failed to delete files from media store", e);
        }

        invalidateCountCache(fileType);
    }

    public void scan(File file) {
        scan(file, Transfers.getIgnorableFiles());
    }

    public Finger finger(boolean local) {
        Finger finger = new Finger();

        finger.uuid = ConfigurationManager.instance().getUUIDString();
        finger.nickname = ConfigurationManager.instance().getNickname();
        finger.frostwireVersion = Constants.FROSTWIRE_VERSION_STRING;
        finger.totalShared = getNumFiles();

        finger.deviceVersion = Build.VERSION.RELEASE;
        finger.deviceModel = Build.MODEL;
        finger.deviceProduct = Build.PRODUCT;
        finger.deviceName = Build.DEVICE;
        finger.deviceManufacturer = Build.MANUFACTURER;
        finger.deviceBrand = Build.BRAND;
        finger.deviceScreen = readScreenMetrics();

        finger.numSharedAudioFiles = getNumFiles(Constants.FILE_TYPE_AUDIO, true);
        finger.numSharedVideoFiles = getNumFiles(Constants.FILE_TYPE_VIDEOS, true);
        finger.numSharedPictureFiles = getNumFiles(Constants.FILE_TYPE_PICTURES, true);
        finger.numSharedDocumentFiles = getNumFiles(Constants.FILE_TYPE_DOCUMENTS, true);
        finger.numSharedApplicationFiles = getNumFiles(Constants.FILE_TYPE_APPLICATIONS, true);
        finger.numSharedRingtoneFiles = getNumFiles(Constants.FILE_TYPE_RINGTONES, true);

        if (local) {
            finger.numTotalAudioFiles = getNumFiles(Constants.FILE_TYPE_AUDIO, false);
            finger.numTotalVideoFiles = getNumFiles(Constants.FILE_TYPE_VIDEOS, false);
            finger.numTotalPictureFiles = getNumFiles(Constants.FILE_TYPE_PICTURES, false);
            finger.numTotalDocumentFiles = getNumFiles(Constants.FILE_TYPE_DOCUMENTS, false);
            finger.numTotalApplicationFiles = getNumFiles(Constants.FILE_TYPE_APPLICATIONS, false);
            finger.numTotalRingtoneFiles = getNumFiles(Constants.FILE_TYPE_RINGTONES, false);
        } else {
            finger.numTotalAudioFiles = finger.numSharedAudioFiles;
            finger.numTotalVideoFiles = finger.numSharedVideoFiles;
            finger.numTotalPictureFiles = finger.numSharedPictureFiles;
            finger.numTotalDocumentFiles = finger.numSharedDocumentFiles;
            finger.numTotalApplicationFiles = finger.numSharedApplicationFiles;
            finger.numTotalRingtoneFiles = finger.numSharedRingtoneFiles;
        }

        return finger;
    }

    public void syncApplicationsProvider() {
        if (!isExternalStorageMounted()) {
            return;
        }

        Thread t = new Thread(new Runnable() {
            public void run() {
                syncApplicationsProviderSupport();
            }
        });
        t.setName("syncApplicationsProvider");
        t.setDaemon(true);
        t.start();
    }

    public void syncMediaStore() {
        if (!isExternalStorageMounted()) {
            return;
        }

        Thread t = new Thread(new Runnable() {
            public void run() {
                syncMediaStoreSupport();
            }
        });
        t.setName("syncMediaStore");
        t.setDaemon(true);
        t.start();
    }

    public boolean isExternalStorageMounted() {
        return com.bt.download.android.util.SystemUtils.isPrimaryExternalStorageMounted();
    }

    public EphemeralPlaylist createEphemeralPlaylist(FileDescriptor fd) {
        List<FileDescriptor> fds = Librarian.instance().getFiles(Constants.FILE_TYPE_AUDIO, FilenameUtils.getPath(fd.filePath), false);

        if (fds.size() == 0) { // just in case
            Log.w(TAG, "Logic error creating ephemeral playlist");
            fds.add(fd);
        }

        EphemeralPlaylist playlist = new EphemeralPlaylist(fds);
        playlist.setNextItem(new PlaylistItem(fd));

        return playlist;
    }

    public void invalidateCountCache() {
        for (FileCountCache c : cache) {
            if (c != null) {
                c.lastTimeCachedShared = 0;
                c.lastTimeCachedOnDisk = 0;
            }
        }
        broadcastRefreshFinger();
    }

    /**
     * @param fileType
     */
    void invalidateCountCache(byte fileType) {
        cache[fileType].lastTimeCachedShared = 0;
        cache[fileType].lastTimeCachedOnDisk = 0;
        broadcastRefreshFinger();
    }

    private void broadcastRefreshFinger() {
        context.sendBroadcast(new Intent(Constants.ACTION_REFRESH_FINGER));
        PeerManager.instance().updateLocalPeer();
    }

    private void syncApplicationsProviderSupport() {
        try {

            List<FileDescriptor> fds = Librarian.instance().getFiles(Constants.FILE_TYPE_APPLICATIONS, 0, Integer.MAX_VALUE, false);

            int packagesSize = fds.size();
            String[] packages = new String[packagesSize];
            for (int i = 0; i < packagesSize; i++) {
                packages[i] = fds.get(i).album;
            }
            Arrays.sort(packages);

            List<ApplicationInfo> applications = context.getPackageManager().getInstalledApplications(0);

            int size = applications.size();

            ArrayList<String> newPackagesList = new ArrayList<String>(size);

            for (int i = 0; i < size; i++) {
                ApplicationInfo appInfo = applications.get(i);

                try {
                    if (appInfo == null) {
                        continue;
                    }

                    newPackagesList.add(appInfo.packageName);

                    File f = new File(appInfo.sourceDir);
                    if (!f.canRead()) {
                        continue;
                    }

                    int index = Arrays.binarySearch(packages, appInfo.packageName);
                    if (index >= 0) {
                        continue;
                    }

                    String data = appInfo.sourceDir;
                    String title = appInfo.packageName;
                    String packageName = appInfo.packageName;
                    String version = "";

                    Apk apk = new Apk(context, appInfo.sourceDir);
                    String[] result = parseApk(apk);
                    if (result != null) {
                        if (result[1] == null) {
                            continue;
                        }
                        title = result[1];
                        version = result[0];
                    }

                    ContentValues cv = new ContentValues();
                    cv.put(ApplicationsColumns.DATA, data);
                    cv.put(ApplicationsColumns.SIZE, f.length());
                    cv.put(ApplicationsColumns.TITLE, title);
                    cv.put(ApplicationsColumns.MIME_TYPE, Constants.MIME_TYPE_ANDROID_PACKAGE_ARCHIVE);
                    cv.put(ApplicationsColumns.VERSION, version);
                    cv.put(ApplicationsColumns.PACKAGE_NAME, packageName);

                    ContentResolver cr = context.getContentResolver();

                    Uri uri = cr.insert(Applications.Media.CONTENT_URI, cv);

                    if (appInfo.icon != 0) {
                        try {
                            InputStream is = null;
                            OutputStream os = null;

                            try {
                                is = apk.openRawResource(appInfo.icon);
                                os = cr.openOutputStream(uri);

                                byte[] buff = new byte[4 * 1024];
                                int n = 0;
                                while ((n = is.read(buff, 0, buff.length)) != -1) {
                                    os.write(buff, 0, n);
                                }

                            } finally {
                                if (os != null) {
                                    os.close();
                                }
                                if (is != null) {
                                    is.close();
                                }
                            }
                        } catch (Throwable e) {
                            Log.e(TAG, "Can't retrieve icon image for application " + appInfo.packageName);
                        }
                    }
                } catch (Throwable e) {
                    Log.e(TAG, "Error retrieving information for application " + appInfo.packageName);
                }
            }

            // clean uninstalled applications
            String[] newPackages = newPackagesList.toArray(new String[0]);
            Arrays.sort(newPackages);

            // simple way n * log(n)
            for (int i = 0; i < packagesSize; i++) {
                String packageName = packages[i];
                if (Arrays.binarySearch(newPackages, packageName) < 0) {
                    ContentResolver cr = context.getContentResolver();
                    cr.delete(Applications.Media.CONTENT_URI, ApplicationsColumns.PACKAGE_NAME + " LIKE '%" + packageName + "%'", null);
                }
            }

        } catch (Throwable e) {
            Log.e(TAG, "Error performing initial applications provider synchronization with device", e);
        }
    }

    private void syncMediaStoreSupport() {
        Set<File> ignorableFiles = Transfers.getIgnorableFiles();

        syncMediaStore(Constants.FILE_TYPE_AUDIO, ignorableFiles);
        syncMediaStore(Constants.FILE_TYPE_PICTURES, ignorableFiles);
        syncMediaStore(Constants.FILE_TYPE_VIDEOS, ignorableFiles);
        syncMediaStore(Constants.FILE_TYPE_RINGTONES, ignorableFiles);

        scan(SystemPaths.getSaveDirectory(Constants.FILE_TYPE_DOCUMENTS));
    }

    private void syncMediaStore(byte fileType, Set<File> ignorableFiles) {
        TableFetcher fetcher = TableFetchers.getFetcher(fileType);

        Cursor c = null;
        try {

            ContentResolver cr = context.getContentResolver();

            String where = MediaColumns.DATA + " LIKE ?";
            String[] whereArgs = new String[]{SystemPaths.getAppStorage().getAbsolutePath() + "%"};

            c = cr.query(fetcher.getContentUri(), new String[]{MediaColumns._ID, MediaColumns.DATA}, where, whereArgs, null);
            if (c == null) {
                return;
            }

            int idCol = c.getColumnIndex(MediaColumns._ID);
            int pathCol = c.getColumnIndex(MediaColumns.DATA);

            List<Integer> ids = new ArrayList<Integer>();

            while (c.moveToNext()) {
                int id = Integer.valueOf(c.getString(idCol));
                String path = c.getString(pathCol);

                if (ignorableFiles.contains(new File(path))) {
                    ids.add(id);
                }
            }

            cr.delete(fetcher.getContentUri(), MediaColumns._ID + " IN " + StringUtils.buildSet(ids), null);

        } catch (Throwable e) {
            Log.e(TAG, "General failure during sync of MediaStore", e);
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    private List<FileDescriptor> getFiles(int offset, int pageSize, TableFetcher fetcher, boolean sharedOnly) {
        return getFiles(offset, pageSize, fetcher, null, null, sharedOnly);
    }

    /**
     * Returns a list of Files.
     * 
     * @param offset
     *            - from where (starting at 0)
     * @param pageSize
     *            - how many results
     * @param fetcher
     *            - An implementation of TableFetcher
     * @param sharedOnly
     *            - if true, retrieves only the fine grained shared files.
     *
     * @return List<FileDescriptor>
     */
    private List<FileDescriptor> getFiles(int offset, int pageSize, TableFetcher fetcher, String where, String[] whereArgs, boolean sharedOnly) {
        List<FileDescriptor> result = new ArrayList<FileDescriptor>();

        Cursor c = null;
        Set<Integer> sharedIds = getSharedFiles(fetcher.getFileType());

        try {

            ContentResolver cr = context.getContentResolver();

            String[] columns = fetcher.getColumns();
            String sort = fetcher.getSortByExpression();

            c = cr.query(fetcher.getContentUri(), columns, where, whereArgs, sort);

            if (c == null || !c.moveToPosition(offset)) {
                return result;
            }

            fetcher.prepare(c);

            int count = 1;

            do {
                FileDescriptor fd = fetcher.fetch(c);

                fd.shared = sharedIds.contains(fd.id);

                if (sharedOnly && !fd.shared) {
                    continue;
                }

                result.add(fd);

            } while (c.moveToNext() && count++ < pageSize);

        } catch (Throwable e) {
            Log.e(TAG, "General failure getting files", e);
        } finally {
            if (c != null) {
                c.close();
            }
        }

        return result;
    }

    public List<FileDescriptor> getFiles(String filepath, boolean exactPathMatch) {
        return getFiles(getFileType(filepath, true), filepath, exactPathMatch);
    }

    /**
     * 
     * @param filepath
     * @param exactPathMatch - set it to false and pass an incomplete filepath prefix to get files in a folder for example.
     * @return
     */
    public List<FileDescriptor> getFiles(byte fileType, String filepath, boolean exactPathMatch) {
        String where = MediaColumns.DATA + " LIKE ?";
        String[] whereArgs = new String[] { (exactPathMatch) ? filepath : "%" + filepath + "%" };

        List<FileDescriptor> fds = Librarian.instance().getFiles(fileType, where, whereArgs);
        return fds;
    }

    private Pair<List<Integer>, List<String>> getAllFiles(byte fileType) {
        Pair<List<Integer>, List<String>> result = new Pair<List<Integer>, List<String>>(new ArrayList<Integer>(), new ArrayList<String>());

        Cursor c = null;

        try {
            TableFetcher fetcher = TableFetchers.getFetcher(fileType);

            ContentResolver cr = context.getContentResolver();

            c = cr.query(fetcher.getContentUri(), new String[] { BaseColumns._ID, MediaColumns.DATA }, null, null, BaseColumns._ID);

            if (c != null) {
                while (c.moveToNext()) {
                    result.first.add(c.getInt(0));
                    result.second.add(c.getString(1));
                }
            }
        } catch (Throwable e) {
            Log.e(TAG, "General failure getting all files", e);
        } finally {
            if (c != null) {
                c.close();
            }
        }

        return result;
    }

    private Set<Integer> getSharedFiles(byte fileType) {
        TreeSet<Integer> result = new TreeSet<Integer>();
        List<Integer> delete = new ArrayList<Integer>();

        Cursor c = null;

        try {
            ContentResolver cr = context.getContentResolver();
            String[] columns = new String[] { SharingColumns.FILE_ID, SharingColumns._ID };
            c = cr.query(Sharing.Media.CONTENT_URI, columns, SharingColumns.SHARED + "=1 AND " + SharingColumns.FILE_TYPE + "=?", new String[] { String.valueOf(fileType) }, null);

            if (c == null || !c.moveToFirst()) {
                return result;
            }

            int fileIdCol = c.getColumnIndex(SharingColumns.FILE_ID);
            int sharingIdCol = c.getColumnIndex(SharingColumns._ID);

            Pair<List<Integer>, List<String>> pair = getAllFiles(fileType);
            List<Integer> files = pair.first;
            List<String> paths = pair.second;

            do {
                int fileId = c.getInt(fileIdCol);
                int sharingId = c.getInt(sharingIdCol);

                int index = Collections.binarySearch(files, fileId);

                try {
                    if (index >= 0) {
                        File f = new File(paths.get(index));
                        if (f.exists() && f.isFile()) {
                            result.add(fileId);
                        } else {
                            delete.add(sharingId);
                        }
                    } else {
                        delete.add(sharingId);
                    }
                } catch (Throwable e) {
                    Log.e(TAG, "Error checking fileId: " + fileId + ", fileType: " + fileId);
                }
            } while (c.moveToNext());

        } catch (Throwable e) {
            Log.e(TAG, "General failure getting shared/unshared files ids", e);
        } finally {
            if (c != null) {
                c.close();
            }

            if (delete.size() > 0) {
                deleteSharedStates(delete);
            }
        }

        return result;
    }

    private void deleteSharedState(byte fileType, int fileId) {
        try {
            ContentResolver cr = context.getContentResolver();
            int deleted = cr.delete(UniversalStore.Sharing.Media.CONTENT_URI, SharingColumns.FILE_ID + "= ? AND " + SharingColumns.FILE_TYPE + " = ?", new String[] { String.valueOf(fileId), String.valueOf(fileType) });
            Log.d(TAG, "deleteSharedState " + deleted + " rows  (fileType: " + fileType + ", fileId: " + fileId + " )");
        } catch (Throwable e) {
            Log.e(TAG, "Failed to delete shared state for fileType=" + fileType + ", fileId=" + fileId, e);
        }
    }

    private void deleteSharedStates(List<Integer> sharingIds) {
        try {
            ContentResolver cr = context.getContentResolver();
            int deleted = cr.delete(UniversalStore.Sharing.Media.CONTENT_URI, SharingColumns._ID + " IN " + StringUtils.buildSet(sharingIds), null);
            Log.d(TAG, "Deleted " + deleted + " shared states");
        } catch (Throwable e) {
            Log.e(TAG, "Failed to delete shared states", e);
        }
    }

    /**
     * Updates the number of files for this type.
     * @param fileType
     */
    private void updateCacheNumFiles(byte fileType, int num, boolean sharedOnly) {
        if (sharedOnly) {
            cache[fileType].updateShared(num);
        } else {
            cache[fileType].updateOnDisk(num);
        }
    }

    /**
     * This function returns an array of string in the following order: version name, label
     * @param apk
     * @return
     */
    private String[] parseApk(Apk apk) {
        try {
            String[] result = new String[3];

            XmlResourceParser parser = apk.getAndroidManifest();

            boolean manifestParsed = true;
            boolean applicationParsed = false;

            while (!manifestParsed || !applicationParsed) {
                int type = parser.next();

                if (type == XmlPullParser.END_DOCUMENT) {
                    break;
                }

                switch (type) {
                case XmlPullParser.START_TAG:
                    String tagName = parser.getName();
                    if (tagName.equals("manifest")) {
                        String versionName = parser.getAttributeValue("http://schemas.android.com/apk/res/android", "versionName");
                        if (versionName != null && versionName.startsWith("@")) {
                            versionName = apk.getString(Integer.parseInt(versionName.substring(1)));
                        }
                        result[0] = versionName;
                        manifestParsed = true;
                    }
                    if (tagName.equals("application")) {
                        String label = parser.getAttributeValue("http://schemas.android.com/apk/res/android", "label");
                        if (label != null && label.startsWith("@")) {
                            label = apk.getString(Integer.parseInt(label.substring(1)));
                        }
                        result[1] = label;
                        applicationParsed = true;
                    }
                    break;
                }
            }

            parser.close();

            return result;
        } catch (Throwable e) {
            return null;
        }
    }

    private void scan(File file, Set<File> ignorableFiles) {
        //if we just have a single file, do it the old way
        if (file.isFile()) {
            if (ignorableFiles.contains(file)) {
                return;
            }

            new UniversalScanner(context).scan(file.getAbsolutePath());
        } else if (file.isDirectory() && file.canRead()) {
            Collection<File> flattenedFiles = DirectoryUtils.getAllFolderFiles(file, null);

            if (ignorableFiles != null && !ignorableFiles.isEmpty()) {
                flattenedFiles.removeAll(ignorableFiles);
            }

            if (flattenedFiles != null && !flattenedFiles.isEmpty()) {
                new UniversalScanner(context).scan(flattenedFiles);
            }
        }
    }

    public ScreenMetrics readScreenMetrics() {
        ScreenMetrics sm = new ScreenMetrics();

        try {
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            context.getResources().getDisplayMetrics();
            DisplayMetrics dm = new DisplayMetrics();
            wm.getDefaultDisplay().getMetrics(dm);

            sm.densityDpi = dm.densityDpi;
            sm.heightPixels = dm.heightPixels;
            sm.widthPixels = dm.widthPixels;
            sm.xdpi = dm.xdpi;
            sm.ydpi = dm.ydpi;
        } catch (Throwable e) {
            Log.e(TAG, "Unable to get the device display dimensions", e);
        }

        return sm;
    }

    public double getScreenSizeInInches() {
        double screenInches = 0;

        try {
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            context.getResources().getDisplayMetrics();
            DisplayMetrics dm = new DisplayMetrics();
            wm.getDefaultDisplay().getMetrics(dm);

            double x = Math.pow(dm.widthPixels / dm.xdpi, 2);
            double y = Math.pow(dm.heightPixels / dm.ydpi, 2);

            screenInches = Math.sqrt(x + y);

        } catch (Throwable e) {
            Log.e(TAG, "Unable to get the device display dimensions", e);
        }

        return screenInches;
    }

    private static class FileCountCache {

        public int shared;
        public int onDisk;
        public long lastTimeCachedShared;
        public long lastTimeCachedOnDisk;

        public FileCountCache() {
            shared = 0;
            onDisk = 0;
            lastTimeCachedShared = 0;
            lastTimeCachedOnDisk = 0;
        }

        public void updateShared(int num) {
            shared = num;
            lastTimeCachedShared = System.currentTimeMillis();
        }

        public void updateOnDisk(int num) {
            onDisk = num;
            lastTimeCachedOnDisk = System.currentTimeMillis();
        }

        public int getCount(boolean onlyShared) {
            return (onlyShared) ? shared : onDisk;
        }

        public boolean cacheValid(boolean onlyShared) {
            long delta = System.currentTimeMillis() - ((onlyShared) ? lastTimeCachedShared : lastTimeCachedOnDisk);
            return delta < Constants.LIBRARIAN_FILE_COUNT_CACHE_TIMEOUT;
        }
    }

    private FileDescriptor getFileDescriptor(File f) {
        FileDescriptor fd = null;
        if (f.exists()) {
            List<FileDescriptor> files = getFiles(f.getAbsolutePath(), false);
            if (!files.isEmpty()) {
                fd = files.get(0);
            }
        }
        return fd;
    }

    public FileDescriptor getFileDescriptor(Uri uri) {
        FileDescriptor fd = null;
        try {
            if (uri != null) {
                if (uri.toString().startsWith("file://")) {
                    fd = getFileDescriptor(new File(uri.getPath()));
                } else {
                    TableFetcher fetcher = TableFetchers.getFetcher(uri);

                    fd = new FileDescriptor();
                    fd.fileType = fetcher.getFileType();
                    fd.id = Integer.valueOf(uri.getLastPathSegment());
                }
            }
        } catch (Throwable e) {
            fd = null;
            // sometimes uri.getLastPathSegment() is not an integer
            e.printStackTrace();
        }
        return fd;
    }

    private byte getFileType(String filename, boolean returnTorrentsAsDocument) {
        byte result = Constants.FILE_TYPE_DOCUMENTS;

        MediaType mt = MediaType.getMediaTypeForExtension(FilenameUtils.getExtension(filename));

        if (mt != null) {
            result = (byte) mt.getId();
        }

        if (returnTorrentsAsDocument && result == Constants.FILE_TYPE_TORRENTS) {
            result = Constants.FILE_TYPE_DOCUMENTS;
        }

        return result;
    }
}
