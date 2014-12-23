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

import com.bt.download.android.R;
import com.bt.download.android.core.ConfigurationManager;
import com.bt.download.android.core.Constants;
import com.bt.download.android.gui.PeerManager;
import com.bt.download.android.gui.activities.MainActivity;

import android.app.Activity;
import android.app.Fragment;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

/**
 * 
 * @author gubatron
 * @author aldenml
 * 
 */
public class BrowsePeersDisabledFragment extends Fragment implements MainFragment {

    private TextView header;
    private Button wifiSharingEnableButton;

    public BrowsePeersDisabledFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_browse_peers_disabled, container, false);

        wifiSharingEnableButton = (Button) view.findViewById(R.id.fragment_browse_peers_disabled_button_enable_wifi_sharing);
        wifiSharingEnableButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                onWifiEnableButtonClick();
            }
        });

        TextView disabledTextView = (TextView) view.findViewById(R.id.fragment_browse_peers_disabled_text_title);
        disabledTextView.setText(Html.fromHtml(getString(R.string.wifi_sharing_disabled)));
        disabledTextView.setMovementMethod(LinkMovementMethod.getInstance());

        return view;
    }

    @Override
    public View getHeader(Activity activity) {
        LayoutInflater inflater = LayoutInflater.from(activity);
        header = (TextView) inflater.inflate(R.layout.view_main_fragment_simple_header, null);
        header.setText(R.string.wifi_sharing);

        return header;
    }

    private void onWifiEnableButtonClick() {
        if (getActivity() instanceof MainActivity) {

            final CharSequence buttonCaption = wifiSharingEnableButton.getText();
            wifiSharingEnableButton.setText("...");
            wifiSharingEnableButton.setEnabled(false);

            AsyncTask<Void, Void, Void> enableWifiTask = new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    ConfigurationManager.instance().setBoolean(Constants.PREF_KEY_NETWORK_USE_UPNP, true);
                    PeerManager.instance().start();
                    return null;
                }

                @Override
                protected void onPostExecute(Void result) {
                    MainActivity activity = (MainActivity) getActivity();
                    activity.switchFragment(R.id.menu_main_peers);
                    wifiSharingEnableButton.setText(buttonCaption);
                    wifiSharingEnableButton.setEnabled(true);
                }

            };

            enableWifiTask.execute();
        }
    }
}