package com.t_oster.liblasercut;

import java.util.LinkedList;
import java.util.List;

/**
 * This class is for easy support for Progress Listeners
 * just extend this class and use the fireProgressChanged
 * and fireTaskNameChanged method.
 * 
 * @author thommy
 */
public abstract class TimeIntensiveOperation
{

  private List<ProgressListener> listeners = new LinkedList<ProgressListener>();

  public void addProgressListener(ProgressListener l)
  {
      listeners.add(l);
  }

  public void removeProgressListener(ProgressListener l)
  {
    listeners.remove(l);
  }

  public void fireProgressChanged(int progress)
  {
    for (ProgressListener l : listeners)
    {
      l.progressChanged(this, progress);
    }
  }

  public void fireTaskChanged(String name)
  {
    for (ProgressListener l : listeners)
    {
      l.taskChanged(this, name);
    }
  }

  private int progress = 0;
  protected void setProgress(int progress){
    if (progress != this.progress){
      this.progress = progress;
      this.fireProgressChanged(this.progress);
    }
  }
}
