package net.lump.print.snmp;


/**
 * The type of subunit within the printer model that this alert
 * is related.  Input, output, and markers are examples of
 * printer model groups, i.e., examples of types of subunits.
 * Wherever possible, the enumerations match the sub-identifier
 * that identifies the relevant table in the Printer MIB.
 *
 * @author M. Troy Bowman
 */
@SuppressWarnings({"UnusedDeclaration"})
public enum AlertGroup
{
   none,
   other,
   reserved2,// (2) is reserved for conformance information
   // Values for Host Resources MIB (3,4)
   hostResourcesMIBStorageTable,
   hostResourcesMIBDeviceTable,
   // Values for Printer MIB (5-18)
   generalPrinter,
   cover,
   localization,
   input,
   output,
   marker,
   markerSupplies,
   markerColorant,
   mediaPath,
   channel,
   interpreter,
   consoleDisplayBuffer,
   consoleLights,
   alert,
   // Values (19) to (29) reserved for Printer MIB
   reserved19,
   reserved20,
   reserved21,
   reserved22,
   reserved23,
   reserved24,
   reserved25,
   reserved26,
   reserved27,
   reserved28,
   reserved29,
   // Values for Finisher MIB (30-33)
   finDevice,
   finSupply,
   finSupplyMediaInput,
   finAttribute,
   // Values (30) to (39) reserved for Finisher MIB
   reserved34,
   reserved35,
   reserved36,
   reserved37,
   reserved38,
   reserved39,
   ;

   public static AlertGroup fromId(int value) {
      if (value < values().length && value > -1) return values()[value];
      else return none;
   }
}
