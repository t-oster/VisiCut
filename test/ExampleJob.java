import net.lump.print.jetdirect.SocketClient;
import net.lump.print.jetdirect.pjl.commands.*;
import net.lump.print.jetdirect.pjl.enums.LanguagePersonality;
import net.lump.print.jetdirect.pjl.events.InputEvent;
import net.lump.print.jetdirect.pjl.events.InputEventListener;
import net.lump.print.jetdirect.pjl.ustatustypes.*;

import static net.lump.print.PrintConstants.*;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

/**
 * This is an example of using this library to talk to a printer.
 */
public class ExampleJob {

   public static void main(String[] args) {

       args = new String[]{"localhost"};
      // check for our expected hostname argument
      if (args.length != 1) {
         System.err.println("please provide a printer hostname or ip address as an argument");
         System.exit(1);
      }

      // resolve the hostname for safety
      InetAddress address = null;
      try
      {
         address = Inet4Address.getByName(args[0]);
      }
      catch (UnknownHostException e)
      {
         System.err.println("please use a printer that exists");
         System.exit(1);
      }

      // this is a job name we will use
      String jobName = "TestJob-" + System.currentTimeMillis() + Math.random();
      // this will hold the PJL commands we will issue
      ArrayList<Command> commands = new ArrayList<Command>();

      // this is a comment PJL command
      commands.add(new Comment(" Testing 1 2 3 "));

      // create a PJL command that uses our listener above for Device events
      UnsolicitedStatus printerStatus = new UnsolicitedStatus(new UnsolicitedDeviceType(UnsolicitedDeviceType.InputValue.ON));
      commands.add(printerStatus);

      // this listener will listen for unsolicited events and print them to stderr
      // you could use these events to notify your application of printer status events
      printerStatus.addInputListener(new InputEventListener() {
         public void inputEventOccurred(InputEvent event)
         {
            if (event.getSource() instanceof UnsolicitedStatusType)
            {
               UnsolicitedStatusType usd = (UnsolicitedStatusType)event.getSource();
               System.err.printf("Device Event: " + usd.toString() + "\n");
            }
         }
      });

      // if you want a printer status at a certain interval, use this.
      // i've used this before as a keepalive to tell my application that the printer is still there
      UnsolicitedStatus timedPrinterStatus = new UnsolicitedStatus(new UnsolicitedTimedType(5));
      commands.add(timedPrinterStatus);
      timedPrinterStatus.addInputListener(new InputEventListener() {
         public void inputEventOccurred(InputEvent event)
         {
            if (event.getSource() instanceof UnsolicitedStatusType)
            {
               UnsolicitedStatusType usd = (UnsolicitedStatusType)event.getSource();
               System.err.printf("Timed Event: " + usd.toString() + "\n");
            }
         }
      });


      // create a PJL job that will display "Troy's Test Job" in the printer's display
      Job j = new Job(jobName, "Troy's Test Job");

      // add a page listener to our PJL job
      j.getPageUStatus().addInputListener(new InputEventListener()
      {
         public void inputEventOccurred(InputEvent event)
         {
            if (event.getSource() instanceof UnsolicitedPageType)
            {
               System.err.println("Page Event: Printed page " + ((UnsolicitedPageType)event.getSource()).getPage());
            }
         }
      });

      // add a job listener to our PJL job
      j.getJobUStatus().addInputListener(new InputEventListener()
      {
         public void inputEventOccurred(InputEvent event)
         {
            if (event.getSource() instanceof UnsolicitedJobType)
            {
               System.err.println("Job Event: " + event.getSource().toString());
            }
         }
      });

      // add the job to our list
      commands.add(j);

      // wrap our Raw command (below) in a Postscript PJL personality
      commands.add(new EnterLanguage(LanguagePersonality.POSTSCRIPT));

      // this is a Raw command, it basically will BLORT out the string you provide to the socket
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

      // add the raw to our list
      commands.add(raw);
      // add an EOJ PJL command
      commands.add(new EndOfJob(jobName));


      // our commands are ready to go now.  Let's
      String output = null;
      try {
         SocketClient sc = SocketClient.getSocket(address);
         output = sc.invokePjlCommands(commands);

         // if there was any unexpected output from our job, it's probably a bad thing, as we weren't expecting it
         // the thing that usually comes from this is postscript errors
         if (output.length() > 0)
         {
            System.err.println("OUTPUT: " + output);
         }
      } catch (IOException e) {
         System.err.println("IO Exception with printer " + address.getCanonicalHostName());
         System.err.println(e.getMessage());
         System.exit(1);
      }

   }
}
