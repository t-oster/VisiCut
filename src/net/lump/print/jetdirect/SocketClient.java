package net.lump.print.jetdirect;

import static net.lump.print.PrintConstants.deepcopy;
import net.lump.print.PrintConstants;
import net.lump.print.jetdirect.pjl.commands.*;
import net.lump.print.jetdirect.pjl.events.InputEvent;
import net.lump.print.jetdirect.pjl.ustatustypes.UnsolicitedJobType;
import net.lump.print.jetdirect.pjl.ustatustypes.UnsolicitedPageType;
import net.lump.print.jetdirect.pjl.ustatustypes.UnsolicitedTimedType;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class manages JetDirect sockets.
 *
 * @author M. Troy Bowman
 */
public class SocketClient
{

   private Socket socket;
   private static final int MINUTE = 60000;

   private SocketClient()
   {
      socket = null;
   }

   /**
    * Get a socket from the socket pool for the provided hostname.
    * JETDIRECT_PORT will be used for the port.
    *
    * @param hostname the string which contains the hostname
    * @return SocketClient
    * @throws IOException if things go awry
    */
   public static SocketClient getSocket(String hostname) throws IOException
   {
      return getSocket(InetAddress.getByName(hostname));
   }

   /**
    * Get a socket from the socket pool for the provided address.
    *
    * @param address the address to connect to
    * @return SocketClient
    * @throws IOException if things go awry
    */
   // all socketPool edits happen in here...
   public static SocketClient getSocket(InetAddress address) throws IOException
   {
      SocketClient socketClient = new SocketClient();
      socketClient.connect(new InetSocketAddress(address, PrintConstants.JETDIRECT_PORT));
      return socketClient;
   }

   private synchronized void connect(InetSocketAddress printer) throws IOException
   {
      socket = new Socket(printer.getAddress(), printer.getPort());
      socket.setKeepAlive(false);
      socket.setSoLinger(false, 0);
      socket.setSoTimeout(15 * MINUTE);
   }

   /**
    * Marks the input stream, and starts reading a head to find the pattern.  When the pattern is matched,
    * a string from the mark to the end of the pattern is returned.  If timeout time has been reached, null is returned.
    *
    * @param bis     the bufferedInputStream
    * @param search  the pattern to search for
    * @param timeout how long to wait for output
    * @return String
    * @throws IOException if the InputStream has a problem.
    */
   public String readUpTo(BufferedInputStream bis, Pattern search, long timeout) throws IOException
   {
      bis.mark(PrintConstants.MAX_READ);

      StringBuffer s = new StringBuffer();
      Matcher matcher;

      do
      {
         // poll for availability on the socket.
         long startTime = System.currentTimeMillis();
         int available;
         while (true)
         {

            // find out the bytes available
            available = bis.available();

            // return null if nothing and timeout
            if (available == 0 && (System.currentTimeMillis() - timeout) > startTime)
            {
               return null;
            }

            // if nothing is available, wait a bit.
            if (available == 0)
            {
               try
               {
                  Thread.sleep(100);
               }
               catch (InterruptedException ignore)
               {
                  // ok, thanks, we're interrupted.
               }
            }
            else
            {
               break;
            }
         }

         // make a buffer the size of available
         byte[] buffer = new byte[available];

         // fill the buffer with available
         int read = bis.read(buffer, 0, available);

         // if we read -1 bytes, we've reached EOF.
         if (read == -1)
         {
            throw new EOFException("EOF Reached");
         }

         s.append(new String(buffer, PrintConstants.CHARSET).substring(0, read));

         matcher = search.matcher(s);
      } while (!matcher.find(0));

      bis.reset();
      // this skip should always skip, because we've already buffered the amount we want to skip to.
      //noinspection ResultOfMethodCallIgnored
      bis.skip(matcher.end());
      bis.mark(PrintConstants.MAX_READ);

      return s.subSequence(0, matcher.start()).toString();
   }

   public String invokePjlCommand(Command command) throws IOException
   {
      return invokePjlCommands(Arrays.asList(command));
   }

   /**
    * Ask the printer to echo something back, read up to that echo and return true if successful.
    *
    * @param bis       the bufferedinputstream to read from
    * @param pw        the printwriter to write to
    * @param sessionId the session id to use to read to
    * @param timeout   the time to wait.
    * @return whether it was successful
    * @throws IOException if we couldn't read/write or any other IO problems.
    */
   private boolean syncStream(BufferedInputStream bis, PrintWriter pw, String sessionId, long timeout) throws IOException {
      return syncStream(bis, pw, sessionId, timeout, null);
   }

