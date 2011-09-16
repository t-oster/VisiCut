/**
 * This file is part of VisiCut.
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
 * @author thommy
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
  
  public abstract String getHostname();
  
  public abstract void setHostname(String hostname);
  
  public abstract int getPort();
  
  public abstract void setPort(int Port);
  
  @Override
  public abstract LaserCutter clone();
}
