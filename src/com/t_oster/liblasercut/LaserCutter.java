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
public abstract class LaserCutter
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
}
