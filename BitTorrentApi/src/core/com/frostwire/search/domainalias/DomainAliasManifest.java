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

package com.frostwire.search.domainalias;

import java.util.List;
import java.util.Map;

/**
 * Modeled after a JSON manifest that could look like this
 * 
 * {
 *   "version": 134,
 *   "lastUpdated":1383936035000,
 *   "aliases": {
 *      "site.com":["siteMirror1.com","siteMirror2.com","siteMirrorN.com"],
 *      "otherSite.com":["otherSite.org","otherSite.net","mirror.otherSite.io"]
 *   }
 *  }
 *  
 * @author gubatron
 * @author aldenml
 *
 */
public class DomainAliasManifest {
    public int version;
    public long lastUpdated;
    public Map<String, List<String>> aliases;
}