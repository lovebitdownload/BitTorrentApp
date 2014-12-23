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

package com.bt.download.android.gui;

import java.util.Arrays;
import java.util.List;

import com.bt.download.android.core.ConfigurationManager;
import com.bt.download.android.core.Constants;
import com.bt.download.android.gui.util.OSUtils;
import com.frostwire.logging.Logger;
import com.frostwire.search.SearchPerformer;
import com.frostwire.search.appia.AppiaSearchPerformer;
import com.frostwire.search.appia.AppiaSearchPerformer.AppiaSearchThrottle;
import com.frostwire.search.archiveorg.ArchiveorgSearchPerformer;
import com.frostwire.search.bitsnoop.BitSnoopSearchPerformer;
import com.frostwire.search.domainalias.DomainAliasManager;
import com.frostwire.search.extratorrent.ExtratorrentSearchPerformer;
import com.frostwire.search.eztv.EztvSearchPerformer;
import com.frostwire.search.frostclick.FrostClickSearchPerformer;
import com.frostwire.search.frostclick.UserAgent;
import com.frostwire.search.mininova.MininovaSearchPerformer;
import com.frostwire.search.monova.MonovaSearchPerformer;
import com.frostwire.search.soundcloud.SoundcloudSearchPerformer;
import com.frostwire.search.tbp.TPBSearchPerformer;
import com.frostwire.search.torlock.TorLockSearchPerformer;
import com.frostwire.search.torrentsfm.TorrentsfmSearchPerformer;
import com.frostwire.search.yify.YifySearchPerformer;
import com.frostwire.search.youtube.YouTubeSearchPerformer;

/**
 * 
 * @author gubatron
 * @author aldenml
 *
 */
public abstract class SearchEngine {
    private static final Logger LOG = Logger.getLogger(SearchEngine.class);
    public static final UserAgent FROSTWIRE_ANDROID_USER_AGENT = new UserAgent(OSUtils.getOSVersionString(), Constants.FROSTWIRE_VERSION_STRING, Constants.FROSTWIRE_BUILD);
    private static final int DEFAULT_TIMEOUT = 10000;

    private final String name;
    private final String preferenceKey;

    private boolean active;
    
    private SearchEngine(String name, String preferenceKey) {
        this.name = name;
        this.preferenceKey = preferenceKey;
        this.active = true;
    }

    public String getName() {
        return name;
    }

    public abstract SearchPerformer getPerformer(long token, String keywords);

    public String getPreferenceKey() {
        return preferenceKey;
    }

