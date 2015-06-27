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

import com.frochr123.helper.PreviewImageExport;
import com.t_oster.visicut.gui.MainView;
import com.t_oster.visicut.VisicutModel;
import com.t_oster.visicut.misc.Helper;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

/**
 * RefreshProjectorThread.java: Thread to refresh the image of the projector frequently
 * @author Christian
 */
public class RefreshProjectorThread extends Thread
{
  // Constant values
  public static final int DEFAULT_PROJECTOR_TIME = 50;
  public static final int DEFAULT_PROJECTOR_SHUTDOWN_THREAD_TIME = 25;
  public static final int DEFAULT_PROJECTOR_LONG_WAIT_TIME = 15000;
  public static final int DEFAULT_PROJECTOR_TIMEOUT = 10000;
  public static final int DEFAULT_PROJECTOR_WIDTH = 1280;
  public static final int DEFAULT_PROJECTOR_HEIGHT = 720;

  // Variables
  private String lastExceptionMessage = "";
  private boolean updateInProgress = false;
  private boolean shutdownInProgress = false;
  private boolean shutdownThreadRunning = false;
  
  // Constructor
  public RefreshProjectorThread()
  {
    super();
    lastExceptionMessage = "";
    updateInProgress = false;
    shutdownInProgress = false;
  }

  // Compute update timer, ensure valid data
  public static int getUpdateTimerMs()
  {
    if (VisicutModel.getInstance().getSelectedLaserDevice() != null)
    {
      if (VisicutModel.getInstance().getSelectedLaserDevice().getProjectorTiming() > 0)
      {
        return VisicutModel.getInstance().getSelectedLaserDevice().getProjectorTiming();
      }
    }

    return DEFAULT_PROJECTOR_TIME;
  }
  
  // Compute width, ensure valid data
  public static int getProjectorWidth()
  {
    if (VisicutModel.getInstance().getSelectedLaserDevice() != null)
    {
      if (VisicutModel.getInstance().getSelectedLaserDevice().getProjectorWidth() > 0)
      {
        return VisicutModel.getInstance().getSelectedLaserDevice().getProjectorWidth();
      }
    }

    return DEFAULT_PROJECTOR_WIDTH;
  }
  
  // Compute height, ensure valid data
  public static int getProjectorHeight()
  {
    if (VisicutModel.getInstance().getSelectedLaserDevice() != null)
    {
      if (VisicutModel.getInstance().getSelectedLaserDevice().getProjectorHeight() > 0)
      {
        return VisicutModel.getInstance().getSelectedLaserDevice().getProjectorHeight();
      }
    }

    return DEFAULT_PROJECTOR_HEIGHT;
  }

  // Check MainView if camera is set to active
  public boolean isActive()
  {
    return MainView.getInstance().isProjectorActive() || isShutdownInProgress();
  }

  public boolean isShutdownInProgress()
  {
    return shutdownInProgress;
  }

  public void startShutdown()
  {
    // Set that thread is running and trying to set shutdown
    if (!shutdownThreadRunning)
    {
      shutdownThreadRunning = true;

      // Need to set shutdownInProgress variable asynchronously, might otherwise cause inconsistencies
      new Thread()
      {
        @Override
        public void run()
        {
          while (true)
          {
            try
            {
              if (updateInProgress)
              {
                Thread.currentThread().sleep(DEFAULT_PROJECTOR_SHUTDOWN_THREAD_TIME);
                continue;
              }

              shutdownInProgress = true;
              shutdownThreadRunning = false;
              break;
            }
            catch (Exception e)
            {
              // Set flag for exception handling, sleep and message
              lastExceptionMessage = "Projector thread exception: Subthread interrupted!";
            }
          }
        }
      }.start();
    }
  }
  
