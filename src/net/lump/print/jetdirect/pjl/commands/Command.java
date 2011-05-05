package net.lump.print.jetdirect.pjl.commands;

import static net.lump.print.PrintConstants.*;
import net.lump.print.jetdirect.pjl.enums.CommandNames;

import java.io.*;

/**
 * An abstract command, which all commands must subclass.
 *
 * @author M. Troy Bowman
 */
public abstract class Command
{

   CommandNames command;

   Command()
   {
   }

   public Command(CommandNames command)
   {
      this.command = command;
   }

   public String toString()
   {
      return String.format("%s %s%s", PJL, command, CRLF);
   }

}
