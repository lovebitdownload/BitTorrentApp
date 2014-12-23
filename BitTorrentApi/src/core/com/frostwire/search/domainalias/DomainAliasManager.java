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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.frostwire.search.SearchPerformer;

/**
 * Simply responsible for maintaining the list of domain aliases and their states for
 * a single domain.
 * 
 * @author gubatron
 * @author aldenml
 *
 */
public final class DomainAliasManager {

    private final String defaultDomain;

    private DomainAlias currentDomainAlias;

    private boolean defaultDomainOnline;

    private final AtomicReference<List<DomainAlias>> aliases;

    public DomainAliasManager(String defaultDomain) {
        this(defaultDomain, Collections.<DomainAlias> emptyList());
    }

    public DomainAliasManager(String defaultDomain, List<DomainAlias> aliases) {
        this.defaultDomain = defaultDomain;
        this.currentDomainAlias = null;
        this.aliases = new AtomicReference<List<DomainAlias>>();
        this.aliases.set(Collections.synchronizedList(aliases));
        this.defaultDomainOnline = true;
    }

    public String getDefaultDomain() {
        return defaultDomain;
    }

    /**
     * Adds new Domain Aliases, keeps the old DomainAlias objects as they were.
     * @param aliasNames
     */
    public void updateAliases(final List<String> aliasNames) {
        List<DomainAlias> newAliasList = new ArrayList<DomainAlias>();

        if (aliasNames != null && aliasNames.size() > 0) {
            for (String alias : aliasNames) {
                DomainAlias domainAlias = new DomainAlias(defaultDomain, alias);
                if (!aliases.get().contains(domainAlias)) {
                    newAliasList.add(domainAlias);
                } else {
                    //we keep the old alias, so we don't forget how many times it might have failed,
                    //when it was checked, etc.
                    newAliasList.add(aliases.get().get(aliases.get().indexOf(domainAlias)));
                }
            }
            Collections.shuffle(newAliasList);

            if (newAliasList.size() > 0) {
                aliases.set(Collections.synchronizedList(newAliasList));
            }
        }
    }

    /**
     * Resets the Domain Aliases list.
     * @param aliasNames
     */
    public void setAliases(final List<String> aliasNames) {
        List<DomainAlias> newAliasList = new ArrayList<DomainAlias>();

        if (aliasNames != null && aliasNames.size() > 0) {
            for (String alias : aliasNames) {
                DomainAlias domainAlias = new DomainAlias(defaultDomain, alias);
                newAliasList.add(domainAlias);
            }
            Collections.shuffle(newAliasList);

            if (newAliasList.size() > 0) {
                aliases.set(Collections.synchronizedList(newAliasList));
            }
        }
    }

    public void markDomainOffline(String offlineDomain) {
        if (offlineDomain.equals(defaultDomain)) {
            defaultDomainOnline = false;
        } else {
            for (DomainAlias domainAlias : aliases.get()) {
                if (domainAlias.alias.equals(offlineDomain)) {
                    domainAlias.markOffline();
                    return;
                }
            }
        }
    }

    public DomainAlias getCurrentDomainAlias() {
        return currentDomainAlias;
    }

    /**
     * Until it doesn't know the default domain name is not accesible
     * it will keep returning the next domain considered to be online.
     * @return
     */
    public String getDomainNameToUse() {
        String result = defaultDomain;
        if (!defaultDomainOnline) {
            if (getCurrentDomainAlias() == null) {
                getNextOnlineDomainAlias();
                if (getCurrentDomainAlias() != null) {
                    result = getCurrentDomainAlias().getAlias();
                }
            } else {
                result = getCurrentDomainAlias().alias;
            }
        }
        return result;
    }

    public void setDomainNameToUse(String alias) {
        List<DomainAlias> aliasList = aliases.get();

        synchronized (aliasList) {
            for (DomainAlias domainAlias : aliasList) {
                if (domainAlias.getAlias().equals(alias)) {
                    currentDomainAlias = domainAlias;
                    return;
                }
            }
        }
    }

    /**
     * Returns the next domain considered as online on the manager's list.
     * null if the current list is empty, null or nobody is online.
     * 
     * This method will update the currentDomainAlias to be used.
     * 
     * This method will not check, checks must have been done in advance
     * 
     * @return
     */
    private DomainAlias getNextOnlineDomainAlias() {
        List<DomainAlias> aliasesList = aliases.get();
        DomainAlias result = null;

        if (currentDomainAlias == null) {
            currentDomainAlias = aliasesList.get(0);
            result = currentDomainAlias;
        } else {
            int currentIndex = aliasesList.indexOf(currentDomainAlias);
            int startingIndex = (currentIndex + 1) % aliasesList.size();
            for (int i = startingIndex; i < aliasesList.size(); i++) {
                DomainAlias alias = aliasesList.get(i);
                if (!alias.equals(currentDomainAlias) && alias.getState() == DomainAliasState.ONLINE) {
                    currentDomainAlias = alias;
                    result = currentDomainAlias;
                    break;
                }
            }
        }

        return result;
    }

    /**
     * Will try to ping all DomainAliases that have not been pinged recently to update
     * their statuses.
     */
    public void checkStatuses(SearchPerformer performer) {
        if (aliases != null && !aliases.get().isEmpty()) {
            List<DomainAlias> toRemove = new ArrayList<DomainAlias>();

            final DomainAliasPongListener pongListener = createPongListener(performer);

            for (DomainAlias alias : aliases.get()) {
                if (alias.getFailedAttempts() <= 3) {
                    alias.checkStatus(pongListener);
                } else {
                    System.out.println("Removing alias " + alias.alias);
                    toRemove.add(alias);
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                }
            }

            if (!toRemove.isEmpty()) {
                aliases.get().removeAll(toRemove);
            }
        } else {
            //be borne again.
            resetAliases();
        }
    }

    private DomainAliasPongListener createPongListener(final SearchPerformer performer) {
        final DomainAliasPongListener pongListener = new DomainAliasPongListener() {

            private AtomicBoolean firstDomainReportedPong = new AtomicBoolean(false);

            @Override
            public void onDomainAliasPong(DomainAlias domainAlias) {
                //as soon as the first one of the aliases reports he's online
                //we'll try to update our active/current domain alias.
                if (domainAlias.getState() == DomainAliasState.ONLINE && firstDomainReportedPong.compareAndSet(false, true)) {
                    System.out.println("DomainAliasManager.DomainAliasPongListener.onDomainAliasPong(): got pong from " + domainAlias.alias);
                    currentDomainAlias = domainAlias; //the magic moment
                    performer.perform();
                    System.out.println("DomainAliasManager.DomainAliasPongListener.onDomainAliasPong(): We've selected a new domain alias: New " + getCurrentDomainAlias().alias + " for " + getDefaultDomain() + " (STATE: " + getCurrentDomainAlias().getState() + ")");
                }
            }

            @Override
            public void onDomainAliasPingFailed(DomainAlias domainAlias) {
                DomainAliasManager.this.markDomainOffline(domainAlias.getAlias());
            }
        };
        return pongListener;
    }

    private void resetAliases() {
        defaultDomainOnline = true;
        if (aliases != null && aliases.get().size() > 0) {
            for (DomainAlias alias : aliases.get()) {
                alias.reset();
            }
        }
    }
}