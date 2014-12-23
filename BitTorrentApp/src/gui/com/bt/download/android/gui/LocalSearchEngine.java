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

package com.bt.download.android.gui;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import android.text.Html;

import com.bt.download.android.core.ConfigurationManager;
import com.bt.download.android.core.Constants;
import com.frostwire.util.StringUtils;
import com.frostwire.search.CrawlPagedWebSearchPerformer;
import com.frostwire.search.CrawledSearchResult;
import com.frostwire.search.FileSearchResult;
import com.frostwire.search.SearchManager;
import com.frostwire.search.SearchManagerImpl;
import com.frostwire.search.SearchManagerListener;
import com.frostwire.search.SearchPerformer;
import com.frostwire.search.SearchResult;
import com.frostwire.search.torrent.TorrentSearchResult;
import com.frostwire.search.youtube.YouTubeCrawledSearchResult;

/**
 * @author gubatron
 * @author aldenml
 * 
 */
public final class LocalSearchEngine {

    private final SearchManager manager;

    // filter constants
    private final int MIN_SEEDS_TORRENT_RESULT;

    private SearchManagerListener listener;

    private long currentSearchToken;
    private List<String> currentSearchTokens;

    private boolean searchFinished;

    private String androidId;

    private static LocalSearchEngine instance;

    public synchronized static void create(String androidId) {
        if (instance != null) {
            return;
        }
        instance = new LocalSearchEngine(androidId);
    }
    
    public static LocalSearchEngine instance() {
        return instance;
    }

    private LocalSearchEngine(String androidId) {
        this.manager = new SearchManagerImpl();
        this.manager.registerListener(new ManagerListener());
        this.MIN_SEEDS_TORRENT_RESULT = ConfigurationManager.instance().getInt(Constants.PREF_KEY_SEARCH_MIN_SEEDS_FOR_TORRENT_RESULT);
        this.androidId = androidId;
    }
    
    public String getAndroidId() {
        return androidId;
    }

    public void registerListener(SearchManagerListener listener) {
        this.listener = listener;
    }

    public void performSearch(String query) {
        if (StringUtils.isNullOrEmpty(query, true)) {
            return;
        }

        manager.stop();

        currentSearchToken = Math.abs(System.nanoTime());
        currentSearchTokens = tokenize(query);
        searchFinished = false;

        for (SearchEngine se : SearchEngine.getEngines()) {
            if (se.isEnabled()) {
                SearchPerformer p = se.getPerformer(currentSearchToken, query);
                manager.perform(p);
            }
        }
    }

    public void cancelSearch() {
        manager.stop();
        currentSearchToken = 0;
        currentSearchTokens = null;
        searchFinished = true;
    }

    public boolean isSearchStopped() {
        return currentSearchToken == 0;
    }

    public boolean isSearchFinished() {
        return searchFinished;
    }

    public void clearCache() {
        CrawlPagedWebSearchPerformer.clearCache();
    }

    public long getCacheSize() {
        return CrawlPagedWebSearchPerformer.getCacheSize();
    }

    private void onFinished(long token) {
        searchFinished = true;
        if (listener != null) {
            listener.onFinished(token);
        }
    }

    private List<SearchResult> filter(SearchPerformer performer, List<SearchResult> results) {
        List<SearchResult> list;

        if (currentSearchTokens == null || currentSearchTokens.isEmpty()) {
            list = Collections.emptyList();
        } else {
            list = filter(results);
        }

        return list;
    }

    private List<SearchResult> filter(List<? extends SearchResult> results) {
        List<SearchResult> list = new LinkedList<SearchResult>();

        try {
            for (SearchResult sr : results) {
                if (sr instanceof TorrentSearchResult) {
                    if (((TorrentSearchResult) sr).getSeeds() == -1) {
                        long creationTime = ((TorrentSearchResult) sr).getCreationTime();
                        long age = System.currentTimeMillis() - creationTime;
                        if (age > 31536000000l) {
                            continue;
                        }
                    } else if (((TorrentSearchResult) sr).getSeeds() < MIN_SEEDS_TORRENT_RESULT) {
                        continue;
                    }
                }

                if (sr instanceof CrawledSearchResult) {
                    if (sr instanceof YouTubeCrawledSearchResult) {
                        // special case for flv files
                        if (!((YouTubeCrawledSearchResult) sr).getFilename().endsWith(".flv")) {
                            list.add(sr);
                        }
                    } else if (filter(new LinkedList<String>(currentSearchTokens), sr)) {
                        list.add(sr);
                    }
                } else {
                    list.add(sr);
                }
            }
        } catch (Throwable e) {
            // possible NPE due to cancel search or some inner error in search results, ignore it and cleanup list
            list.clear();
        }

        return list;
    }

    private boolean filter(List<String> tokens, SearchResult sr) {
        StringBuilder sb = new StringBuilder();

        sb.append(sr.getDisplayName());
        if (sr instanceof CrawledSearchResult) {
            sb.append(((CrawledSearchResult) sr).getParent().getDisplayName());
        }

        if (sr instanceof FileSearchResult) {
            sb.append(((FileSearchResult) sr).getFilename());
        }

        String str = sanitize(sb.toString());
        str = normalize(str);

        Iterator<String> it = tokens.iterator();
        while (it.hasNext()) {
            String token = it.next();
            if (str.contains(token)) {
                it.remove();
            }
        }

        return tokens.isEmpty();
    }

    private String sanitize(String str) {
        str = Html.fromHtml(str).toString();
        str = str.replaceAll("\\.torrent|www\\.|\\.com|\\.net|[\\\\\\/%_;\\-\\.\\(\\)\\[\\]\\n\\rÐ&~{}\\*@\\^'=!,¡|#ÀÁ]", " ");
        str = StringUtils.removeDoubleSpaces(str);

        return str.trim();
    }

    private String normalize(String token) {
        String norm = Normalizer.normalize(token, Normalizer.Form.NFKD);
        norm = norm.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        norm = norm.toLowerCase(Locale.US);

        return norm;
    }

    private Set<String> normalizeTokens(Set<String> tokens) {
        Set<String> normalizedTokens = new HashSet<String>();

        for (String token : tokens) {
            String norm = normalize(token);
            normalizedTokens.add(norm);
        }

        return normalizedTokens;
    }

    private List<String> tokenize(String keywords) {
        keywords = sanitize(keywords);

        Set<String> tokens = new HashSet<String>(Arrays.asList(keywords.toLowerCase(Locale.US).split(" ")));

        return new ArrayList<String>(normalizeTokens(tokens));
    }

    private final class ManagerListener implements SearchManagerListener {

        @Override
        public void onResults(SearchPerformer performer, List<? extends SearchResult> results) {
            if (listener != null && !performer.isStopped()) {
                if (performer.getToken() == currentSearchToken) { // one more additional protection
                    @SuppressWarnings("unchecked")
                    List<SearchResult> filtered = filter(performer, (List<SearchResult>) results);
                    if (!filtered.isEmpty()) {
                        listener.onResults(performer, filtered);
                    }
                } else {
                    performer.stop(); // why? just in case there is an inner error in an alternative search manager
                }
            }
        }

        @Override
        public void onFinished(long token) {
            if (token == currentSearchToken) {
                LocalSearchEngine.this.onFinished(token);
            }
        }
    }
}
