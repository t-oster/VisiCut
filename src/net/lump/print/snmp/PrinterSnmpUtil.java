package net.lump.print.snmp;

import java.io.IOException;
import java.net.*;
import java.util.HashMap;
import java.util.TreeMap;

import net.lump.lib.util.Interval;
import snmp.*;

import static net.lump.print.snmp.PrinterSnmpUtil.Command.*;


/**
 * Utility class for printer SNMP queries.
 *
 * @author M. Troy Bowman
 */
public class PrinterSnmpUtil
{

   private InetAddress printer;
   private String community;
   private SNMPv1CommunicationInterface comInterface;

   public static final int DEFAULT_VERSION = 0; //SNMPv1
   public static final String DEFAULT_COMMUNITY = "public";

   public PrinterSnmpUtil(String printerHostName, String community) throws UnknownHostException
   {
      this(InetAddress.getByName(printerHostName), community);
   }

   public PrinterSnmpUtil(String printerHostName) throws UnknownHostException
   {
      this(InetAddress.getByName(printerHostName));
   }

   public PrinterSnmpUtil(InetAddress printerAddress)
   {
      this(printerAddress, DEFAULT_COMMUNITY);
   }

   public PrinterSnmpUtil(InetAddress printerAddress, String community)
   {
      printer = printerAddress;
      this.community = community;
   }

   public SNMPv1CommunicationInterface connect() throws SocketException
   {
      if (comInterface == null)
      {
         comInterface = new SNMPv1CommunicationInterface(DEFAULT_VERSION, printer, this.community);
         comInterface.setSocketTimeout(5000); // five second timeout
      }
      return comInterface;
   }

   public void disconnect() throws SocketException
   {
      comInterface.closeConnection();
      comInterface = null;
   }

   public TreeMap<String, Object> getPrinterStats()
      throws IOException, SNMPGetException, SNMPBadValueException
   {
      TreeMap<String, String> query = new TreeMap<String, String>();
      query.put("on-off-line", OnOffLine.OID);
      query.put("uptime", "1.3.6.1.2.1.1.3.0");
      query.put("location", "1.3.6.1.2.1.1.6.0");
      query.put("model", "1.3.6.1.2.1.25.3.2.1.3.1");
      query.put("serial", "1.3.6.1.2.1.43.5.1.1.17.1");
      query.put("pjl", "1.3.6.1.2.1.43.15.1.1.3.1.2");
      query.put("pcl", "1.3.6.1.2.1.43.15.1.1.3.1.3");
      query.put("postscript", "1.3.6.1.2.1.43.15.1.1.3.1.5");
      query.put("active-jobid", "1.3.6.1.4.1.11.2.3.9.4.2.1.1.6.2.1.1.0");
      query.put("display-status", "1.3.6.1.4.1.11.2.3.9.1.1.3.0");
      query.put("status", "1.3.6.1.4.1.11.2.4.3.1.2.0");
//      query.put("display-lines", "1.3.6.1.2.1.43.16.5.1.2.1.x");

      TreeMap<String, Object> output = queryFor(query);
      output.put("on-off-line",
                 output.get("on-off-line") == null
                 ? OnOffLine.Unknown
                 : OnOffLine.fromId(Integer.parseInt((String)output.get("on-off-line"))));

      output.put("alert-table", getAlertTable());
      return output;
   }

