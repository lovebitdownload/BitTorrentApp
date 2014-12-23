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

import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Logger;

import sun.net.httpserver.Code;

import com.bt.download.android.gui.Librarian;
import com.frostwire.localpeer.Finger;
import com.frostwire.util.JsonUtils;
import com.sun.net.httpserver.HttpExchange;

/**
 * @author gubatron
 * @author aldenml
 *
 */
class FingerHandler extends AbstractHandler {

    private static final Logger LOG = Logger.getLogger(FingerHandler.class.getName());

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        assertUPnPActive();

        OutputStream os = null;

        try {
            String response = getResponse(exchange);

            exchange.sendResponseHeaders(Code.HTTP_OK, response.length());

            os = exchange.getResponseBody();

            os.write(response.getBytes());

        } catch (IOException e) {
            LOG.warning("Error serving finger");
            throw e;
        } finally {
            if (os != null) {
                os.close();
            }
            exchange.close();
        }
    }

    private String getResponse(HttpExchange exchange) {
        Finger finger = Librarian.instance().finger(false);
        return JsonUtils.toJson(finger);
    }
}
