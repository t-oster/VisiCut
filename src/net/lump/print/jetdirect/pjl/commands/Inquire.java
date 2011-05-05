package net.lump.print.jetdirect.pjl.commands;

import static net.lump.print.PrintConstants.*;
import net.lump.print.jetdirect.pjl.Variable;
import net.lump.print.jetdirect.pjl.enums.CommandNames;
import net.lump.print.jetdirect.pjl.events.InputEvent;
import java.io.IOException;
import java.util.regex.Pattern;

/**
 * .
 *
 * @author M. Troy Bowman
 */
public class Inquire extends InputCommand
{
   Variable variable;

   private Inquire()
   {
   }

   public Inquire(Variable variable)
   {
      super(CommandNames.INQUIRE);
      this.variable = variable;
      this.outputRequired = true;
   }

   public String toString()
   {
      String out = String.format("%s %s", PJL, command);
      if (variable.getLParm() != null)
      {
         out += String.format(" LPARM:%s", variable.getLParm().toString());
      }
      out += String.format(" %s%s", variable.getName(), CRLF);
      return out;
   }

   public String getInputTriggerLine()
   {
      return String.format("%s %s %s", PJL, command, variable.getName());
   }

   public Variable getVariable()
   {
      return variable;
   }

   public void readInput(String input) throws IOException
   {
      if (input == null || input.matches("^\\s*$"))
      {
         return;
      }

      // nuke whitespace
      input = EOL_OR_FFp.matcher(input).replaceAll("");

      String oldValue = variable.getValue();
      variable.setValue(input);
      if (!oldValue.equals(variable.getValue()))
      {
         fireEvent(new InputEvent(this));
      }
      hasHadOutput = true;
   }

   public Pattern getInputEndPattern()
   {
      return FFp;
   }
}
