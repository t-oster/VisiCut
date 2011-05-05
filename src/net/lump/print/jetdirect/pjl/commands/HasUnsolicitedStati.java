package net.lump.print.jetdirect.pjl.commands;

import java.util.Vector;

/**
 * This kind of command can be watched for an unsolicited status.  An object which implements
 * unsolicited status may have any number of UStatus objects associated with it.
 *
 * @author M. Troy Bowman
 */
public interface HasUnsolicitedStati
{
   public Vector<UnsolicitedStatus> getUStati();
}
