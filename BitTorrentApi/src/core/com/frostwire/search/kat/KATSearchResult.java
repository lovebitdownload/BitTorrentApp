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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

import com.frostwire.search.torrent.AbstractTorrentSearchResult;
import com.frostwire.util.StringUtils;

/**
 * 
 * @author gubatron
 * @author aldenml
 *
 */
public class KATSearchResult extends AbstractTorrentSearchResult {

    private final KATItem item;
    private final String filename;
    private final String torrentUrl;
    private final long creationTime;

    public KATSearchResult(KATItem item) {
        this.item = item;

        this.filename = buildFilename(item);
        this.torrentUrl = buildUrl();
        this.creationTime = buildCreationTime(item);
    }

    @Override
    public String getDisplayName() {
        return item.title;
    }

    @Override
    public String getTorrentUrl() {
        return torrentUrl;
    }

    @Override
    public String getFilename() {
        return filename;
    }

    @Override
    public long getCreationTime() {
        return creationTime;
    }

    @Override
    public String getHash() {
        return item.hash;
    }

    @Override
    public long getSize() {
        return item.size;
    }

    @Override
    public String getSource() {
        return "KAT";
    }

    @Override
    public int getSeeds() {
        return item.seeds;
    }

    @Override
    public String getDetailsUrl() {
        return item.link;
    }

    private String buildFilename(KATItem item) {
        String titleNoTags = item.title.replace("<b>", "").replace("</b>", "");
        return titleNoTags + ".torrent";
    }

    private String buildUrl() {
        return "magnet:?xt=urn:btih:" + getHash() + "&dn=" + StringUtils.encodeUrl(getFilename()) + "&tr=http%3A%2F%2Ftracker.publicbt.com%2Fannounce";
    }

    private long buildCreationTime(KATItem item) {
        //Saturday 26 Jan 2008 01:01:52 +0000
        SimpleDateFormat date = new SimpleDateFormat("EEEE dd MMM yyyy HH:mm:ss Z ", Locale.US);
        long result = System.currentTimeMillis();
        try {
            result = date.parse(item.pubDate).getTime();
        } catch (ParseException e) {
        }
        return result;
    }
}