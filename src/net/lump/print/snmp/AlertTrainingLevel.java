package net.lump.print.snmp;


/**
 * These are training levels required for printer alerts.
 *
 * @author M. Troy Bowman
 */
@SuppressWarnings({"UnusedDeclaration"})
public enum AlertTrainingLevel
{
   reserved0,
   Other,
   Unknown,
   /**
    * Alerts that can be fixed without prior
    * training either because the action to correct
    * the alert is obvious or the printer can help the
    * untrained person fix the problem. A typical
    * example of such an alert is reloading paper
    * trays and emptying output bins on a low end
    * printer.
    */
   Untrained,
   /**
    * Alerts that require an intermediate or moderate
    * level of knowledge of the printer and its
    * sub-units. A typical examples of alerts that
    * a trained operator can handle is replacing
    * toner cartridges.
    */
   Trained,
   /**
    * Alerts that typically require advanced
    * training and technical knowledge of the printer
    * and its sub-units. An example of a technical
    * person would be a manufacture's Field Service
    * representative, or other person formally
    * trained by the manufacturer or similar
    * representative.
    */
   FieldService,
   /**
    * Alerts that have to do with overall
    * operation of and configuration of the printer.
    * Examples of management events are configuration
    * change of sub-units
    */
   Management,
   NoInterventionRequired;

   public static AlertTrainingLevel fromId(int value) {
      if (value < values().length && value > -1) return values()[value];
      else return Unknown;
   }
}
