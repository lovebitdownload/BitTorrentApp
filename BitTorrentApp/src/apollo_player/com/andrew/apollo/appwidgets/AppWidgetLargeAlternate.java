package com.andrew.apollo.appwidgets;

import com.andrew.apollo.MusicPlaybackService;

public class AppWidgetLargeAlternate {

    public static final String CMDAPPWIDGETUPDATE = "app_widget_large_alternate_update";

    private static AppWidgetLargeAlternate mInstance;

    public static synchronized AppWidgetLargeAlternate getInstance() {
        if (mInstance == null) {
            mInstance = new AppWidgetLargeAlternate();
        }
        return mInstance;
    }

    public void notifyChange(MusicPlaybackService musicPlaybackService, String what) {
    }

    public void performUpdate(final MusicPlaybackService service, final int[] appWidgetIds) {
    }
}
