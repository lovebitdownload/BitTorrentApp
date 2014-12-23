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

import java.io.Closeable;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import sun.net.httpserver.Code;

import com.bt.download.android.core.ConfigurationManager;
import com.bt.download.android.core.FileDescriptor;
import com.bt.download.android.gui.Librarian;
import com.bt.download.android.gui.services.Engine;
import com.bt.download.android.gui.transfers.PeerHttpUpload;
import com.bt.download.android.gui.transfers.TransferManager;
import com.sun.net.httpserver.HttpExchange;

/**
 * @author gubatron
 * @author aldenml
 *
 */
class DownloadHandler extends AbstractHandler {

    private static final Logger LOG = Logger.getLogger(DownloadHandler.class.getName());

    
    @Override
    public void handle(final HttpExchange exchange) throws IOException {
        Engine.instance().getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    internalHandler(exchange);
                } catch (IOException e) {
                    LOG.log(Level.WARNING, "DownloadHandler async handle error", e);
                }
            }
        });
    }
    
    public void internalHandler(HttpExchange exchange) throws IOException {
        assertUPnPActive();

        OutputStream os = null;
        FileInputStream fis = null;

        byte type = -1;
        int id = -1;

        PeerHttpUpload upload = null;

        try {

            List<NameValuePair> query = URLEncodedUtils.parse(exchange.getRequestURI(), "UTF-8");

            for (NameValuePair item : query) {
                if (item.getName().equals("type")) {
                    type = Byte.parseByte(item.getValue());
                }
                if (item.getName().equals("id")) {
                    id = Integer.parseInt(item.getValue());
                }
            }

            if (type == -1 || id == -1) {
                exchange.sendResponseHeaders(Code.HTTP_BAD_REQUEST, 0);
                return;
            }

            if (TransferManager.instance().getActiveUploads() >= ConfigurationManager.instance().maxConcurrentUploads()) {
                sendBusyResponse(exchange);
                return;
            }

            FileDescriptor fd = Librarian.instance().getFileDescriptor(type, id);
            if (fd == null) {
                throw new IOException("There is no such file shared");
            }

            upload = TransferManager.instance().upload(fd);

            exchange.getResponseHeaders().add("Content-Type", fd.mime);
            exchange.sendResponseHeaders(Code.HTTP_OK, fd.fileSize);

            os = exchange.getResponseBody();

            fis = new FileInputStream(fd.filePath);

            byte[] buffer = new byte[4 * 1024];
            int n;
            int count = 0;

            while ((n = fis.read(buffer, 0, buffer.length)) != -1) {
                os.write(buffer, 0, n);
                upload.addBytesSent(n);

                if (upload.isCanceled()) {
                    try {
                        throw new IOException("Upload cancelled");
                    } finally {
                        os.close();
                    }
                }
                
                count += n;
                if (count > 4096) {
                    count = 0;
                    Thread.yield();
                }
            }

        } catch (IOException e) {
            LOG.log(Level.INFO, "Error uploading file type=" + type + ", id=" + id);
            throw e;
        } finally {
            close(os);
            close(fis);

            try {
                exchange.close();
            } catch (Throwable e) {
                // ignore
            }

            if (upload != null) {
                upload.complete();
            }
        }
    }

    private void close(Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (Throwable e) {
                // ignore
            }
        }
    }

    private void sendBusyResponse(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().add("Retry-After", "10"); // retry in 10 seconds
        exchange.sendResponseHeaders(Code.HTTP_UNAVAILABLE, 0);
    }
}
