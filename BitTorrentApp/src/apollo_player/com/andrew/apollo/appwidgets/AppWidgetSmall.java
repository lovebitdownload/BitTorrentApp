package com.andrew.apollo.appwidgets;

import com.andrew.apollo.MusicPlaybackService;

public class AppWidgetSmall {

    public static final String CMDAPPWIDGETUPDATE = "app_widget_small_update";

    private static AppWidgetSmall mInstance;

    public static synchronized AppWidgetSmall getInstance() {
        if (mInstance == null) {
            mInstance = new AppWidgetSmall();
        }
        return mInstance;
    }

    public void notifyChange(MusicPlaybackService musicPlaybackService, String what) {
    }

    public void performUpdate(final MusicPlaybackService service, final int[] appWidgetIds) {
    }
}
