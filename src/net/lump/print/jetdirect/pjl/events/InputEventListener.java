package net.lump.print.jetdirect.pjl.events;

import java.util.EventListener;

/**
 * This is a listener which listens for input events.
 *
 * @author M. Troy Bowman
 */
public abstract interface InputEventListener extends EventListener
{
   public void inputEventOccurred(InputEvent event);
}
