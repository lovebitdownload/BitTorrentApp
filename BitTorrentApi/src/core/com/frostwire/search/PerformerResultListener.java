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

package com.frostwire.search;

import java.util.LinkedList;
import java.util.List;

/**
 * @author gubatron
 * @author aldenml
 *
 */
public final class PerformerResultListener implements SearchListener {

    private final SearchManagerImpl manager;

    public PerformerResultListener(SearchManagerImpl manager) {
        this.manager = manager;
    }

    @Override
    public void onResults(SearchPerformer performer, List<? extends SearchResult> results) {
        List<SearchResult> list = new LinkedList<SearchResult>();

        for (SearchResult sr : results) {
            if (sr instanceof CrawlableSearchResult) {
                CrawlableSearchResult csr = (CrawlableSearchResult) sr;

                if (csr.isComplete()) {
                    list.add(sr);
                }

                manager.crawl(performer, csr);
            } else {
                list.add(sr);
            }
        }

        if (!list.isEmpty()) {
            manager.onResults(performer, list);
        }
    }

    public SearchManager getSearchManager() {
        return manager;
    }
}
