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

package com.frostwire.localpeer;

import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

import com.frostwire.logging.Logger;
import com.frostwire.util.JsonUtils;

/**
 * 
 * Not thread safe.
 * 
 * @author gubatron
 * @author aldenml
 *
 */
public final class LocalPeerManagerImpl implements LocalPeerManager {

    private static final Logger LOG = Logger.getLogger(LocalPeerManagerImpl.class);

    private static final String JMDNS_NAME = "LocalPeerManagerJmDNS";
    private static final String SERVICE_TYPE = "_fw_local_peer._tcp.local.";
    private static final String PEER_PROPERTY = "peer";

    private final MulticastLock lock;

    private final ServiceListener serviceListener;
    private final Map<String, LocalPeer> cache;

    private JmDNS jmdns;
    private ServiceInfo serviceInfo;
    private LocalPeerManagerListener listener;

    public LocalPeerManagerImpl(MulticastLock lock) {
        this.lock = lock;

        this.serviceListener = new JmDNSServiceListener();
        this.cache = new ConcurrentHashMap<String, LocalPeer>();
    }

    public LocalPeerManagerImpl() {
        this(null);
    }

    public LocalPeerManagerListener getListener() {
        return listener;
    }

    public void setListener(LocalPeerManagerListener listener) {
        this.listener = listener;
    }

    @Override
    public boolean isRunning() {
        return jmdns != null;
    }

    @Override
    public void start(InetAddress addr, LocalPeer peer) {
        try {
            cache.clear();

            if (jmdns != null) {
                LOG.warn("JmDNS already working, review the logic");
                stop();
            }

            if (lock != null) {
                lock.acquire();
            }

            if (addr != null) {
                jmdns = JmDNS.create(addr, JMDNS_NAME);
            } else {
                jmdns = JmDNS.create(JMDNS_NAME);
            }
            jmdns.addServiceListener(SERVICE_TYPE, serviceListener);

            serviceInfo = createService(peer, jmdns);
            jmdns.registerService(serviceInfo);

        } catch (Throwable e) {
            LOG.error("Unable to start local peer manager", e);
        }
    }

    @Override
    public void start(LocalPeer peer) {
        start(null, peer);
    }

    @Override
    public void stop() {
        try {
            if (jmdns != null) {

                triggerLocalServiceRemoved();

                jmdns.removeServiceListener(SERVICE_TYPE, serviceListener);
                jmdns.unregisterAllServices();

                try {
                    jmdns.close();
                } catch (IOException e) {
                    LOG.error("Error closing JmDNS", e);
                }

                jmdns = null;
            }

            cache.clear();

            if (lock != null) {
                try {
                    lock.release();
                } catch (RuntimeException re) {
                    LOG.error("RuntimeException caught: Could not release multicast lock", re);
                }
            }
        } catch (Throwable e) {
            LOG.error("Error stopping local peer manager", e);
        }
    }

    @Override
    public void update(LocalPeer peer) {
        try {
            if (jmdns != null) {
                serviceInfo.setText(createProps(peer, jmdns));
            }
        } catch (Throwable e) {
            LOG.error("Error refreshing local peer manager", e);
        }
    }

    private ServiceInfo createService(LocalPeer peer, JmDNS jmdns) {
        return ServiceInfo.create(SERVICE_TYPE, peer.nickname, peer.port, 0, 0, false, createProps(peer, jmdns));
    }

    private Map<String, Object> createProps(LocalPeer peer, JmDNS jmdns) {
        if (jmdns != null) { // fix ip address
            peer = peer.withAddress(getHostAddress(jmdns)).withLocal(false);
        }
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(PEER_PROPERTY, JsonUtils.toJson(peer));
        return props;
    }

    private String getHostAddress(JmDNS jmdns) {
        if (jmdns == null) {
            throw new IllegalArgumentException("jmdns can't be null");
        }

        try {
            return jmdns.getInetAddress().getHostAddress();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void triggerLocalServiceRemoved() {
        if (listener != null && serviceInfo != null) {
            try {
                LocalPeer peer = cache.get(serviceInfo.getKey());
                if (peer != null) {
                    peer = peer.withLocal(true);
                    cache.remove(serviceInfo.getKey());
                    listener.peerRemoved(peer);
                }
            } catch (Throwable e) {
                LOG.error("Error in client listener", e);
            }
        }
    }

    private final class JmDNSServiceListener implements ServiceListener {

        @Override
        public void serviceResolved(ServiceEvent event) {
            if (listener != null) {
                try {
                    ServiceInfo info = event.getInfo();

                    LocalPeer peer = getPeer(info);
                    if (peer != null) {
                        if (isLocal(info, peer)) {
                            peer = peer.withLocal(true);
                        } else {
                            peer = peer.withLocal(false);
                        }
                        cache.put(info.getKey(), peer);
                        listener.peerResolved(peer);
                    }
                } catch (Throwable e) {
                    LOG.error("Error in client listener", e);
                }
            }
        }

        @Override
        public void serviceRemoved(ServiceEvent event) {
            if (listener != null) {
                try {
                    ServiceInfo info = event.getInfo();

                    LocalPeer peer = cache.get(info.getKey());
                    if (peer != null) {
                        cache.remove(info.getKey());
                        listener.peerRemoved(peer);
                    }
                } catch (Throwable e) {
                    LOG.error("Error in client listener", e);
                }
            }
        }

        @Override
        public void serviceAdded(ServiceEvent event) {
            if (jmdns != null) {
                jmdns.requestServiceInfo(event.getType(), event.getName(), 1);
            }
        }

        private LocalPeer getPeer(ServiceInfo info) {
            LocalPeer peer = null;

            try {
                String address = info.getHostAddresses()[0];
                int port = info.getPort();

                String json = info.getPropertyString(PEER_PROPERTY);
                if (json != null) {
                    peer = JsonUtils.toObject(json, LocalPeer.class);

                    // update peer with actual address and port
                    peer = peer.withAddress(address).withPort(port);
                }
            } catch (Throwable e) {
                LOG.error("Unable to extract peer info from service event", e);
            }

            return peer;
        }

        private boolean isLocal(ServiceInfo info, LocalPeer p) {
            return info.getKey().equals(serviceInfo.getKey()) && p.address.equals(getHostAddress(jmdns));
        }
    }
}
