package net.lump.print.snmp;

public enum JobInfoState
{
   Unknown0,
   Unknown1,
   Unknown2,
   Aborted("The print job was aborted."), //(3),
   WaitingForResources("The print job requires resources that are not currently available.  Example resources that can "
                       + "cause the job to wait include the print engine or PDL processor being unavailable.  The "
                       + "print engine could be unavailable due to paper out, paper jam, staple out, stapler jam, "
                       + "marking agent low, output bin full, etc.  The PDL processor could be unavailable due to "
                       + "an off-line condition.  Each printer specific object specification should state which "
                       + "conditions cause a job to be waiting for resources and also state which objects can be "
                       + "retrieved by an application to determine the exact cause of a resource being unavailable."),//4
   Printed("The job has printed.  The related JOB-INFO-OUTCOME object indicates if any problems were encountered while "
           + "the job was processed."), //(5),
   Retained("The job can be printed"), //(6),
   Terminating("The job was aborted or cancelled and is currently is terminating."), //(7),
   Interrupted("The job has been interrupted.  The job can be continued."), // (8),
   Paused("The job has been paused.  The job can be continuted."), // (9)
   Cancelled("The job has been cancelled."), //(10),
   Processing("The job is currently being printed."), //(11);
   ;

   private String description;

   private JobInfoState()
   {
   }

   private JobInfoState(String description)
   {
      this.description = description;
   }

   public String getDescription()
   {
      return description;
   }

   public static String OID = "1.3.6.1.4.1.11.2.3.9.4.2.1.1.6.5.15"; // + jobid + ".0"

   public static JobInfoState fromId(int value) {
      if (value < values().length && value > -1) return values()[value];
      else return Unknown0;
   }
}
