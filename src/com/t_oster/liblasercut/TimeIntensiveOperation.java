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
package com.t_oster.liblasercut;

import java.util.LinkedList;
import java.util.List;

/**
 * This class is for easy support for Progress Listeners
 * just extend this class and use the fireProgressChanged
 * and fireTaskNameChanged method.
 * 
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
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