   /**
    * Ask the printer to echo something back, read up to that echo and return true if successful.
    *
    * @param bis       the bufferedinputstream to read from
    * @param pw        the printwriter to write to
    * @param sessionId the session id to use to read to
    * @param timeout   the time to wait.
    * @param job       if this is not null, snmp queries will be made and waiting events will be issued
    * @return whether it was successful
    * @throws IOException if we couldn't read/write or any other IO problems.
    */
   private boolean syncStream(BufferedInputStream bis, PrintWriter pw, String sessionId, long timeout, Job job)
      throws IOException
   {
      // Start PJL
      pw.append(PrintConstants.ENTER_PJL);

      // make sure we're synced with the printer
      Echo echo = new Echo(sessionId);
      pw.append(echo.toString());
      pw.flush();

      long startTime = System.currentTimeMillis();
      int loops = 0;

      // read until we get our echo so we can be synced, unless timeout is reached.
      while ((System.currentTimeMillis() - timeout) < startTime)
      {
         // wait up to 5 seconds for a line
         String line = readUpTo(bis, PrintConstants.EOL_OR_FFp, MINUTE/12);

         // if we read a line and it equals our echo, we're synced.
         if (line != null && line.equals(echo.getInputTriggerLine()))
         {
            return true;
         }

         // if we were provided a job, we're not in the first loop, and the job is in a waiting state, fire a waiting event.
         if (job != null && loops > 0 &&
             ((UnsolicitedJobType)job.getJobUStatus().getUStatusType()).getStatus() == UnsolicitedJobType.Status.WAITING) {
            job.getJobUStatus().fireEvent(new InputEvent(deepcopy(job.getJobUStatus().getUStatusType())));
         }

         loops++;
      }

      return false;
   }

