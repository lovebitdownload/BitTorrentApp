/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011-2014, FrostWire(R). All rights reserved.
 
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

package com.frostwire.search;

import java.io.IOException;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;

import com.frostwire.logging.Logger;
import com.frostwire.search.domainalias.DomainAliasManager;
import com.frostwire.util.HttpClient;
import com.frostwire.util.HttpClientFactory;
import com.frostwire.util.StringUtils;
import com.frostwire.util.UserAgentGenerator;

/**
 * 
 * @author gubatron
 * @author aldenml
 *
 */
public abstract class WebSearchPerformer extends AbstractSearchPerformer {

    private static final Logger LOG = Logger.getLogger(WebSearchPerformer.class);

    private static final String DEFAULT_USER_AGENT = UserAgentGenerator.getUserAgent();

    private static final String[] STREAMABLE_EXTENSIONS = new String[] { "mp3", "ogg", "wma", "wmv", "m4a", "aac", "flac", "mp4", "flv", "mov", "mpg", "mpeg", "3gp", "m4v", "webm" };

    private final String keywords;
    private final String encodedKeywords;
    private final int timeout;
    private final HttpClient client;

    private final DomainAliasManager domainAliasManager;

    public WebSearchPerformer(DomainAliasManager domainAliasManager, long token, String keywords, int timeout) {
        super(token);

        if (domainAliasManager == null) {
            throw new IllegalArgumentException("domainAliasManager can't be null");
        }

        this.domainAliasManager = domainAliasManager;
        this.keywords = keywords;
        this.encodedKeywords = StringUtils.encodeUrl(keywords);
        this.timeout = timeout;
        this.client = HttpClientFactory.newInstance();
    }

    public final String getKeywords() {
        return keywords;
    }

    public final String getEncodedKeywords() {
        return encodedKeywords;
    }

    @Override
    public void crawl(CrawlableSearchResult sr) {
        LOG.warn("Review your logic, calling deep search without implementation for: " + sr);
    }

    /**
     * Allow to perform the HTTP operation using the same internal http client.
     * 
     * @param url
     * @return the web page (html)
     */
    public String fetch(String url) throws IOException {
        return fetch(url, null, null);
    }

    public String fetch(String url, String cookie, Map<String, String> customHeaders) throws IOException {
        return client.get(url, timeout, DEFAULT_USER_AGENT, null, cookie, customHeaders);
    }

    public String post(String url, Map<String, String> formData) {
        return client.post(url, timeout, DEFAULT_USER_AGENT, formData);
    }

    /**
     * Allow to perform the HTTP operation using the same internal http client.
     * 
     * @param url
     * @return the raw bytes from the http connection
     */
    public final byte[] fetchBytes(String url) {
        return fetchBytes(url, null, timeout);
    }

    protected final byte[] fetchBytes(String url, String referrer, int timeout) {
        if (url.startsWith("htt")) { // http(s)
            return client.getBytes(url, timeout, DEFAULT_USER_AGENT, referrer);
        } else {
            return null;
        }
    }

    protected final boolean isStreamable(String filename) {
        String ext = FilenameUtils.getExtension(filename);
        for (String s : STREAMABLE_EXTENSIONS) {
            if (s.equals(ext)) {
                return true; // fast return
            }
        }

        return false;
    }

    public String getDomainNameToUse() {
        return domainAliasManager.getDomainNameToUse();
    }

    public String getDefaultDomainName() {
        return domainAliasManager.getDefaultDomain();
    }

    public DomainAliasManager getDomainAliasManager() {
        return domainAliasManager;
    }

    /**
     * The current domain has failed, mark it offline and let's try check if other mirrors are alive.
     */
    protected final void checkAccesibleDomains() {
        LOG.debug("WebSearchPerformer.checkAccesibleDomains()! " + getDefaultDomainName() + " Performer failed, marking " + getDomainNameToUse() + " offline, checking domains.");

        domainAliasManager.markDomainOffline(getDomainNameToUse());
        domainAliasManager.checkStatuses(this);
    }
}