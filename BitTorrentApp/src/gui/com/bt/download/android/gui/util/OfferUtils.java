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

package com.bt.download.android.gui.util;

import com.bt.download.android.core.ConfigurationManager;
import com.bt.download.android.core.Constants;
import com.frostwire.logging.Logger;

import android.content.Context;

public class OfferUtils {

    private static final Logger LOG = Logger.getLogger(OfferUtils.class);

    /**
     * True if user has enabled support for frostwire, Appia is enabled and it's not an Amazon distribution build.
     * @return
     */
    public static boolean isfreeAppsEnabled() {
        ConfigurationManager config = null;
        boolean isFreeAppsEnabled = false;
        try {
            config = ConfigurationManager.instance();
            isFreeAppsEnabled = (config.getBoolean(Constants.PREF_KEY_GUI_SUPPORT_FROSTWIRE) && config.getBoolean(Constants.PREF_KEY_GUI_INITIALIZE_APPIA)) && !OSUtils.isAmazonDistribution();
            //config.getBoolean(Constants.PREF_KEY_GUI_SHOW_FREE_APPS_MENU_ITEM);
        } catch (Throwable t) {
        }
        return isFreeAppsEnabled;
    }

    public static boolean isAppiaSearchEnabled() {
        ConfigurationManager config = null;
        boolean isAppiaSearchEnabled = false;
        try {
            config = ConfigurationManager.instance();
            isAppiaSearchEnabled = (config.getBoolean(Constants.PREF_KEY_GUI_SUPPORT_FROSTWIRE) && config.getBoolean(Constants.PREF_KEY_GUI_USE_APPIA_SEARCH)) && !OSUtils.isAmazonDistribution();
        } catch (Throwable t) {
        }
        return isAppiaSearchEnabled;
    }

    public static void startOffercastLockScreen(final Context context) throws Exception {
        if (!OSUtils.isAmazonDistribution()) {
            try {
                /*
                OffercastSDK offercast = OffercastSDK.getInstance(context);
                offercast.authorize();
                LOG.info("Offercast started.");
                 */
            } catch (Exception e) {
                LOG.error("Offercast could not start.", e);
            }
        }
    }
}