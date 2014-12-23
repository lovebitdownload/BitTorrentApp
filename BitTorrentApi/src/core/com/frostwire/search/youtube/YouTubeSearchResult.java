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

package com.frostwire.search.youtube;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

import org.apache.commons.io.FilenameUtils;

import com.frostwire.search.AbstractFileSearchResult;
import com.frostwire.search.CrawlableSearchResult;

/**
 * @author gubatron
 * @author aldenml
 *
 */
public class YouTubeSearchResult extends AbstractFileSearchResult implements CrawlableSearchResult {

    private final String filename;
    private final String displayName;
    private final long creationTime;
    private final String videoUrl;
    private final String source;
    private final long size;
    private final boolean testConnection;

    public YouTubeSearchResult(YouTubeEntry entry, boolean testConnection) {
        this.filename = entry.title.title + ".youtube";
        this.displayName = FilenameUtils.getBaseName(filename);
        this.creationTime = readCreationTime(entry);
        this.videoUrl = readVideoUrl(entry);
        this.source = buildSource(entry);
        this.size = buildSize(entry);
        this.testConnection = testConnection;
    }

    @Override
    public String getFilename() {
        return filename;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public long getSize() {
        return size;
    }

    @Override
    public long getCreationTime() {
        return creationTime;
    }

    @Override
    public String getSource() {
        return source;
    }

    @Override
    public String getDetailsUrl() {
        return videoUrl;
    }

    @Override
    public boolean isComplete() {
        return true;
    }
    
    boolean testConnection() {
        return testConnection;
    }


    private long readCreationTime(YouTubeEntry entry) {
        try {
            //2010-07-15T16:02:42
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            return dateFormat.parse(entry.published.title.replace("000Z", "")).getTime();
        } catch (ParseException e) {
            return System.currentTimeMillis();
        }
    }

    private String readVideoUrl(YouTubeEntry entry) {
        String url = null;

        for (YouTubeEntryLink link : entry.link) {
            if (link.rel.equals("alternate")) {
                url = link.href;
            }
        }

        url = url.replace("https://", "http://").replace("&feature=youtube_gdata", "");

        return url;
    }

    private String buildSource(YouTubeEntry entry) {
        if (entry.author != null && entry.author.size() > 0 && entry.author.get(0).name != null && entry.author.get(0).name.title != null) {
            return "YouTube - " + entry.author.get(0).name.title;
        } else {
            return "YouTube";
        }
    }
    
    private long buildSize(YouTubeEntry entry) {
        try {
            return entry.mediagroup.mediacontent.get(0).duration;
        } catch (Throwable t) {
            return UNKNOWN_SIZE;
        }
    }

    @Override
    public String getThumbnailUrl() {
        return null;
    }
}