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

package com.frostwire.search.torrent;

import java.util.LinkedList;
import java.util.List;

import com.frostwire.logging.Logger;
import com.frostwire.search.CrawlRegexSearchPerformer;
import com.frostwire.search.CrawlableSearchResult;
import com.frostwire.search.MaxIterCharSequence;
import com.frostwire.search.PerformersHelper;
import com.frostwire.search.SearchMatcher;
import com.frostwire.search.SearchResult;
import com.frostwire.search.domainalias.DomainAliasManager;
import com.google.code.regexp.Matcher;
import com.google.code.regexp.Pattern;

/**
 * 
 * @author gubatron
 * @author aldenml
 *
 */
public abstract class TorrentRegexSearchPerformer<T extends CrawlableSearchResult> extends CrawlRegexSearchPerformer<CrawlableSearchResult> {

    private final Pattern preliminarSearchResultspattern;
    private final Pattern htmlDetailPagePattern;
    private final static Logger LOG = Logger.getLogger(TorrentRegexSearchPerformer.class);

    public TorrentRegexSearchPerformer(DomainAliasManager domainAliasManager, long token, String keywords, int timeout, int pages, int numCrawls, int regexMaxResults, String preliminarSearchResultsRegex, String htmlDetailPagePatternRegex) {
        super(domainAliasManager, token, keywords, timeout, pages, numCrawls, regexMaxResults);
        this.preliminarSearchResultspattern = Pattern.compile(preliminarSearchResultsRegex);
        this.htmlDetailPagePattern = Pattern.compile(htmlDetailPagePatternRegex);
    }

    @Override
    public Pattern getPattern() {
        return preliminarSearchResultspattern;
    }

    @Override
    protected String getCrawlUrl(CrawlableSearchResult sr) {
        String crawlUrl = null;

        if (sr instanceof TorrentCrawlableSearchResult) {
            crawlUrl = ((TorrentCrawlableSearchResult) sr).getTorrentUrl();
        } else {
            crawlUrl = sr.getDetailsUrl();
        }

        return crawlUrl;
    }
    
    

    @Override
    protected List<? extends SearchResult> crawlResult(CrawlableSearchResult sr, byte[] data) throws Exception {
        List<SearchResult> list = new LinkedList<SearchResult>();

        if (sr instanceof TorrentCrawlableSearchResult) {
            //in case we fetched a torrent's info (magnet, or the .torrent itself) to obtain 
            list.addAll(PerformersHelper.crawlTorrent(this, (TorrentCrawlableSearchResult) sr, data));
        } else {
            String html = reduceHtml(new String(data, "UTF-8"));

            if (html != null) {
                Matcher matcher = htmlDetailPagePattern.matcher(new MaxIterCharSequence(html, 2 * html.length()));
    
                try {
                    if (matcher.find()) {
                        T searchResult = fromHtmlMatcher(sr, SearchMatcher.from(matcher));
                        if (searchResult != null) {
                            list.add(searchResult);
                        }
                    } else {
                        LOG.error("Update Necessary:  Search broken for " + sr.getClass().getPackage().getName() + " (please notify dev-team on twitter @frostwire or write to contact@frostwire.com if you keep seeing this message.)");
                    }
                } catch (Exception e) {
                    throw new Exception("URL:" + sr.getDetailsUrl(), e);
                }
            } else {
                LOG.error("Update Necessary: HTML could not be reduced for optimal search. Search broken for " + sr.getClass().getPackage().getName() + " (please notify dev-team on twitter @frostwire or write to contact@frostwire.com if you keep seeing this message.)");
            }
        }

        return list;
    }

    /**
     * Sometimes the HTML_REGEX has to work on too big of an HTML file.
     * In order to minimize the chance for long backtracking times we can
     * override this methods to specify what offsets of the HTML file our
     * REGEX should be focusing on.
     */
    protected int prefixOffset(String html) {
        return 0;
    }
    
    /**
     * Sometimes the HTML_REGEX has to work on too big of an HTML file.
     * In order to minimize the chance for long backtracking times we can
     * override this methods to specify what offsets of the HTML file our
     * REGEX should be focusing on.
     */
    protected int suffixOffset(String html) {
        return html.length();
    }

    private String reduceHtml(String html) {
        int preOffset = prefixOffset(html);
        int sufOffset = suffixOffset(html);
        if (preOffset == -1 || sufOffset == -1) {
            html = null;
        } else if (preOffset > 0 || sufOffset < html.length()) {
            html = new String(html.substring(preOffset, sufOffset).toCharArray());
        }
        return html;
    }

    protected abstract T fromHtmlMatcher(CrawlableSearchResult sr, SearchMatcher matcher);
}
