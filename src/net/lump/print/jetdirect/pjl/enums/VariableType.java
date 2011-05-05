package net.lump.print.jetdirect.pjl.enums;

import java.io.Serializable;


/**
 * Types of variables.
 *
 * @author M. Troy Bowman
 */
public enum VariableType implements Serializable
{
   ENUMERATED,
   RANGE,
   STRING,
   TABLE;

   public static String join(char character)
   {
      String variables = "";
      for (VariableType c : values())
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
