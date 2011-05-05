package net.lump.print.jetdirect.pjl.commands;

import net.lump.print.jetdirect.pjl.enums.CommandNames;

/**
 * The OPMSG command prompts the printer to display the specified
 * message and go offline. Use this command to display a message and
 * halt printing until the operator presses the On Line, Continue, or
 * Reset key.
 *
 * @author M. Troy Bowman
 */
public class OpMessage extends Message
{

   private OpMessage()
   {
   }

   public OpMessage(String message)
   {
      super(CommandNames.OPMSG, message);
   }
}