   public TreeMap<String, Object> getPrinterJobInfo(int job) throws IOException, SNMPBadValueException, SNMPGetException
   {
      TreeMap<String, String> query = new TreeMap<String, String>();

      query.put("jobname", "1.3.6.1.4.1.11.2.3.9.4.2.1.1.6.5.1." + job + ".0");
      query.put("pages-processed", "1.3.6.1.4.1.11.2.3.9.4.2.1.1.6.5.12." + job + ".0");
      query.put("pages-printed", "1.3.6.1.4.1.11.2.3.9.4.2.1.1.6.5.13." + job + ".0");
      query.put("job-size", "1.3.6.1.4.1.11.2.3.9.4.2.1.1.6.5.14." + job + ".0");
      query.put("job-info-state", JobInfoState.OID + "." + job + ".0");
      query.put("job-info-outcome", JobInfoOutcome.OID + "." + job + ".0");
      for (Integer x : new Integer[]{1, 2, 3, 4, 5})
      {
         query.put("jobinfo-attr-" + x, "1.3.6.1.4.1.11.2.3.9.4.2.1.1.6.5.23." + x + "." + job + ".0");
      }

      TreeMap<String, Object> output = queryFor(query);

//      (String)output.get("job-info-state");

      if (output.get("jobname") != null)
      {
         output.put("jobname", ((String)output.get("jobname")).replace("\u0001", "").replace("\u0015", ""));
      }

      if (output.get("job-info-state") != null)
      {
         output.put("job-info-state", JobInfoState.fromId(Integer.parseInt((String)output.get("job-info-state"))));
      }
      if (output.get("job-info-outcome") != null)
      {
         output.put("job-info-outcome", JobInfoOutcome.fromId(Integer.parseInt((String)output.get("job-info-outcome"))));
      }
      return output;
   }

   public TreeMap<String, Object> getPrinterJobInfoAttr(int job) throws IOException, SNMPBadValueException, SNMPGetException
   {
      TreeMap<String, String> query = new TreeMap<String, String>();
      for (Integer x : new Integer[]{1, 2, 3, 4, 5})
      {
         query.put("jobinfo-1", "1.3.6.1.4.1.11.2.3.9.4.2.1.1.6.5.23." + x + "." + job + ".0");
      }
      return queryFor(query);
   }

   public TreeMap<String, Object> queryFor(TreeMap<String, String> query)
      throws IOException, SNMPGetException, SNMPBadValueException
   {
      TreeMap<String, Object> results = new TreeMap<String, Object>();
      String[] mibs = query.values().toArray(new String[query.size()]);
      String[] keys = query.keySet().toArray(new String[query.size()]);

      for (int x = 0; x < keys.length; x++)
      {
         SNMPVarBindList newVars;
         try
         {
            newVars = getComInterface().getMIBEntry(mibs[x]);
            SNMPSequence pair = (SNMPSequence)(newVars.getSNMPObjectAt(0));
            if (pair.getSNMPObjectAt(1) == null)
            {
               getComInterface().getNextMIBEntry(mibs[x]);
               pair = (SNMPSequence)(newVars.getSNMPObjectAt(0));
            }
            String value = pair.getSNMPObjectAt(1).toString();
            results.put(keys[x], value);
         }
         catch (SNMPGetException ignore)
         {
            results.put(keys[x], null);
         }
      }

      return results;
   }

   enum AlertName
   {
      bogus,
      alert_index,
      severity_level,
      training_level,
      group,
      group_index,
      location,
      code,
      description,
      time;

      public static AlertName fromId(int value) {
         if (value < values().length && value > -1) return values()[value];
         else return bogus;
      }
   }

   public TreeMap<Integer, HashMap<String, Object>> getAlertTable()
      throws IOException, SNMPGetException, SNMPBadValueException
   {
      TreeMap<Integer, HashMap<String, Object>> output = new TreeMap<Integer, HashMap<String, Object>>();
      HashMap<String, Object> row;

      String alertOid = "1.3.6.1.2.1.43.18.1.1";
      String oid = alertOid;
      while (true)
      {
         SNMPVarBindList vars = getComInterface().getNextMIBEntry(oid);

         SNMPObjectIdentifier id = (SNMPObjectIdentifier)((SNMPSequence)(vars.getSNMPObjectAt(0))).getSNMPObjectAt(0);
         // stop if we're out of scope
         if (!id.toString().startsWith(alertOid))
         {
            break;
         }

         // set the current oid to the one found
         oid = id.toString();

         // the last part of the oid, which is changing oids
         String oidInfo[] = id.toString().substring(alertOid.length() + 1).split("\\.");

         Integer rowId = Integer.parseInt(oidInfo[2]);
         if (output.get(rowId) == null)
         {
            output.put(rowId, new HashMap<String, Object>());
         }
         row = output.get(rowId);

         AlertName name = AlertName.fromId(Integer.parseInt(oidInfo[0]));

         Object value = ((SNMPSequence)(vars.getSNMPObjectAt(0))).getSNMPObjectAt(1);

         switch (name)
         {
            case severity_level:
               value = AlertSeverityLevel.fromId(Integer.valueOf(value.toString()));
               break;
            case training_level:
               value = AlertTrainingLevel.fromId(Integer.valueOf(value.toString()));
               break;
            case group:
               value = AlertGroup.fromId(Integer.valueOf(value.toString()));
               break;
            case code:
               value = AlertCode.fromId(Integer.valueOf(value.toString()));
               break;
            default: // (group_index location alert_index description time)
               value = value.toString();
               break;
         }

         row.put(name.name(), value);
      }
      return output;
   }

