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

import com.bt.download.android.core.ConfigurationManager;
import com.bt.download.android.core.Constants;
import com.bt.download.android.gui.httpserver.HttpServerManager;
import com.frostwire.localpeer.AndroidMulticastLock;
import com.frostwire.localpeer.LocalPeer;
import com.frostwire.localpeer.LocalPeerManager;
import com.frostwire.localpeer.LocalPeerManagerImpl;
import com.frostwire.localpeer.LocalPeerManagerListener;

import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps track of the Peers we know.
 * 
 * @author gubatron
 * @author aldenml
 * 
 */
public final class PeerManager {

    private static final String TAG = "FW.PeerManager";

    private final Map<String, Peer> addressMap;

    private final PeerComparator peerComparator;

    private static PeerManager instance;

    private final LocalPeerManager peerManager;
    private final HttpServerManager httpServerManager;

    public static PeerManager instance() {
        if (instance == null) {
            instance = new PeerManager();
        }
        return instance;
    }

    private PeerManager() {
        this.addressMap = new ConcurrentHashMap<String, Peer>();

        this.peerComparator = new PeerComparator();

        this.peerManager = new LocalPeerManagerImpl(new AndroidMulticastLock(NetworkManager.instance().getWifiManager()));
        this.peerManager.setListener(new LocalPeerManagerListener() {

            @Override
            public void peerResolved(LocalPeer peer) {
                onMessageReceived(peer, true);
            }

            @Override
            public void peerRemoved(LocalPeer peer) {
                onMessageReceived(peer, false);
            }
        });

        this.httpServerManager = new HttpServerManager();
    }

    public Peer getLocalPeer() {
        return new Peer(createLocalPeer(), true);
    }

    public void onMessageReceived(LocalPeer p, boolean added) {
        if (p != null) {
            Peer peer = new Peer(p, p.local);

            updatePeerCache2(peer, !added);
        }
    }

    /**
     * This returns a shadow-copy of the peer cache as an ArrayList plus the local peer.
     * 
     * @return
     */
    public List<Peer> getPeers() {
        List<Peer> peers = new ArrayList<Peer>();

        peers.addAll(addressMap.values());

        Collections.sort(peers, peerComparator);

        return peers;
    }

    /**
     * @param uuid
     * @return
     */
    public Peer findPeerByKey(String key) {
        if (key == null) {
            return null;
        }

        Peer p = addressMap.get(key);

        return p;
    }

    public void clear() {
        addressMap.clear();
    }

    public void removePeer(Peer p) {
        try {
            updatePeerCache2(p, true);
        } catch (Throwable e) {
            Log.e(TAG, "Error removing peer from manager", e);
        }
    }

    public void start() {
        if (!peerManager.isRunning()) {
            httpServerManager.start(NetworkManager.instance().getListeningPort());
            try {
                peerManager.start(NetworkManager.instance().getMulticastInetAddress(), createLocalPeer());
            } catch (IOException e) {
                peerManager.start(null, createLocalPeer());
            }
        }
    }

    public void stop() {
        httpServerManager.stop();
        peerManager.stop();
    }

    public void updateLocalPeer() {
        peerManager.update(createLocalPeer());
    }

    private LocalPeer createLocalPeer() {
        String address = "0.0.0.0";
        int port = NetworkManager.instance().getListeningPort();
        int numSharedFiles = Librarian.instance().getNumFiles();
        String nickname = ConfigurationManager.instance().getNickname();
        String clientVersion = Constants.TORRENTCOW_VERSION;
        int deviceType = Constants.DEVICE_MAJOR_TYPE_PHONE;

        return new LocalPeer(address, port, true, nickname, numSharedFiles, deviceType, clientVersion);
    }

    private void updatePeerCache2(Peer peer, boolean disconnected) {
        if (disconnected) {
            addressMap.remove(peer.getKey());
        } else {
            addressMap.put(peer.getKey(), peer);
            updatePeerCache(peer, disconnected);
        }
    }

    /**
     * Invoke this method whenever you have new information about a peer. For
     * now we invoke this whenever we receive a ping.
     * 
     * @param peer
     * @param disconnected
     */
    private void updatePeerCache(Peer peer, boolean disconnected) {
        // first time we hear from a peer
        if (!addressMap.containsKey(peer.getKey())) {
            // no more ghosts...
            if (disconnected) {
                return;
            }

            addressMap.put(peer.getKey(), peer);
            Log.v(TAG, String.format("Adding new peer, total=%s: %s", addressMap.size(), peer));
        } else {
            if (!disconnected) {
                addressMap.put(peer.getKey(), peer); // touch the element and updates the properties
            } else {
                addressMap.remove(peer.getKey());
            }
        }
    }

    private static final class PeerComparator implements Comparator<Peer> {
        public int compare(Peer lhs, Peer rhs) {
            if (lhs.isLocalHost()) {
                return -1;
            }
            if (rhs.isLocalHost()) {
                return 1;
            }
            return lhs.getNickname().compareTo(rhs.getNickname());
        }
    }
}