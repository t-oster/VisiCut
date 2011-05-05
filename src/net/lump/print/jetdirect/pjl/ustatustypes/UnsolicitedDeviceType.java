package net.lump.print.jetdirect.pjl.ustatustypes;

import java.io.Serializable;

/**
 * A DEVICE Unsolicited Status type.
 * This status type gives notifications of device status changes.
 *
 * @author M. Troy Bowman
 */
public class UnsolicitedDeviceType extends UnsolicitedStatusType implements Serializable
{
   public enum InputValue
   {
      ON, VERBOSE, OFF
   }

   private InputValue inputValue;

   private UnsolicitedDeviceType()
   {
   }

   public UnsolicitedDeviceType(InputValue inputValue)
   {
      inputVariable = "DEVICE";
      this.inputValue = inputValue;
   }

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

