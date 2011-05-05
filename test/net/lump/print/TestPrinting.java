package net.lump.print;

import java.util.*;

import net.lump.print.jetdirect.SocketClient;
import net.lump.print.jetdirect.pjl.Variable;
import net.lump.print.jetdirect.pjl.commands.*;
import net.lump.print.jetdirect.pjl.commands.Set;
import net.lump.print.jetdirect.pjl.enums.LanguagePersonality;
import net.lump.print.jetdirect.pjl.enums.VariableCategory;
import net.lump.print.jetdirect.pjl.events.InputEvent;
import net.lump.print.jetdirect.pjl.events.InputEventListener;
import net.lump.print.jetdirect.pjl.ustatustypes.UnsolicitedDeviceType;
import net.lump.print.jetdirect.pjl.ustatustypes.UnsolicitedJobType;
import net.lump.print.jetdirect.pjl.ustatustypes.UnsolicitedPageType;
import net.lump.print.jetdirect.pjl.ustatustypes.UnsolicitedStatusType;
import net.lump.print.snmp.JobInfoOutcome;
import net.lump.print.snmp.JobInfoState;
import net.lump.print.snmp.PrinterSnmpUtil;
import org.testng.annotations.Test;

import static net.lump.print.PrintConstants.*;

public class TestPrinting
{

   public static final String printer = "10.49.1.251";

   /*
    * This tests the querying of the printer uptime through snmp.
    *
    * @throws Exception
    */
   @Test
   public static void testSnmpStats() throws Exception
   {
      PrinterSnmpUtil snmp = new PrinterSnmpUtil(printer);
      snmp.connect();
      TreeMap<String, Object> results = snmp.getPrinterStats();

      long uptime = Long.parseLong((String)results.get("uptime"));
      assert uptime > 0;

      for (String field : new String[]{"model", "pcl", "pjl", "postscript", "serial", "uptime", "on-off-line",
            "active-jobid"})
      {
         if (results.get(field) instanceof String)
         {
            System.out.println(field + ": " + results.get(field));
            assert results.get(field) != null && ((String)results.get(field)).length() > 0 : field + " is null or invalid";
         }
      }
   }

   @Test
   public static void testJobCancel() throws Exception
   {
      final boolean canceled[] = new boolean[]{false};
      final boolean[] foundOutput = new boolean[]{false};

      String rawStart = "StartPostScriptPrintJob";
      Raw raw = new Raw(
            "%!PS" + CRLF
                  + "(" + rawStart + "\\r\\n) print" + CRLF
                  + "(Hello World\r\n\r\n) print" + CRLF
            , rawStart, EOT_OR_EOUELp, true);
      raw.addInputListener(new InputEventListener()
      {
         public void inputEventOccurred(InputEvent event)
         {
            if (event.getSource() instanceof Raw)
            {
               System.err.println(((Raw)event.getSource()).getOutput());
               if (((Raw)event.getSource()).getOutput().indexOf("Hello World") > -1)
               {
                  foundOutput[0] = true;
               }
            }
         }
      });

      final PrinterSnmpUtil snmp = new PrinterSnmpUtil(printer);
      snmp.connect();

      final HashMap<String, TreeMap<String, Object>> info = new HashMap<String, TreeMap<String, Object>>();

      String jobName = "Testing Cancel " + System.currentTimeMillis() + Math.random();
      Job job = new Job(jobName, "Testing Cancel").appendAttr("This is testing whether I can cancel a job");

      job.addJobListener(new InputEventListener() {
         public void inputEventOccurred(InputEvent event) {
            if (event.getSource() instanceof UnsolicitedJobType) {

               if (((UnsolicitedJobType)event.getSource()).getStatus() == UnsolicitedJobType.Status.IN_PROGRESS) {
                  try {
                     Integer id = ((UnsolicitedJobType)event.getSource()).getId();
                     info.put("stats", snmp.getPrinterStats());
                     if (info.get("stats").get("active-jobid") == null) {
                        System.err.println(printer + " didn't let us query the job id");
                     }
                     info.put("info",
                           snmp.getPrinterJobInfo(Integer.parseInt((String)info.get("stats").get("active-jobid"))));
                     if (id == null) {
                        id = Integer.parseInt((String)info.get("stats").get("active-jobid"));
                        ((UnsolicitedJobType)event.getSource()).setId(id);
                     }
                     if (id != null && !snmp.cancelJob(id)) {
                        System.err.println(printer + " didn't allow cancel of job");
                     }
                  } catch (Exception e) {
                     e.printStackTrace();
                  }
               } else if (((UnsolicitedJobType)event.getSource()).getStatus() == UnsolicitedJobType.Status.CANCELED) {
                  canceled[0] = true;
               } else {
                  System.err.println(event.getSource());
               }
            }
         }
      });

      // fire an event before it starts.
      job.getJobUStatus().fireEvent(new InputEvent(job.getJobUStatus().getUStatusType()));

      ArrayList<Command> commands = new ArrayList<Command>();
      commands.add(new Comment("Testing 1 2 3"));
      commands.add(job);
      commands.add(new EnterLanguage(LanguagePersonality.POSTSCRIPT));
      commands.add(raw);
      commands.add(new EndOfJob(jobName));

      SocketClient sc = SocketClient.getSocket(printer);
      sc.invokePjlCommands(commands);

      info.put("stats", snmp.getPrinterStats());

      Integer id = ((UnsolicitedJobType)job.getJobUStatus().getUStatusType()).getId();
      if (id != null)
      {
         TreeMap<String, Object> ns = snmp.getPrinterJobInfo(id);
         assert ns.get("job-info-state") == JobInfoState.Cancelled : "snmp job state was not canceled";
      }
      assert canceled[0] : "didn't receive a canceled event";
      assert !foundOutput[0] : "there was output, and we should have canceled it before the output came.";

      snmp.disconnect();
   }

