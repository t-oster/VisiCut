package net.lump.print.jetdirect.pjl.commands;

import net.lump.print.PrintConstants;
import net.lump.print.jetdirect.pjl.Variable;
import net.lump.print.jetdirect.pjl.enums.CommandNames;
import net.lump.print.jetdirect.pjl.enums.LanguagePersonality;

/**
 * The SET command enables you to change the value of PJL Current
 * Environment variables for the duration of a PJL job, or until a PJL
 * reset condition defaults the value. Use this command to create a
 * job-specific environment.
 * <p/>
 * The SET command enables you to specify either general variables
 * which are used by all personalities, or printer language-specific
 * variables. Printer language-specific variables must be specified using
 * the LPARM : personality option. Features that are not printer
 * language-specific cannot be specified with the LPARM option.
 * <p/>
 * Values modified with the SET command do not affect the User Default
 * Environment values. Feature settings changed with the SET
 * command are valid until the next PJL reset condition.
 * <p/>
 * A separate SET command must be sent for each environment
 * variable you specify. The command may be used to set any
 * environment variable except CPLOCK, DISKLOCK, PASSWORD,
 * and the read-only variables. See the ?Environment Variables? section
 * of this chapter for a discussion of each environment variable.
 *
 * @author M. Troy Bowman
 */
public class Set extends Command
{
   LanguagePersonality language;
   String variable;
   String value;

   public Set()
   {
      this(null, null, null);
   }

   public Set(Variable variable)
   {
      this(variable.getName(), variable.getValue(), variable.getLParm());
   }

   // this is private because you need to create a set command from a Variable
   private Set(String variable, String value, LanguagePersonality language)
   {
      super(CommandNames.SET);
      setPersonality(language);
      setVariable(variable, value);
   }

   /**
    * Set the personality of this variable.
    *
    * @param language comes from Enter.Personality
    */
   public void setPersonality(LanguagePersonality language)
   {
      this.language = language;
   }

   /**
    * See appendix of PJL Tech reference manual for variables and values.
    *
    * @param variable the variable name
    * @param value    the value of the variable
    */
   public void setVariable(String variable, String value)
   {
      this.variable = variable;
      this.value = value;
   }

   public String toString()
   {
      if (this.variable == null || this.variable.equals(""))
      {
         return "";
      }
      String out = String.format("%s %s", PrintConstants.PJL, command);
      if (language != null)
      {
         out += String.format(" LPARM:%s", language);
      }
      out += String.format(" %s=%s%s", variable, value, PrintConstants.CRLF);
      return out;
   }
}
