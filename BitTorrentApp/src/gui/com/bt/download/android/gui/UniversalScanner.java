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
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.net.Uri;
import android.os.SystemClock;

import com.bt.download.android.core.ConfigurationManager;
import com.bt.download.android.core.Constants;
import com.bt.download.android.core.FileDescriptor;
import com.bt.download.android.core.MediaType;
import com.bt.download.android.core.providers.UniversalStore;
import com.bt.download.android.core.providers.UniversalStore.Documents;
import com.bt.download.android.core.providers.UniversalStore.Documents.DocumentsColumns;
import com.bt.download.android.gui.util.UIUtils;
import com.frostwire.logging.Logger;

/**
 * 
 * @author gubatron
 * @author aldenml
 * 
 */
public final class UniversalScanner {

    private static final Logger LOG = Logger.getLogger(UniversalScanner.class);

    private final Context context;

    public UniversalScanner(Context context) {
        this.context = context;
    }

    public void scan(final String filePath) {
        scan(Arrays.asList(new File(filePath)));
    }

    public void scan(final Collection<File> filesToScan) {
        new MultiFileAndroidScanner(filesToScan).scan();
    }

    private static void shareFinishedDownload(FileDescriptor fd) {
        if (fd != null) {
            if (ConfigurationManager.instance().getBoolean(Constants.PREF_KEY_TRANSFER_SHARE_FINISHED_DOWNLOADS)) {
                fd.shared = true;
                Librarian.instance().updateSharedStates(fd.fileType, Arrays.asList(fd));
            }
            Librarian.instance().invalidateCountCache(fd.fileType);
        }
    }

    private void scanDocument(String filePath) {
        File file = new File(filePath);

        if (documentExists(filePath, file.length())) {
            return;
        }

        String displayName = FilenameUtils.getBaseName(file.getName());

        ContentResolver cr = context.getContentResolver();

        ContentValues values = new ContentValues();

        values.put(DocumentsColumns.DATA, filePath);
        values.put(DocumentsColumns.SIZE, file.length());
        values.put(DocumentsColumns.DISPLAY_NAME, displayName);
        values.put(DocumentsColumns.TITLE, displayName);
        values.put(DocumentsColumns.DATE_ADDED, System.currentTimeMillis());
        values.put(DocumentsColumns.DATE_MODIFIED, file.lastModified());
        values.put(DocumentsColumns.MIME_TYPE, UIUtils.getMimeType(filePath));

        Uri uri = cr.insert(Documents.Media.CONTENT_URI, values);

        FileDescriptor fd = new FileDescriptor();
        fd.fileType = Constants.FILE_TYPE_DOCUMENTS;
        fd.id = Integer.valueOf(uri.getLastPathSegment());

        shareFinishedDownload(fd);
    }

    private boolean documentExists(String filePath, long size) {
        boolean result = false;

        Cursor c = null;

        try {
            ContentResolver cr = context.getContentResolver();
            c = cr.query(UniversalStore.Documents.Media.CONTENT_URI, new String[] { DocumentsColumns._ID }, DocumentsColumns.DATA + "=?" + " AND " + DocumentsColumns.SIZE + "=?", new String[] { filePath, String.valueOf(size) }, null);
            result = c != null && c.getCount() != 0;
        } catch (Throwable e) {
            LOG.warn("Error detecting if file exists: " + filePath, e);
        } finally {
            if (c != null) {
                c.close();
            }
        }

        return result;
    }

    private final class MultiFileAndroidScanner implements MediaScannerConnectionClient {

        private MediaScannerConnection connection;
        private final Collection<File> files;
        private int numCompletedScans;

        public MultiFileAndroidScanner(Collection<File> filesToScan) {
            this.files = filesToScan;
            numCompletedScans = 0;
        }

        public void scan() {
            try {
                connection = new MediaScannerConnection(context, this);
                connection.connect();
            } catch (Throwable e) {
                LOG.warn("Error scanning file with android internal scanner, one retry", e);
                SystemClock.sleep(1000);
                connection = new MediaScannerConnection(context, this);
                connection.connect();
            }
        }

        public void onMediaScannerConnected() {
            try {
                /** should only arrive here on connected state, but let's double check since it's possible */
                if (connection.isConnected() && files != null && !files.isEmpty()) {
                    for (File f : files) {
                        connection.scanFile(f.getAbsolutePath(), null);
                    }
                }
            } catch (IllegalStateException e) {
                LOG.warn("Scanner service wasn't really connected or service was null", e);
                //should we try to connect again? don't want to end up in endless loop
                //maybe destroy connection?
            }
        }

