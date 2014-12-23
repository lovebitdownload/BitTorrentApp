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
import android.util.Log;

import com.bt.download.android.core.providers.UniversalStore.Sharing;
import com.bt.download.android.core.providers.UniversalStore.Sharing.Media;
import com.bt.download.android.core.providers.UniversalStore.Sharing.SharingColumns;

/**
 * This provider is used to specify which files are shared or not.
 * If a file is new, it will not appear here until it's "unshared"
 * This helps to keep data synchronization to a minimum.
 *
 * @author gubatron
 * @author aldenml
 * 
 */
public class SharingProvider extends ContentProvider {

    private static final String TAG = "FW.SharingProvider";

    private static final String DATABASE_NAME = "sharing.db";
    private static final int DATABASE_VERSION = 1;
    private static final String SHARING_TABLE_NAME = "sharing";

    private static final int SHARING_ALL = 1;

    private static final UriMatcher uriMatcher;
    private static HashMap<String, String> sharingProjectionMap;

    private DatabaseHelper databaseHelper;

    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(UniversalStore.UNIVERSAL_SHARING_AUTHORITY, "sharing", SHARING_ALL);

        sharingProjectionMap = new HashMap<String, String>();
        sharingProjectionMap.put(SharingColumns._ID, SharingColumns._ID);
        sharingProjectionMap.put(SharingColumns.FILE_ID, SharingColumns.FILE_ID);
        sharingProjectionMap.put(SharingColumns.FILE_TYPE, SharingColumns.FILE_TYPE);
        sharingProjectionMap.put(SharingColumns.SHARED, SharingColumns.SHARED);
    }

    @Override
    public boolean onCreate() {

        databaseHelper = new DatabaseHelper(getContext());

        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        if (!accept()) {
            return null;
        }

        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        qb.setTables(SHARING_TABLE_NAME);

        switch (uriMatcher.match(uri)) {
        case SHARING_ALL:
            qb.setProjectionMap(sharingProjectionMap);
            break;

        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        // Get the database and run the query
        SQLiteDatabase db = databaseHelper.getReadableDatabase();

        Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, null);

        // Tell the cursor what uri to watch, so it knows when its source data
        // changes
        c.setNotificationUri(getContext().getContentResolver(), uri);

        return c;
    }

    @Override
    public String getType(Uri uri) {
        switch (uriMatcher.match(uri)) {
        case SHARING_ALL:
            return Media.CONTENT_TYPE;
        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        if (!accept()) {
            return null;
        }

        // Validate the requested uri
        if (uriMatcher.match(uri) != SHARING_ALL) {
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        ContentValues values;

        if (initialValues != null) {
            values = new ContentValues(initialValues);
        } else {
            values = new ContentValues();
        }

        // Make sure that the fields are all set
        if (values.containsKey(SharingColumns.FILE_ID) == false) {
            values.put(SharingColumns.FILE_ID, -1);
        }

        if (values.containsKey(SharingColumns.FILE_TYPE) == false) {
            values.put(SharingColumns.FILE_TYPE, -1);
        }

        if (values.containsKey(SharingColumns.SHARED) == false) {
            values.put(SharingColumns.SHARED, 0);
        }

        SQLiteDatabase db = databaseHelper.getWritableDatabase();

        long rowId = db.insert(SHARING_TABLE_NAME, "", values);

        if (rowId > 0) {
            Uri sharingUri = ContentUris.withAppendedId(Sharing.Media.CONTENT_URI, rowId);
            getContext().getContentResolver().notifyChange(sharingUri, null);

            return sharingUri;
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
        case SHARING_ALL:
            count = db.delete(SHARING_TABLE_NAME, where, whereArgs);
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
        case SHARING_ALL:
            count = db.update(SHARING_TABLE_NAME, values, where, whereArgs);
            break;
        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);

        return count;
    }

    private boolean accept() {
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
    }

    /**
     * This class helps open, create, and upgrade the database file.
     */
    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + SHARING_TABLE_NAME + " (" + SharingColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," + SharingColumns.FILE_ID + " INTEGER," + SharingColumns.FILE_TYPE + " INTEGER," + SharingColumns.SHARED + " INTEGER);");

            db.execSQL("CREATE INDEX idx_sharing ON sharing (" + SharingColumns.FILE_TYPE + "," + SharingColumns.SHARED + ")");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading sharing database from version " + oldVersion + " to " + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS " + SHARING_TABLE_NAME);
            db.execSQL("DROP INDEX IF EXISTS idx_sharing");
            onCreate(db);
        }
    }
}