package com.andrew.apollo.appwidgets;

import com.andrew.apollo.MusicPlaybackService;

public class AppWidgetLarge {
    
    public static final String CMDAPPWIDGETUPDATE = "app_widget_large_update";

    private static AppWidgetLarge mInstance;

    public static synchronized AppWidgetLarge getInstance() {
        if (mInstance == null) {
            mInstance = new AppWidgetLarge();
        }
        return mInstance;
    }

    public void notifyChange(MusicPlaybackService musicPlaybackService, String what) {
    }
    
    public void performUpdate(final MusicPlaybackService service, final int[] appWidgetIds) {
    }
}
