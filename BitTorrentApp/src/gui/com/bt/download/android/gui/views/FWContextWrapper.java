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

import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;

import com.bt.download.android.R;
import com.frostwire.logging.Logger;

/**
 * @author gubatron
 * @author aldenml
 * 
 */
public class FWContextWrapper extends ContextWrapper {

    private static final Logger LOG = Logger.getLogger(FWContextWrapper.class);

    private final Resources resources;

    public FWContextWrapper(Context ctx) {
        super(ctx);
        this.resources = new ResourcesWrapper(ctx.getResources());
    }

    @Override
    public Resources getResources() {
        return resources;
    }

    private static final class ResourcesWrapper extends Resources {

        private final Resources res;

        private final int overscroll_edge;
        private final int overscroll_glow;

        public ResourcesWrapper(Resources res) {
            super(res.getAssets(), res.getDisplayMetrics(), res.getConfiguration());

            this.res = res;

            this.overscroll_edge = getPlatformDrawableId("overscroll_edge");
            this.overscroll_glow = getPlatformDrawableId("overscroll_glow");
        }

        @Override
        public Drawable getDrawable(int id) throws NotFoundException {
            Drawable d = null;

            if (id == this.overscroll_edge) {
                d = res.getDrawable(R.drawable.overscroll_edge);
            } else if (id == this.overscroll_glow) {
                d = res.getDrawable(R.drawable.overscroll_glow);
            } else {
                d = super.getDrawable(id);
            }

            return d;
        }

        private int getPlatformDrawableId(String name) {
            int id = 0;
            try {
                id = Class.forName("com.android.internal.R$drawable").getField(name).getInt(null);
            } catch (Throwable e) {
                LOG.warn("Cannot find internal resource class");
            }
            return id;
        }
    }
}
