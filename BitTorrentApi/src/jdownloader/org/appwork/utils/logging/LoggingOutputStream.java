/**
 * Copyright (c) 2009 - 2011 AppWork UG(haftungsbeschränkt) <e-mail@appwork.org>
 * 
 * This file is part of org.appwork.utils.logging
 * 
 * This software is licensed under the Artistic License 2.0,
 * see the LICENSE file or http://www.opensource.org/licenses/artistic-license-2.0.php
 * for details
 */
package org.appwork.utils.logging;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author daniel, inspired by
 *         http://blogs.sun.com/nickstephen/entry/java_redirecting_system_out_and
 * 
 */
public class LoggingOutputStream extends ByteArrayOutputStream {

    private final Logger logger;
    private final Level  level;

    public LoggingOutputStream(final Logger logger, final Level level) {
        this.logger = logger;
        this.level = level;
    }

    @Override
    public void flush() throws IOException {
        synchronized (this) {
            super.flush();
            if (this.count >= 0) {
                final String record = this.toString().trim();
                if (record.length() > 0) {
                    this.logger.logp(this.level, "", "", record);
                }
            }
            super.reset();
        }
    }
}