   public boolean cancelJob(Integer jobIdNumber) throws IOException
   {
      SNMPInteger jobId = new SNMPInteger(jobIdNumber);
      return write("1.3.6.1.4.1.11.2.3.9.4.2.1.1.6.1.2.0", jobId);
   }

   private void queryAndCancelCurrentJob()
   {
      TreeMap<String, Object> stats = null;
      try
      {
         stats = getPrinterStats();

         // if this is the first run and the active jobid is null, give the message and return
         if (stats.get("active-jobid") == null)
         {
            System.out.println("Could not find a current job ID.");
            System.out.println("Either there is no job, or the printer doesn't support querying the current job.");
         }
         else {
            Integer jobId = Integer.parseInt((String)stats.get("active-jobid"));
            if (jobId.equals(-1)) {
               System.out.println("There is no current job to cancel.");
            }
            else {
               cancelJob(jobId);
               System.out.println("Requested that job ID " + jobId + " be cancelled succesfully.");
            }
         }
      }
      catch (SNMPGetException e)
      {
         System.out.println("The snmp get command failed.");
      }
      catch (SNMPBadValueException e)
      {
         System.out.println("The snmp get command returned a bad value.");
      }
      catch (IOException e)
      {
         System.out.println("Could not talk to printer " + printer.getCanonicalHostName() + ".");
      }
   }

   public boolean resetPrinter(InetAddress printer) throws IOException {
      return write("1.3.6.1.2.1.43.5.1.1.3.1", new SNMPInteger(4));
   }

   public boolean write(String oid, SNMPObject object) throws IOException
   {
      connect();

      try
      {
         getComInterface().setMIBEntry(oid, object);
         return true;
      }
      catch (SNMPException e)
      {
         return false;
      }
      catch (SNMPBadValueException e)
      {
         return false;
      }
   }

   public InetAddress getPrinter()
   {
      return printer;
   }

   public void setPrinter(InetAddress printer)
   {
      this.printer = printer;
   }

   public String getCommunity()
   {
      return community;
   }

   public void setCommunity(String community)
   {
      this.community = community;
   }

   public SNMPv1CommunicationInterface getComInterface() throws SocketException
   {
      return connect();
   }


   enum Command { help, query, cancel_current_job, reset }

   /**
    * This allows us to call this class directly to find out about printer stuff.
    *
    * @param args arguments
    */
   public static void main(String[] args)
   {
      InetAddress address = null;
      Command command = query;

      for (String arg : args) {
         if (arg.startsWith("-")) {
            if (arg.equals("-q")) command = query;
            if (arg.equals("-h")) command = help;
            if (arg.equals("-r")) command = reset;
            if (arg.equals("-c")) command = cancel_current_job;
         }
         else {
            try
            {
               address = Inet4Address.getByName(arg);
            }
            catch (UnknownHostException ignore) {}
         }
      }

      if (address == null) {
         System.err.println("Please supply a valid printer hostname as an argument.");
         System.exit(1);
      }

      switch (command) {
         case help:
            System.out.println("usage: PrinterSnmpUtil [-h|-r|-c] soslpX");
            System.out.println("\t-h - this help");
            System.out.println("\t-q - [default] query the printer for vital info");
            System.out.println("\t-r - reset this printer through snmp");
            System.out.println("\t-c - cancel the current running job");
            System.out.println("\t the last one of these commands in the argument list will be executed");
            break;
         case query:
            new PrinterSnmpUtil(address).printStats();
            break;
         case cancel_current_job:
            new PrinterSnmpUtil(address).queryAndCancelCurrentJob();
            break;
         case reset:
            try {
               if (new PrinterSnmpUtil(address).resetPrinter(address)) {
                  System.out.println("successfully sent snmp reset to " + address.getCanonicalHostName());
               }
               else {
                  System.out.println("couldn't complete reset command on " + address.getCanonicalHostName());
               }
            }
            catch (IOException ioe) {
               System.out.println("couldn't talk to address " + address.getCanonicalHostName());
            }
            break;
         default:
            System.err.println("command not implemented.");
      }
   }

