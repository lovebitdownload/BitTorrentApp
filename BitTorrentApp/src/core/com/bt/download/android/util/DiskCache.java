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

package com.bt.download.android.util;

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.frostwire.logging.Logger;
import com.squareup.okhttp.internal.DiskLruCache;
import com.squareup.okhttp.internal.DiskLruCache.Editor;
import com.squareup.okhttp.internal.DiskLruCache.Snapshot;
import com.squareup.okhttp.internal.Util;

/**
 * 
 * @author gubatron
 * @author aldenml
 *
 */
public final class DiskCache {

    private static final Logger LOG = Logger.getLogger(DiskCache.class);

    private static final int APP_VERSION = 1;
    private static final int VALUE_COUNT = 1;
    private static final int IO_BUFFER_SIZE = 4 * 1024;

    private final DiskLruCache cache;

    public DiskCache(File directory, long size) throws IOException {
        this.cache = DiskLruCache.open(directory, APP_VERSION, VALUE_COUNT, size);
    }

    public boolean containsKey(String key) {
        return cache.containsKey(encodeKey(key));
    }

    public Entry get(String key) {
        Entry entry = null;
        Snapshot snapshot = null;

        try {
            snapshot = cache.get(encodeKey(key));

            if (snapshot != null) {
                entry = new Entry(snapshot);
            }

        } catch (IOException e) {
            LOG.warn("Error getting value from internal DiskLruCache", e);
        }

        return entry;
    }

    public void put(String key, byte[] data) {
        Editor editor = null;

        try {
            editor = cache.edit(encodeKey(key));

            if (editor != null) {
                writeTo(editor, data);
                editor.commit();
            }

        } catch (IOException e) {
            LOG.warn("Error writing value to internal DiskLruCache", e);
            try {
                if (editor != null) {
                    editor.abort();
                }
            } catch (IOException ignored) {
            }
        }
    }

    public void put(String key, InputStream in) {
        Editor editor = null;

        try {
            editor = cache.edit(encodeKey(key));

            if (editor != null) {
                writeTo(editor, in);
                editor.commit();
            }

        } catch (IOException e) {
            LOG.warn("Error writing value to internal DiskLruCache", e);
            try {
                if (editor != null) {
                    editor.abort();
                }
            } catch (IOException ignored) {
            }
        }
    }

    public void remove(String key) {
        try {
            cache.remove(encodeKey(key));
        } catch (IOException e) {
            LOG.warn("Error deleting value from internal DiskLruCache: ", e);
        }
    }

    public long size() {
        return cache.size();
    }

    public long maxSize() {
        return cache.getMaxSize();
    }

    public void delete() throws IOException {
        cache.delete();
    }

    private void writeTo(Editor editor, byte[] data) throws IOException {
        OutputStream out = new BufferedOutputStream(editor.newOutputStream(0), IO_BUFFER_SIZE);
        try {
            out.write(data);
        } finally {
            out.close();
        }
    }

    private void writeTo(Editor editor, InputStream in) throws IOException {
        OutputStream out = new BufferedOutputStream(editor.newOutputStream(0), IO_BUFFER_SIZE);
        try {
            Util.copy(in, out); // using Util from okhttp only for consistency.
        } finally {
            out.close();
        }
    }

    private String encodeKey(String key) {
        return Util.hash(key);
    }

    public static final class Entry implements Closeable {

        private final Snapshot snapshot;

        public Entry(Snapshot snapshot) {
            this.snapshot = snapshot;
        }

        public InputStream getInputStream() {
            return new SnapshotInputStream(snapshot);
        }

        @Override
        public void close() {
            snapshot.close();
        }
    }

    private static final class SnapshotInputStream extends FilterInputStream {

        private final Snapshot snapshot;

        public SnapshotInputStream(Snapshot snapshot) {
            super(snapshot.getInputStream(0));
            this.snapshot = snapshot;
        }

        @Override
        public void close() throws IOException {
            snapshot.close();
            super.close();
        }
    }
}