package net.lump.print.jetdirect.pjl.ustatustypes;

import static net.lump.print.PrintConstants.deepcopy;
import net.lump.print.PrintConstants;
import net.lump.print.jetdirect.pjl.events.InputEvent;
import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A PAGE Unsolicited Status type.  This type gets the page of the job that has just been printed.
 *
 * @author M. Troy Bowman
 */
public class UnsolicitedPageType extends UnsolicitedOnOffType implements Serializable
{
   Integer page = 0;
   Integer id = null;

   private UnsolicitedPageType()
   {
   }

   public UnsolicitedPageType(InputValue inputValue)
   {
      inputVariable = "PAGE";
      this.inputValue = inputValue;
   }

   public void setJobId(Integer jobId)
   {
      id = jobId;
   }

   public Integer getPage()
   {
      return page;
   }

   public Integer getId()
   {
      return id;
   }

   /**
    * Reads variables from a BufferedInputStream, which comes after
    * a @PJL USTATUS JOB (which should be what watchForLine() returns.)
    *
    * @param input the BufferedInputStream
    */
   public void readInput(String input)
   {
      Integer id = null;

      if (input == null || input.matches("^\\s*$"))
      {
         return;
      }
      boolean changed = false;

      for (String line : input.split(PrintConstants.EOLp.pattern()))
      {

         Matcher m = Pattern.compile("^(?:(ID)=)?(\\d+)\\s*$").matcher(line);
         if (m.matches() && m.group(2) != null)
         {
            if (m.group(1) != null && m.group(1).equals("ID"))
            {
               id = new Integer(m.group(2));
            }
            else
            {
               Integer page = Integer.parseInt(m.group(2));
               if (!this.page.equals(page))
               {
                  this.page = page;
                  changed = true;
               }
            }
         }
      }

      if (changed &&
          // only do this if either we're not watching IDs, or the ids are defined and they're equal
          (id == null || this.id == null || this.id.equals(id)))
      {
         fireEvent(new InputEvent(deepcopy(this)));
      }
   }

   @Override public String toString()
   {
      return "Printed page number " + getPage();
   }
}

