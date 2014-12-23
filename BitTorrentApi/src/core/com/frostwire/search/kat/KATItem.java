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

import com.frostwire.search.torrent.ComparableTorrentJsonItem;

/**
 * KickAssTorrents Search Result Item.
 * 
 * @author gubatron
 * @author aldenml
 *
 */
public class KATItem implements ComparableTorrentJsonItem {
	/**
    {
            "title": "Paulo Coelhos English E-Books",
            "link": "http://kat.ph/paulo-coelhos-english-e-books-t90846.html",
            "guid": "http://kat.ph/paulo-coelhos-english-e-books-t90846.html",
            "pubDate": "Saturday 26 Jan 2008 01:01:52 +0000",
            "torrentLink": "https://torcache.net/torrent/6ED30C045470C16A2BD985BBDE504710790FA117.torrent?title=[kat.ph]paulo.coelhos.english.e.books",
            "files": 8,
            "comments": 9,
            "hash": "6ed30c045470c16a2bd985bbde504710790fa117",
            "peers": 42,
            "seeds": 36,
            "leechs": 6,
            "size": 8838656,
            "votes": 19,
            "verified": 1
    }
	*/
    
    public String title;
    public String link;
    public String pubDate;
    public String torrentLink;
    public String hash;
    public int seeds;
    public long size;
    
    public int verified;
    
    @Override
    public int getSeeds() {
        return seeds;
    }
}