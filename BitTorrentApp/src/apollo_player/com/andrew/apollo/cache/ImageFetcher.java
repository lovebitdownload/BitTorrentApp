/*
 * Copyright (C) 2012 Andrew Neal Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.andrew.apollo.cache;

import com.andrew.apollo.MusicPlaybackService;
import com.andrew.apollo.utils.MusicUtils;
import com.bt.download.android.R;
import com.bt.download.android.util.ImageUtils;

import android.content.ContentUris;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Looper;
import android.widget.ImageView;

/**
 * A subclass of {@link ImageWorker} that fetches images from a URL.
 */
public class ImageFetcher {

    /**
     * Used to distinguish album art from artist images
     */
    public static final String ALBUM_ART_SUFFIX = "album";

    public static final int IO_BUFFER_SIZE_BYTES = 1024;

    private final Context context;

    /**
     * Default album art
     */
    private final Bitmap mDefault;

    private static ImageFetcher sInstance = null;

    /**
     * Creates a new instance of {@link ImageFetcher}.
     *
     * @param context The {@link Context} to use.
     */
    public ImageFetcher(final Context context) {
        this.context = context.getApplicationContext();
        this.mDefault = ((BitmapDrawable) context.getResources().getDrawable(R.drawable.artwork_default)).getBitmap();
    }

    /**
     * Used to create a singleton of the image fetcher
     *
     * @param context The {@link Context} to use
     * @return A new instance of this class.
     */
    public static final ImageFetcher getInstance(final Context context) {
        if (sInstance == null) {
            sInstance = new ImageFetcher(context.getApplicationContext());
        }
        return sInstance;
    }

    /**
     * Used to fetch the current artwork.
     */
    public void loadCurrentArtwork(final ImageView imageView) {
        loadImage(generateAlbumCacheKey(MusicUtils.getAlbumName(), MusicUtils.getArtistName()), MusicUtils.getArtistName(), MusicUtils.getAlbumName(), MusicUtils.getCurrentAlbumId(), imageView);
    }

    /**
     * Finds cached or downloads album art. Used in {@link MusicPlaybackService}
     * to set the current album art in the notification and lock screen
     *
     * @param albumName The name of the current album
     * @param albumId The ID of the current album
     * @param artistName The album artist in case we should have to download
     *            missing artwork
     * @return The album art as an {@link Bitmap}
     */
    public Bitmap getArtwork(final String albumName, final long albumId, final String artistName) {
        if (albumId < 0) {
            return null;
        }

        Bitmap artwork = null;

        Uri uri = ContentUris.withAppendedId(ImageUtils.ALBUM_THUMBNAILS_URI, albumId);

        if (isMain()) {
            artwork = ImageUtils.getAlbumArt(context, String.valueOf(albumId));
        } else {
            artwork = ImageUtils.get(context, uri);
        }

        return artwork != null ? artwork : getDefaultArtwork();
    }

    /**
     * Generates key used by album art cache. It needs both album name and artist name
     * to let to select correct image for the case when there are two albums with the
     * same artist.
     *
     * @param albumName The album name the cache key needs to be generated.
     * @param artistName The artist name the cache key needs to be generated.
     * @return
     */
    public static String generateAlbumCacheKey(final String albumName, final String artistName) {
        if (albumName == null || artistName == null) {
            return null;
        }
        return new StringBuilder(albumName).append("_").append(artistName).append("_").append(ALBUM_ART_SUFFIX).toString();
    }

    /**
     * @return The deafult artwork
     */
    public Bitmap getDefaultArtwork() {
        return mDefault;
    }

    protected void loadImage(final String key, final String artistName, final String albumName, final long albumId, final ImageView imageView) {
        Uri uri = ContentUris.withAppendedId(ImageUtils.ALBUM_THUMBNAILS_URI, albumId);
        ImageUtils.load(context, uri, imageView, R.drawable.artwork_default);
    }

    static boolean isMain() {
        return Looper.getMainLooper().getThread() == Thread.currentThread();
    }
}
