package com.t_oster.visicut.gui.beans;

import java.awt.image.RenderedImage;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

/**
 * This class represents a Webcam providing Images of the Lasercutter,
 * either generated or live
 * @author thommy
 */
public class LaserCam
{

  protected int refreshInterval = 0;
  public static final String PROP_REFRESHINTERVAL = "refreshInterval";

  /**
   * Get the value of refreshInterval
   *
   * @return the value of refreshInterval
   */
  public int getRefreshInterval()
  {
    return refreshInterval;
  }
  private Thread refreshTread = new Thread()
  {

    @Override
    public void run()
    {
      while (LaserCam.this.getRefreshInterval() != 0)
      {
        try
        {
          Thread.sleep(LaserCam.this.getRefreshInterval() * 1000);
        }
        catch (InterruptedException ex)
        {
          //
        }
        LaserCam.this.setImageURL(LaserCam.this.getImageURL());
      }
    }
  };

  /**
   * Set the value of refreshInterval
   *
   * @param refreshInterval new value of refreshInterval
   */
  public void setRefreshInterval(int refreshInterval)
  {
    int oldRefreshInterval = this.refreshInterval;
    this.refreshInterval = refreshInterval;
    propertyChangeSupport.firePropertyChange(PROP_REFRESHINTERVAL, oldRefreshInterval, refreshInterval);
    if (oldRefreshInterval == 0 && refreshInterval != 0)
    {
      this.refreshTread.start();
    }
  }

  public LaserCam()
  {
    try
    {
      this.setImageURL(new File("test/files/lasercutter.jpg").toURI().toURL());
    }
    catch (MalformedURLException ex)
    {
      Logger.getLogger(LaserCam.class.getName()).log(Level.SEVERE, null, ex);
    }
  }
  protected RenderedImage capturedImage = null;
  public static final String PROP_CAPTUREDIMAGE = "capturedImage";
  public static final String PROP_IMAGEURL = "imageURL";
  protected URL imageURL = null;

  /**
   * Get the value of imageURL
   *
   * @return the value of imageURL
   */
  public URL getImageURL()
  {
    return imageURL;
  }

  /**
   * Set the value of imageURL
   *
   * @param imageURL new value of imageURL
   */
  public final void setImageURL(URL imageURL)
  {
    URL oldURL = this.imageURL;
    this.imageURL = imageURL;
    try
    {
      this.setCapturedImage(imageURL == null ? null : ImageIO.read(imageURL));
    }
    catch (IOException ex)
    {
      Logger.getLogger(LaserCam.class.getName()).log(Level.SEVERE, null, ex);
    }

    propertyChangeSupport.firePropertyChange(PROP_IMAGEURL, oldURL, imageURL);


  }

  /**
   * Get the value of capturedImage
   *
   * @return the value of capturedImage
   */
  public RenderedImage getCapturedImage()
  {
    return capturedImage;
  }

  /**
   * Set the value of capturedImage
   *
   * @param capturedImage new value of capturedImage
   */
  public void setCapturedImage(RenderedImage capturedImage)
  {
      RenderedImage oldCapturedImage = this.capturedImage;
      this.capturedImage = capturedImage;
      propertyChangeSupport.firePropertyChange(PROP_CAPTUREDIMAGE, oldCapturedImage, capturedImage);
  }
  private PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

  /**
   * Add PropertyChangeListener.
   *
   * @param listener
   */
  public void addPropertyChangeListener(PropertyChangeListener listener)
  {
    propertyChangeSupport.addPropertyChangeListener(listener);
  }

  /**
   * Remove PropertyChangeListener.
   *
   * @param listener
   */
  public void removePropertyChangeListener(PropertyChangeListener listener)
  {
    propertyChangeSupport.removePropertyChangeListener(listener);
  }
}
