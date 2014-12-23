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
 * @Deprecated Use org.appwork.utils.event.DefaultEvent instead
 */

@Deprecated
public abstract class DefaultIntEvent extends DefaultEvent {
    /**
     * ID of this Event
     */
    private final int eventID;

    /**
     * Parameters of this event.
     */
    private Object[]       parameters = null;

    /**
     * Creates a new Event
     * 
     * @param caller
     *            The Object that fires this event
     * @param eventID
     *            The Event's id
     */
    public DefaultIntEvent(final Object caller, final int eventID) {
        super(caller);

        this.eventID = eventID;
    }

    /**
     * 
     * @param caller
     *            The Object that fires this event
     * @param eventID
     *            The Event's id
     * @param parameters
     *            a parameter object
     */

    public DefaultIntEvent(final Object caller, final int eventID, final Object... parameters) {
        this(caller, eventID);
        this.parameters = parameters;
    }

    /**
     * @return the {@link DefaultIntEvent#eventID}
     * @see DefaultIntEvent#eventID
     */
    public int getEventID() {
        return this.eventID;
    }

    /**
     * @return the {@link DefaultIntEvent#parameters}
     * @see DefaultIntEvent#parameters
     */
    public Object getParameter() {
        if (this.parameters == null || this.parameters.length == 0) { return null; }
        return this.parameters[0];
    }

    public Object[] getParameters() {

        return this.parameters;
    }
}
