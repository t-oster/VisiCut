package net.lump.print.jetdirect.pjl.commands;

import net.lump.print.PrintConstants;
import static net.lump.print.PrintConstants.*;
import net.lump.print.jetdirect.pjl.Variable;
import static net.lump.print.jetdirect.pjl.commands.Info.SRF.*;
import net.lump.print.jetdirect.pjl.enums.*;
import net.lump.print.jetdirect.pjl.events.InputEvent;
import java.io.IOException;
import java.util.HashMap;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The INFO command requests a specified category of information.
 * Use this command to find the printer model,
 * configuration, memory available, page count, status information, PJL
 * file system information, and a list of the printer variables, including
 * environmental, printer language-dependent, and unsolicited status
 * variables.
 *
 * @author M. Troy Bowman
 */
public class Info extends InputCommand
{

   HashMap<String, Variable> variables = new HashMap<String, Variable>();
   VariableCategory category;

   private Info()
   {
   }

   public Info(VariableCategory category)
   {
      super(CommandNames.INFO);
      this.category = category;
      this.outputRequired = true;
   }

   private String line()
   {
      return String.format("%s %s %s", PJL, command, category);
   }

   public String toString()
   {
      return line() + CRLF;
   }

   public String getInputTriggerLine()
   {
      return line();
   }

   public Variable getVariable(String variable)
   {
      return variables.get(variable);
   }

   public java.util.Set<String> getVariableNames()
   {
      return variables.keySet();
   }

   public VariableCategory getCategory()
   {
      return category;
   }

   enum SRF
   { // start regex fields
      ALL,
      LPARM,
      PERSONALITY,
      NAME,
      VALUE,
      NUMBER,
      TYPE,
      READONLY
   }

   public void readInput(String input) throws IOException
   {
      final String readonly = "READONLY";

      // if the string has nothing of consequence, just ignore it.
      if (input == null || input.matches("^\\s*$"))
      {
         return;
      }

      // make a hash of the old variables for change management
      HashMap<String, Variable> oldVariables = new HashMap<String, Variable>();
      oldVariables.putAll(variables);

      // this pattern catches the way in which variables are expressed.
      Pattern startPattern = Pattern.compile(String.format(
         "^(?:(?:(LPARM)\\s?:\\s?(%s)|IPARM\\s?:\\s?\\S+?)? )?([\\w ]*\\w)(?:=\\\"?([\\w\\.]+)\\\"?)?(?: \\[(?:(?:(\\d+) (%s))?(?: (%s))?)\\])?.*$",
         LanguagePersonality.join('|'),
         VariableType.join('|'),
         readonly));
      Pattern appendPattern = Pattern.compile("^\\s+(\\S+)\\s*$");

      Variable currentVariable = null;

      // split the string into lines and analyze each line in turn.
      for (String line : input.split(PrintConstants.EOLp.pattern()))
      {

         // if the line is empty, skip it.
         if (line == null || line.matches("^\\s*$"))
         {
            continue;
         }

         // if the line matches our variable pattern,
         Matcher m = startPattern.matcher(line);
         if (m.matches() && m.group(NAME.ordinal()) != null)
         {

            // set the current variable to the one we're on now.
            currentVariable = new Variable(m.group(NAME.ordinal()), m.group(VALUE.ordinal()));

            variables.put(currentVariable.getName(), currentVariable);

            if (m.group(NUMBER.ordinal()) != null && m.group(TYPE.ordinal()) != null)
            {
               int size = Integer.parseInt(m.group(NUMBER.ordinal()));
               currentVariable.setType(VariableType.valueOf(m.group(TYPE.ordinal())));
               currentVariable.setPossibilities(new Vector<String>(size));
            }

            if (m.group(LPARM.ordinal()) != null && m.group(PERSONALITY.ordinal()) != null)
            {
               currentVariable.setLParm(LanguagePersonality.valueOf(m.group(PERSONALITY.ordinal())));
            }

            // set readonly if this is not a VARIABLES category, or this variable is read-only.
            currentVariable.setReadOnly(
               this.category != VariableCategory.VARIABLES
               || m.group(READONLY.ordinal()) != null && m.group(READONLY.ordinal()).equals(readonly));
         }

         m = appendPattern.matcher(line);
         if (currentVariable != null
             && currentVariable.getPossibilities() != null
             && m.matches()
             && m.group(1) != null)
         {
            currentVariable.getPossibilities().add(m.group(1));
         }
      }

      boolean changed = false;
      // check for changes
      for (String s : variables.keySet())
      {
         if ((!oldVariables.containsKey(s)) || (!oldVariables.get(s).equals(variables.get(s))))
         {
            changed = true;
            fireEvent(new InputEvent(variables.get(s)));
         }
      }

      // fire an event for the entire info object if changed is true;
      if (changed)
      {
         fireEvent(new InputEvent(deepcopy(this)));
      }

      // we've officially had output now.
      hasHadOutput = true;
   }

   public Pattern getInputEndPattern()
   {
      return FFp;
   }
}
