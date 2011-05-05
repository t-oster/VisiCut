package net.lump.print.jetdirect.pjl.commands;

import net.lump.print.jetdirect.pjl.enums.CommandNames;

/**
 * The STMSG command prompts the printer to display the specified
 * message, go offline, and return a status message indicating which
 * key the operator pressed to return the printer online. Use this
 * command to display a customized message, to halt printing until the
 * operator presses the On Line, Continue, or Reset key, and to take a
 * different action depending on which key the user presses.
 *
 * @author M. Troy Bowman
 */
public class StatusMessage extends Message
{
   public StatusMessage(String message)
   {
      super(CommandNames.STMSG, message);
   }
}
