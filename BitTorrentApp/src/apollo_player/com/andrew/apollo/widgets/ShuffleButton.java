/*
 * Copyright (C) 2012 Andrew Neal Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.andrew.apollo.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;

import com.andrew.apollo.MusicPlaybackService;
import com.andrew.apollo.utils.MusicUtils;
import com.bt.download.android.R;

/**
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class ShuffleButton extends ImageButton implements OnClickListener {

    /**
     * @param context The {@link Context} to use
     * @param attrs The attributes of the XML tag that is inflating the view.
     */
    public ShuffleButton(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        // Control playback (cycle shuffle)
        setOnClickListener(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onClick(final View v) {
        MusicUtils.cycleShuffle();
        updateShuffleState();
    }

    /**
     * Sets the correct drawable for the shuffle state.
     */
    public void updateShuffleState() {
        switch (MusicUtils.getShuffleMode()) {
        case MusicPlaybackService.SHUFFLE_NORMAL:
            setContentDescription(getResources().getString(R.string.accessibility_shuffle_all));
            setImageResource(R.drawable.btn_playback_shuffle_all);
            break;
        case MusicPlaybackService.SHUFFLE_AUTO:
            setContentDescription(getResources().getString(R.string.accessibility_shuffle_all));
            setImageResource(R.drawable.btn_playback_shuffle_all);
            break;
        case MusicPlaybackService.SHUFFLE_NONE:
            setContentDescription(getResources().getString(R.string.accessibility_shuffle));
            setImageResource(R.drawable.btn_playback_shuffle);
            break;
        default:
            break;
        }
    }

}
