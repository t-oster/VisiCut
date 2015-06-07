/**
 * This file is part of VisiCut.
 * Copyright (C) 2011 - 2013 Thomas Oster <thomas.oster@rwth-aachen.de>
 * RWTH Aachen University - 52062 Aachen, Germany
 *
 *     VisiCut is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     VisiCut is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with VisiCut.  If not, see <http://www.gnu.org/licenses/>.
 **/
package com.frochr123.periodicthreads;

import com.t_oster.visicut.gui.MainView;
import com.t_oster.visicut.VisicutModel;

/**
 * RefreshCameraThread.java: Thread to refresh the image of camera frequently
 * @author Christian
 */
public class RefreshCameraThread extends Thread
{
  // Constant values
  public static final int DEFAULT_CAMERA_TIME = 50;
  public static final int DEFAULT_CAMERA_LONG_WAIT_TIME = 15000;
  
  // Constructor
  public RefreshCameraThread()
  {
    super();
  }

  // Compute update timer, ensure valid data
  public int getUpdateTimerMs()
  {
    if (VisicutModel.getInstance().getSelectedLaserDevice() != null)
    {
      if (VisicutModel.getInstance().getSelectedLaserDevice().getCameraTiming() > 0)
      {
        return VisicutModel.getInstance().getSelectedLaserDevice().getCameraTiming();
      }
    }

    return DEFAULT_CAMERA_TIME;
  }

  // Check MainView if camera and background are set to active
  protected boolean isActive()
  {
    return MainView.getInstance().isCameraActive() && MainView.getInstance().isPreviewPanelShowBackgroundImage();
  }
 
  // Run method
  @Override
  public void run()
  {
    while (true)
    {
      try
      {
        // Check if last run caused exception
        if (!MainView.getInstance().getCameraCapturingError().isEmpty())
        {
          // Sleep extra long time, avoid spamming of warnings
          MainView.getInstance().getDialog().showWarningMessage(MainView.getInstance().getCameraCapturingError());
          MainView.getInstance().resetCameraCapturingError();
          Thread.currentThread().sleep(DEFAULT_CAMERA_LONG_WAIT_TIME);
          continue;
        }

        // Check if thread should be working
        if (!isActive())
        {
          // Sleep long time, thread not active at all
          Thread.currentThread().sleep(getUpdateTimerMs() * 5);
          continue;
        }
        
        // Call capture new image if everyhing is fine
        if (!MainView.getInstance().getVisiCam().isEmpty())
        {
          MainView.getInstance().captureImage();
        }

        // Sleep to give other threads computation time
        Thread.currentThread().sleep(getUpdateTimerMs());
      }
      catch (InterruptedException e)
      {
        // On interrupt, close thread, should never happen
        return;
      }
    }
  }
}
