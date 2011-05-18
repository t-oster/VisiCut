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
public interface LaserCutter {
    /**
     * Performs sanity checks on the LaserJob and sends it to the Cutter
     * @param job
     * @throws IllegalJobException if the Job didn't pass the SanityCheck
     * @throws Exception  if there is a Problem with the Communication or Queue
     */
    public void sendJob(LaserJob job) throws IllegalJobException, Exception;
    /**
     * Returns the available Resolutions in DPI
     * @return 
     */
    public List<Integer> getResolutions();
    /**
     * Returns the Maximum width of a LaserJob in mm
     * @return 
     */
    public double getBedWidth();
    /**
     * Returns the Maximum height of a LaserJob in mm
     * @return 
     */
    public double getBedHeight();
}
