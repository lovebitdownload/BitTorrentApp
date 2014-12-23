/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011-2013, FrostWire(R). All rights reserved.
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

package com.bt.download.android.gui.transfers;

import com.frostwire.transfers.TransferItem;

import java.util.Date;
import java.util.List;

/**
 * @author gubatron
 * @author aldenml
 *
 */
public interface Transfer {

    public String getDisplayName();

    public String getStatus();

    public int getProgress();

    public long getSize();

    public Date getDateCreated();

    public long getBytesReceived();

    public long getBytesSent();

    public long getDownloadSpeed();

    public long getUploadSpeed();

    public long getETA();

    public boolean isComplete();

    public List<TransferItem> getItems();

    public void cancel();
    
    public String getDetailsUrl();
}
