
package com.bt.download.android.util;

import com.bt.download.android.R;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.ContentLengthInputStream;
import com.nostra13.universalimageloader.core.display.RoundedBitmapDisplayer;
import com.nostra13.universalimageloader.core.download.BaseImageDownloader;

import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Video;
import android.util.Log;
import android.widget.ImageView;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 加载图片的工具类
 */
public final class ImageUtils {

    private static final int MICRO_KIND = Images.Thumbnails.MICRO_KIND;

    private static final String SCHEME_IMAGE = "image";

    private static final String SCHEME_IMAGE_SLASH = SCHEME_IMAGE + "://";

    private static final String APPLICATION_AUTHORITY = "application";

    private static final String ALBUM_AUTHORITY = "album";

    private static final String VIDEO_AUTHORITY = "video";

    public static final Uri APPLICATION_THUMBNAILS_URI = Uri.parse(SCHEME_IMAGE_SLASH
            + APPLICATION_AUTHORITY);

    public static final Uri ALBUM_THUMBNAILS_URI = Uri.parse(SCHEME_IMAGE_SLASH + ALBUM_AUTHORITY);

    public static final Uri VIDEO_THUMBNAILS_URI = Uri.parse(SCHEME_IMAGE_SLASH + VIDEO_AUTHORITY);

    private static ImageLoader imageLoader = ImageLoader.getInstance();

    /**
     * WARNING: this method does not make use of the cache. it is here to be
     * used only (so far) on the notification window view and the RC Interface
     * (things like Lock Screen, Android Wear), which run on another process
     * space. If you try to use a cached image there, you will get some nasty
     * exceptions, therefore you will need this. For loading album art inside
     * the application Activities/Views/Fragments, take a look at
     * FileListAdapter and how it uses the ImageLoader.
     * 
     * @param context
     * @param albumId
     * @return
     */
    public static Bitmap getAlbumArt(Context context, String albumId) {
        Bitmap bitmap = null;
        try {
            Cursor cursor = context.getContentResolver().query(
                    Uri.withAppendedPath(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, albumId),
                    new String[] {
                        MediaStore.Audio.AlbumColumns.ALBUM_ART
                    }, null, null, null);
            try {
                if (cursor.moveToFirst()) {
                    String albumArt = cursor.getString(0);
                    bitmap = BitmapFactory.decodeFile(albumArt);
                }
            } finally {
                cursor.close();
            }
        } catch (Throwable e) {
            Log.e("", "Error getting album art", e);
        }

        return bitmap;
    }

    /**
     * 获取图片缩略图
     */
    public static Bitmap getImageThumbnail(Context mContext, long id) {
        return Images.Thumbnails.getThumbnail(mContext.getContentResolver(), id, MICRO_KIND, null);
    }

    /**
     * 获取视频缩略图
     */
    public static Bitmap getVideoThumbnail(Context mContext, long id) {
        return Video.Thumbnails.getThumbnail(mContext.getContentResolver(), id, MICRO_KIND, null);
    }

    private static DisplayImageOptions getDisplayImageOptions() {
        return new DisplayImageOptions.Builder().cacheInMemory(true).cacheOnDisc(true)
                .displayer(new RoundedBitmapDisplayer(2)).build();
    }

    private static DisplayImageOptions getDisplayImageOptions(int placeholderResId) {
        return new DisplayImageOptions.Builder().cacheInMemory(true).cacheOnDisc(true).showImageOnLoading(placeholderResId)
                .displayer(new RoundedBitmapDisplayer(2)).build();
    }

    public static void load(String imageUrl, ImageView target) {
        imageLoader.displayImage(imageUrl, target, getDisplayImageOptions());
    }

    public static void load(String imageUrl, ImageView target, int placeholderResId) {
        imageLoader.displayImage(imageUrl, target, getDisplayImageOptions(placeholderResId));
    }

    public static void load(Context context, Uri uri, ImageView target) {
        imageLoader.displayImage(uri.toString(), target, getDisplayImageOptions());
    }

    public static void load(Context context, Uri uri, ImageView target, int placeholderResId) {
        imageLoader.displayImage(uri.toString(), target, getDisplayImageOptions(placeholderResId));
    }

    public static Bitmap get(Context context, Uri uri) {
        return imageLoader.loadImageSync(uri.toString());
    }

    public static class ImageDownloader extends BaseImageDownloader {
        private Context mContext;

        public ImageDownloader(Context context) {
            super(context);

            this.mContext = context;
        }

        @Override
        protected InputStream getStreamFromOtherSource(String imageUri, Object extra)
                throws IOException {

            if (imageUri.startsWith(SCHEME_IMAGE)) {
                Bitmap bitmap = null;
                try {
                    if (imageUri.startsWith(APPLICATION_THUMBNAILS_URI.toString())) {
                        String pkgName = imageUri.substring(APPLICATION_THUMBNAILS_URI.toString()
                                .length() + 1);
                        PackageManager pm = context.getPackageManager();
                        BitmapDrawable icon = (BitmapDrawable)pm.getApplicationIcon(pkgName);
                        bitmap = icon.getBitmap();
                    } else if (imageUri.startsWith(ALBUM_THUMBNAILS_URI.toString())) {
                        String albumId = imageUri.substring(ALBUM_THUMBNAILS_URI.toString().length() + 1);
                        bitmap = getAlbumArt(context, albumId);
                    } else if (imageUri.startsWith(VIDEO_THUMBNAILS_URI.toString())) {
                        int id = Integer.valueOf(imageUri.substring(VIDEO_THUMBNAILS_URI.toString().length() + 1));
                        bitmap = getVideoThumbnail(mContext, id);
                    }

                    if (bitmap == null) {
                        bitmap = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.file_default);
                    }

                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                    byte[] imageInByte = stream.toByteArray();
                    ByteArrayInputStream bis = new ByteArrayInputStream(imageInByte);
                    return new ContentLengthInputStream(bis, imageInByte.length);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                return null;
            } else {
                return super.getStreamFromOtherSource(imageUri, extra);
            }
        }
    }
}
