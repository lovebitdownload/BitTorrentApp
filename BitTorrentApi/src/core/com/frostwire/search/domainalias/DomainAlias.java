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

import java.util.concurrent.ExecutorService;

import com.frostwire.util.HttpClient;
import com.frostwire.util.HttpClientFactory;
import com.frostwire.util.ThreadPool;

/**
 * @author gubatron
 * @author aldenml
 *
 */
public class DomainAlias {

    public static final ExecutorService executor = ThreadPool.newThreadPool("DomainAliasCheckers", true);

    public final String original;
    public final String alias; 
    private DomainAliasState aliasState;
    private long lastChecked;
    private int failedAttempts;

    private final static long DOMAIN_ALIAS_CHECK_INTERVAL_MILLISECONDS = 5000;//time to wait before we check again this domain alias after it's been marked offline.
    private final static int DOMAIN_ALIAS_CHECK_TIMEOUT_MILLISECONDS = 3500;

    public DomainAlias(String original, String alias) {
        this.original = original;
        this.alias = alias;
        lastChecked = -1;
        aliasState = DomainAliasState.UNCHECKED;
        failedAttempts = 0;
    }

    public long getLastChecked() {
        return lastChecked;
    }

    public DomainAliasState getState() {
        return aliasState;
    }

    public String getOriginal() {
        return original;
    }

    public String getAlias() {
        return alias;
    }

    public void checkStatus(final DomainAliasPongListener pongListener) {
        if (aliasState != DomainAliasState.CHECKING) {
            long timeSinceLastCheck = System.currentTimeMillis() - lastChecked;

            if (timeSinceLastCheck > DOMAIN_ALIAS_CHECK_INTERVAL_MILLISECONDS) {
                Thread r = new Thread("DomainAlias-Pinger (" + original + "=>" + alias + ")") {
                    @Override
                    public void run() {
                        pingAlias(pongListener);
                    }
                };
                executor.execute(r);
            } else {
                System.out.println("DomainAlias.checkStatus: Too early to ping again " + alias);
            }
        } else {
            System.out.println("DomainAlias.checkStatus: Not checking " + alias +" because it's still CHECKING");
        }
    }

    private void pingAlias(final DomainAliasPongListener pongListener) {
        aliasState = DomainAliasState.CHECKING;
        lastChecked = System.currentTimeMillis();
        if (ping(alias)) {
            System.out.println(alias + " Domain alias pong! ");
            aliasState = DomainAliasState.ONLINE;  
            failedAttempts = 0;
            pongListener.onDomainAliasPong(this);
        } else {
            pingFailed();
            pongListener.onDomainAliasPingFailed(this);
        }
    }
    
    private static boolean ping(String domainName) {
        boolean pong = false;
        try {
            HttpClient httpClient = HttpClientFactory.newInstance();
            String string = httpClient.get("http://"+domainName, DOMAIN_ALIAS_CHECK_TIMEOUT_MILLISECONDS);
            pong = string != null && string.length()> 0;
        } catch (Throwable t) {
            System.out.println("No pong from " + domainName + ".\n");
        }
        return pong;
    }
    
    private void pingFailed() {
        aliasState = DomainAliasState.OFFLINE;
        failedAttempts++;
    }
    
    public void markOffline() {
        aliasState = DomainAliasState.OFFLINE;
        lastChecked = System.currentTimeMillis();
    }
    
    public int getFailedAttempts() {
        return failedAttempts;
    }

    @Override
    public boolean equals(Object obj) {
        DomainAlias other = (DomainAlias) obj;
        return other!=null && this.original.equals(other.original) && this.alias.equals(other.alias);
    }

    @Override
    public int hashCode() {
        return (this.original.hashCode() * 29) + (this.alias.hashCode() * 13);
    }
    
    @Override
    public String toString() {
        return "("+original+" => "+alias + " [" + aliasState + "])";
    }

    public void reset() {
        this.aliasState = DomainAliasState.UNCHECKED;
        this.failedAttempts = 0;
        this.lastChecked = -1;
    }
}
