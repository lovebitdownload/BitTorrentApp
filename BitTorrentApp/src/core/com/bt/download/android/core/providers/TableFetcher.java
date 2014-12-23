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

package com.bt.download.android.core.providers;

import android.database.Cursor;
import android.net.Uri;

import com.bt.download.android.core.FileDescriptor;

/**
 * @author gubatron
 * @author aldenml
 *
 */
public interface TableFetcher {

    public String[] getColumns();

    public String getSortByExpression();

    public Uri getContentUri();

    public void prepare(Cursor cur);

    public FileDescriptor fetch(Cursor cur);

    public byte getFileType();
}