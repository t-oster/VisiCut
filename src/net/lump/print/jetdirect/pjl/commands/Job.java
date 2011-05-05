package net.lump.print.jetdirect.pjl.commands;

import static net.lump.print.PrintConstants.*;
import net.lump.print.jetdirect.pjl.enums.CommandNames;
import net.lump.print.jetdirect.pjl.events.InputEventListener;
import net.lump.print.jetdirect.pjl.ustatustypes.UnsolicitedJobType;
import net.lump.print.jetdirect.pjl.ustatustypes.UnsolicitedPageType;
import java.util.ArrayList;
import java.util.Vector;

/**
 * The JOB command informs the printer of the start of a PJL job and
 * synchronizes the job and page status information. It also is used to
 * specify which pages of a job are printed. Use the JOB/EOJ
 * commands for spooling and related applications to monitor printing
 * status, name a job, print portions of a job, or to mark job boundaries
 * to keep the printer from treating a single print job as multiple jobs (for
 * example, when printing a job with a banner page). Also, in jobs sent
 * to those printers supporting the PASSWORD option, use a JOB
 * command to specify the correct password. For printers that support
 * the DISPLAY variable, the JOB command can be used to display a
 * control panel message while printing the job.
 *
 * @author M. Troy Bowman
 */
public class Job extends Command implements HasUnsolicitedStati
{
   private String name;
   private Integer start = 1;
   private Integer end;
   private String display;
   private Integer password;
   private ArrayList<String> attrs = new ArrayList<String>();

   private UnsolicitedStatus jobUStatus;
   private UnsolicitedStatus pageUStatus;

   public Job()
   {
      this(null, null, null, null);
   }

   public Job(String name, String display)
   {
      this(name, null, null, display);
   }

   public Job(String name)
   {
      this(name, null, null, null);
   }

   public Job(String name, Integer start, Integer end, String display)
   {
      super(CommandNames.JOB);
      setName(name);
      setStart(start);
      setEnd(end);
      setDisplay(display);
      jobUStatus = new UnsolicitedStatus(new UnsolicitedJobType(UnsolicitedJobType.InputValue.ON, this.name));
      pageUStatus = new UnsolicitedStatus(new UnsolicitedPageType(UnsolicitedJobType.InputValue.ON));
   }

   public Job appendAttr(String string) {
      attrs.add(string);
      return this;
   }

   public static String generateName(String prefix)
   {
      return String.format("%s-%s-%s", prefix, System.currentTimeMillis(), Math.random());
   }

   /**
    * Set Name.
    *
    * @param name The command option NAME tags the
    *             print job with a job name. The variable job name can be any
    *             combination of printable characters and spaces or horizontal
    *             tab characters, with a maximum of 80 significant characters.
    *             The job name variable is a string and must be enclosed in
    *             double quotes, as shown in the command syntax. If the NAME
    *             option is included, the unsolicited job status includes the job
    *             name (if unsolicited job status is enabled).
    */
   public void setName(String name)
   {
      if (name.length() > 80)
      {
         name = name.substring(0, 80);
      }
      this.name = name;
   }

   /**
    * Set Start.
    *
    * @param start The command option START is used to
    *              provide a non-printing mode for skipping to a selected portion
    *              of the job. It indicates the first page that is printed. If the
    *              START option is omitted, the printer starts printing at the
    *              beginning of the job. If the end of the job comes before the
    *              START page, no pages are printed.
    */
   public void setStart(Integer start)
   {
      this.start = start == null ? null : Math.abs(start);
   }

   /**
    * Set End.
    *
    * @param end The command option END indicates the
    *            page number of the last page to be printed. The last page
    *            variable is relative to page 1 of the print job. If the END variable
    *            is omitted, the printer prints to the end of the job. If the end of
    *            the job is encountered before the START page, no pages are
    *            printed. If the end of job is encountered before the END page,
    *            printing terminates. Additionally, if the START page is greater
    *            than the END page, no pages are printed.
    */
   public void setEnd(Integer end)
   {
      this.end = end == null ? null : Math.abs(end);
   }

   /**
    * Set Display text.
    *
    * @param display The command option DISPLAY
    *                is used to display a job message on the control panel display.
    *                The message is displayed when the printer begins to work on
    *                this job and is removed when the last page of this job reaches
    *                the output bin. The variable "display text" can be any
    *                combination of printable characters and spaces or horizontal
    *                tabs, with a maximum of 80 characters. The actual number of
    *                characters displayed depends on the printer. The display limit
    *                can be determined by sending an INFO CONFIG command to
    *                the printer. The printer will return DISPLAY LINES = value and
    *                DISPLAY CHARACTER SIZE = value as part of the response.
    */
   public void setDisplay(String display)
   {
      if (display != null && display.length() > 80)
      {
         display = display.substring(0, 80);
      }
      this.display = display;
   }

