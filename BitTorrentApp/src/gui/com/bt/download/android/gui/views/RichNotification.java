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

package com.bt.download.android.gui.views;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bt.download.android.R;


public class RichNotification extends LinearLayout {
	public static final List<Integer> wasDismissed = new ArrayList<Integer>();
	private final boolean titleUnderlined;
	private final String title;
	private String description;
	private final Drawable icon;
	private OnClickListener clickListener;
	
	public RichNotification(Context context, AttributeSet attrs) {
		super(context, attrs);		
		TypedArray attributes = context.obtainStyledAttributes(attrs, R.styleable.RichNotification);
		icon = attributes.getDrawable(R.styleable.RichNotification_rich_notification_icon);
		titleUnderlined = attributes.getBoolean(R.styleable.RichNotification_rich_notification_title_underlined, false);
        if (titleUnderlined) {
        	title = "<u>" + attributes.getString(R.styleable.RichNotification_rich_notification_title) + "</u>";
        } else {
        	title = attributes.getString(R.styleable.RichNotification_rich_notification_title);
        }
		description = attributes.getString(R.styleable.RichNotification_rich_notification_description);
		attributes.recycle();
		clickListener = null;
	}


	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		View.inflate(getContext(), R.layout.view_rich_notification, this);
		
		ImageView imageViewIcon = (ImageView) findViewById(R.id.view_rich_notification_icon);
		if (imageViewIcon != null && icon != null) {
			imageViewIcon.setBackgroundDrawable(icon);
		}
		
		OnClickListener onClickNotificationListener = new OnClickListener() {
			@Override
			public void onClick(View v) {
				onClickNotification();
			}
		};

		TextView textViewTitle = updateTextViewText(R.id.view_rich_notification_title, (titleUnderlined) ? Html.fromHtml(title) : title, onClickNotificationListener);
		TextView textViewDescription = updateTextViewText(R.id.view_rich_notification_description, description, onClickNotificationListener);
		
		//the limited android XML api won't allow android:fontFamily in XML
		//and the allowed values for android:typeFace don't include Roboto (just sans | serif...
		//font downloaded at http://developer.android.com/design/style/typography.html
		Typeface mFont = Typeface.createFromAsset(getContext().getAssets(), "fonts/Roboto-Light.ttf");
		textViewTitle.setTypeface(mFont,Typeface.BOLD);
		textViewDescription.setTypeface(mFont, Typeface.NORMAL);
		
		ImageButton dismissButton = (ImageButton) findViewById(R.id.view_rich_notification_close_button);
		dismissButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				onDismiss();				
			}
		});
	}
	
	private TextView updateTextViewText(int textViewId, CharSequence text, OnClickListener onClickNotificationListener) {
		TextView textView = (TextView) findViewById(textViewId);
		if (textView != null && text != null) {
			textView.setText(text);
		}
		
		if (textView != null && onClickNotificationListener != null) {
			textView.setOnClickListener(onClickNotificationListener);
		}
		
		return textView;
	}


	public void setOnClickListener(OnClickListener listener) {
		clickListener = listener;
	}
	
	public boolean wasDismissed() {
		return wasDismissed.contains(this.getId());
	}
	
	public void setDescription(String newDescription) {
		description = newDescription;
		updateTextViewText(R.id.view_rich_notification_description, description, null);
	}
	
	protected void onDismiss() {
		wasDismissed.add(getId());
		setVisibility(View.GONE);
	}

	protected void onClickNotification() {
		if (clickListener != null) {
			clickListener.onClick(this);
		}
	}
}