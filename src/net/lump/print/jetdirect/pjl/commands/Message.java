package net.lump.print.jetdirect.pjl.commands;

import static net.lump.print.PrintConstants.*;
import net.lump.print.jetdirect.pjl.enums.CommandNames;

/**
 * Generic message for the message commands.
 *
 * @author M. Troy Bowman
 */
public abstract class Message extends Command
{
   String message;

   Message()
   {
   }

   public Message(CommandNames command, String message)
   {
      super(command);
      this.message = message;
   }

   /**
    * Set the message.
    *
    * @param message The variable message can be any
    *                combination of printable characters (except quotation marks,
    *                character 34) and spaces, with a limit of 1 line of 16 characters
    *                The message variable is a string
    *                and must be enclosed in double quotes as shown in the
    *                command syntax.
    */
   public void setMessage(String message)
   {
      message = message.replaceAll("\"", "");
      this.message = message;
   }

   public String toString()
   {
      return String.format("%s %s DISPLAY=\"%s\"%s", PJL, command, message, CRLF);
   }
}