    public boolean isEnabled() {
        return isActive() && ConfigurationManager.instance().getBoolean(preferenceKey);
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public String toString() {
        return name;
    }

    public static List<SearchEngine> getEngines() {
        return ALL_ENGINES;
    }

    public static SearchEngine forName(String name) {
        for (SearchEngine engine : getEngines()) {
            if (engine.getName().equals(name)) {
                return engine;
            }
        }

        return null;
    }

    public static final SearchEngine EXTRATORRENT = new SearchEngine("Extratorrent", Constants.PREF_KEY_SEARCH_USE_EXTRATORRENT) {
        @Override
        public SearchPerformer getPerformer(long token, String keywords) {
            return new ExtratorrentSearchPerformer(new DomainAliasManager("extratorrent.cc"), token, keywords, DEFAULT_TIMEOUT);
        }
    };

    public static final SearchEngine MININOVA = new SearchEngine("Mininova", Constants.PREF_KEY_SEARCH_USE_MININOVA) {
        @Override
        public SearchPerformer getPerformer(long token, String keywords) {
            return new MininovaSearchPerformer(new DomainAliasManager("www.mininova.org"), token, keywords, DEFAULT_TIMEOUT);
        }
    };

    public static final SearchEngine YOUTUBE = new SearchEngine("YouTube", Constants.PREF_KEY_SEARCH_USE_YOUTUBE) {
        @Override
        public SearchPerformer getPerformer(long token, String keywords) {
            return new YouTubeSearchPerformer(new DomainAliasManager("gdata.youtube.com"), token, keywords, DEFAULT_TIMEOUT);
        }
    };

    public static final SearchEngine SOUNCLOUD = new SearchEngine("Soundcloud", Constants.PREF_KEY_SEARCH_USE_SOUNDCLOUD) {
        @Override
        public SearchPerformer getPerformer(long token, String keywords) {
            return new SoundcloudSearchPerformer(new DomainAliasManager("api.sndcdn.com"), token, keywords, DEFAULT_TIMEOUT);
        }
    };

    public static final SearchEngine ARCHIVE = new SearchEngine("Archive.org", Constants.PREF_KEY_SEARCH_USE_ARCHIVEORG) {
        @Override
        public SearchPerformer getPerformer(long token, String keywords) {
            return new ArchiveorgSearchPerformer(new DomainAliasManager("archive.org"), token, keywords, DEFAULT_TIMEOUT);
        }
    };

    public static final SearchEngine FROSTCLICK = new SearchEngine("FrostClick", Constants.PREF_KEY_SEARCH_USE_FROSTCLICK) {
        @Override
        public SearchPerformer getPerformer(long token, String keywords) {
            return new FrostClickSearchPerformer(new DomainAliasManager("api.frostclick.com"), token, keywords, DEFAULT_TIMEOUT, FROSTWIRE_ANDROID_USER_AGENT);
        }
    };

    public static final SearchEngine BITSNOOP = new SearchEngine("BitSnoop", Constants.PREF_KEY_SEARCH_USE_BITSNOOP) {
        @Override
        public SearchPerformer getPerformer(long token, String keywords) {
            return new BitSnoopSearchPerformer(new DomainAliasManager("bitsnoop.com"), token, keywords, DEFAULT_TIMEOUT);
        }
    };

    public static final SearchEngine TORLOCK = new SearchEngine("TorLock", Constants.PREF_KEY_SEARCH_USE_TORLOCK) {
        @Override
        public SearchPerformer getPerformer(long token, String keywords) {
            return new TorLockSearchPerformer(new DomainAliasManager("www.torlock.com"), token, keywords, DEFAULT_TIMEOUT);
        }
    };

    public static final SearchEngine EZTV = new SearchEngine("Eztv", Constants.PREF_KEY_SEARCH_USE_EZTV) {
        @Override
        public SearchPerformer getPerformer(long token, String keywords) {
            return new EztvSearchPerformer(new DomainAliasManager("eztv.it"), token, keywords, DEFAULT_TIMEOUT);
        }
    };
    
    public static final SearchEngine APPIA = new SearchEngine("Appia", Constants.PREF_KEY_SEARCH_USE_APPIA) {
        private AppiaSearchThrottle throttle = new AppiaSearchThrottle();
        
        @Override
        public SearchPerformer getPerformer(long token, String keywords) {
            return new AppiaSearchPerformer(new DomainAliasManager(AppiaSearchPerformer.HTTP_SERVER_NAME), token, keywords, DEFAULT_TIMEOUT, FROSTWIRE_ANDROID_USER_AGENT, LocalSearchEngine.instance().getAndroidId(), throttle);
        }
    };
    
    public static final SearchEngine TPB = new SearchEngine("TPB", Constants.PREF_KEY_SEARCH_USE_TPB) {
        @Override
        public SearchPerformer getPerformer(long token, String keywords) {
            TPBSearchPerformer performer = null;
            if (NetworkManager.instance().isDataWIFIUp()) {
                performer = new TPBSearchPerformer(new DomainAliasManager("thepiratebay.se"), token, keywords, DEFAULT_TIMEOUT);
            } else {
                LOG.info("No TPBSearchPerformer, WiFi not up");
            }
            return performer;
        }
    };
    
    public static final SearchEngine MONOVA = new SearchEngine("Monova", Constants.PREF_KEY_SEARCH_USE_MONOVA) {
        @Override
        public SearchPerformer getPerformer(long token, String keywords) {
            MonovaSearchPerformer performer = null;
            if (NetworkManager.instance().isDataWIFIUp()) {
                performer = new MonovaSearchPerformer(new DomainAliasManager("www.monova.org"), token, keywords, DEFAULT_TIMEOUT);
            } else {
                LOG.info("No MonovaSearchPerformer, WiFi not up");
            }
            return performer;
        }
    };
    
    public static final SearchEngine YIFY = new SearchEngine("Yify", Constants.PREF_KEY_SEARCH_USE_YIFY) {
        @Override
        public SearchPerformer getPerformer(long token, String keywords) {
            YifySearchPerformer performer = null;
            if (NetworkManager.instance().isDataWIFIUp()) {
                performer = new YifySearchPerformer(new DomainAliasManager("www.yify-torrent.org"), token, keywords, DEFAULT_TIMEOUT);
            } else {
                LOG.info("No YifySearchPerformer, WiFi not up");
            }
            return performer;
        }
    };
    
    public static final SearchEngine TORRENTSFM = new SearchEngine("Torrents.fm", Constants.PREF_KEY_SEARCH_USE_TORRENTSFM) {
        @Override
        public SearchPerformer getPerformer(long token, String keywords) {
            TorrentsfmSearchPerformer performer = null;
            if (NetworkManager.instance().isDataWIFIUp()) {
                performer = new TorrentsfmSearchPerformer(new DomainAliasManager("torrents.fm"), token, keywords, DEFAULT_TIMEOUT);
            } else {
                LOG.info("No TorrentsfmSearchPerformer, WiFi not up");
            }
            return performer;
        }
    };

    private static final List<SearchEngine> ALL_ENGINES = Arrays.asList(TPB, YIFY, YOUTUBE, FROSTCLICK, MONOVA, MININOVA, BITSNOOP, EXTRATORRENT, SOUNCLOUD, ARCHIVE, TORLOCK, EZTV, TORRENTSFM, APPIA);
}