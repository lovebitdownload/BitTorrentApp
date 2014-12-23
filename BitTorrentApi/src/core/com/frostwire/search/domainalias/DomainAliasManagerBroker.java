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

package com.frostwire.search.domainalias;

import com.frostwire.logging.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/** This guy is in charge of managing DomainAliasManager instances
 * fetching the domain list files and updating each of the domain alias managers it has.
 * If you need a DomainAliasManager you just give it the default 
 * @author gubatron
 *
 */
public class DomainAliasManagerBroker implements DomainAliasManifestFetcherListener {

    private static final Logger LOG = Logger.getLogger(DomainAliasManagerBroker.class);

    private final HashMap<String, DomainAliasManager> managers;
    
    public DomainAliasManagerBroker() {
        managers = new HashMap<String, DomainAliasManager>();
        fetchDomainAliasManifest();
    }

    private void fetchDomainAliasManifest() {
        new Thread("DomainAliasManagerBroker-domain-alias-manifest-fetcher") {
            @Override
            public void run() {
                DefaultDomainAliasManifestFetcher fetcher = new DefaultDomainAliasManifestFetcher(DomainAliasManagerBroker.this); 
                fetcher.fetchManifest();
            }

        }.start();
    }

    public DomainAliasManager getDomainAliasManager(String defaultDomainKey) {
        if (!managers.containsKey(defaultDomainKey)) {
            managers.put(defaultDomainKey, new DomainAliasManager(defaultDomainKey));
        }
        return managers.get(defaultDomainKey);
    }

    @Override
    public void onManifestFetched(DomainAliasManifest aliasManifest) {
        updateManagers(aliasManifest);
    }

    @Override
    public void onManifestNotFetched() {
        LOG.error("DomainAliasManagerBroker:  Could not fetch alias list, should we try again later? attempts left");
        //attempts++;?? timestamp, to try again later. etc.
    }

    private void updateManagers(DomainAliasManifest aliasManifest) {
        Map<String, List<String>> aliases = aliasManifest.aliases;
        Set<Entry<String, List<String>>> aliasSet = aliases.entrySet();
        for (Entry<String, List<String>> entry : aliasSet) {
            final List<String> aliasNames = entry.getValue();
            DomainAliasManager domainAliasManager = getDomainAliasManager(entry.getKey());
            domainAliasManager.updateAliases(aliasNames);
        }
    }
}
