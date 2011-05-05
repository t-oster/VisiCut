package net.lump.print.jetdirect.pjl.ustatustypes;

import static net.lump.print.PrintConstants.deepcopy;
import static net.lump.print.PrintConstants.*;
import net.lump.print.jetdirect.pjl.events.InputEvent;
import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A model for an Unsolicited Status type which has STATUS output variables.
 *
 * @author M. Troy Bowman
 */
public abstract class UnsolicitedStatusType extends UnsolicitedType implements Serializable
{
   private Integer code = -1;
   private String display = "";
   private Boolean online = false;

   public static enum UnsolicitedOutputVariable
   {
      CODE, DISPLAY, ONLINE;

      public static String join(char character)
      {
         String variables = "";
         for (UnsolicitedOutputVariable ov : values())
         {
            if (variables.length() > 0)
            {
               variables += character;
            }
            variables += ov.toString();
         }
         return variables;
      }
   }

   public Integer getCode()
   {
      return code;
   }

   public String getDisplay()
   {
      return display;
   }

   public Boolean getOnline()
   {
      return online;
   }

   /**
    * Reads variables from a BufferedInputStream, which comes after
    * a @PJL USTATUS JOB (which should be what watchForLine() returns.)
    *
    * @param input String
    */
   public void readInput(String input)
   {
      boolean changed = false;

      if (input == null || input.matches("^\\s*$"))
      {
         return;
      }

      // this pattern catches the way in which variables are expressed.
      Pattern p = Pattern.compile(
         String.format("^(%s)(?:=\"?(.+?)\"?)?$", UnsolicitedOutputVariable.join('|')));

      for (String line : input.split(EOLp.pattern()))
      {

         // continue on empty line
         if (line == null || input.matches("^\\s*$"))
         {
            continue;
         }

         Matcher m = p.matcher(line);
         if (m.matches())
         {
            if (m.group(1) != null)
            {
               switch (UnsolicitedOutputVariable.valueOf(m.group(1)))
               {
                  case CODE:
                     if (m.group(2) != null)
                     {
                        if (code != null && !m.group(2).equals(code.toString()))
                        {
                           changed = true;
                        }
                        code = Integer.parseInt(m.group(2));
                     }
                     break;
                  case DISPLAY:
                     if (m.group(2) != null)
                     {
                        if (display != null && !m.group(2).equals(display))
                        {
                           changed = true;
                        }
                        display = m.group(2);
                     }
                     break;
                  case ONLINE:
                     boolean online = m.group(2) != null && m.group(2).equalsIgnoreCase("TRUE");
                     if (this.online != online)
                     {
                        changed = true;
                     }
                     this.online = online;
               }
            }
         }
      }

      if (changed)
      {
         fireEvent(new InputEvent(deepcopy(this)));
      }
   }

   public String toString() {
      return String.format("Code: %s Online: %s Display: %s", getCode(), getOnline(), getDisplay());
   }
}
