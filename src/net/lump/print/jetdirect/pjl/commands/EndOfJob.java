package net.lump.print.jetdirect.pjl.commands;

import static net.lump.print.PrintConstants.*;
import net.lump.print.jetdirect.pjl.enums.CommandNames;
import net.lump.print.jetdirect.pjl.commands.Command;

/**
 * The EOJ command marks the end of the job started with the previous
 * JOB command. The EOJ command:
 * <ul><li>Resets the PJL Current Environment variables to their default
 * (NVRAM) values, as if the printer powered down and then
 * powered up again.</li>
 * <li>Resets the page number associated with unsolicited
 * page status.</li>
 * </ul>
 *
 * @author M. Troy Bowman
 */
public class EndOfJob extends Command
{
   private String name;

   public EndOfJob()
   {
      this(null);
   }

   public EndOfJob(String name)
   {
      super(CommandNames.EOJ);
      setName(name);
   }

   /**
    * Set the EOJ name.
    *
    * @param name Using the EOJ command, you can
    *             name your print job. The job name variable is a string and must
    *             be enclosed in double quotes as shown in the command
    *             syntax. The job name string need not be the same name used
    *             in the JOB command. If the NAME option is included, the
    *             unsolicited end-of-job status includes the job name
    *             (if unsolicited job status is enabled).
    */
   public void setName(String name)
   {
      this.name = name;
   }

   public String toString()
   {
      String out = String.format("%s %s ", PJL, command);
      if (name != null)
      {
         out += String.format(" NAME=\"%s\"", name.replaceAll("\"", ""));
      }
      out += CRLF;
      // auto set jobid to default of off
      out += String.format("%s SET JOBID=OFF%s", PJL, CRLF);
      return out;
   }
}
