package com.t_oster.liblasercut;

/**
 * This Interface represents a listener
 * for progress of 
 * Operations which require much time
 * 
 * @author thommy
 */
public interface ProgressListener
{
  public void progressChanged(Object source, int percent);
  public void taskChanged(Object source, String taskName);
}