   @Test
   // this just looks at jobs already on the printer
   // ToDo: create a job to check :)
   public static void testSnmpJobInfo() throws Exception
   {

      PrinterSnmpUtil snmp = new PrinterSnmpUtil(printer);
      snmp.connect();
//      TreeMap<String, Object> results = snmp.getPrinterJobInfo(1221);
      TreeMap<String, Object> results = snmp.getPrinterJobInfo(1203);

      for (int x = 1; x < 5; x++)
      {
         if (results.get("jobinfo-attr-" + x) != null)
         {
            System.out.println(results.get("jobinfo-attr-" + x));
         }
      }

      assert results.get("jobname") != null : "jobname is null";
      System.out.println("job name: " + results.get("jobname"));
      assert results.get("pages-processed") != null : "pages-processed is null";
      System.out.println("pages processed: " + (results.get("pages-processed")));
      assert results.get("pages-printed") != null : "pages-printed is null";
      System.out.println("pages printed: " + (results.get("pages-printed")));
      assert results.get("job-size") != null : "job-size is null";
      System.out.println("pages job-size: " + (results.get("job-size")));
      assert results.get("job-info-state") != null : "job-info-state is null";
      System.out.println("job state: " + ((JobInfoState)results.get("job-info-state")).name());
      System.out.println("job state description: " + ((JobInfoState)results.get("job-info-state")).getDescription());
      assert results.get("job-info-outcome") != null : "job-info-outcome is null";
      System.out.println("job outcome: " + ((JobInfoOutcome)results.get("job-info-outcome")).name());
   }

   /**
    * This test tests a postscript job which has output, and whether the output is properly harvested.
    *
    * @throws Exception duh
    */
   @Test
   public static void testPostScriptQuery() throws Exception
   {

      String jobName = "PJLTestJob-" + System.currentTimeMillis() + Math.random();
      final boolean[] foundOutput = new boolean[]{false};

      ArrayList<Command> commands = new ArrayList<Command>();

      commands.add(new Comment(" Testing 1 2 3 "));

      UnsolicitedStatus
            printerStatus =
            new UnsolicitedStatus(new UnsolicitedDeviceType(UnsolicitedDeviceType.InputValue.ON));
      printerStatus.addInputListener(new InputEventListener()
      {
         public void inputEventOccurred(InputEvent event)
         {
            if (event.getSource() instanceof UnsolicitedDeviceType)
            {
               UnsolicitedDeviceType usd = (UnsolicitedDeviceType)event.getSource();
               System.err.printf("Code: %s Online: %s Display: %s%n",
                     usd.getCode(), usd.getOnline(), usd.getDisplay());
            }
         }
      });
      commands.add(printerStatus);

      Job j = new Job(jobName, "Troy's Test Job");
      j.getPageUStatus().addInputListener(new InputEventListener()
      {
         public void inputEventOccurred(InputEvent event)
         {
            if (event.getSource() instanceof UnsolicitedPageType)
            {
               System.err.println("Printed page " + ((UnsolicitedPageType)event.getSource()).getPage());
            }
         }
      });
      j.getJobUStatus().addInputListener(new InputEventListener()
      {
         public void inputEventOccurred(InputEvent event)
         {
            if (event.getSource() instanceof UnsolicitedJobType)
            {
               System.err.println(event.getSource().toString());
            }
         }
      });
      commands.add(j);
      commands.add(new EnterLanguage(LanguagePersonality.POSTSCRIPT));

      String rawStart = "StartPostScriptPrintJob";
      Raw raw = new Raw(
            "%!PS" + CRLF
                  + "(" + rawStart + "\\r\\n) print" + CRLF
                  + "(Hello World\r\n\r\n) print" + CRLF
            , rawStart, EOT_OR_EOUELp, true);
      raw.addInputListener(new InputEventListener()
      {
         public void inputEventOccurred(InputEvent event)
         {
            if (event.getSource() instanceof Raw)
            {
               System.err.println(((Raw)event.getSource()).getOutput());
               if (((Raw)event.getSource()).getOutput().indexOf("Hello World") > -1)
               {
                  foundOutput[0] = true;
               }
            }
         }
      });
      commands.add(raw);
      commands.add(new EndOfJob(jobName));

      SocketClient sc;
      sc = SocketClient.getSocket(printer);
      String output = sc.invokePjlCommands(commands);
      if (output.length() > 0)
      {
         System.err.println("OUTPUT: " + output);
      }

      assert foundOutput[0] : "I couldn't find the output from the PostScript job I was waiting for.";
   }

