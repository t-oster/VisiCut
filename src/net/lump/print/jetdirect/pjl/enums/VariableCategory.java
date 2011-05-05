package net.lump.print.jetdirect.pjl.enums;

import java.io.Serializable;

/**
 * A variable category.
 *
 * @author M. Troy Bowman
 */
@SuppressWarnings({"UnusedDeclaration"})
public enum VariableCategory implements Serializable
{
   /**
    * Provides the printer model number, such as ?LaserJet 4.?
    */
   ID,
   /**
    * Provides configuration information, such as how
    * many and which paper sizes are available in this
    * printer.
    */
   CONFIG,
   /**
    * Returns PJL file system information.
    */
   FILESYS,
   /**
    * Identifies the amount of memory available
    */
   MEMORY,
   /**
    * Returns the number of pages printed by the print engine.
    */
   STATUS,
   /**
    * Lists environmental and printer language-dependent variables,
    * the possible variable values, and the current variable settings.
    */
   VARIABLES,
   /**
    * Lists the unsolicited status variables provided by the printer,
    * the possible variable values, and the current variable settings.
    */
   USTATUS;

   public static String join(char character)
   {
      String variables = "";
      for (VariableCategory c : values())
      {
         if (variables.length() > 0)
         {
            variables += character;
         }
         variables += c.toString();
      }
      return variables;
   }
}