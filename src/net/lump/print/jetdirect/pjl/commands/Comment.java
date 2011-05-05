package net.lump.print.jetdirect.pjl.commands;

import static net.lump.print.PrintConstants.*;
import net.lump.print.jetdirect.pjl.enums.CommandNames;
import net.lump.print.jetdirect.pjl.commands.Command;

/**
 * The COMMENT command enables you to add a line of information as
 * a comment. Use this command whenever you want to add an
 * explanation to PJL commands
 *
 * @author M. Troy Bowman
 */
public class Comment extends Command
{
   private String remarks;

   /**
    * An empty comment.
    */
   public Comment()
   {
      super(CommandNames.COMMENT);
   }

   /**
    * Constructor.
    *
    * @param remarks Roman-8 character codes 33
    *                through 255 and <WS>, starting
    *                with a printable character.
    */
   public Comment(String remarks)
   {
      super(CommandNames.COMMENT);
      this.remarks = remarks;
   }

   public String toString()
   {
      return String.format("%s %s %s%s", PJL, command, remarks, CRLF);
   }
}
