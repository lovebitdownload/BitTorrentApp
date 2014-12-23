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

package com.bt.download.android.core.providers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;
import android.util.Log;

import com.bt.download.android.core.Constants;
import com.bt.download.android.core.providers.UniversalStore.Applications;
import com.bt.download.android.core.providers.UniversalStore.Applications.ApplicationsColumns;
import com.bt.download.android.core.providers.UniversalStore.Applications.Media;

/**
 * @author gubatron
 * @author aldenml
 *
 */
public class ApplicationsProvider extends ContentProvider {

    private static final String TAG = "FW.ApplicationsProvider";

    private static final String DATABASE_NAME = "applications.db";
    private static final int DATABASE_VERSION = 2;
    private static final String APPLICATIONS_TABLE_NAME = "applications";

    private static final int APPLICATIONS_ALL = 1;
    private static final int APPLICATIONS_ID = 2;

    private static final UriMatcher uriMatcher;
    private static final HashMap<String, String> projectionMap;

    private DatabaseHelper databaseHelper;

    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(UniversalStore.UNIVERSAL_APPLICATIONS_AUTHORITY, "applications", APPLICATIONS_ALL);
        uriMatcher.addURI(UniversalStore.UNIVERSAL_APPLICATIONS_AUTHORITY, "applications/#", APPLICATIONS_ID);