   private void printStats() {
      try
      {
         TreeMap<String, Object> stats = getPrinterStats();

         System.out.printf("%15s: %s%n", "Printer", printer.getCanonicalHostName());
         System.out.printf("%15s: %s%n", "Location", stats.get("location"));
         System.out.printf("%15s: %s%n", "Uptime", Interval.span(Integer.parseInt((String)stats.get("uptime")) * 10));
         System.out.printf("%15s: %s%n", "Model", stats.get("model"));
         System.out.printf("%15s: %s%n", "PCL", stats.get("pcl"));
         System.out.printf("%15s: %s%n", "PJL", stats.get("pjl"));
         System.out.printf("%15s: %s%n", "PostScript", stats.get("postscript"));
         System.out.printf("%15s: %s%n", "Serial Number", stats.get("serial"));
         System.out.printf("%15s: %s%n", "Online", ((OnOffLine)stats.get("on-off-line")).name());
         System.out.printf("%15s: %s%n", "Status", stats.get("status"));
         System.out.printf("%15s: %s%n", "Display Status", stats.get("display-status"));

         TreeMap<String, Object> jobInfo;
         if (stats.get("active-jobid") != null && !stats.get("active-jobid").equals("-1"))
         {
            Integer jobId = Integer.parseInt((String)stats.get("active-jobid"));
            jobInfo = getPrinterJobInfo(jobId);
            System.out.printf("%15s: %s%n", "Current Job", stats.get("active-jobid"));

            for (String key : jobInfo.keySet())
            {
               if (jobInfo.get(key) != null)
               {
                  String out = "";
                  if (jobInfo.get(key) instanceof JobInfoOutcome)
                  {
                     out = ((JobInfoOutcome)jobInfo.get(key)).name();
                  }
                  else if (jobInfo.get(key) instanceof JobInfoState)
                  {
                     out = ((JobInfoState)jobInfo.get(key)).name();
                  }
                  else
                  {
                     out = jobInfo.get(key).toString();
                  }
                  System.out.printf("%20s: %s%n", key, out);
               }
            }
         }

         // if device status isn't null, lets fabricate a device event, just to make sure the client gets it.
         if ((stats.get("alert-table") != null && ((TreeMap)stats.get("alert-table")).size() > 0))
         {
            System.out.printf("%15s:%n", "Alert Table");
            @SuppressWarnings({"unchecked"})
            TreeMap<Integer, HashMap<String, Object>> table =
               (TreeMap<Integer, HashMap<String, Object>>)stats.get("alert-table");

            for (Integer key : table.keySet())
            {
               HashMap row = table.get(key);

               String description = (String)row.get("description");
               String location = (String)row.get("location");

               System.out.printf("%20s  %s%n%20s: %s%n%20s: %s%n%20s: %s%n%20s: %s%n%20s: %s%n",
                                 "----------------", "----------------",
                                 "Description",
                                 description,
                                 "Code",
                                 location,
                                 "Severity",
                                 ((AlertSeverityLevel)row.get("severity_level")).name(),
                                 "Alert Code",
                                 ((AlertCode)row.get("code")).name(),
                                 "Training Level",
                                 ((AlertTrainingLevel)row.get("training_level")).name());
            }
         }
      }
      catch (UnknownHostException e)
      {
         System.err.println("First argument is host name of printer. " + e.getMessage());
         System.exit(1);
      }
      catch (IOException e)
      {
         System.err.println("Can't talk to printer: " + e.getMessage());
      }
      catch (SNMPBadValueException e)
      {
         e.printStackTrace();
      }
      catch (SNMPGetException e)
      {
         e.printStackTrace();
      }
   }
}