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

package com.bt.download.android.gui.util;

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import android.app.Application;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

/**
 * @author gubatron
 * @author aldenml
 *
 */
public class Apk {

    private static final String TAG = "FW.Apk";

    private static Constructor<AssetManager> assetManagerConstructor;
    private static Method assetManagerAddAssetPathMethod;

    private AssetManager assetManager;
    private Resources resources;

    private String path;

    static {
        try {
            assetManagerConstructor = AssetManager.class.getDeclaredConstructor();
            assetManagerConstructor.setAccessible(true);
            assetManagerAddAssetPathMethod = AssetManager.class.getDeclaredMethod("addAssetPath", String.class);
            assetManagerAddAssetPathMethod.setAccessible(true);
        } catch (Throwable e) {
            Log.e(TAG, "Error creating relfection objects", e);
        }
    }

    public Apk(Context context, String path) {
        this.path = path;

        try {
            assetManager = assetManagerConstructor.newInstance();
            assetManagerAddAssetPathMethod.invoke(assetManager, path);

            DisplayMetrics metrics = new DisplayMetrics();
            WindowManager windowManager = (WindowManager) context.getSystemService(Application.WINDOW_SERVICE);
            windowManager.getDefaultDisplay().getMetrics(metrics);

            resources = new Resources(assetManager, metrics, null);

        } catch (Throwable e) {
            Log.e(TAG, "Error loading resources for application: " + path, e);
        }
    }

    public String getPath() {
        return path;
    }

    public XmlResourceParser getAndroidManifest() {
        try {
            return assetManager.openXmlResourceParser("AndroidManifest.xml");
        } catch (Throwable e) {
            return null;
        }
    }

    public String getString(int id) {
        try {
            return resources.getString(id);
        } catch (Throwable e) {
            return null;
        }
    }

    public Drawable getDrawable(int id) {
        try {
            return resources.getDrawable(id);
        } catch (Throwable e) {
            return null;
        }
    }

    public InputStream openRawResource(int id) {
        try {
            return resources.openRawResource(id);
        } catch (Throwable e) {
            return null;
        }
    }
}
