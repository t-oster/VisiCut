package net.lump.print.jetdirect.pjl.commands;

import static net.lump.print.PrintConstants.*;
import net.lump.print.jetdirect.pjl.enums.CommandNames;
import net.lump.print.jetdirect.pjl.enums.LanguagePersonality;

/**
 * The ENTER command enables the specified language (such as
 * PCL or PostScript). Use this command to select the printer language
 * for printing subsequent data.
 *
 * @author M. Troy Bowman
 */
public class EnterLanguage extends Command
{

   private LanguagePersonality language;

   /**
    * The ENTER command must be positioned immediately before any
    * personality-specific data. The selected personality begins parsing
    * immediately after the <LF> that terminates the ENTER command.
    *
    * @param language The personality variable must be set to PCL,
    *                 POSTSCRIPT, ESCP, or one of the other supported
    *                 personalities. Personalities besides PCL and PostScript can
    *                 be added to some printers by plugging in additional hardware,
    *                 such as cartridges or ROM SIMMs. If your application handles
    *                 status readback, you can request a list of all valid personalities
    *                 present in the printer using the INFO command.
    */
   public EnterLanguage(LanguagePersonality language)
   {
      super(CommandNames.ENTER);
      this.language = language;
   }

   public String toString()
   {
      return String.format("%s %s LANGUAGE=%s%s", PJL, command, language, CRLF);
   }
}
