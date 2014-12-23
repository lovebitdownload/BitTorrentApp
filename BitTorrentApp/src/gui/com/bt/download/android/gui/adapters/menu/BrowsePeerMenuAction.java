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

package com.bt.download.android.gui.adapters.menu;

import android.content.Context;
import android.content.Intent;

import com.bt.download.android.R;
import com.bt.download.android.core.Constants;
import com.bt.download.android.gui.Peer;
import com.bt.download.android.gui.activities.BrowsePeerActivity;
import com.bt.download.android.gui.views.MenuAction;

/**
 * @author gubatron
 * @author aldenml
 *
 */
public class BrowsePeerMenuAction extends MenuAction {

    private final Peer peer;

    public BrowsePeerMenuAction(Context context, int textId, Peer peer) {
        super(context, R.drawable.contextmenu_icon_user, textId);
        this.peer = peer;
    }

    public BrowsePeerMenuAction(Context context, Peer peer) {
        this(context, R.string.browse_peer, peer);
    }

    @Override
    protected void onClick(Context context) {
        if (peer != null) {
            Intent i = new Intent(context, BrowsePeerActivity.class);
            i.putExtra(Constants.EXTRA_PEER_UUID, peer.getKey());
            context.startActivity(i);
        }
    }
}
