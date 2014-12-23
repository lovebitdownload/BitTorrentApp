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

import java.util.List;

import com.frostwire.logging.Logger;

/**
 * 
 * @author gubatron
 * @author aldenml
 *
 */
public abstract class AbstractSearchPerformer implements SearchPerformer {

    private static final Logger LOG = Logger.getLogger(AbstractSearchPerformer.class);

    private final long token;

    private SearchListener listener;
    private boolean stopped;

    public AbstractSearchPerformer(long token) {
        this.token = token;
    }

    @Override
    public long getToken() {
        return token;
    }

    @Override
    public void registerListener(SearchListener listener) {
        this.listener = listener;
    }

    @Override
    public void stop() {
        this.stopped = true;
    }

    @Override
    public boolean isStopped() {
        return stopped;
    }

    protected void onResults(SearchPerformer performer, List<? extends SearchResult> results) {
        try {
            if (listener != null) {
                listener.onResults(performer, results);
            }
        } catch (Throwable e) {
            LOG.warn("Error sending results back to receiver: " + e.getMessage());
        }
    }
}
