package net.lump.print.snmp;


/**
 * The code that describes the type of alert for this entry in
 * the table.  There are different codes for each
 * sub-unit type: for example, Media Supply Low and Media
 * Supply Empty are Aler codes for the Input sub-unit.
 *
 * @author M. Troy Bowman
 */
@SuppressWarnings({"UnusedDeclaration"})
public enum AlertCode
{

   other(1),
   /**
    * an event that is not represented
    * by one of the alert codes
    * specified below. *
    */
   unknown(2),
   /**
    * The following generic codes are common to
    * multiple groups.  The NMS may examine the
    * prtAlertGroup object to determine what group
    * to query for further information.*
    */
   coverOpen(3),
   coverClosed(4),
   interlockOpen(5),
   interlockClosed(6),
   configurationChange(7),
   jam(8),
   /**
    * The subunit tray, bin, etc. has been removed.
    */
   subunitMissing(9),
   subunitLifeAlmostOver(10),
   subunitLifeOver(11),
   subunitAlmostEmpty(12),
   subunitEmpty(13),
   subunitAlmostFull(14),
   subunitFull(15),
   subunitNearLimit(16),
   subunitAtLimit(17),
   subunitOpened(18),
   subunitClosed(19),
   subunitTurnedOn(20),
   subunitTurnedOff(21),
   subunitOffline(22),
   subunitPowerSaver(23),
   subunitWarmingUp(24),
   subunitAdded(25),
   subunitRemoved(26),
   subunitResourceAdded(27),
   subunitResourceRemoved(28),
   subunitRecoverableFailure(29),
   subunitUnrecoverableFailure(30),

   subunitRecoverableStorageError(31),

   subunitUnrecoverableStorageError(32),

   subunitMotorFailure(33),
   subunitMemoryExhausted(34),
   subunitUnderTemperature(35),
   subunitOverTemperature(36),
   subunitTimingFailure(37),
   subunitThermistorFailure(38),

   // General Printer group
   /**
    * @deprecated use {@link #AlertCode#coverOpened}
    */
   doorOpen(501), // DEPRECATED
   /**
    * @deprecated use {@link AlertCode#coverClosed}
    */
   doorClosed(502),
   powerUp(503),
   powerDown(504),
   /**
    * The printer has been reset by some
    * network management station(NMS)
    * writing into 'prtGeneralReset'.
    */
   printerNMSReset(505),
   /**
    * The printer has been reset manually.
    */
   printerManualReset(506),
   /**
    * The printer is ready to print. (i.e.,
    * not warming up, not in power save
    * state, not adjusting print quality,
    * etc.).
    */
   printerReadyToPrint(507),

   // Input Group
   inputMediaTrayMissing(801),
   inputMediaSizeChange(802),
   inputMediaWeightChange(803),
   inputMediaTypeChange(804),
   inputMediaColorChange(805),
   inputMediaFormPartsChange(806),
   inputMediaSupplyLow(807),
   inputMediaSupplyEmpty(808),
   /**
    * An interpreter has detected that a
    * different medium is need in this input
    * tray subunit.  The prtAlertDescription may
    * be used to convey a human readable
    * description of the medium required to
    * satisfy the request.
    */
   inputMediaChangeRequest(809),
   /**
    * An interpreter has detected that manual
    * input is required in this subunit.  The
    * prtAlertDescription may be used to convey
    * a human readable description of the medium
    * required to satisfy the request.
    */
   inputManualInputRequest(810),
   /**
    * The input tray failed to position correctly.
    */
   inputTrayPositionFailure(811),
   inputTrayElevationFailure(812),
   inputCannotFeedSizeSelected(813),
   // Output Group
   outputMediaTrayMissing(901),
   outputMediaTrayAlmostFull(902),
   outputMediaTrayFull(903),
   outputMailboxSelectFailure(904),
   // Marker group
   markerFuserUnderTemperature(1001),
   markerFuserOverTemperature(1002),
   markerFuserTimingFailure(1003),
   markerFuserThermistorFailure(1004),
   markerAdjustingPrintQuality(1005),
   // Marker Supplies group
   markerTonerEmpty(1101),
   markerInkEmpty(1102),
   markerPrintRibbonEmpty(1103),
   markerTonerAlmostEmpty(1104),
   markerInkAlmostEmpty(1105),
   markerPrintRibbonAlmostEmpty(1106),
   markerWasteTonerReceptacleAlmostFull(1107),
   markerWasteInkReceptacleAlmostFull(1108),
   markerWasteTonerReceptacleFull(1109),
   markerWasteInkReceptacleFull(1110),
   markerOpcLifeAlmostOver(1111),
   markerOpcLifeOver(1112),
   markerDeveloperAlmostEmpty(1113),
   markerDeveloperEmpty(1114),
   markerTonerCartridgeMissing(1115),
   // Media Path Device Group
   mediaPathMediaTrayMissing(1301),
   mediaPathMediaTrayAlmostFull(1302),
   mediaPathMediaTrayFull(1303),
   mediaPathCannotDuplexMediaSelected(1304),

   // Interpreter Group
   interpreterMemoryIncrease(1501),
   interpreterMemoryDecrease(1502),
   interpreterCartridgeAdded(1503),
   interpreterCartridgeDeleted(1504),
   interpreterResourceAdded(1505),
   interpreterResourceDeleted(1506),
   interpreterResourceUnavailable(1507),
   /**
    * The interpreter has encountered a page
    * that is too complex for the resources that
    * are available.
    */
   interpreterComplexPageEncountered(1509),

   // Alert Group
   /**
    * A binary change event entry has been
    * removed from the alert table.  This unary
    * change alert table entry is added to the
    * end of the alert table.
    */
   alertRemovalOfBinaryChangeEntry(1801);

   int id;

   private AlertCode(int id)
   {
      this.id = id;
   }

   public int getId()
   {
      return id;
   }

   public static AlertCode fromId(int id) {
      AlertCode retval = unknown;
      for (AlertCode ac : values())
      {
         if (ac.getId() == id)
         {
            retval = ac;
            break;
         }
      }
      return retval;
   }
}
