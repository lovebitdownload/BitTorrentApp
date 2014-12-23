/**
 * 
 */
package org.appwork.utils.event;

import java.util.EventListener;

/**
 * @author $Author: unknown$
 * 
 */
public interface DefaultEventListener<E> extends EventListener {

    /**
     * @param event
     */
    abstract public void onEvent(E event);

}
