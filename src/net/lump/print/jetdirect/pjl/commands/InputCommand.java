package net.lump.print.jetdirect.pjl.commands;

import net.lump.print.jetdirect.pjl.enums.CommandNames;
import net.lump.print.jetdirect.pjl.events.InputEvent;
import net.lump.print.jetdirect.pjl.events.InputEventListener;
import javax.swing.event.EventListenerList;

/**
 * This implements the listeners for a command which expects input.
 *
 * @author M. Troy Bowman
 */
public abstract class InputCommand extends Command implements Input
{
   boolean outputRequired = false;
   boolean hasHadOutput = false;
   private EventListenerList listenerList = new EventListenerList();

   public InputCommand(CommandNames command)
   {
      super(command);
   }

   InputCommand()
   {
      super();
   }

   public void fireEvent(InputEvent e)
   {
      Object[] listeners = listenerList.getListenerList();
      for (int i = listeners.length - 2; i >= 0; i -= 2)
      {
         if (listeners[i] == InputEventListener.class)
         {
            ((InputEventListener)listeners[i + 1]).inputEventOccurred(e);
         }
      }
   }

   public void addInputListener(InputEventListener l)
   {
      listenerList.add(InputEventListener.class, l);
   }

   public boolean isOutputRequired()
   {
      return outputRequired;
   }

   public void setOutputRequired(boolean outputRequired)
   {
      this.outputRequired = outputRequired;
   }

   public boolean hasHadOutput()
   {
      return hasHadOutput;
   }

   public void abortOutputRequired() {
      outputRequired = false;
   }

}
