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

package com.frostwire.search.torlock;

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
public class TorLockSearchPerformer extends TorrentRegexSearchPerformer<TorLockSearchResult> {

    private static final int MAX_RESULTS = 10;
    private static final String REGEX = "(?is)<a href=/torrent/([0-9]*?/.*?\\.html)>";
    private static final String HTML_REGEX = "(?is).*?<td><b>Name:</b></td><td>(.*?).torrent</td>.*?<td><b>Size:</b></td><td>(.*?) in .*? file.*?</td>.*?<td><b>Added:</b></td><td>Uploaded on (.*?) by .*?</td>.*?<font color=#FF5400><b>(.*?)</b></font> seeders.*?<td align=center><a href=\"/tor/(.*?).torrent\"><img.*?";

    public TorLockSearchPerformer(DomainAliasManager domainAliasManager, long token, String keywords, int timeout) {
        super(domainAliasManager, token, keywords, timeout, 1, 2 * MAX_RESULTS, MAX_RESULTS, REGEX, HTML_REGEX);
    }

    @Override
    protected String getUrl(int page, String encodedKeywords) {
        String transformedKeywords = encodedKeywords.replace("0%20", "-");
        return "http://" + getDomainNameToUse() + "/all/torrents/" + transformedKeywords + ".html";
    }

    @Override
    public CrawlableSearchResult fromMatcher(SearchMatcher matcher) {
        String itemId = matcher.group(1);
        return new TorLockTempSearchResult(getDomainNameToUse(),itemId);
    }

    @Override
    protected TorLockSearchResult fromHtmlMatcher(CrawlableSearchResult sr, SearchMatcher matcher) {
        return new TorLockSearchResult(getDomainNameToUse(),sr.getDetailsUrl(), matcher);
    }
    
    /*
    public static void main(String[] args) throws Exception {
        DomainAliasManagerBroker domainBroker = new DomainAliasManagerBroker();
        DomainAliasManager aliasManager = domainBroker.getDomainAliasManager("www.torlock.net");
        TorLockSearchPerformer performer = new TorLockSearchPerformer(aliasManager, 21312, "whatever", 5000);
        TorLockTempSearchResult tempSearchResult = new TorLockTempSearchResult(performer.getDomainName(), "2457897");
        
        byte[] data = FileUtils.readFileToByteArray(new File("/Users/gubatron/Desktop/torlocktest.html"));
        performer.crawlResult(tempSearchResult, data);
        
        //new TorLockTempSearchResult(performer.getDomainName(), tempSearchResult.getDetailsUrl(), )
    }
    */
}
