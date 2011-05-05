package net.lump.print.jetdirect.pjl.commands;

import net.lump.print.PrintConstants;
import net.lump.print.jetdirect.pjl.enums.CommandNames;
import net.lump.print.jetdirect.pjl.ustatustypes.UnsolicitedType;

import java.io.IOException;
import java.io.Serializable;
import java.util.regex.Pattern;

/**
 * The USTATUS command is used to enable or disable unsolicited
 * printer status. Unlike the status information solicited by sending the
 * INQUIRE, DINQUIRE, or INFO commands, unsolicited status is sent
 * automatically when the status changes. Use the USTATUS command
 * when you want to know:
 * <ul><li>Device status changes (such as printer open, paper jams, and paper out conditions)</li>
 * <li>Job status changes (when a JOB command is encountered,
 * the job completely prints, or the job is canceled)</li>
 * <li>Page status changes (when each printed page reaches the output tray)</li>
 * <li>Timed status (periodic status report at a specified time interval)</li></ul>
 * <p/>
 * Unlike solicited status, the USTATUS command does not have an
 * immediate response. Instead, unsolicited status messages are sent
 * only when the printer status changes.
 *
 * @author M. Troy Bowman
 */
public class UnsolicitedStatus extends InputCommand implements Serializable
{
   UnsolicitedType type;

   private UnsolicitedStatus()
   {
   }

   public UnsolicitedStatus(UnsolicitedType type)
   {
      this(type, false);
   }

   public UnsolicitedStatus(UnsolicitedType type, boolean required)
   {
      super(CommandNames.USTATUS);
      type.setUnsolicitedStatusCommand(this);
      this.type = type;
      setOutputRequired(required);
   }

   private String line()
   {
      return String.format("%s %s %s", PrintConstants.PJL, command, type.getInputVariable());
   }

   public String toString()
   {
      return String.format("%s = %s%s", line(), type.getInputValueString(), PrintConstants.CRLF);
   }

   public String getInputTriggerLine()
   {
      return line();
   }

   public void readInput(String input) throws IOException
   {
      // read input using the special type's method
      type.readInput(input);

      // we've officially had some output now
      hasHadOutput = true;
   }

   public Pattern getInputEndPattern()
   {
      return PrintConstants.FFp;
   }

   public UnsolicitedType getUStatusType()
   {
      return type;
   }

   public void setToOff()
   {
      type.setToOff();
   }
}
