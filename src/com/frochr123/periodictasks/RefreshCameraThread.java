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
package com.frochr123.periodictasks;

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
  public static final int DEFAULT_CAMERA_ERROR_WAIT_TIME = 10000;

  // Constructor
  public RefreshCameraThread()
  {
    super();
  }

  // Compute update timer, ensure valid data
  public static int getUpdateTimerMs()
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
  public static boolean isActive()
  {
    return MainView.getInstance().isCameraActive() && MainView.getInstance().isPreviewPanelShowBackgroundImage();
  }

  // Run method
  @Override
  public void run()
  {
    int errorCounter = 0; // how many errors since last successful capture?
    while (true)
    {
      try
      {
        // Check if last image capture has finished
        String error = MainView.getInstance().getCameraCapturingError();
        if (error != null)
        {
          MainView.getInstance().resetCameraCapturingError();
          if (error.isEmpty()) {
            // successfully captured
            errorCounter = 0;
            MainView.getInstance().getDialog().removeMessageWithId("camera error");
          } else {
            // error has occured
            errorCounter++;
            MainView.getInstance().getDialog().showWarningMessageOnce(error, "camera error", 0);
            // no timeout for this message, to prevent flickering when fetching an image takes very long.
            // The message will be cleared anyway,
            // either when disabling the camera via MainView.cameraActiveMenuItemActionPerformed
            // or after a successful capture (removeMessageWithId few lines above this comment)
            
            // sleep some extra time, so that VisiCam isn't overloaded            
            // increase waiting time after each error
            // quick retry after the first error, it could have been a network glitch
            Thread.sleep(Math.max(errorCounter, 2) * DEFAULT_CAMERA_ERROR_WAIT_TIME / 2 + getUpdateTimerMs());
            continue;
          }
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
