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
package com.tur0kk;

import com.github.sarxos.webcam.Webcam;
import com.t_oster.visicut.VisicutModel;
import com.t_oster.visicut.gui.MainView;
import com.t_oster.visicut.misc.Helper;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.net.URLConnection;
import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

/**
 * This thread abstracts the process of loading an image and displaying it in a label frequently. 
 * A soruce can be detected webcam (webcam = true) or the VisiCam (webcam = false and URL of VisiCam set in LaserDevice)
 * @author Sven
 */
public class TakePhotoThread extends Thread
{
  // Constants
  public static final int PHOTO_RESOLUTION_SMALL = 1;  // 176 x 144
  public static final int PHOTO_RESOLUTION_MEDIUM = 2; // 320 x 240
  public static final int PHOTO_RESOLUTION_HIGH = 3;   // 640 x 480

  // Variables
  private JLabel lblPhoto; // display target
  private boolean webcam; // true = use detected webcam, false = use visicamUrl
  private String visicamUrl; // URL of VisiCam in network
  private int photoResolution; // Identifier from static variables to set resolution of images
  private int framerateMs; // Integer value for the update interval of the camera in milliseconds
  private Image latestRawImage = null; // Store latest raw image for access by calling code

  private boolean running = true; // internal flat to know when to stop

  public TakePhotoThread(JLabel lblPhoto, boolean webcam, int photoResolution){
    this.lblPhoto = lblPhoto;
    this.webcam = webcam;
    this.visicamUrl = MainView.getInstance().getVisiCam();
    this.photoResolution = photoResolution;
    this.framerateMs = 20;
    this.latestRawImage = null;
  }
  
  @Override
  public void interrupt(){
    this.running = false;
  }
  
  @Override
  public void run()
  {
    try{
      
      // open attached webcam
      if(this.webcam){
        Webcam cam = Webcam.getDefault();
        
        // Set resolution of image
        int width = 0;
        int height = 0;

        switch (getPhotoResolution())
        {
          case PHOTO_RESOLUTION_SMALL:
            width = 176;
            height = 144;
            break;
          case PHOTO_RESOLUTION_MEDIUM:
            width = 320;
            height = 240;
            break;
          case PHOTO_RESOLUTION_HIGH:
          default:
            width = 640;
            height = 480;
            break;
        }

        if (width > 0 && height > 0)
        {
          cam.setViewSize(new Dimension(width, height));
        }

        // Open camera
        cam.open();
      }
      
      // frequently take picture and display
      while(this.running){
        ImageIcon picture = takePicture();

        displayPicture(picture);

        Thread.currentThread().sleep(getFramerateMs());     
      }
    }
    catch(Exception ex){
      // close thread
    }
    
    closeCamera();

  }
  
  // takes an image and displays it in the target label
  private void displayPicture(ImageIcon image){
    final ImageIcon picture = image;

    if(this.running){ // prevent displaying after terminating
      SwingUtilities.invokeLater(new Runnable() {
        public void run()
        {
          lblPhoto.setIcon(picture);
        }
      });
    }
  }
  
  private void closeCamera(){
    // Only close camera if camera was activated by this thread
    // Causes issues on fast switch between Visicam / Webcam capturing
    // because of arbitrary execution order: Webcam start, Visicam close
    if (this.webcam)
    {
      Webcam cam = Webcam.getDefault();
      if(cam != null && cam.isOpen()){
        cam.close();
      }
    }
  }
  
  /*
   * handles the picture taking depending on the given flag, uses webcam if webcam = tue and visicam if webcam = false
   */
  private ImageIcon takePicture(){
    ImageIcon imageIcon = null;
    if(this.webcam){ // webcam
      // take picture
      Webcam cam = Webcam.getDefault();
      if(cam.isOpen()){
        // read image from webcam and convert to ImageIcon
        BufferedImage image = cam.getImage();
        imageIcon = new ImageIcon(image);
      }
    }
    else{ // visicam
      try{
        // read out image from VisiCam
        URL src = new URL(this.visicamUrl);

        if (src != null)
        {
          URLConnection conn = src.openConnection();
        
          // HTTP authentication
          if (VisicutModel.getInstance() != null && VisicutModel.getInstance().getSelectedLaserDevice() != null)
          {
            String encodedCredentials = Helper.getEncodedCredentials(VisicutModel.getInstance().getSelectedLaserDevice().getURLUser(), VisicutModel.getInstance().getSelectedLaserDevice().getURLPassword());
            if (!encodedCredentials.isEmpty())
            {
              conn.setRequestProperty("Authorization", "Basic " + encodedCredentials);
            }
          }

          ImageInputStream stream = new MemoryCacheImageInputStream(conn.getInputStream());
          BufferedImage img = ImageIO.read(stream);
          imageIcon = new ImageIcon(img);
        }
      }
      catch(Exception e){
        return null;
      } 
    }

    // Store raw image
    Image rawImage = imageIcon.getImage();
    latestRawImage = rawImage;
    
    // Compute correct width / height relations
    float scaleFactor = Math.min((float)(lblPhoto.getWidth()) / (float)(rawImage.getWidth(null)), (float)(lblPhoto.getHeight()) / (float)(rawImage.getHeight(null)));
    int width = (int)(rawImage.getWidth(null) * scaleFactor);
    int height = (int)(rawImage.getHeight(null) * scaleFactor);

    // Scale image to fit label
    Image scaledImage = rawImage.getScaledInstance(width, height, Image.SCALE_SMOOTH);
    ImageIcon picture = new ImageIcon(scaledImage);
    return picture;
  }
  
  // webcam detected if library finds a webcam
  public static boolean isWebCamDetected(){
    Webcam webcam = Webcam.getDefault();
    if (webcam != null) { 
      return true;
    } else {
      return false;
    }
  }
  
  // MainView handles VisiCam, basically the MainView checks if the choosen LaserDevice defines a VisiCam url
  public static boolean isVisiCamDetected(){
    return MainView.getInstance().isVisiCamDetected();
  }

  // Getters and setters
  // Disallow setting of resolution, needs to be set before camera starts => Constructor
  public int getPhotoResolution()
  {
    return photoResolution;
  }

  public int getFramerateMs()
  {
    return framerateMs;
  }

  public void setFramerateMs(int framerateMs)
  {
    this.framerateMs = framerateMs;
  }

  public Image getLatestRawImage()
  {
    return latestRawImage;
  }
}
