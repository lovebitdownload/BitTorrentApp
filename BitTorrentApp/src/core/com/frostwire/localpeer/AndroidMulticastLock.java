/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011-2014, FrostWire(R). All rights reserved.
 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.frostwire.localpeer;

import java.util.concurrent.atomic.AtomicInteger;

import android.net.wifi.WifiManager;

/**
 * 
 * @author gubatron
 * @author aldenml
 *
 */
public final class AndroidMulticastLock implements MulticastLock {

   public AndroidMulticastLock(WifiManager wifi) {
    }

    @Override
    public void acquire() {
    }

    @Override
    public void release() {
    }

    @Override
    public boolean isHeld() {
        return false;
    }

}