   /*
    * This test tests actually sending a postscript print job.
    * It will print something, that's why it's disabled.
    */
   @Test
   public static void testPostScriptPrintJob() throws Exception
   {
      String jobName = "PJLJob-" + System.currentTimeMillis() + Math.random();

      ArrayList<Command> commands = new ArrayList<Command>();

      commands.add(new Comment(" Testing 1 2 3 "));

      InputEventListener unsolicitedStatusListener = new InputEventListener()
      {
         public void inputEventOccurred(InputEvent event)
         {
            if (event.getSource() instanceof UnsolicitedStatusType)
            {
               UnsolicitedStatusType usd = (UnsolicitedStatusType)event.getSource();
               System.err.printf(usd.toString() + "\n");
            }
         }
      };

//      UnsolicitedStatus timedPrinterStatus = new UnsolicitedStatus(new UnsolicitedTimedType(5));
//      timedPrinterStatus.addInputListener(unsolicitedStatusListener);
//      commands.add(timedPrinterStatus);

      UnsolicitedStatus printerStatus = new UnsolicitedStatus(new UnsolicitedDeviceType(UnsolicitedDeviceType.InputValue.ON));
      printerStatus.addInputListener(unsolicitedStatusListener);
      commands.add(printerStatus);

      Job j = new Job(jobName, "Troy's Test Job");
      j.getPageUStatus().addInputListener(new InputEventListener()
      {
         public void inputEventOccurred(InputEvent event)
         {
            if (event.getSource() instanceof UnsolicitedPageType)
            {
               System.err.println("Printed page " + ((UnsolicitedPageType)event.getSource()).getPage());
            }
         }
      });
      j.getJobUStatus().addInputListener(new InputEventListener()
      {
         public void inputEventOccurred(InputEvent event)
         {
            if (event.getSource() instanceof UnsolicitedJobType)
            {
               System.err.println(event.getSource().toString());
            }
         }
      });
      commands.add(j);
      commands.add(new EnterLanguage(LanguagePersonality.POSTSCRIPT));
      Raw raw = new Raw(
            "%!PS" + CRLF
                  + "5 setlinewidth" + CRLF
                  + "100 100 moveto" + CRLF
                  + "0 300 rlineto" + CRLF
                  + "300 0 rlineto" + CRLF
                  + "0 -300 rlineto" + CRLF
                  + "closepath" + CRLF
                  + "stroke" + CRLF
                  + "185 240 moveto" + CRLF
                  + "/Helvetica findfont 20 scalefont setfont" + CRLF
                  + "(PostScript Job) show" + CRLF
                  + "showpage" + CRLF);

      commands.add(raw);
      commands.add(new EndOfJob(jobName));

      SocketClient sc;
      sc = SocketClient.getSocket(printer);
      String output = sc.invokePjlCommands(commands);
      if (output.length() > 0)
      {
         System.err.println("OUTPUT: " + output);
      }

      // one page should have been printed
      assert ((UnsolicitedPageType)j.getPageUStatus().getUStatusType()).getPage()
            == 1 : "Pages printed were not 1";

      // job should be in finished state
      assert ((UnsolicitedJobType)j.getJobUStatus().getUStatusType()).getStatus()
            == UnsolicitedJobType.Status.FINISHED : "Job was not finished";
   }

