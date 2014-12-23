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

package com.frostwire.search.bitsnoop;

import com.frostwire.search.CrawlableSearchResult;
import com.frostwire.search.SearchMatcher;
import com.frostwire.search.domainalias.DomainAliasManager;
import com.frostwire.search.torrent.TorrentRegexSearchPerformer;

/**
 * 
 * @author gubatron
 * @author aldenml
 *
 */
public class BitSnoopSearchPerformer extends TorrentRegexSearchPerformer<BitSnoopSearchResult> {

    private static final int MAX_RESULTS = 10;
    private static final String REGEX = "(?is)<span class=\"icon cat.*?</span> <a href=\"(.*?)\">.*?<div class=\"torInfo\"";
    private static final String HTML_REGEX = "(?is).*?Help</a>, <a href=\"magnet:\\?xt=urn:btih:([0-9a-fA-F]{40})&dn=(.*?)\" onclick=\".*?Magnet</a>.*?<a href=\"(.*?)\" title=\".*?\" class=\"dlbtn.*?title=\"Torrent Size\"><strong>(.*?)</strong>.*?title=\"Availability\"></span>(.*?)</span></td>.*?<li>Added to index &#8212; (.*?) \\(.{0,50}?\\)</li>.*?";

    public BitSnoopSearchPerformer(DomainAliasManager domainAliasManager, long token, String keywords, int timeout) {
        super(domainAliasManager, token, keywords, timeout, 1, 2 * MAX_RESULTS, MAX_RESULTS, REGEX, HTML_REGEX);
    }

    @Override
    protected String getUrl(int page, String encodedKeywords) {
        return "http://"+getDomainNameToUse()+"/search/all/" + encodedKeywords + "/c/d/" + page + "/";
    }

    @Override
    public CrawlableSearchResult fromMatcher(SearchMatcher matcher) {
        String itemId = matcher.group(1);
        return new BitSnoopTempSearchResult(getDomainNameToUse(), itemId);
    }

    @Override
    protected BitSnoopSearchResult fromHtmlMatcher(CrawlableSearchResult sr, SearchMatcher matcher) {
        return new BitSnoopSearchResult(sr.getDetailsUrl(), matcher);
    }
}
