/**
 * Copyright (c) 2009 - 2010 AppWork UG(haftungsbeschränkt) <e-mail@appwork.org>
 * 
 * This file is part of org.appwork.utils
 * 
 * This software is licensed under the Artistic License 2.0,
 * see the LICENSE file or http://www.opensource.org/licenses/artistic-license-2.0.php
 * for details
 */
package org.appwork.utils;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.FileNameMap;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Locale;

public class Files {

    /**
     * Returns the fileextension for a file with the given name
     * 
     * @see #getFileNameWithoutExtension(String)
     * @param name
     * @return
     */
    public static String getExtension(final String name) {
        if (name == null) { return null; }
        final int index = name.lastIndexOf(".");
        if (index < 0) { return null; }
        return name.substring(index + 1).toLowerCase();
    }

    /**
     * return all files ( and folders if includeDirectories is true ) for the
     * given files
     * 
     * @param includeDirectories
     * @param files
     * @return
     */
    public static java.util.List<File> getFiles(final boolean includeDirectories, final boolean includeFiles, final File... files) {
        return Files.getFiles(new FileFilter() {

            @Override
            public boolean accept(final File pathname) {
                if (includeDirectories && pathname.isDirectory()) { return true; }
                if (includeFiles && pathname.isFile()) { return true; }
                return false;
            }
        }, files);
    }

    /**
     * @param b
     * @param c
     * @param filter
     * @param source
     * @return
     */
    public static java.util.List<File> getFiles(final FileFilter filter, final File... files) {
        final java.util.List<File> ret = new ArrayList<File>();
        if (files != null) {
            for (final File f : files) {
                if (!f.exists()) {
                    continue;
                }
                if (filter == null || filter.accept(f)) {
                    ret.add(f);
                }

                if (f.isDirectory()) {

                    ret.addAll(Files.getFiles(filter, f.listFiles()));
                }
            }
        }
        return ret;
    }
}
