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

package com.bt.download.android.gui.adapters;

import java.lang.ref.WeakReference;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView.RecyclerListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.andrew.apollo.model.Song;
import com.andrew.apollo.utils.MusicUtils;
import com.bt.download.android.R;
import com.bt.download.android.gui.adapters.SongAdapter.MusicHolder.DataHolder;
import com.frostwire.util.Ref;

/**
 * @author gubatron
 * @author aldenml
 *
 */
public final class SongAdapter extends ArrayAdapter<Song> {

    private DataHolder[] data;

    public SongAdapter(Context ctx) {
        super(ctx, 0);
    }

    @Override
    public View getView(final int position, View convertView, final ViewGroup parent) {
        // Recycle ViewHolder's items
        MusicHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.view_queue_list_item, parent, false);
            holder = new MusicHolder(convertView);

            // clear icon
            holder.icon.get().setImageDrawable(null);
            holder.icon.get().setImageBitmap(null);

            convertView.setTag(holder);
        } else {
            holder = (MusicHolder) convertView.getTag();
        }

        DataHolder dataHolder = data[position];

        holder.title.get().setText(dataHolder.title);
        holder.author.get().setText(dataHolder.author);

        if (MusicUtils.getCurrentAudioId() == dataHolder.itemId) {
            holder.title.get().setTextColor(getContext().getResources().getColor(R.color.app_text_highlight));
            holder.author.get().setTextColor(getContext().getResources().getColor(R.color.app_text_highlight));
            holder.icon.get().setImageResource(R.drawable.ic_headphones);
        } else {
            holder.title.get().setTextColor(getContext().getResources().getColor(R.color.app_text_primary));
            holder.author.get().setTextColor(getContext().getResources().getColor(R.color.app_text_primary));
            holder.icon.get().setImageDrawable(null);
            holder.icon.get().setImageBitmap(null);
        }

        return convertView;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    public void buildCache() {
        data = new DataHolder[getCount()];
        for (int i = 0; i < getCount(); i++) {
            Song song = getItem(i);

            data[i] = new DataHolder();
            data[i].itemId = song.mSongId;
            data[i].title = song.mSongName;
            data[i].author = song.mArtistName;
        }
    }

    public void unload() {
        clear();
        data = null;
    }

    public static final class MusicHolder {

        public WeakReference<TextView> title;

        public WeakReference<TextView> author;

        public WeakReference<ImageView> icon;

        public MusicHolder(View view) {
            title = Ref.weak((TextView) view.findViewById(R.id.view_queue_list_item_title));
            author = Ref.weak((TextView) view.findViewById(R.id.view_queue_list_item_author));
            icon = Ref.weak((ImageView) view.findViewById(R.id.view_queue_list_item_icon));
        }

        public final static class DataHolder {

            public long itemId;

            public String title;

            public String author;

            public Bitmap icon;
        }
    }

    public static final class RecycleHolder implements RecyclerListener {

        @Override
        public void onMovedToScrapHeap(final View view) {
            MusicHolder holder = (MusicHolder) view.getTag();
            if (holder == null) {
                holder = new MusicHolder(view);
                view.setTag(holder);
            }

            if (Ref.alive(holder.title)) {
                holder.title.get().setText(null);
            }

            if (Ref.alive(holder.author)) {
                holder.author.get().setText(null);
            }

            if (Ref.alive(holder.icon)) {
                holder.icon.get().setImageDrawable(null);
                holder.icon.get().setImageBitmap(null);
            }
        }
    }
}
