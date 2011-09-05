/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.visicut;

import com.t_oster.liblasercut.LaserCutter;
import java.awt.geom.AffineTransform;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 *
 * @author thommy
 */
public class Preferences
{

  public Preferences()
  {
  }
  
  protected LaserCutter laserCutter = null;
  public static final String PROP_LASERCUTTER = "laserCutter";
  protected String[] availableImporters = null;

  /**
   * Get the value of availableImporters
   *
   * @return the value of availableImporters
   */
  public String[] getAvailableImporters()
  {
    return availableImporters;
  }

  /**
   * Set the value of availableImporters
   *
   * @param availableImporters new value of availableImporters
   */
  public void setAvailableImporters(String[] availableImporters)
  {
    this.availableImporters = availableImporters;
  }

  /**
   * Get the value of laserCutter
   *
   * @return the value of laserCutter
   */
  public LaserCutter getLaserCutter()
  {
    return laserCutter;
  }

  /**
   * Set the value of laserCutter
   *
   * @param laserCutter new value of laserCutter
   */
  public void setLaserCutter(LaserCutter laserCutter)
  {
    LaserCutter oldLaserCutter = this.laserCutter;
    this.laserCutter = laserCutter;
    propertyChangeSupport.firePropertyChange(PROP_LASERCUTTER, oldLaserCutter, laserCutter);
  }
  protected String backgroundImageURL = null;
  public static final String PROP_BACKGROUNDIMAGEURL = "backgroundImageURL";

  /**
   * Get the value of backgroundImageURL
   *
   * @return the value of backgroundImageURL
   */
  public String getBackgroundImageURL()
  {
    return backgroundImageURL;
  }

  /**
   * Set the value of backgroundImageURL
   *
   * @param backgroundImageURL new value of backgroundImageURL
   */
  public void setBackgroundImageURL(String backgroundImageURL)
  {
    String oldBackgroundImageURL = this.backgroundImageURL;
    this.backgroundImageURL = backgroundImageURL;
    propertyChangeSupport.firePropertyChange(PROP_BACKGROUNDIMAGEURL, oldBackgroundImageURL, backgroundImageURL);
  }

  protected AffineTransform camCalibration = null;
  public static final String PROP_CAMCALIBRATION = "camCalibration";

  /**
   * Get the value of camCalibration
   *
   * @return the value of camCalibration
   */
  public AffineTransform getCamCalibration()
  {
    return camCalibration;
  }

  /**
   * Set the value of camCalibration
   *
   * @param camCalibration new value of camCalibration
   */
  public void setCamCalibration(AffineTransform camCalibration)
  {
    AffineTransform oldCamCalibration = this.camCalibration;
    this.camCalibration = camCalibration;
    propertyChangeSupport.firePropertyChange(PROP_CAMCALIBRATION, oldCamCalibration, camCalibration);
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
