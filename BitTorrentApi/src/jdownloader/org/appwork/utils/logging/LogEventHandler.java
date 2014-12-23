/**
 * Copyright (c) 2009 - 2010 AppWork UG(haftungsbeschränkt) <e-mail@appwork.org>
 * 
 * This file is part of org.appwork.utils.logging
 * 
 * This software is licensed under the Artistic License 2.0,
 * see the LICENSE file or http://www.opensource.org/licenses/artistic-license-2.0.php
 * for details
 */
package org.appwork.utils.logging;

import java.util.ArrayList;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

/*WARNING: this class logs into memory, can cause OOM if not emptied once in a while*/
public class LogEventHandler extends Handler {
    private static final LogEventHandler INSTANCE  = new LogEventHandler();
    private LogEventSender               eventSender;
    private boolean                      doLogging = false;

    private LogEventHandler() {
        super();
        synchronized (lock) {
            cache = new ArrayList<LogRecord>();
        }
        eventSender = new LogEventSender();
        doLogging = false;
    }

    /*
     * use this function to enable/disable logging to memory. disabling it will
     * clear logbuffer
     */
    public void enableLogging(boolean b) {
        if (b == false && doLogging == true) {
            /* clear logged entries */
            synchronized (lock) {
                cache.clear();
            }
        }
        doLogging = b;
    }

    public boolean isEnabled() {
        return doLogging;
    }

    /**
     * @return the {@link LogEventHandler#eventSender}
     * @see LogEventHandler#eventSender
     */
    public LogEventSender getEventSender() {
        return eventSender;
    }

    public static LogEventHandler getInstance() {
        return INSTANCE;
    }

    private java.util.List<LogRecord> cache;
    private Object               lock = new Object();

    public java.util.List<LogRecord> getCache() {
        synchronized (lock) {
            return new ArrayList<LogRecord>(cache);
        }
    }

    public void close() {
    }

    public void flush() {
    }

    public void publish(LogRecord logRecord) {
        if (doLogging) {
            this.cache.add(logRecord);
            getEventSender().fireEvent(new LogEvent(this, LogEvent.NEW_RECORD, logRecord));
        }
    }

}
