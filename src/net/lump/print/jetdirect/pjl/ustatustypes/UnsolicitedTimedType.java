package net.lump.print.jetdirect.pjl.ustatustypes;

import java.io.Serializable;

/**
 * A TIMED unsolicited status type..
 *
 * @author M. Troy Bowman
 */
public class UnsolicitedTimedType extends UnsolicitedStatusType implements Serializable
{

   private Integer inputInterval = 0;

   private UnsolicitedTimedType()
   {
   }

   /**
    * Timed variable.
    *
    * @param inputInterval must be a number 0, or from 5 to 300.
    */
   public UnsolicitedTimedType(int inputInterval)
   {
      inputVariable = "TIMED";
      setInputValue(inputInterval);
   }

   public String getInputValueString()
   {
      return inputInterval.toString();
   }

   public Integer getInputValue()
   {
      return inputInterval;
   }

   public void setInputValue(Integer inputInterval)
   {
      if (inputInterval != 0 && (inputInterval < 5 || inputInterval > 300))
      {
         throw new IllegalArgumentException("interval must be a number from 5 thru 300");
      }
      this.inputInterval = inputInterval;
   }

   public void setToOff()
   {
      this.inputInterval = 0;
   }
}