        public void onScanCompleted(String path, Uri uri) {
            /** This will work if onScanCompleted is invoked after scanFile finishes. */
            numCompletedScans++;
            if (numCompletedScans == files.size()) {
                connection.disconnect();
            }

            MediaType mt = MediaType.getMediaTypeForExtension(FilenameUtils.getExtension(path));

            if (uri != null && !path.contains("/Android/data/" + context.getPackageName())) {
                if (mt != null && mt.getId() == Constants.FILE_TYPE_DOCUMENTS) {
                    scanDocument(path);
                } else {
                    //LOG.debug("Scanned new file: " + uri);
                    FileDescriptor fd = Librarian.instance().getFileDescriptor(uri);
                    if (fd != null) {
                        shareFinishedDownload(fd);
                    }
                }
            } else {
                if (path.endsWith(".apk")) {
                    //LOG.debug("Can't scan apk for security concerns: " + path);
                } else if (mt != null) {
                    if (mt.getId() == Constants.FILE_TYPE_AUDIO ||
                        mt.getId() == Constants.FILE_TYPE_VIDEOS ||
                        mt.getId() == Constants.FILE_TYPE_PICTURES) {
                        scanPrivateFile(uri, path, mt);
                    }
                } else {
                    scanDocument(path);
                    //LOG.debug("Scanned new file as document: " + path);
                }
            }
        }
    }

    /**
     * Android geniuses put a .nomedia file on the .../Android/data/ folder
     * inside the secondary external storage path, therefore, all attempts
     * to use MediaScannerConnection to scan a media file fail. Therefore we
     * have this method to insert the file's metadata manually on the content provider.
     * @param path
     */
    private void scanPrivateFile(Uri oldUri, String filePath, MediaType mt) {
        try {
            int n = context.getContentResolver().delete(oldUri, null, null);
            if (n > 0) {
                LOG.debug("Deleted from Files provider: " + oldUri);
            }
            Uri uri = nativeScanFile(context, filePath);

            if (uri != null) {
                FileDescriptor fd = new FileDescriptor();
                fd.fileType = (byte) mt.getId();
                fd.id = Integer.valueOf(uri.getLastPathSegment());

                shareFinishedDownload(fd);
            }
        } catch (Throwable e) {
            // eat
            e.printStackTrace();
        }
    }

    private static Uri nativeScanFile(Context context, String path) {
        try {
            File f = new File(path);

            Class<?> clazz = Class.forName("android.media.MediaScanner");

            Constructor<?> mediaScannerC = clazz.getDeclaredConstructor(Context.class);
            Object scanner = mediaScannerC.newInstance(context);

            Field mClientF = clazz.getDeclaredField("mClient");
            mClientF.setAccessible(true);
            Object mClient = mClientF.get(scanner);

            Method scanSingleFileM = clazz.getDeclaredMethod("scanSingleFile", String.class, String.class, String.class);
            Uri fileUri = (Uri) scanSingleFileM.invoke(scanner, f.getAbsolutePath(), "external", "data/raw");
            int n = context.getContentResolver().delete(fileUri, null, null);
            if (n > 0) {
                LOG.debug("Deleted from Files provider: " + fileUri);
            }

            Field mNoMediaF = mClient.getClass().getDeclaredField("mNoMedia");
            mNoMediaF.setAccessible(true);
            mNoMediaF.setBoolean(mClient, false);
            
            // This is only for HTC (tested only on HTC One M8)
            try {
                Field mFileCacheF = clazz.getDeclaredField("mFileCache");
                mFileCacheF.setAccessible(true);
                mFileCacheF.set(scanner, new HashMap<String, Object>());
            } catch (Throwable e) {
                // no an HTC, I need some time to refactor this hack
            }

            Method doScanFileM = mClient.getClass().getDeclaredMethod("doScanFile", String.class, String.class, long.class, long.class, boolean.class, boolean.class, boolean.class);
            Uri mediaUri = (Uri) doScanFileM.invoke(mClient, f.getAbsolutePath(), null, f.lastModified(), f.length(), false, true, false);

            Method releaseM = clazz.getDeclaredMethod("release");
            releaseM.invoke(scanner);

            return mediaUri;

        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
    }

    public void scanDir(File privateDir) {
        scan(FileUtils.listFiles(privateDir, null, true));
    }
}
