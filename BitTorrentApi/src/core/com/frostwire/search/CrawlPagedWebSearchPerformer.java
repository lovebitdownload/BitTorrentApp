/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011-2014,, FrostWire(R). All rights reserved.
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

import com.frostwire.logging.Logger;
import com.frostwire.search.domainalias.DomainAliasManager;
import com.frostwire.search.torrent.TorrentSearchResult;
import com.frostwire.util.ByteUtils;
import com.frostwire.util.OSUtils;

import java.util.List;

/**
 * @author gubatron
 * @author aldenml
 *
 */
public abstract class CrawlPagedWebSearchPerformer<T extends CrawlableSearchResult> extends PagedWebSearchPerformer {

    private static final Logger LOG = Logger.getLogger(CrawlPagedWebSearchPerformer.class);

    private static final int DEFAULT_CRAWL_TIMEOUT = 10000; // 10 seconds.
    private static final int FAILED_CRAWL_URL_CACHE_LIFETIME = 900000; // 15 minutes.
    private static final int DEFAULT_MAGNET_DOWNLOAD_TIMEOUT = OSUtils.isAndroid() ? 4000 : 20000; // 4 seconds for android, 20 seconds for desktop.

    private static CrawlCache cache = null;
    private static MagnetDownloader magnetDownloader = null;

    private int numCrawls;

    public CrawlPagedWebSearchPerformer(DomainAliasManager domainAliasManager, long token, String keywords, int timeout, int pages, int numCrawls) {
        super(domainAliasManager, token, keywords, timeout, pages);
        this.numCrawls = numCrawls;
    }

    public static void setCache(CrawlCache cache) {
        CrawlPagedWebSearchPerformer.cache = cache;
    }

    public static MagnetDownloader getMagnetDownloader() {
        return magnetDownloader;
    }

    public static void setMagnetDownloader(MagnetDownloader magnetDownloader) {
        CrawlPagedWebSearchPerformer.magnetDownloader = magnetDownloader;
    }

    @Override
    public void crawl(CrawlableSearchResult sr) {
        if (numCrawls > 0) {
            numCrawls--;

            T obj = cast(sr);
            if (obj != null) {

                String url = getCrawlUrl(obj);

                byte[] failed = cacheGet("failed:" + url);
                if (failed != null) {
                    long failedWhen = ByteUtils.byteArrayToLong(failed);
                    if ((System.currentTimeMillis() - failedWhen) < FAILED_CRAWL_URL_CACHE_LIFETIME) {
                        //if the failed request is still fresh we stop
                        return;
                    } else {
                        cacheRemove("failed:" + url);
                    }
                }

                if (url != null) {
                    byte[] data = cacheGet(url);

                    if (sr instanceof TorrentSearchResult) {
                        String infohash = ((TorrentSearchResult) sr).getHash();
                        if (data == null) {
                            // maybe we've already cached it by infohash (happens quite a bit)
                            data = cacheGet(infohash);
                        } else {
                            cachePut(infohash, data);
                        }
                    }

                    if (data == null) { // not a big deal about synchronization here
                        LOG.debug("Downloading data for: " + url);

                        if (url.startsWith("magnet")) {
                            data = fetchMagnet(url);
                        } else {
                            data = fetchBytes(url, sr.getDetailsUrl(), DEFAULT_CRAWL_TIMEOUT);
                        }

                        //we put this here optimistically hoping this is actually
                        //valid data. if no data can be crawled from this we remove it
                        //from the cache. we do this because this same data may come
                        //from another search engine and this way we avoid the
                        //expense of performing another download.
                        if (data != null) {
                            cachePut(url, data);

                            if (sr instanceof TorrentSearchResult) {
                                // if the search result has an infohash we can use...
                                String infohash = ((TorrentSearchResult) sr).getHash();
                                cachePut(infohash, data);
                            }
                        } else {
                            LOG.warn("Failed to download data: " + url);
                            cachePut("failed:" + url, ByteUtils.longToByteArray(System.currentTimeMillis()));
                        }
                    }

                    try {
                        if (data != null) {
                            List<? extends SearchResult> results = crawlResult(obj, data);
                            if (results != null) {
                                onResults(this, results);
                            }
                        }
                    } catch (Throwable e) {
                        LOG.warn("Error creating crawled results from downloaded data: " + e.getMessage());
                        cacheRemove(url); // invalidating cache data
                    }
                } else {
                    try {
                        List<? extends SearchResult> results = crawlResult(obj, null);
                        if (results != null) {
                            onResults(this, results);
                        }
                    } catch (Throwable e) {
                        LOG.warn("Error creating crawled results from search result alone: " + obj.getDetailsUrl() + ", e=" + e.getMessage());
                    }
                }
            }
        }
    }

    protected abstract String getCrawlUrl(T sr);

    protected abstract List<? extends SearchResult> crawlResult(T sr, byte[] data) throws Exception;

    protected byte[] fetchMagnet(String magnet) {
        if (magnetDownloader != null) {
            return magnetDownloader.download(magnet, DEFAULT_MAGNET_DOWNLOAD_TIMEOUT);
        } else {
            LOG.warn("Magnet downloader not set, download not supported: " + magnet);
            return null;
        }
    }

    private byte[] cacheGet(String key) {
        if (cache != null) {
            synchronized (cache) {
                return cache.get(key);
            }
        } else {
            return null;
        }
    }

    private void cachePut(String key, byte[] data) {
        if (cache != null) {
            synchronized (cache) {
                cache.put(key, data);
            }
        }
    }

    private void cacheRemove(String key) {
        if (cache != null) {
            synchronized (cache) {
                cache.remove(key);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private T cast(CrawlableSearchResult sr) {
        try {
            return (T) sr;
        } catch (ClassCastException e) {
            LOG.warn("Something wrong with the logic, need to pass a crawlable search result with the correct type");
        }

        return null;
    }

    public static void clearCache() {
        if (cache != null) {
            synchronized (cache) {
                cache.clear();
            }
        }
    }

    public static long getCacheSize() {
        long result = 0;
        if (cache != null) {
            synchronized (cache) {
                result = cache.size();
            }
        }
        return result;
    }
}