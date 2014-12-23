/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011, 2012, FrostWire(TM). All rights reserved.
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

package com.bt.download.android.gui.httpserver;

import java.net.InetSocketAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.bt.download.android.gui.NetworkManager;
import com.sun.net.httpserver.HttpServer;

/**
 * @author gubatron
 * @author aldenml
 *
 */
public class HttpServerManager {

    private static final Logger LOG = Logger.getLogger(HttpServerManager.class.getName());

    private HttpServer httpServer;

    public HttpServerManager() {
    }

    public void start(int port) {
        if (!NetworkManager.instance().isDataWIFIUp()) {
            return;
        }

        if (httpServer != null) {
            return;
        }

        try {

            httpServer = HttpServer.create(new InetSocketAddress(port), 10);

            httpServer.createContext("/finger", new FingerHandler());
            httpServer.createContext("/browse", new BrowseHandler());
            httpServer.createContext("/download", new DownloadHandler());

            httpServer.start();

        } catch (Throwable e) {
            LOG.log(Level.WARNING, "Failed to start http server", e);
        }
    }

    public void stop() {
        if (httpServer == null) {
            return;
        }

        try {
            httpServer.stop(0);
        } catch (Throwable e) {
            LOG.log(Level.WARNING, "Something wrong stopping the HTTP server", e);
        } finally {
            httpServer = null;
        }
    }
}
