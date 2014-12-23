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

package com.frostwire.search.youtube;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import com.frostwire.search.CrawlPagedWebSearchPerformer;
import com.frostwire.search.SearchResult;
import com.frostwire.search.domainalias.DomainAliasManager;
import com.frostwire.search.extractors.YouTubeExtractor;
import com.frostwire.search.extractors.YouTubeExtractor.LinkInfo;
import com.frostwire.util.JsonUtils;

/**
 * 
 * @author gubatron
 * @author aldenml
 *
 */
public class YouTubeSearchPerformer extends CrawlPagedWebSearchPerformer<YouTubeSearchResult> {

    private static final int MAX_RESULTS = 15;

    public YouTubeSearchPerformer(DomainAliasManager domainAliasManager, long token, String keywords, int timeout) {
        super(domainAliasManager, token, keywords, timeout, 1, MAX_RESULTS);
    }

    @Override
    protected String getCrawlUrl(YouTubeSearchResult sr) {
        return null;
    }

    @Override
    protected List<? extends SearchResult> crawlResult(YouTubeSearchResult sr, byte[] data) throws Exception {
        List<YouTubeCrawledSearchResult> list = new LinkedList<YouTubeCrawledSearchResult>();

        List<LinkInfo> infos = new YouTubeExtractor().extract(sr.getDetailsUrl(), sr.testConnection());

        LinkInfo dashVideo = null;
        LinkInfo dashAudio = null;
        LinkInfo demuxVideo = null;

        for (LinkInfo inf : infos) {
            if (!isDash(inf)) {
                list.add(new YouTubeCrawledStreamableSearchResult(sr, inf, null));
            } else {
                if (inf.fmt == 137) {// 1080p
                    dashVideo = inf;
                }
                if (inf.fmt == 141) {// 256k
                    dashAudio = inf;
                }
                if (inf.fmt == 140 && dashAudio == null) {// 128k
                    dashAudio = inf;
                }
                if (inf.fmt == 22 || inf.fmt == 84) {
                    demuxVideo = inf;
                }
            }
        }

        if (dashVideo != null && dashAudio != null) {
            list.add(new YouTubeCrawledSearchResult(sr, dashVideo, dashAudio));
        }

        if (dashAudio != null) {
            list.add(new YouTubeCrawledStreamableSearchResult(sr, null, dashAudio));
        } else {
            if (demuxVideo != null) {
                list.add(new YouTubeCrawledStreamableSearchResult(sr, null, demuxVideo));
            }
        }

        return list;
    }

    @Override
    protected String getUrl(int page, String encodedKeywords) {
        return String.format(Locale.US, "https://gdata.youtube.com/feeds/api/videos?q=%s&orderby=relevance&start-index=1&max-results=%d&alt=json&prettyprint=true&v=2", encodedKeywords, MAX_RESULTS);
    }

    @Override
    protected List<? extends SearchResult> searchPage(String page) {
        List<SearchResult> result = new LinkedList<SearchResult>();

        String json = fixJson(page);
        YouTubeResponse response = JsonUtils.toObject(json, YouTubeResponse.class);

        boolean testConnection = true;
        for (YouTubeEntry entry : response.feed.entry) {
            if (!isStopped()) {
                YouTubeSearchResult sr = new YouTubeSearchResult(entry, testConnection);
                result.add(sr);
                
                if (testConnection) {
                    testConnection = false;
                }
            }
        }

        return result;
    }

    private String fixJson(String json) {
        return json.replace("\"$t\"", "\"title\"").
                replace("\"yt$userId\"", "\"ytuserId\"").
                replace("\"media$group\"", "\"mediagroup\"").
                replace("\"media$content\"", "\"mediacontent\"");
    }

    private boolean isDash(LinkInfo info) {
        switch (info.fmt) {
        case 133:
        case 134:
        case 135:
        case 136:
        case 137:
        case 139:
        case 140:
        case 141:
            return true;
        default:
            return false;
        }
    }
}
