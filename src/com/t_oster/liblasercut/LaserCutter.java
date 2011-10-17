/**
 * This file is part of VisiCut.
 * Copyright (C) 2011 Thomas Oster <thomas.oster@rwth-aachen.de>
 * RWTH Aachen University - 52062 Aachen, Germany
 * 
 *     VisiCut is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *    VisiCut is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 * 
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with VisiCut.  If not, see <http://www.gnu.org/licenses/>.
 **/
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.liblasercut;

import java.util.List;

/**
 *
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public abstract class LaserCutter implements Cloneable
{

  /**
   * Performs sanity checks on the LaserJob and sends it to the Cutter
   * @param job
   * @throws IllegalJobException if the Job didn't pass the SanityCheck
   * @throws Exception  if there is a Problem with the Communication or Queue
   */
  public abstract void sendJob(LaserJob job) throws IllegalJobException, Exception;

  /**
   * Returns the available Resolutions in DPI
   * @return 
   */
  public abstract List<Integer> getResolutions();

  /**
   * Returns the Maximum width of a LaserJob in mm
   * @return 
   */
  public abstract double getBedWidth();

  /**
   * Returns the Maximum height of a LaserJob in mm
   * @return 
   */
  public abstract double getBedHeight();
  
  /**
   * Returns a List of Attributes, needed for 
   * configuring the Lasercutter (eg. IP, Port...)
   * @return 
   */
  public abstract List<String> getSettingAttributes();
  
  /**
   * Returns the <value> of the setting <attribute>
   * @param attribute
   * @return 
   */
  public abstract String getSettingValue(String attribute);
  
  /**
   * Sets the setting named <attribute> to <value>
   * @param attribute
   * @param value 
   */
  public abstract void setSettingValue(String attribute, String value);
  
  /**
   * Returns an estimated time, how long the job would take
   * in seconds
   * @param job
   * @return 
   */
  public abstract int estimateJobDuration(LaserJob job);
  
  @Override
  public abstract LaserCutter clone();
}
