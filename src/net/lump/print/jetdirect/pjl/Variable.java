package net.lump.print.jetdirect.pjl;

import net.lump.print.jetdirect.pjl.commands.Set;
import net.lump.print.jetdirect.pjl.enums.LanguagePersonality;
import net.lump.print.jetdirect.pjl.enums.VariableType;
import java.io.*;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A PJL variable.
 *
 * @author M. Troy Bowman
 */
public class Variable implements Serializable
{
   private String name;
   private boolean readOnly = false;
   private LanguagePersonality lParm;
   private String value;
   private Vector<String> possibilities;
   private VariableType type;

   public Variable()
   {
   }

   public Variable(String name)
   {
      this(name, null);
   }

   public Variable(String name, String value)
   {
      this.name = name;
      this.value = value;
   }

   public String getName()
   {
      return name;
   }

   public void setName(String name)
   {
      this.name = name;
   }

   public boolean readOnly()
   {
      return readOnly;
   }

   public void setReadOnly(boolean readonly)
   {
      this.readOnly = readonly;
   }

   public LanguagePersonality getLParm()
   {
      return lParm;
   }

   public void setLParm(LanguagePersonality lparm)
   {
      this.lParm = lparm;
   }

   public String getValue()
   {
      return value;
   }

   /**
    * Check whether a value is valid, providing a boolean instead of an exception on its validity.
    *
    * @param value a string which defines the value.
    * @return boolean
    * @throws IllegalStateException if the variable hasn't been properly initialized with the INFO command.
    */
   public boolean isValidValue(String value)
   {
      boolean valid = true;
      try
      {
         checkValue(value);
      }
      catch (IllegalArgumentException e)
      {
         valid = false;
      }
      return valid;
   }

   /**
    * Check whether a value is a valid value for this variable.
    *
    * @param value a string which defines the value.
    * @throws IllegalArgumentException if the value is invalid.
    * @throws IllegalStateException    if the variable hasn't been initialized with the INFO command.
    */
   public <T extends Comparable> void checkValue(String value)
   {
      String illegalState = "You must initialize variables with the INFO command";

      switch (type)
      {
         case ENUMERATED:
            if (possibilities.isEmpty())
            {
               throw new IllegalStateException(illegalState);
            }
            if (!possibilities.contains(value))
            {
               throw new IllegalArgumentException(
                  String.format("Variable %s does not contain %s in its enumeration", name, value));
            }
            break;
         case RANGE:
            if (possibilities.isEmpty())
            {
               throw new IllegalStateException(illegalState);
            }

            boolean isFloat = false;
            boolean isNumber = false;
            Pattern p = Pattern.compile("^\\-?\\d+(\\.\\d+)?$");

            for (String s : possibilities)
            {
               Matcher m = p.matcher(s);
               if (m.matches())
               {
                  isNumber = true;
                  if (m.group(1) != null)
                  {
                     isFloat = true;
                  }
               }
            }

            Comparable start;
            Comparable end;
            Comparable cValue;

            if (isFloat)
            {
               if (Double.parseDouble(possibilities.get(0)) < Double.parseDouble(possibilities.get(1)))
               {
                  start = Double.parseDouble(possibilities.get(0));
                  end = Double.parseDouble(possibilities.get(1));
               }
               else
               {
                  start = Double.parseDouble(possibilities.get(1));
                  end = Double.parseDouble(possibilities.get(0));
               }
               cValue = Double.parseDouble(value);
            }
            else if (isNumber)
            {
               if (Long.parseLong(possibilities.get(0)) < Long.parseLong(possibilities.get(1)))
               {
                  start = Long.parseLong(possibilities.get(0));
                  end = Long.parseLong(possibilities.get(1));
               }
               else
               {
                  start = Long.parseLong(possibilities.get(1));
                  end = Long.parseLong(possibilities.get(0));
               }
               cValue = Long.parseLong(value);
            }
            else
            {
               if (possibilities.get(0).compareTo(possibilities.get(1)) < 0)
               {
                  start = possibilities.get(0);
                  end = possibilities.get(1);
               }
               else
               {
                  start = possibilities.get(1);
                  end = possibilities.get(0);
               }
               cValue = value;
            }
            // ugh, can't I do polymorphism here?
            // noinspection unchecked
            if ((cValue.compareTo(start) < 0) || (cValue.compareTo(end) > 0))
            {
               throw new IllegalArgumentException(
                  String.format("Variable %s is not within range %s and %s", name, start, end));
            }

            break;
         case STRING:
            // nothing to this.
            break;
         case TABLE:
            throw new IllegalStateException("You can't change table values.");
      }
   }

   public void setValue(String value)
   {
      if (readOnly)
      {
         throw new IllegalStateException(name + " is read only");
      }
      checkValue(value);
      this.value = value;
   }

   public Vector<String> getPossibilities()
   {
      return possibilities;
   }

   public void setPossibilities(Vector<String> possibilities)
   {
      this.possibilities = possibilities;
   }

   public VariableType getType()
   {
      return type;
   }

   public void setType(VariableType type)
   {
      this.type = type;
   }

   @Override
   public boolean equals(Object o)
   {
      if (this == o)
      {
         return true;
      }
      if (o == null || getClass() != o.getClass())
      {
         return false;
      }

      Variable variable = (Variable)o;

      if (readOnly != variable.readOnly)
      {
         return false;
      }
      if (lParm != variable.lParm)
      {
         return false;
      }
      if (name != null ? !name.equals(variable.name) : variable.name != null)
      {
         return false;
      }
      if (possibilities != null ? !possibilities.equals(variable.possibilities) : variable.possibilities != null)
      {
         return false;
      }
      if (type != variable.type)
      {
         return false;
      }
      if (value != null ? !value.equals(variable.value) : variable.value != null)
      {
         return false;
      }

      return true;
   }

   @Override
   public int hashCode()
   {
      int result = name != null ? name.hashCode() : 0;
      result = 31 * result + (readOnly ? 1 : 0);
      result = 31 * result + (lParm != null ? lParm.hashCode() : 0);
      result = 31 * result + (value != null ? value.hashCode() : 0);
      result = 31 * result + (possibilities != null ? possibilities.hashCode() : 0);
      result = 31 * result + (type != null ? type.hashCode() : 0);
      return result;
   }

   @Override
   public String toString()
   {
      return "Variable{" +
             "name='" + name + '\'' +
             ", value='" + value + '\'' +
             ", lParm=" + lParm +
             ", type=" + type +
             ", readOnly=" + readOnly +
             ", possibilities=" + possibilities +
             '}';
   }

   /**
    * Returns a Set command for this variable.
    *
    * @return Set
    */
   public Set getSetCommand()
   {
      return new Set(this);
   }

   public Variable copy()
   {
      final ByteArrayOutputStream baos = new ByteArrayOutputStream();
      Variable out = null;
      try
      {
         (new ObjectOutputStream(baos)).writeObject(this);
         out = (Variable)(new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()))).readObject();
      }
      catch (IOException e)
      {
         // I don't think we'll have an IO exception with this, ever.
      }
      catch (ClassNotFoundException e)
      {
         // We already know about this class, silly.
      }
      return out;
   }
}