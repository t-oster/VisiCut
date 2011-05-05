package net.lump.print.jetdirect.pjl.enums;

import java.io.Serializable;

public enum CommandNames implements Serializable
{
   // kernel commands
   COMMENT,
   ENTER,

   // JOB Separation Commands
   JOB,
   EOJ,

   // queries
   INQUIRE,
//        DINQUIRE,
   ECHO,
   INFO,
   USTATUS,
   USTATUSOFF,
   RESET,
   SET,

   // messages
   RDYMSG,
   OPMSG,
   STMSG,

   //proprietary
   DMINFO,

   // custom
   RAW
}
