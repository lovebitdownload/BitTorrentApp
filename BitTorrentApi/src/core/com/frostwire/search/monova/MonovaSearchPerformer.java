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

package com.frostwire.search.monova;

import java.io.IOException;

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
public class MonovaSearchPerformer extends TorrentRegexSearchPerformer<MonovaSearchResult> {

    private static final int MAX_RESULTS = 10;
    private static final String REGEX = "(?is)<a href=\"http://www.monova.org/torrent/([0-9]*?)/(.*?).html";
    private static final String HTML_REGEX = "(?is)<div id=\"downloadbox\" .*?Zoink</a>.*?<a href=\"(.*?)\" alt=\"Download!\" rel=\"nofollow\">Torcache<.*?<strong>Added:</strong>(.*?)<div class=\"clear-both\">.*?<font color=\"[A-Za-z]*?\">(.*?)</font> seeds.*?<strong>Total size:</strong><div .*?>(.*?)</div><div class=\"clear-both\">.*?Hash:</strong><div .*?>(.*?)</div><div class=\"clear-both\">";

    public MonovaSearchPerformer(DomainAliasManager domainAliasManager, long token, String keywords, int timeout) {
        super(domainAliasManager, token, keywords, timeout, 1, 2 * MAX_RESULTS, MAX_RESULTS, REGEX, HTML_REGEX);
    }

    @Override
    protected String getUrl(int page, String encodedKeywords) {
        return "http://"+getDomainNameToUse()+"/search.php?sort=5&term=" + encodedKeywords;
    }

    @Override
    protected String fetchSearchPage(String url) throws IOException {
        return fetch(url, "MONOVA=1; MONOVA-ADULT=0; MONOVA-NON-ADULT=1;", null);
    }

    @Override
    public CrawlableSearchResult fromMatcher(SearchMatcher matcher) {
        String itemId = matcher.group(1);
        String fileName = matcher.group(2);
        return new MonovaTempSearchResult(getDomainNameToUse(),itemId, fileName);
    }

    @Override
    protected MonovaSearchResult fromHtmlMatcher(CrawlableSearchResult sr, SearchMatcher matcher) {
        MonovaSearchResult candidate = new MonovaSearchResult(sr.getDetailsUrl(), matcher);
        if (candidate.getSeeds() < 40 || candidate.getDaysOld() > 200) {
            //since we can only do monova using magnets, we better have seeds or else we'll
            //suck in UX.
            candidate = null;
        }
        return candidate;
    }
    
    /**
    public static void main(String[] args) throws Throwable {

        byte[] readAllBytes = Files.readAllBytes(Paths.get("/Users/gubatron/Desktop/monova_input.txt"));
        String fileStr = new String(readAllBytes,"utf-8");
        Pattern pattern = Pattern.compile(HTML_REGEX);
        Matcher matcher = pattern.matcher(fileStr);
        
        System.out.println("find? : " + matcher.find());
        
        System.out.println("group 1: " + matcher.group(1));
        System.out.println("group 2: " + matcher.group(2));
        System.out.println("group 3: " + matcher.group(3));
        System.out.println("group 4: " + matcher.group(4));
        System.out.println("group 5: " + matcher.group(5));
    }
    */
}