  // Function to update projector image
  public void updateProjectorImage()
  {
    if (!updateInProgress && !shutdownThreadRunning)
    {
      updateInProgress = true;

      new Thread()
      {
        @Override
        public void run()
        {
          try
          {
            if (VisicutModel.getInstance() != null && VisicutModel.getInstance().getSelectedLaserDevice() != null && VisicutModel.getInstance().getSelectedLaserDevice().getProjectorURL() != null && !VisicutModel.getInstance().getSelectedLaserDevice().getProjectorURL().isEmpty())
            {
              BufferedImage img = PreviewImageExport.generateImage(getProjectorWidth(), getProjectorHeight(), !isShutdownInProgress());

              ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
              PreviewImageExport.writePngToOutputStream(outputStream, img);
              byte[] imagePostDataByte = outputStream.toByteArray();

              // Create HTTP client and cusomized config for timeouts
              CloseableHttpClient httpClient = HttpClients.createDefault();
              RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(DEFAULT_PROJECTOR_TIMEOUT).setConnectTimeout(DEFAULT_PROJECTOR_TIMEOUT).setConnectionRequestTimeout(DEFAULT_PROJECTOR_TIMEOUT).build();

              // Create HTTP Post request
              HttpPost httpPost = new HttpPost(VisicutModel.getInstance().getSelectedLaserDevice().getProjectorURL());
              httpPost.setConfig(requestConfig);

              // Insert file upload
              MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();
              multipartEntityBuilder.addBinaryBody("data", imagePostDataByte, ContentType.APPLICATION_OCTET_STREAM, "data");
              HttpEntity httpEntity = multipartEntityBuilder.build();
              httpPost.setEntity(httpEntity);

              // Set authentication information
              String encodedCredentials = Helper.getEncodedCredentials(VisicutModel.getInstance().getSelectedLaserDevice().getURLUser(), VisicutModel.getInstance().getSelectedLaserDevice().getURLPassword());
              if (!encodedCredentials.isEmpty())
              {
                httpPost.addHeader("Authorization", "Basic " + encodedCredentials);
              }

              // Send request
              CloseableHttpResponse res = httpClient.execute(httpPost);
              
              // React to possible server side errors
              if (res.getStatusLine() == null || res.getStatusLine().getStatusCode() != HttpStatus.SC_OK)
              {
                throw new Exception("Server sent wrong HTTP status code: " + new Integer(res.getStatusLine().getStatusCode()).toString());
              }

              // Close everything correctly
              res.close();
              httpClient.close();
            }
          }
          // This is caused internally in apache commons library for wrong authentication
          // Would need to add additional apache commons library file to get a correct exception back for that
          catch (NoClassDefFoundError error)
          {
            // Set flag for exception handling, sleep and message
            lastExceptionMessage = "Projector thread exception: Authentication error!";
          }
          catch (Exception e)
          {
            // Set flag for exception handling, sleep and message
            lastExceptionMessage = "Projector thread exception (2): " + e.getMessage();
          }
          
          updateInProgress = false;

          // Need to check if shutdown is set here first, otherwise these asynchronous calls
          // would always overwrite a call to shutdown in progress = true
          if (shutdownInProgress)
          {
            shutdownInProgress = false;
          }
        }
      }.start();
    }
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
        if (!lastExceptionMessage.isEmpty())
        {
          // Sleep extra long time, avoid spamming of warnings
          MainView.getInstance().getDialog().showWarningMessage(lastExceptionMessage);
          lastExceptionMessage = "";
          Thread.currentThread().sleep(DEFAULT_PROJECTOR_LONG_WAIT_TIME);
          continue;
        }

        // Check if thread should be working
        if (!isActive())
        {
          // Sleep long time, thread not active at all
          Thread.currentThread().sleep(getUpdateTimerMs() * 5);
          continue;
        }
        
        // Call update projector image with filled image
        updateProjectorImage();

        // Sleep to give other threads computation time
        Thread.currentThread().sleep(getUpdateTimerMs());
      }
      catch (Exception e)
      {
        // Set flag for exception handling, sleep and message
        lastExceptionMessage = "Projector thread exception (1): " + e.getMessage();
      }
    }
  }
}
