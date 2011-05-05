package net.lump.print.jetdirect.pjl.commands;

import static net.lump.print.PrintConstants.*;
import net.lump.print.jetdirect.pjl.enums.CommandNames;
import java.io.IOException;
import java.util.regex.Pattern;

/**
 * .
 *
 * @author M. Troy Bowman
 */
public class DmInfo extends InputCommand
{
   String name;
   String value;

   public DmInfo(String variableName, String value)
   {
      super(CommandNames.DMINFO);
      this.name = variableName;
      this.value = value;
      this.outputRequired = true;
   }

   public String line()
   {
      return String.format("%s %s %s=\"%s\"", PJL, command, name, value);
   }

   public String toString()
   {
      return line() + CRLF;
   }

   public String getInputTriggerLine()
   {
      return line();
   }

   public Pattern getInputEndPattern()
   {
      return FFp;
   }

   public void readInput(String input) throws IOException
   {
      hasHadOutput = true;
   }



}
