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

package com.frostwire.search.torrentsfm;

import java.io.File;
import java.io.FileInputStream;

import com.frostwire.search.CrawlableSearchResult;
import com.frostwire.search.MaxIterCharSequence;
import com.frostwire.search.SearchMatcher;
import com.frostwire.search.domainalias.DomainAliasManager;
import com.frostwire.search.torrent.TorrentRegexSearchPerformer;
import com.google.code.regexp.Matcher;
import com.google.code.regexp.Pattern;

/**
 * Search Performer for torrents.com / torrents.fm
 * @author gubatron
 * @author aldenml
 *
 */
public class TorrentsfmSearchPerformer extends TorrentRegexSearchPerformer<TorrentsfmSearchResult> {

    private static final int MAX_RESULTS = 20;
    private static final String REGEX = "(?is)<li class=\"grid_6 alpha omega\"><div class=\"grid_3 alpha omega\"><a title=\"(.*?)\" href=\'(.*?)\'>.*?</span> Download</a></div></li>";
    private static final String HTML_REGEX = "(?is)<section id=\"download\" class=\"grid_24\"><div class=\"grid_6 alpha suffix_1\">.*?<img src=\"(?<thumbnail>.*?)\".*?/>.*?</div><div class=\"grid_17 omega\"><h1 id=\"torrent_title\">(?<title>.*?)</h1>.*?<div class=\"size\">(?<filesize>.*?)</div>.*?<span title=\"(?<seeds>[0-9]*) seeds / [0-9]* leechers\">.*?(<dl class=\"date\"><dt>Created</dt><dd>(?<created>.*?)</dd></dl>).*?<a class=\"download\".*?data-track=\"Download,Magnet,File / Big Button\" data-downloader=\"1\" href=\"(?<magnet>.*?)\"><span class=\"icon download-button\">";
    private static final String EO_REGEX = "<span class=\"icon download-button\">";
    private static final int EO_REGEX_LENGTH = EO_REGEX.length();;
    
    // matcher groups: 1 -> thumbnail url
    //                 2 -> title
    //                 3 -> file size (needs parsing)
    //                 4 -> seeds
    //                 5 -> creation date, e.g. 2013-10-17 11:53:27
    //                 6 -> magnet url

    public TorrentsfmSearchPerformer(DomainAliasManager domainAliasManager, long token, String keywords, int timeout) {
        super(domainAliasManager, token, keywords, timeout, 1, 2 * MAX_RESULTS, MAX_RESULTS, REGEX, HTML_REGEX);
    }

    @Override
    protected String getUrl(int page, String encodedKeywords) {
        String searchParameter = encodedKeywords.replace(" ", "_");
        return "http://" + getDomainNameToUse() + "/search/" + searchParameter;
    }

    @Override
    public CrawlableSearchResult fromMatcher(SearchMatcher matcher) {
        String displayName = matcher.group(1);
        String itemId = matcher.group(2);
        return new TorrentsfmTempSearchResult(getDomainNameToUse(), itemId, displayName);
    }

    @Override
    protected TorrentsfmSearchResult fromHtmlMatcher(CrawlableSearchResult sr, SearchMatcher matcher) {
        return new TorrentsfmSearchResult(getDomainNameToUse(), sr.getDetailsUrl(), matcher);
    }
    
    @Override
    protected int prefixOffset(String html) {
        return html.indexOf("<section id=\"download\"");
    }
    
    @Override
    protected int suffixOffset(String html) {
        return html.indexOf(EO_REGEX) + EO_REGEX_LENGTH;
    }
    
    public static void main(String[] args) throws Throwable {
        File f = new File("/Users/gubatron/Desktop/tfm.txt");
        FileInputStream fis = new FileInputStream(f);
        byte[] readAllBytes = new byte[(int) f.length()]; //we know it's a small file.
        fis.read(readAllBytes);
        
        //byte[] readAllBytes = Files.readAllBytes(Paths.get());
        String fileStr = new String(readAllBytes,"utf-8");

        //Pattern pattern = Pattern.compile(REGEX);
        Pattern pattern = Pattern.compile(HTML_REGEX);
        //Matcher matcher = pattern.matcher(fileStr);
        Matcher matcher = pattern.matcher(new MaxIterCharSequence(fileStr, 2 * fileStr.length()));
        //SearchMatcher searchMatcher = new SearchMatcher(matcher);
        
        int found = 0;
        while (matcher.find()) {
            found++;
            System.out.println("\nfound " + found);
            
            //test REGEX
            /*
            System.out.println("group 1: " + matcher.group(1));
            System.out.println("group 2: " + matcher.group(2));
            */
            
            //test HTML_REGEX
            System.out.println("thumbnail: " + matcher.group("thumbnail"));
            System.out.println("title: " + matcher.group("title"));

            System.out.println("filesize: " + matcher.group("filesize"));
            System.out.println("seeds: " + matcher.group("seeds"));
            //System.out.println("createdGroup: " + matcher.group("createdGroup"));
            System.out.println("created: " + matcher.group("created"));
            System.out.println("magnet url: " + matcher.group("magnet"));
            System.out.println("===");
        }
        System.out.println("-done (found: " + found + ")-");
        
        
    }
}