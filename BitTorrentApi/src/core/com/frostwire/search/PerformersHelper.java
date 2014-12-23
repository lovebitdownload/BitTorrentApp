/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011-2014,, FrostWire(R). All rights reserved.
 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.frostwire.search;

import com.frostwire.jlibtorrent.FileEntry;
import com.frostwire.jlibtorrent.FileStorage;
import com.frostwire.jlibtorrent.TorrentInfo;
import com.frostwire.search.torrent.TorrentCrawlableSearchResult;
import com.frostwire.search.torrent.TorrentCrawledSearchResult;
import com.google.code.regexp.Matcher;
import com.google.code.regexp.Pattern;
import org.gudy.azureus2.core3.torrent.TOTorrentException;

import java.util.LinkedList;
import java.util.List;

import static com.google.code.regexp.Pattern.compile;

/**
 * @author gubatron
 * @author aldenml
 */
public final class PerformersHelper {
    private static final Pattern MAGNET_HASH_PATTERN = Pattern.compile("magnet\\:\\?xt\\=urn\\:btih\\:([a-fA-F0-9]){40}");

    private PerformersHelper() {
    }

    public static List<? extends SearchResult> searchPageHelper(RegexSearchPerformer<?> performer, String page, int regexMaxResults) {
        List<SearchResult> result = new LinkedList<SearchResult>();
        SearchMatcher matcher = SearchMatcher.from(performer.getPattern().matcher(new MaxIterCharSequence(page, 2 * page.length())));
        int max = regexMaxResults;
        int i = 0;
        boolean matcherFound = false;

        do {
            try {
                matcherFound = matcher.find();
            } catch (Throwable t) {
                matcherFound = false;
                t.printStackTrace();
            }

            if (matcherFound) {
                SearchResult sr = performer.fromMatcher(matcher);
                if (sr != null) {
                    result.add(sr);
                    i++;
                }
            }
        } while (matcherFound && i < max && !performer.isStopped());

        return result;
    }

    /**
     * This method is only public allow reuse inside the package search, consider it a private API
     */
    public static List<? extends SearchResult> crawlTorrent(SearchPerformer performer, TorrentCrawlableSearchResult sr, byte[] data) throws TOTorrentException {
        List<TorrentCrawledSearchResult> list = new LinkedList<TorrentCrawledSearchResult>();

        TorrentInfo ti = TorrentInfo.bdecode(data);

        int numFiles = ti.getNumFiles();

        for (int i = 0; !performer.isStopped() && i < numFiles; i++) {
            FileEntry fe = ti.getFileAt(i);

            if (fe.isPadFile() || fe.hasHiddenAttribute()) {
                continue;
            }

            list.add(new TorrentCrawledSearchResult(sr, ti, i, fe));
        }

        return list;
    }

    public static String parseInfoHash(String url) {
        String result = null;
        final Matcher matcher = MAGNET_HASH_PATTERN.matcher(url);
        try {
            if (matcher.find()) {
                result = matcher.group(1);
            }
        } catch (Throwable t) {
            System.out.println("Could not parse magnet out of " + url);
            t.printStackTrace();
        }
        return result;
    }
}