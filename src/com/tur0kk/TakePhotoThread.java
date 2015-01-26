/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tur0kk;

import com.github.sarxos.webcam.Webcam;
import com.t_oster.visicut.gui.MainView;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

/**
 *
 * @author Sven
 */
public class TakePhotoThread extends Thread
{

  JLabel lblPhoto;
  boolean webcam;
  String visicamUrl;
  boolean running = true;
  
  public TakePhotoThread(JLabel lblPhoto, boolean webcam){
    this.lblPhoto = lblPhoto;
    this.webcam = webcam;
    this.visicamUrl = MainView.getInstance().getVisiCam();
  }
  
  @Override
  public void interrupt(){
    this.running = false;
  }
  
  @Override
  public void run()
  {
    try{
      
      if(this.webcam){
        Webcam cam = Webcam.getDefault();
        cam.open();
      }
      
      while(this.running){
        ImageIcon picture = takePicture();

        displayPicture(picture);

        Thread.currentThread().sleep(100);     
      }
    }
    catch(Exception ex){
      // close thread
    }
    
    closeCamera();

  }
  
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
    Webcam cam = Webcam.getDefault();
    if(cam.isOpen()){
      cam.close();      
    }
  }
  
  private ImageIcon takePicture(){
    ImageIcon imageIcon = null;
    if(this.webcam){ // webcam
      // take picture
      Webcam cam = Webcam.getDefault();
      if(cam.isOpen()){
        BufferedImage image = cam.getImage();
        imageIcon = new ImageIcon(image);
      }
    }
    else{ // visicam
      try{
        URL src = new URL(this.visicamUrl);
        imageIcon = new ImageIcon(src);
      }
      catch(Exception e){
        return null;
      } 
    }
    // scale to label
    Image rawImage = imageIcon.getImage();
    Image scaledImage = rawImage.getScaledInstance(
      lblPhoto.getWidth(),
      lblPhoto.getHeight(),
      Image.SCALE_SMOOTH);
    ImageIcon picture = new ImageIcon(scaledImage);
    return picture;
  }
  
  public static boolean isWebCamDetected(){
    Webcam webcam = Webcam.getDefault();
    if (webcam != null) { 
      return true;
    } else {
      return false;
    }
  }
  
  public static boolean isVisiCamDetected(){
    return MainView.getInstance().isVisiCamDetected();
  }
  
}
