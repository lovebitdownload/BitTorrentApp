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
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.Toast;

import com.andrew.apollo.utils.MusicUtils;
import com.bt.download.android.R;
import com.bt.download.android.gui.util.UIUtils;

/**
 * A custom {@link ImageButton} that represents the "play and pause" button.
 * 
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class PlayPauseButton extends ImageButton implements OnClickListener {

    /**
     * @param context The {@link Context} to use
     * @param attrs The attributes of the XML tag that is inflating the view.
     */
    public PlayPauseButton(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        // Control playback (play/pause)
        setOnClickListener(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onClick(final View v) {
        MusicUtils.playOrPause();
        updateState();
    }

    /**
     * Sets the correct drawable for playback.
     */
    public void updateState() {
        if (MusicUtils.isPlaying()) {
            setContentDescription(getResources().getString(R.string.accessibility_pause));
            setImageResource(R.drawable.btn_playback_pause);
        } else {
            setContentDescription(getResources().getString(R.string.accessibility_play));
            setImageResource(R.drawable.btn_playback_play);
            UIUtils.showToastMessage(getContext(), getContext().getString(R.string.player_paused_press_and_hold_to_stop), Toast.LENGTH_SHORT, Gravity.CENTER_VERTICAL, 0, 10);
        }
    }

}