   /*
    * This test tests variable retrieval and modification.
    */
   @Test
   public static void testInfoQuery() throws Exception
   {
      String jobName = "TestInfoJob-" + System.currentTimeMillis() + Math.random();
      final String PSV = "ECONOMODE";
      final String on = "ON";
      final String off = "OFF";
      final String target;

      ArrayList<Command> commands = new ArrayList<Command>();
      commands.add(new Comment(" Testing 1 2 3 "));

      UnsolicitedStatus printerStatus = new UnsolicitedStatus(
            new UnsolicitedDeviceType(UnsolicitedDeviceType.InputValue.ON));

      printerStatus.addInputListener(new InputEventListener() {
         public void inputEventOccurred(InputEvent event) {
            if (event.getSource() instanceof UnsolicitedDeviceType) {
               UnsolicitedDeviceType usd = (UnsolicitedDeviceType)event.getSource();
               System.err.printf("Code: %s Online: %s Display: %s%n",
                     usd.getCode(), usd.getOnline(), usd.getDisplay());
            }
         }
      });
      commands.add(printerStatus);

      Job j = new Job(jobName, "Troy's Test Job");
      j.getPageUStatus().addInputListener(new InputEventListener()
      {
         public void inputEventOccurred(InputEvent event)
         {
            if (event.getSource() instanceof UnsolicitedPageType)
            {
               System.err.println("Printed page " + ((UnsolicitedPageType)event.getSource()).getPage());
            }
         }
      });
      j.getJobUStatus().addInputListener(new InputEventListener() {
         public void inputEventOccurred(InputEvent event) {
            if (event.getSource() instanceof UnsolicitedJobType) {
               System.err.println(event.getSource().toString());
            }
         }
      });
      commands.add(j);
      Info info = new Info(VariableCategory.VARIABLES);
      info.addInputListener(new InputEventListener()
      {
         public void inputEventOccurred(InputEvent event)
         {
            if (event.getSource() instanceof Variable)
            {
               System.err.println("Variable changed/added: " + event.getSource());
            }
            if (event.getSource() instanceof Info)
            {
               System.err.println("Info object changed: " + event.getSource());
            }
         }
      });
      commands.add(info);
      Info infoConfig = new Info(VariableCategory.CONFIG);
      infoConfig.addInputListener(new InputEventListener() {
         public void inputEventOccurred(InputEvent event) {
            if (event.getSource() instanceof Variable) {
               System.err.println("config variable " + event.getSource());
            }
         }
      });
      commands.add(infoConfig);
      commands.add(new EndOfJob(jobName));

      SocketClient sc = SocketClient.getSocket(printer);
      String output = sc.invokePjlCommands(commands);
      if (output.length() > 0)
      {
         System.err.println("OUTPUT: " + output);
      }

      // set the target to the opposite of what it already is.
      target = info.getVariable(PSV).getValue().equals(on) ? off : on;

      assert infoConfig.getVariable("MEMORY").getValue().matches("^\\d+$")
            : "Didn't get a number for the config variable MEMORY";

      Variable newVariable = info.getVariable(PSV).copy();
      newVariable.setValue(target);
      jobName = "TestInfoJob-" + System.currentTimeMillis() + Math.random();
      Vector<Command> commands2 = new Vector<Command>();
      Job j2 = new Job(jobName);
      commands2.add(j2);
      commands2.add(new Set(newVariable));
      Inquire inquire = new Inquire(info.getVariable(PSV));
      commands2.add(inquire);
      commands2.add(new EndOfJob(jobName));

      inquire.addInputListener(new InputEventListener()
      {
         public void inputEventOccurred(InputEvent event)
         {
            System.err.println("Changed: " + ((Inquire)event.getSource()).getVariable().toString());
         }
      });

      sc = SocketClient.getSocket(printer);
      output = sc.invokePjlCommands(commands2);
      if (output.length() > 0)
      {
         System.err.println("OUTPUT: " + output);
      }

      // verify ptsize is a number
      assert inquire.getVariable().getValue().matches("^(OFF|ON)$")
            : PSV + " of " + info.getVariable(PSV).getValue() + " isn't expected";
      // verify that the one set through the event == the target value
      assert inquire.getVariable().getValue().equals(target)
            : "event variable doesn't match the target variable";
   }
}