        projectionMap = new HashMap<String, String>();
        projectionMap.put(ApplicationsColumns._ID, ApplicationsColumns._ID);
        projectionMap.put(ApplicationsColumns.DATA, ApplicationsColumns.DATA);
        projectionMap.put(ApplicationsColumns.TITLE, ApplicationsColumns.TITLE);
        projectionMap.put(ApplicationsColumns.VERSION, ApplicationsColumns.VERSION);
        projectionMap.put(ApplicationsColumns.SIZE, ApplicationsColumns.SIZE);
        projectionMap.put(ApplicationsColumns.PACKAGE_NAME, ApplicationsColumns.PACKAGE_NAME);
        projectionMap.put(ApplicationsColumns.DATE_ADDED, ApplicationsColumns.DATE_ADDED);
        projectionMap.put(ApplicationsColumns.DATE_MODIFIED, ApplicationsColumns.DATE_MODIFIED);

    }

    @Override
    public boolean onCreate() {
        Context context = getContext();

        databaseHelper = new DatabaseHelper(context);

        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        if (!accept()) {
            return null;
        }

        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        qb.setTables(APPLICATIONS_TABLE_NAME);

        switch (uriMatcher.match(uri)) {
        case APPLICATIONS_ALL:
            qb.setProjectionMap(projectionMap);
            break;

        case APPLICATIONS_ID:
            qb.setProjectionMap(projectionMap);
            qb.appendWhere(ApplicationsColumns._ID + "=" + uri.getPathSegments().get(1));
            break;

        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        // If no sort order is specified use the default
        String orderBy;

        if (TextUtils.isEmpty(sortOrder)) {
            orderBy = Applications.DEFAULT_SORT_ORDER;
        } else {
            orderBy = sortOrder;
        }

        // Get the database and run the query
        SQLiteDatabase db = databaseHelper.getReadableDatabase();

        Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, orderBy);

        // Tell the cursor what uri to watch, so it knows when its source data
        // changes
        c.setNotificationUri(getContext().getContentResolver(), uri);

        return c;
    }

    @Override
    public String getType(Uri uri) {
        switch (uriMatcher.match(uri)) {
        case APPLICATIONS_ALL:
            return Media.CONTENT_TYPE;
        case APPLICATIONS_ID:
            return Media.CONTENT_TYPE_ITEM;
        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        if (!accept()) {
            return null;
        }

        if (uriMatcher.match(uri) != APPLICATIONS_ALL) {
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        ContentValues cv;

        if (initialValues != null) {
            cv = new ContentValues(initialValues);
        } else {
            cv = new ContentValues();
        }

        Long now = Long.valueOf(System.currentTimeMillis()/1000);

        if (cv.containsKey(ApplicationsColumns.DATA) == false) {
            cv.put(ApplicationsColumns.DATA, "");
        }

        if (cv.containsKey(ApplicationsColumns.SIZE) == false) {
            cv.put(ApplicationsColumns.SIZE, 0);
        }

        if (cv.containsKey(ApplicationsColumns.DISPLAY_NAME) == false) {
            cv.put(ApplicationsColumns.DISPLAY_NAME, "");
        }

        if (cv.containsKey(ApplicationsColumns.TITLE) == false) {
            cv.put(ApplicationsColumns.TITLE, "");
        }

        if (cv.containsKey(ApplicationsColumns.DATE_ADDED) == false) {
            cv.put(ApplicationsColumns.DATE_ADDED, now);
        }

        if (cv.containsKey(ApplicationsColumns.DATE_MODIFIED) == false) {
            cv.put(ApplicationsColumns.DATE_MODIFIED, now);
        }

        if (cv.containsKey(ApplicationsColumns.MIME_TYPE) == false) {
            cv.put(ApplicationsColumns.MIME_TYPE, Constants.MIME_TYPE_ANDROID_PACKAGE_ARCHIVE);
        }

        if (cv.containsKey(ApplicationsColumns.PACKAGE_NAME) == false) {
            cv.put(ApplicationsColumns.PACKAGE_NAME, "");
        }

        if (cv.containsKey(ApplicationsColumns.VERSION) == false) {
            cv.put(ApplicationsColumns.VERSION, "");
        }

        SQLiteDatabase db = databaseHelper.getWritableDatabase();

        long rowId = db.insert(APPLICATIONS_TABLE_NAME, "", cv);

        if (rowId > 0) {
            Uri applicationUri = ContentUris.withAppendedId(Applications.Media.CONTENT_URI, rowId);
            getContext().getContentResolver().notifyChange(applicationUri, null);

            return applicationUri;
        }

        throw new SQLException("Failed to insert row into " + uri);
    }

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        if (!accept()) {
            return 0;
        }

        SQLiteDatabase db = databaseHelper.getWritableDatabase();

        int count;

        switch (uriMatcher.match(uri)) {
        case APPLICATIONS_ALL:
            count = db.delete(APPLICATIONS_TABLE_NAME, where, whereArgs);
            break;

        case APPLICATIONS_ID:
            String applicationId = uri.getPathSegments().get(1);
            count = db.delete(APPLICATIONS_TABLE_NAME, ApplicationsColumns._ID + "=" + applicationId + (!TextUtils.isEmpty(where) ? " AND (" + where + ")" : ""), whereArgs);
            break;

        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);

        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
        if (!accept()) {
            return 0;
        }

        SQLiteDatabase db = databaseHelper.getWritableDatabase();

        int count;

        switch (uriMatcher.match(uri)) {
        case APPLICATIONS_ALL:
            count = db.update(APPLICATIONS_TABLE_NAME, values, where, whereArgs);
            break;

        case APPLICATIONS_ID:
            String applicationId = uri.getPathSegments().get(1);
            count = db.update(APPLICATIONS_TABLE_NAME, values, ApplicationsColumns._ID + "=" + applicationId + (!TextUtils.isEmpty(where) ? " AND (" + where + ")" : ""), whereArgs);
            break;

        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);

        return count;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        if (!accept()) {
            return null;
        }

        File root = new File(Environment.getExternalStorageDirectory(), "/Android/data/com.bt.download.android/cache");
        root.mkdirs();
        File path = new File(root, uri.getEncodedPath().replace("/", "_"));

        int imode = 0;
        if (mode.contains("w")) {
            imode |= ParcelFileDescriptor.MODE_WRITE_ONLY;
            if (!path.exists()) {
                try {
                    path.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        if (mode.contains("r")) {
            imode |= ParcelFileDescriptor.MODE_READ_ONLY;
        }
        if (mode.contains("+")) {
            imode |= ParcelFileDescriptor.MODE_APPEND;
        }

        return ParcelFileDescriptor.open(path, imode);
    }

    private boolean accept() {
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
    }

    /**
     * This class helps open, create, and upgrade the database file.
     */
    private static class DatabaseHelper extends SQLiteOpenHelper {

        public DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(final SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + APPLICATIONS_TABLE_NAME + " (" + ApplicationsColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," + ApplicationsColumns.DATA + " TEXT," + ApplicationsColumns.SIZE + " INTEGER," + ApplicationsColumns.DISPLAY_NAME + " TEXT," + ApplicationsColumns.TITLE
                    + " TEXT," + ApplicationsColumns.DATE_ADDED + " INTEGER," + ApplicationsColumns.DATE_MODIFIED + " INTEGER," + ApplicationsColumns.MIME_TYPE + " TEXT," + ApplicationsColumns.PACKAGE_NAME + " TEXT," + ApplicationsColumns.VERSION + " TEXT" + ");");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading applications database from version " + oldVersion + " to " + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS " + APPLICATIONS_TABLE_NAME);
            onCreate(db);
        }
    }
}