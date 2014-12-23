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

package com.frostwire.search.kat;

import java.util.List;

import com.frostwire.search.domainalias.DomainAliasManager;
import com.frostwire.search.torrent.TorrentJsonSearchPerformer;
import com.frostwire.util.JsonUtils;

/**
 * 
 * @author gubatron
 * @author aldenml
 *
 */
public class KATSearchPerformer extends TorrentJsonSearchPerformer<KATItem, KATSearchResult> {

    public KATSearchPerformer(DomainAliasManager domainAliasManager, long token, String keywords, int timeout) {
        super(domainAliasManager, token, keywords, timeout, 1);
    }

    @Override
    protected String getUrl(int page, String encodedKeywords) {
        return "http://"+getDomainNameToUse()+"/json.php?q=" + encodedKeywords;
    }

    @Override
    protected List<KATItem> parseJson(String json) {
        KATResponse response = JsonUtils.toObject(json, KATResponse.class);
        response.fixItems();
        return response.list;
    }

    @Override
    protected KATSearchResult fromItem(KATItem item) {
        return new KATSearchResult(item);
    }
}