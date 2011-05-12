/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.liblasercut;

/**
 *
 * @author thommy
 */
public interface LaserCutter {
    public void sendJob(LaserJob job) throws IllegalJobException, Exception;
    public int[] getResolutions();
    public int getBedWidth();
    public int getBedHeight();
}
