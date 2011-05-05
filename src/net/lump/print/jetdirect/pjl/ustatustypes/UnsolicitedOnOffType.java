package net.lump.print.jetdirect.pjl.ustatustypes;

import java.io.Serializable;

/**
 * A model for an Unsolicited Status type which only input values of ON and OFF.
 *
 * @author M. Troy Bowman
 */
public abstract class UnsolicitedOnOffType extends UnsolicitedType implements Serializable
{
   public enum InputValue
   {
      ON, OFF
   }

   InputValue inputValue;

   public String getInputValueString()
   {
      return inputValue.toString();
   }

   public InputValue getInputValue()
   {
      return inputValue;
   }

   public void setInputValue(InputValue inputValue)
   {
      this.inputValue = inputValue;
   }

   public void setToOff()
   {
      this.inputValue = InputValue.OFF;
   }
}

