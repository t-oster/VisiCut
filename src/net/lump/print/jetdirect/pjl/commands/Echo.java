package net.lump.print.jetdirect.pjl.commands;

import static net.lump.print.PrintConstants.*;
import net.lump.print.jetdirect.pjl.enums.CommandNames;
import java.io.IOException;
import java.util.regex.Pattern;

/**
 * The ECHO command prompts the printer to return a specified
 * message to the host computer. Use the ECHO command to
 * synchronize the printer with the host computer to ensure that the
 * status received is the requested status information.
 * <p/>
 * Status responses are directed to the printer's I/O port from which the
 * request is received. When using status readback, applications must
 * synchronize status messages to ensure that status responses are
 * indeed the requested status. To clear any possible unread status
 * responses requested by previous applications, upon starting up, an
 * application should use the ECHO command.
 * <p/>
 * If unsolicited status is enabled, applications should properly handle
 * receiving unsolicited status responses at any time. In particular, be
 * aware that applications may receive an unsolicited status response
 * after requesting printer status information.
 *
 * @author M. Troy Bowman
 */
public class Echo extends net.lump.print.jetdirect.pjl.commands.InputCommand
{
   String words;

   private Echo()
   {
   }

   public Echo(String words)
   {
      super(CommandNames.ECHO);
      setWords(words);
      this.outputRequired = false;
   }

   /**
    * Set the words to echo.
    *
    * @param words must begin with a
    *              printable character, and can contain any Roman-8 character
    *              from 33 through 255, in addition to space characters and
    *              horizontal tabs. The <words> parameter is not a string
    *              variable, and therefore need not be enclosed in quotation
    *              marks. The parameter should be unique, such as the time
    *              indicated by the real time clock at the moment the ECHO
    *              command is issued. A unique message eliminates the
    *              possibility of duplicate messages being echoed by different
    *              applications. The maximum length for <words> is 80 bytes.
    */
   public void setWords(String words)
   {
      words = words.replaceAll("\\r\\n|\\r|\\n", "");
      if (words.length() > 80)
      {
         words = words.substring(0, 80);
      }
      this.words = words;
   }

   public String line()
   {
      return String.format("%s %s %s", PJL, command, words);
   }

   public String toString()
   {
      return line() + CRLF;
   }

   public String getInputTriggerLine()
   {
      return line();
   }

   public void readInput(String input) throws IOException
   {
      hasHadOutput = true;
   }

   public Pattern getInputEndPattern()
   {
      return FFp;
   }
}
