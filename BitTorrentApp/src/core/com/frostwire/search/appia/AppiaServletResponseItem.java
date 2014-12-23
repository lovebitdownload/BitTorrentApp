package com.frostwire.search.appia;

public class AppiaServletResponseItem {
    public String displayName;
    public String description;
    public String categoryName;
    
    /** A thumbnail URL. */
    public String thumbnailURL;
    
    /** The URL we're to send the user to */
    public String clickProxyURL;

    /** Tracking "Pixel" URL to be invoked on the client if the ad is shown*/
    public String impressionTrackingURL;
    
    /** Could use this appId==package name, to check if app is already there */
    public String appId;
    
    /** e.g. "Android 2.2" */
    public String minOSVersion;
}