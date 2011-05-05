package net.lump.print.snmp;

public enum JobInfoOutcome
{
   Unknown0,
   Unknown1,
   Unknown2,
   Ok,
   WanringsEncountered,
   ErrorsEncountered;
   public static String OID = "1.3.6.1.4.1.11.2.3.9.4.2.1.1.6.5.19"; // + jobid + ".0"

   public static JobInfoOutcome fromId(int value) {
      if (value < values().length && value > -1) return values()[value];
      else return Unknown0;
   }
}
