package net.lump.print.snmp;

@SuppressWarnings({"UnusedDeclaration"})
public enum OnOffLine
{
   Unknown,
   Online,
   Offline,
   OfflineAtEndOfJob;
   public static String OID = "1.3.6.1.4.1.11.2.3.9.4.2.1.1.2.5.0";

   public static OnOffLine fromId(int value) {
      if (value < values().length && value > -1) return values()[value];
      else return Unknown;
   }

}
