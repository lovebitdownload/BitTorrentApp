package com.andrew.apollo.appwidgets;

import com.andrew.apollo.MusicPlaybackService;

public class RecentWidgetProvider {

    public static final String CMDAPPWIDGETUPDATE = "app_widget_recents_update";

    private static RecentWidgetProvider mInstance;

    public static synchronized RecentWidgetProvider getInstance() {
        if (mInstance == null) {
            mInstance = new RecentWidgetProvider();
        }
        return mInstance;
    }

    public void notifyChange(MusicPlaybackService musicPlaybackService, String what) {
    }

    public void performUpdate(final MusicPlaybackService service, final int[] appWidgetIds) {
    }
}
