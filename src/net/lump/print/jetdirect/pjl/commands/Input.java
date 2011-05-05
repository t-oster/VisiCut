package net.lump.print.jetdirect.pjl.commands;

import net.lump.print.jetdirect.pjl.events.InputEvent;
import net.lump.print.jetdirect.pjl.events.InputEventListener;
import java.io.IOException;
import java.io.Serializable;
import java.util.regex.Pattern;

/**
 * This interface indicates that the command expects input.
 *
 * @author M. Troy Bowman
 */
public interface Input extends Serializable
{
   /**
    * This is the line which begins the reading of the input.
    *
    * @return String
    */
   public String getInputTriggerLine();

   /**
    * This is the method which specially handles the input.
    *
    * @param input String
    * @throws IOException for IO propblems
    */
   public void readInput(String input) throws IOException;

   /**
    * This string indicates that the end of the body of the event has been reached.
    *
    * @return String usually a Form Feed.
    */
   public Pattern getInputEndPattern();

   /**
    * This will notify all listeners that an input event occurred.
    *
    * @param event InputEvent
    */
   public void fireEvent(InputEvent event);

   /**
    * Add a listener to the list of listeners for input events.
    *
    * @param eventListener InputEventListener
    */
   public void addInputListener(InputEventListener eventListener);

   /**
    * Whether we've received output for this command yet.
    *
    * @return boolean
    */
   public boolean hasHadOutput();

   /**
    * Whether this input class requires output to be received before the SocketClient moves on.
    *
    * @return boolean
    */
   public boolean isOutputRequired();

   /**
    * Abort the requirement for output.  This will have no effect if it already isn't required.
    */
   public void abortOutputRequired();
}
