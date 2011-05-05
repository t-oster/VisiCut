package net.lump.print.jetdirect.pjl.enums;

import java.io.Serializable;

@SuppressWarnings({"UnusedDeclaration"})
public enum LanguagePersonality implements Serializable
{
   PCL, POSTSCRIPT, PCLXL, ESCP;

   public static String join(char character)
   {
      String variables = "";
      for (LanguagePersonality c : values())
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