package net.lump.print.jetdirect.pjl.ustatustypes;

import static net.lump.print.PrintConstants.deepcopy;
import net.lump.print.PrintConstants;
import net.lump.print.jetdirect.pjl.events.InputEvent;
import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A JOB Unsolicited Status type.  This notifies when a job has begun or ended.
 * If this is a postscript job, we've extended this to find out whether errored out (failed).
 *
 * @author M. Troy Bowman
 */
public class UnsolicitedJobType extends UnsolicitedOnOffType implements Serializable
{

   public enum UnsolicitedJobOutputVariable
   {
      START, CANCELED, END, NAME, PAGES, ID, RESULT;

      public static String join(char character)
      {
         String variables = "";
         for (UnsolicitedJobOutputVariable ov : values())
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

   public enum Status
   {
      WAITING,
      IN_PROGRESS,
      CANCELED,
      FINISHED
   }

   public enum Result
   {
      UNKNOWN,
      OK,
      USER_CANCELED,
      HOST_CANCELED,
      RESOURCE_CANCELED;

      private String raw = null;

      public void setRaw(String s)
      {
         raw = s;
      }

      public String getRaw()
      {
         return raw;
      }
   }

   private Status status = Status.WAITING;
   private String name;
   private Integer pages;
   private Integer id;
   private Result result = Result.UNKNOWN;

   private UnsolicitedJobType()
   {
   }

   public UnsolicitedJobType(InputValue inputValue, String jobName)
   {
      inputVariable = "JOB";
      this.inputValue = inputValue;
      this.name = jobName;
   }

   public Status getStatus()
   {
      return status;
   }

   public String getName()
   {
      return name;
   }

   public Integer getPages()
   {
      return pages;
   }

   public Integer getId()
   {
      return id;
   }

   public Result getResult()
   {
      return result;
   }

   /**
    * Reads variables from a BufferedInputStream, which comes after
    * a @PJL USTATUS JOB (which should be what watchForLine() returns.)
    *
    * @param input String
    */
   public void readInput(String input)
   {

      // keep track of whether the state has changed.
      boolean changed = false;
      String name = null;
      Integer id = null;

      // if the string has nothing of consequence, just ignore it.
      if (input == null || input.matches("^\\s*$"))
      {
         return;
      }

      // this pattern catches the way in which variables are expressed.
      Pattern p = Pattern.compile(
         String.format("^(%s)(?:=\"?(.*?)\"?)?$", UnsolicitedJobOutputVariable.join('|')));

      // split the string into lines and analyze each line in turn.
      for (String line : input.split(PrintConstants.EOLp.pattern()))
      {

         // if the line is empty, skip it.
         if (line == null || line.matches("^\\s*$"))
         {
            continue;
         }

         // if the line matches our variable pattern,
         Matcher m = p.matcher(line);
         if (m.matches())
         {
            if (m.group(1) != null)
            {
               switch (UnsolicitedJobOutputVariable.valueOf(m.group(1)))
               {
                  case START:
                     if (status != null && status != Status.IN_PROGRESS)
                     {
                        changed = true;
                     }
                     status = Status.IN_PROGRESS;

                     break;
                  case END:
                     if (status != null && status != Status.FINISHED)
                     {
                        changed = true;
                     }
                     status = Status.FINISHED;
                     break;
                  case NAME:
                     if (m.group(2) != null)
                     {
                        name = m.group(2);
                     }
                     break;
                  case ID:
                     if (m.group(2) != null)
                     {
                        id = Integer.parseInt(m.group(2));
                     }
                     // only set ID if it hasn't been set yet.
                     if (this.id == null) {
                        this.id = id;
                     }
                     break;
                  case RESULT:
                     if (m.group(2) != null)
                     {
                        try
                        {
                           if (result != Result.valueOf(m.group(2))) {
                              result = Result.valueOf(m.group(2));
                              changed = true;
                           }
                        }
                        catch (IllegalArgumentException e)
                        {
                           result = Result.UNKNOWN;
                        }
                        result.setRaw(m.group(2));
                     }
                     break;
                  case CANCELED:
                     if (status != null && status != Status.CANCELED)
                     {
                        changed = true;
                     }
                     status = Status.CANCELED;
                     result = result == Result.UNKNOWN ? Result.USER_CANCELED : result;
                     break;
                  case PAGES:
                     if (m.group(2) != null)
                     {
                        pages = Integer.parseInt(m.group(2));
                     }
                     break;
               }
            }
         }
      }

      // make sure the result gets set if the result is unknown and we're in a canceled state.
      if (this.status == Status.CANCELED && this.result == Result.UNKNOWN) {
         this.result = Result.USER_CANCELED;
      }

      // make sure the state is set to OK if state is still unknown and we're finished.
      if (this.status == Status.FINISHED && this.result == Result.UNKNOWN) {
         this.result = Result.OK;
      }

      // fire the event only if the job name matches the name we're looking for.
      if (changed && ((name != null && name.equals(this.name))
                      || (this.id != null && id != null && this.id.equals(id))))
      {
         fireEvent(new InputEvent(deepcopy(this)));
      }
   }

   /**
    * Use this when you have to set the ID through other means because the printer doesn't give the job ID with job events.
    * @param id the id to set it to.
    */
   public void setId(Integer id) {
      this.id = id;
   }

   @Override public String toString()
   {
      if (status == UnsolicitedJobType.Status.FINISHED)
      {
         return String.format("Job: %s%s%s Status: %s Pages printed: %d",
                              getName(),
                              getId() == null ? "" : " ID: " + getId(),
                              getResult() == null ? "" : " Result: " + getResult().name(),
                              getStatus().name(), getPages());
      }
      else
      {
         return String.format("Job: %s%s Status: %s", getName(),
                              getId() == null ? "" : " ID: " + getId(), getStatus().name());
      }
   }
}
