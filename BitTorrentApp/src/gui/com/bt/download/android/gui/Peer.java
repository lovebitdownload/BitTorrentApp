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

package com.bt.download.android.gui;

import java.io.IOException;
import java.util.List;

import com.bt.download.android.core.FileDescriptor;
import com.bt.download.android.core.HttpFetcher;
import com.frostwire.localpeer.Finger;
import com.frostwire.localpeer.LocalPeer;
import com.frostwire.util.HttpClient;
import com.frostwire.util.HttpClientFactory;
import com.frostwire.util.JsonUtils;

/**
 * 
 * @author gubatron
 * @author aldenml
 * 
 */
public final class Peer {

    private static final int BROWSE_HTTP_TIMEOUT = 10000;

    private String udn;
    private String address;
    private int listeningPort;

    /**
     * 16 bytes (128bit - UUID identifier letting us know who is the sender)
     */
    private String nickname;
    private int numSharedFiles;
    private String clientVersion;
    private int deviceMajorType;

    private int hashCode = -1;
    private final boolean localhost;

    private String key;
    private final LocalPeer p;

    private final HttpClient httpClient;

    public Peer(LocalPeer p, boolean localhost) {
        this.p = p;
        this.key = p.address + ":" + p.port;
        this.address = p.address;
        this.listeningPort = p.port;

        this.nickname = p.nickname;
        this.numSharedFiles = p.numSharedFiles;
        this.deviceMajorType = p.deviceType;
        this.clientVersion = p.clientVersion;
        this.localhost = localhost;

        this.hashCode = key.hashCode();
        this.httpClient = HttpClientFactory.newInstance();
    }

    public String getUdn() {
        return udn;
    }

    public String getAddress() {
        return address;
    }

    public int getListeningPort() {
        return listeningPort;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public int getNumSharedFiles() {
        return numSharedFiles;
    }

    public int getDeviceMajorType() {
        return deviceMajorType;
    }

    public String getClientVersion() {
        return clientVersion;
    }

    public boolean isLocalHost() {
        return localhost;
    }

    public String getFingerUri() {
        return "http://" + address + ":" + listeningPort + "/finger";
    }

    public String getBrowseUri(byte fileType) {
        return "http://" + address + ":" + listeningPort + "/browse?type=" + fileType;
    }

    public String getDownloadUri(FileDescriptor fd) {
        return "http://" + address + ":" + listeningPort + "/download?type=" + fd.fileType + "&id=" + fd.id;
    }

    public Finger finger() {
        if (localhost) {
            return Librarian.instance().finger(localhost);
        } else {
            String uri = getFingerUri();
            byte[] data = new HttpFetcher(uri).fetch();
            String json = new String(data);
            return JsonUtils.toObject(json, Finger.class);
        }
    }

    public List<FileDescriptor> browse(byte fileType) {
        if (localhost) {
            return Librarian.instance().getFiles(fileType, 0, Integer.MAX_VALUE, false);
        } else {
            String url = getBrowseUri(fileType);

            String json = null;
            try {
                json = httpClient.get(url, BROWSE_HTTP_TIMEOUT);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            return JsonUtils.toObject(json, FileDescriptorList.class).files;
        }
    }

    @Override
    public String toString() {
        return "Peer(" + nickname + "@" + (address != null ? address : "unknown") + ", v:" + clientVersion + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof Peer)) {
            return false;
        }

        return hashCode() == ((Peer) o).hashCode();
    }

    @Override
    public int hashCode() {
        return this.hashCode != -1 ? this.hashCode : super.hashCode();
    }

    public String getKey() {
        return key;
    }

    private static final class FileDescriptorList {
        public List<FileDescriptor> files;
    }
}