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

package com.bt.download.android.gui.fragments;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.bt.download.android.R;
import com.bt.download.android.core.ConfigurationManager;
import com.bt.download.android.core.Constants;
import com.bt.download.android.gui.Peer;
import com.bt.download.android.gui.PeerManager;
import com.bt.download.android.gui.activities.MainActivity;
import com.bt.download.android.gui.adapters.PeerListAdapter;
import com.bt.download.android.gui.services.Engine;
import com.bt.download.android.gui.util.UIUtils;
import com.bt.download.android.gui.views.AbstractFragment;
import com.bt.download.android.gui.views.TimerObserver;
import com.bt.download.android.gui.views.TimerService;
import com.bt.download.android.gui.views.TimerSubscription;

/**
 * 
 * @author gubatron
 * @author aldenml
 * 
 */
public class BrowsePeersFragment extends AbstractFragment implements TimerObserver, MainFragment {

    private PeerListAdapter adapter;

    private View header;
    private ListView list;
    
    private TimerSubscription subscription;

    public BrowsePeersFragment() {
        super(R.layout.fragment_browse_peers);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setRetainInstance(true);

        if (Engine.instance().isStarted() && ConfigurationManager.instance().getBoolean(Constants.PREF_KEY_NETWORK_USE_UPNP)) {
            PeerManager.instance().start();
        }

        setupAdapter();
    }
    
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        subscription = TimerService.subscribe(this, 2);
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        
        subscription.unsubscribe();
    }

    @Override
    public void onTime() {
        List<Peer> peers = PeerManager.instance().getPeers();
        adapter.updateList(peers);
    }

    @Override
    public View getHeader(Activity activity) {
        LayoutInflater inflater = LayoutInflater.from(activity);
        header = inflater.inflate(R.layout.view_browse_peers_header, null);

        TextView title = (TextView) header.findViewById(R.id.view_browse_peers_header_text_title);
        title.setText(R.string.wifi_sharing);

        ImageButton wifiSharingOffButton = (ImageButton) header.findViewById(R.id.view_browse_peers_header_image_button_toggle_wifi_sharing);
        wifiSharingOffButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                onWifiSharingOffButtonClicked();
            }
        });

        return header;
    }
    
    @Override
    protected void initComponents(View rootView) {
        list = findView(rootView, R.id.fragment_browse_peers_list);
    }

    private void onWifiSharingOffButtonClicked() {
        UIUtils.showYesNoDialog(getActivity(), R.string.are_you_sure_wifi_sharing_off, R.string.are_you_sure, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                turnOffWifiSharingAndRefresh();
            }
        });

    }

    private void turnOffWifiSharingAndRefresh() {
        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                ConfigurationManager.instance().setBoolean(Constants.PREF_KEY_NETWORK_USE_UPNP, false);
                PeerManager.instance().stop();
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                MainActivity activity = (MainActivity) getActivity();
                activity.switchFragment(R.id.menu_main_peers);
            }
        };

        task.execute();
    }

    private void setupAdapter() {
        adapter = new PeerListAdapter(BrowsePeersFragment.this.getActivity(), new ArrayList<Peer>());
        list.setAdapter(adapter);
        onTime();
    }
}