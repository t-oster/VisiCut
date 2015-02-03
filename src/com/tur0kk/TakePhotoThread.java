package com.tur0kk;

import com.github.sarxos.webcam.Webcam;
import com.t_oster.visicut.gui.MainView;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.net.URL;
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

  JLabel lblPhoto; // display target
  boolean webcam; // true = use detected webcam, false = use visicamUrl
  String visicamUrl; // URL of VisiCam in network
  
  boolean running = true; // internal flat to know when to stop
  
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
      
      // open attached webcam
      if(this.webcam){
        Webcam cam = Webcam.getDefault();
        cam.open();
      }
      
      // frequently take picture and display
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
    Webcam cam = Webcam.getDefault();
    if(cam.isOpen()){
      cam.close();      
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
  
}
