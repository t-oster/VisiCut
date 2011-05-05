package net.lump.print.jetdirect.pjl.commands;

import static net.lump.print.PrintConstants.*;
import static net.lump.print.PrintConstants.deepcopy;

import net.lump.print.jetdirect.pjl.enums.CommandNames;
import net.lump.print.jetdirect.pjl.events.InputEvent;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A raw command simply outputs the exact string it was created with..
 *
 * @author M. Troy Bowman
 */
public class Raw extends InputCommand
{
   String content;
   String eventTriggerLine;
   Pattern inputEndPattern;
   String output = "";
   boolean errorState = false;

   private Raw()
   {
   }

   public Raw(String content)
   {
      this(content, null, EOT_OR_EOUELp, false);
   }

   public Raw(String content, String triggerLine)
   {
      this(content, triggerLine, EOT_OR_EOUELp, false);
   }

   public Raw(String content, String triggerLine, boolean outputRequired)
   {
      this(content, triggerLine, EOT_OR_EOUELp, outputRequired);
   }

   public Raw(String content, String triggerLine, Pattern endPattern, boolean outputRequired)
   {
      super(CommandNames.RAW);
      this.content = content;
      setInputTriggerLine(triggerLine, endPattern);
      setOutputRequired(outputRequired);
   }

   public String toString()
   {
      return content;
   }

   public Raw prependContent(String prepend)
   {
      this.content = prepend + this.content;
      return this;
   }

   public Raw appendContent(String append)
   {
      this.content += append;
      return this;
   }

   /**
    * Since PostScript can output data, in order to know that postScript has begun output, we'll need
    * to have PostScript output something we can identify as the start of the output.  We also need to know
    * how far to go until the end of the output is reached.  Since both of these are interdependent, they
    * must be set at the same time.
    *
    * @param triggerLine the line to watch for to begin getting input from the raw commands.
    * @param endPattern  the pattern to read up to when we find the trigger line.
    */
   public void setInputTriggerLine(String triggerLine, Pattern endPattern)
   {
      this.eventTriggerLine = triggerLine;
      this.inputEndPattern = endPattern;
   }

   public String getInputTriggerLine()
   {
      return this.eventTriggerLine;
   }

   public String getOutput()
   {
      return output;
   }

   /**
    * Return whether this command has errored.
    *
    * @return boolean
    */
   public boolean isErrorState()
   {
      return errorState;
   }

   public void readInput(String input) throws IOException
   {
      // check for postscript errors and set error state.
      Matcher m = java.util.regex.Pattern.compile("^%%\\[\\s*(Error: .*?)\\s*\\]%%").matcher(input);
      if (m.find() && m.group(1) != null)
      {
         errorState = true;
      }

      // append to our output
      output += input;

      // fire input event
      fireEvent(new InputEvent(deepcopy(this)));

      // we've officially had output now.
      hasHadOutput = true;
   }

   public Pattern getInputEndPattern()
   {
      return inputEndPattern;
   }
}
