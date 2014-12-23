package com.frostwire.search.appia;

import com.bt.download.android.core.MediaType;
import com.frostwire.search.AbstractFileSearchResult;
import com.frostwire.search.torrent.ComparableTorrentJsonItem;

public class AppiaSearchResult extends AbstractFileSearchResult implements ComparableTorrentJsonItem {

    public String clickProxyURL;
    public String impressionTrackingURL;
    public String displayName;
    public String description;
    public String thumbnailURL;
    public String appId;
    public String categoryName;
    
    //kept for cloning purposes.
    private final AppiaServletResponseItem servletResponseItem;
    
    private final MediaType mediaType;
    
    private static final String CAT_ANDROID_GAMES_ALL_GAMES = "9";
    private static final String CAT_ANDROID_APPS_ALL_APPS = "33";
    private static final String CAT_ANDROID_APPS_BOOKS_REFERENCE = "2";
    private static final String CAT_ANDROID_APPS_MEDIA_N_VIDEO = "19";
    protected static final String CAT_ANDROID_APPS_MUSIC = "21";
    private static final String CAT_ANDROID_APPS_PHOTOGRAPHY = "24";
    
    public AppiaSearchResult(AppiaServletResponseItem item, String appiaCategoryId) {
        servletResponseItem = item;
        
        clickProxyURL = item.clickProxyURL;
        impressionTrackingURL = item.impressionTrackingURL;
        displayName = item.displayName;
        description = item.description;
        thumbnailURL = item.thumbnailURL;
        appId = item.appId;
        categoryName = item.categoryName;
        
        if (appiaCategoryId.equals(CAT_ANDROID_APPS_MUSIC)) {
            mediaType = MediaType.getAudioMediaType();
        } else if (appiaCategoryId.equals(CAT_ANDROID_APPS_MEDIA_N_VIDEO)) {
            mediaType = MediaType.getVideoMediaType();
        } else if (appiaCategoryId.equals(CAT_ANDROID_APPS_PHOTOGRAPHY)) {
            mediaType = MediaType.getImageMediaType();
        } else if (appiaCategoryId.equals(CAT_ANDROID_APPS_ALL_APPS)) {
            mediaType = MediaType.getApplicationsMediaType();
        } else if (appiaCategoryId.equals(CAT_ANDROID_APPS_BOOKS_REFERENCE)) {
            mediaType = MediaType.getDocumentMediaType();
        } else if (appiaCategoryId.equals(CAT_ANDROID_GAMES_ALL_GAMES)) {
            mediaType = MediaType.getTorrentMediaType();
        } else {
            mediaType = null;
        }
    }
    
    /** Copy constructor in case you want an instance on another media type, no mutability. */
    public AppiaSearchResult(AppiaSearchResult searchResult, String appiaCatId) {
        //yes, the category name won't be the corresponding name, but we just don't have it
        //if we have to do this.
        this(searchResult.getAppiaServletResponseItem(), appiaCatId);
    }
    
    public String getImpressionTrackingURL() {
        return impressionTrackingURL;
    }
    
    public String getDescription() {
        return description;
    }
    
    @Override
    public String getThumbnailUrl() {
        return thumbnailURL;
    }
    
    public String getAppId() {
        return appId;
    }
    
    public String getCategoryName() {
        return categoryName;
    }
    
    @Override
    public int getSeeds() {
        return 5000;
    }

    @Override
    public String getFilename() {
        return displayName;
    }

    @Override
    public long getSize() {
        return -1;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String getDetailsUrl() {
        return clickProxyURL;
    }

    @Override
    public String getSource() {
        return "Appia";
    }
    
    public MediaType getMediaType() {
        return mediaType;
    }

    /** to be used in copy/update constructor */
    private AppiaServletResponseItem getAppiaServletResponseItem() {
        return servletResponseItem;
    }
}