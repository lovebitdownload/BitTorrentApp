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

import java.util.List;

import com.frostwire.search.domainalias.DomainAliasManager;

/**
 * 
 * @author gubatron
 * @author aldenml
 *
 */
public abstract class PagedRegexSearchPerformer<T extends SearchResult> extends PagedWebSearchPerformer implements RegexSearchPerformer<T> {

    private final int regexMaxResults;

    public PagedRegexSearchPerformer(DomainAliasManager domainAliasManager, long token, String keywords, int timeout, int pages, int regexMaxResults) {
        super(domainAliasManager, token, keywords, timeout, pages);
        this.regexMaxResults = regexMaxResults;
    }

    @Override
    protected final List<? extends SearchResult> searchPage(String page) {
        return PerformersHelper.searchPageHelper(this, page, regexMaxResults);
    }
}
