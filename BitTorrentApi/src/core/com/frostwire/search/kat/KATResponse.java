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

import java.util.Iterator;
import java.util.List;

import com.frostwire.util.StringUtils;

/**
 * 
 * KAT.ph (KickAssTorrents) JSON Responses look like this:
 *
 *{
    "title": "Kickasstorrents paulo coelho",
    "link": "http://kat.ph",
    "description": "BitTorrent Search: paulo coelho",
    "language": "en-us",
    "ttl": 60,
    "total_results": "72",
        {
            "title": "Paulo Coelhos English E-Books",
            "category": "Books",
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
        },
        {
            "title": "La Bruja de Portobello - Paulo Coelho [Ebook][Spanish]",
            "category": "Books",
            "link": "http://kat.ph/la-bruja-de-portobello-paulo-coelho-ebook-spanish-t6077184.html",
            "guid": "http://kat.ph/la-bruja-de-portobello-paulo-coelho-ebook-spanish-t6077184.html",
            "pubDate": "Tuesday 27 Dec 2011 07:56:05 +0000",
            "torrentLink": "https://torcache.net/torrent/525E3AC031B7B1C2E5E68F6DCE82E746653F930E.torrent?title=[kat.ph]la.bruja.de.portobello.paulo.coelho.ebook.spanish",
            "files": 8,
            "comments": 0,
            "hash": "525e3ac031b7b1c2e5e68f6dce82e746653f930e",
            "peers": 27,
            "seeds": 25,
            "leechs": 2,
            "size": 9339732,
            "votes": 0,
            "verified": 1
        },
 * 
 * @author gubatron
 * @author aldenml
 *
 */
public class KATResponse {

    public List<KATItem> list;

    /**
     * Include only verified results to keep users safe.
     */
    public void fixItems() {
        if (list != null && list.size() > 0) {
            Iterator<KATItem> iterator = list.iterator();
            while (iterator.hasNext()) {
                KATItem next = iterator.next();

                //Take out non-verified results and
                //elements missing mandatory data
                if (next.verified == 0 || StringUtils.isNullOrEmpty(next.title) || StringUtils.isNullOrEmpty(next.hash) || StringUtils.isNullOrEmpty(next.torrentLink) || StringUtils.isNullOrEmpty(next.link) || next.size <= 0) {
                    iterator.remove();
                }
            }
        }
    }
}