   @SuppressWarnings({"ConstantConditions"})
   public synchronized String invokePjlCommands(List<Command> commandList)
      throws IOException
   {
      Vector<Command> commands = new Vector<Command>(commandList);

      String returnValue = "";

      if (commands.size() == 0)
      {
         throw new IllegalArgumentException("list must contain one or more commands");
      }

      Job job = null;
      int jobCommands = 0;
      boolean foundTimedUStatus = false;

      for (Command command : commands)
      {
         if (command instanceof Job)
         {
            jobCommands++;
            // set job command variable for convenience
            job = (Job)command;
         }
         // check for any unsolicited timed types
         if (command instanceof UnsolicitedStatus
             && ((UnsolicitedStatus)command).getUStatusType() instanceof UnsolicitedTimedType)
         {
            foundTimedUStatus = true;
         }
      }
      if (jobCommands > 1)
      {
         throw new IllegalArgumentException(
            "there can be only one Job command in the list of commands per invocation");
      }

      // if there was no timed status in the command, lets prepend it.
      if (!foundTimedUStatus) {
         commands.insertElementAt(new UnsolicitedStatus(new UnsolicitedTimedType(5)), 0);
      }

      try
      {
         String sessionId = String.format("Session %s-%s", System.currentTimeMillis(), Math.random());

         // get the output stream and open a print writer on it.
         final OutputStream os = socket.getOutputStream();
         final PrintWriter pw = new PrintWriter(new OutputStreamWriter(os, PrintConstants.CHARSET));

         // make a buffered input stream
         final InputStream rawInputStream = socket.getInputStream();
         final BufferedInputStream bis = new BufferedInputStream(rawInputStream);

         // wait for up to 15 minutes to sync up.
         // the printer may not be ours yet (someone else may be using it) so we have to wait.
         if (!syncStream(bis, pw, sessionId, 15 * MINUTE, job))
         {
            // we couldn't sync up.
            throw new IOException("failed to sync with printer");
         }

         Vector<Input> inputs = new Vector<Input>();

         // append all the commands
         for (Command command : commands)
         {
            // if this command has unsolicited stati
            if (command instanceof HasUnsolicitedStati)
            {
               Vector<UnsolicitedStatus> unsolicitedStati
                  = ((HasUnsolicitedStati)command).getUStati();

               // add all the unsolicited stati to the ones we're watching for.
               inputs.addAll(unsolicitedStati);

               // append all the unsolicited status commands to the stream
               for (UnsolicitedStatus unsolicitedStatus : unsolicitedStati)
               {
                  pw.append(unsolicitedStatus.toString());
               }

               // flush the unsolicited stati commands down the pipe.
               pw.flush();
            }
            else if (command instanceof Input)
            {
               // if it's just an input command, we don't have to issue the command
               // just add it to the list we're watching for.
               inputs.add((Input)command);
            }

            // if this is a raw command (e.g., PCL, PostScript)...
            if (command instanceof Raw)
            {
               pw.append(command.toString());
               pw.append(PrintConstants.UEL_EXIT);  // exit from raw
               pw.append(PrintConstants.ENTER_PJL); // reenter pjl for subsequent commands
            }
            // this isn't raw, just append the command.
            else
            {
               pw.append(command.toString());
            }

            pw.flush();
         }

         // if we have any stati to watch...
         if (inputs.size() > 0)
         {

            // the time we're willing to wait before checking if we're still alive
            long timeout = MINUTE / 2;
            boolean errorState = false;

            // loop until breakout
            while (true)
            {
               String line;

               // loop in trying to read a line until an error or until timed out
               while (true)
               {
                  // read the next line.
                  line = readUpTo(bis, PrintConstants.EOL_OR_FFp, timeout);

                  // if the line isn't null, we're golden.
                  if (line != null)
                  {
                     break;
                  }
                  // if the line is null, the read timed out.  lets try to ask if the printer is still there.
                  else
                  {
                     // the printer is ours right now, so our timeout shouldn't have to be too long
                     if (!syncStream(bis, pw, sessionId, timeout))
                     // we couldn't sync up.
                     {
                        throw new IOException("failed to sync with printer");
                     }
                  }
               }

               // if the line is empty, ignore.
               if (line.matches("^\\s*$"))
               {
                  continue;
               }

               boolean foundTrigger = false;
               for (Input input : inputs)
               {
                  // if the input doesn't have an input trigger line, and end pattern, skip it.
                  if (input.getInputTriggerLine() == null)
                  {
                     continue;
                  }

                  // check to see if the line matches.
                  if (line.matches("^(?:" + PrintConstants.UEL + ")?" + input.getInputTriggerLine()))
                  {
                     // it matched, read to the end pattern and feed it to the parser.
                     // the printer is ours right now, so our timeout shouldn't have to be too long
                     String body = readUpTo(bis, input.getInputEndPattern(), timeout);
                     if (body == null)
                     {
                        // the printer is ours right now, so our timeout shouldn't have to be too long
                        if (!syncStream(bis, pw, sessionId, timeout))
                        // we couldn't sync up.
                        {
                           throw new IOException("failed to sync with printer");
                        }
                        // we can't continue this line of thought, the syncstream flushed it.
                        continue;
                     }

                     input.readInput(body);

                     if (input instanceof Raw && ((Raw)input).isErrorState())
                     {
                        errorState = true;
                     }

                     // we found what we were looking for
                     foundTrigger = true;
                     break;
                  }
               }

               // if we just received and in_progress, set the jobID for pages, so we can reliably identify them
               // we also want to start our finished timer.
               if (job != null && job.getStatus() == UnsolicitedJobType.Status.IN_PROGRESS)
               {
                  ((UnsolicitedPageType)job.getPageUStatus().getUStatusType()).setJobId(job.getJobId());
               }

               // if we didn't find what we were expecting, add it to the output
               if (!foundTrigger)
               {
                  returnValue += line + PrintConstants.CRLF;
               }

               // start allDone as true -- anything we found that isn't done will nullify it.
               boolean allDone = true;

               // if the job isn't finished, we're not done yet.
               if (job != null && job.getStatus() != UnsolicitedJobType.Status.FINISHED)
               {
                  allDone = false;
               }

               // if job is finished but we haven't had output that require input, we're not done yet.
               else
               {
                  for (Input input : inputs)
                  {
                     // if it's canceled or finished, lets' nuke any output requireds
                     if (job != null
                         && (job.getStatus() == UnsolicitedJobType.Status.CANCELED
                             || job.getStatus() == UnsolicitedJobType.Status.FINISHED)
                         && (!(input instanceof UnsolicitedStatus)
                             && input.isOutputRequired() && !input.hasHadOutput()))
                     {
                        input.abortOutputRequired();
                     }

                     // if we still have things we're waiting for, keep waiting.
                     if (input.isOutputRequired() && !input.hasHadOutput())
                     {
                        allDone = false;
                     }
                  }
               }

               // if the job is finished stop watching.
               if (errorState || allDone)
               {
                  break;
               }
            }
         }

         // if we're here, we're done looking for inputs.
         // if we still have uStati, lets turn 'em all off.
         if (inputs.size() > 0)
         {
            pw.append(PrintConstants.UEL);
            for (Input input : inputs)
            {
               if (input instanceof UnsolicitedStatus)
               {
                  UnsolicitedStatus us = (UnsolicitedStatus)input;
                  us.setToOff();
                  pw.append(us.toString());
                  pw.flush();
               }
            }
         }

         // tell the printer we're done with pjl before leaving socket for possible reuse
         pw.append(PrintConstants.UEL_EXIT);
         pw.flush();

         // try to flush any garbage left on the socket
         int available;
         while ((available = rawInputStream.available()) > 0)
         {
            byte[] buffer = new byte[available];
            //noinspection ResultOfMethodCallIgnored
            rawInputStream.read(buffer, 0, available);
         }
      }
      finally
      {
         // close the socket up
         socket.close();
         socket = null;
      }

      return returnValue;
   }
}
