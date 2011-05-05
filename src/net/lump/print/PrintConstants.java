package net.lump.print;

import java.io.*;
import java.util.regex.Pattern;


/**
 * This class holds static constants that all print classes may need.
 *
 * @author M. Troy Bowman
 */
public class PrintConstants
{

   /**
    * standard jetdirect port.
    */
   public static final int JETDIRECT_PORT = 9100;

   /**
    * printers' default charset.
    */
   public static final String CHARSET = "US-ASCII";

   /**
    * an defacto maximum read for buffered input.
    */
   public static final int MAX_READ = 5242880;

   /*************************************************************************/
   // known special characters
   /**
    * escape.
    */
   public static final char ESC = 0x1b;
   /**
    * end of transmission (EOF).
    */
   public static final char EOT = 0x04;
   /**
    * carriage return.
    */
   public static final String CR = "\r";
   /**
    * linefeed/newline.
    */
   public static final String LF = "\n";
   /**
    * form feed.
    */
   public static final String FF = "\f";
   /**
    * carriage return, line feed.
    */
   public static final String CRLF = CR + LF;
   /**
    * PostScript begin string.
    */
   public static final String PS = "%!PS";

   /*************************************************************************/
   // pjl strings
   /**
    * the pjl string.
    */
   public static final String PJL = "@PJL";
   private static final String UNESCAPED_UEL = "%-12345X";
   /**
    * the uel, gets the printer's attention to be able issue pjl commands.
    */
   public static final String UEL = ESC + UNESCAPED_UEL;
   /**
    * a full uel and enter into pjl.
    */
   public static final String ENTER_PJL = UEL + PJL + CRLF;
   /**
    * exit uel.
    */
   public static final String UEL_EXIT = UEL + CRLF;

   /*************************************************************************/
   // special character patterns
   /**
    * end of line pattern.
    */
   public static final Pattern EOLp = Pattern.compile("(?:\\r\\n|\\r|\\n)");
   /**
    * end of line or form feed pattern.
    */
   public static final Pattern EOL_OR_FFp = Pattern.compile("(?:\\r\\n|\\r|\\n|\\f)");
   /**
    * form feed pattern.
    */
   public static final Pattern FFp = Pattern.compile("\\f");
   /**
    * end of transmission (EOF) pattern.
    */
   public static final Pattern EOTp = Pattern.compile("\\x04");
   /**
    * escape from enter pattern.
    */
   public static final Pattern EOUELp = Pattern.compile("\\e" + UNESCAPED_UEL);
   /**
    * end of transmission or escape from enter pattern.
    */
   public static final Pattern EOT_OR_EOUELp = Pattern.compile("(?:\\e" + UNESCAPED_UEL + "|\\x04)");



   /**
    * Deep-copy an object.
    *
    * @param obj to be copied.
    * @return a deep-copied object.
    */
   @SuppressWarnings({"unchecked"})
   public static <T> T deepcopy(T obj)
   {
      final ByteArrayOutputStream baos = new ByteArrayOutputStream();
      T out = null;
      try
      {
         (new ObjectOutputStream(baos)).writeObject(obj);
         out =
             (T)(new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()))).readObject();
      }
      catch (IOException e)
      {
         System.err.println("unexpected I/O exception during deep copy");
         // I don't think we'll have an IO exception with this, ever.
      }
      catch (ClassNotFoundException e)
      {
         System.err.println("unexpected ClassNotFoundException during deep copy");
         // Considering this is using generics, the object passed in is an instance
         // of an already defined class, so I don't see how this could get thrown.
      }

      // return copied object or null.
      return out;
   }

}
