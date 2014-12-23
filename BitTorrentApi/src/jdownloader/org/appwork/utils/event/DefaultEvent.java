/**
 * Copyright (c) 2009 - 2010 AppWork UG(haftungsbeschränkt) <e-mail@appwork.org>
 * 
 * This file is part of org.appwork.utils.event
 * 
 * This software is licensed under the Artistic License 2.0,
 * see the LICENSE file or http://www.opensource.org/licenses/artistic-license-2.0.php
 * for details
 */
package org.appwork.utils.event;

/**
 * Abstract Eventclass. All Events should be derived from this class to asuire
 * compatibility to the EventSystem.
 * 
 * @author $Author: unknown$
 * 
 */
public abstract class DefaultEvent {
    /**
     * The caller that fired this event
     */
    private final Object caller;

    /**
     * Creates a new Event
     * 
     * @param caller
     *            The Object that fires this event
     */
    public DefaultEvent(final Object caller) {
        this.caller = caller;

    }

    /**
     * @return the {@link DefaultEvent#caller}
     * @see DefaultEvent#caller
     */
    public Object getCaller() {
        return this.caller;
    }

}