   /**
    * Set password.
    *
    * @param password The command option PASSWORD
    *                 allows the application to modify the NVRAM variables if the
    *                 password matches the active password variable. (Using PJL,
    *                 the NVRAM variables are modified using either the DEFAULT
    *                 or INITIALIZE commands; some printer language commands
    *                 may also modify NVRAM variables.) Passwords are set using
    *                 the DEFAULT command. The default password value is 0,
    *                 which indicates PJL security is disabled?any job can modify
    *                 printer feature settings using the DEFAULT or INITIALIZE
    *                 commands. If any other password value is active, PJL jobs
    *                 must issue the correct password value or they are disabled
    *                 from using the DEFAULT or INITIALIZE commands. (See the
    *                 ?PJL Job Security? section at the end of this chapter.)
    */
   public void setPassword(Integer password)
   {
      if (password != null)
      {
         password = Math.abs(password);
         if (password > 65535)
         {
            throw new IllegalArgumentException("password must be between 0 and 65535");
         }
      }
      this.password = password;
   }

   public String getName()
   {
      return name;
   }

   public Integer getStart()
   {
      return start;
   }

   public Integer getEnd()
   {
      return end;
   }

   public String getDisplay()
   {
      return display;
   }

   public Integer getPassword()
   {
      return password;
   }

   public String toString()
   {
      // auto set jobid to on so we get to actually see the id and result.
      String out = String.format("%s SET JOBID=ON%s", PJL, CRLF);

      out += String.format("%s %s ", PJL, command);
      if (name != null)
      {
         out += String.format(" NAME=\"%s\"", name.replaceAll("\"", ""));
      }
      if (start != null)
      {
         out += String.format(" START=%d", start);
      }
      if (end != null)
      {
         out += String.format(" END=%d", end);
      }
      if (password != null)
      {
         out += String.format(" PASSWORD=%d", end);
      }
      if (display != null)
      {
         out += String.format(" DISPLAY=\"%s\"", display.replaceAll("\"", ""));
      }
      out += CRLF;
      for (String attr : attrs) {
         out += String.format("%s SET JOBATTR=\"%s\"%s", PJL, attr.replaceAll("\"",""), CRLF);
      }
      return out;
   }

   /**
    * Get the status of the job
    *
    * @return see USJob.Status
    */
   public UnsolicitedJobType.Status getStatus()
   {
      return ((UnsolicitedJobType)(getJobUStatus().getUStatusType())).getStatus();
   }

   /**
    * Return the total pages for this job.
    *
    * @return Integer will be null if printer hasn't finished processing the entire job yet.
    */
   public Integer getTotalPages()
   {
      if (getStatus() != UnsolicitedJobType.Status.FINISHED)
      {
         return null;
      }
      return ((UnsolicitedJobType)(getJobUStatus().getUStatusType())).getPages();
   }

   /**
    * Return the pages printed so far.
    *
    * @return Integer will be null if the job hasn't begun printing yet.
    */
   public Integer getPagesPrinted()
   {
      if (getStatus() == UnsolicitedJobType.Status.WAITING)
      {
         return null;
      }
      return ((UnsolicitedPageType)(getJobUStatus().getUStatusType())).getPage();
   }

   public Integer getJobId()
   {
      if (getStatus() == UnsolicitedJobType.Status.WAITING)
      {
         return null;
      }
      return ((UnsolicitedJobType)(getJobUStatus().getUStatusType())).getId();
   }

   /**
    * Return the UStatus object which pertains to job status.
    *
    * @return UStatus
    */
   public UnsolicitedStatus getJobUStatus()
   {
      return jobUStatus;
   }

   /**
    * Return the UStatus object wich pertains to page status.
    *
    * @return UStatus
    */
   public UnsolicitedStatus getPageUStatus()
   {
      return pageUStatus;
   }

   /**
    * Add a listener for job events.
    *
    * @param inputListener a UStatusListener
    */
   public void addJobListener(InputEventListener inputListener)
   {
      getJobUStatus().addInputListener(inputListener);
   }

   /**
    * Add a listener for page events.
    *
    * @param inputListener a UStatusListener
    */
   public void addPageListener(InputEventListener inputListener)
   {
      getPageUStatus().addInputListener(inputListener);
   }

   /**
    * Return a vector of all of the UStatus objects which should be sent to the printer
    * if unsolicited events are to be processed.  Also, the thread that watches for the
    * UStatus.inputEventTriggerLine() must then call the UStatus.readInputEvent() in order to
    * have the UStatus type parse the output and call listeners appropriately.
    *
    * @return Vector&lt;UStatus&gt;
    */
   public Vector<UnsolicitedStatus> getUStati()
   {
      Vector<UnsolicitedStatus> uStati = new Vector<UnsolicitedStatus>();
      uStati.add(getJobUStatus());
      uStati.add(getPageUStatus());
      return uStati;
   }
}
