package net.lump.print.jetdirect.pjl.events;

import java.io.Serializable;
import java.util.EventObject;

/**
 * This event is usually fired when an input event is received.
 *
 * @author M. Troy Bowman
 */
public class InputEvent extends EventObject implements Serializable
{
   public InputEvent(Object source)
   {
      super(source);
   }
}
