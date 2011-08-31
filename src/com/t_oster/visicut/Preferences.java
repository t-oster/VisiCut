/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.visicut;

import com.t_oster.liblasercut.LaserCutter;
import com.t_oster.visicut.gui.beans.LaserCam;
import java.awt.geom.AffineTransform;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 *
 * @author thommy
 */
public class Preferences
{

  protected LaserCam laserCam = new LaserCam();
  public static final String PROP_LASERCAM = "laserCam";

  /**
   * Get the value of laserCam
   *
   * @return the value of laserCam
   */
  public LaserCam getLaserCam()
  {
    return laserCam;
  }

  /**
   * Set the value of laserCam
   *
   * @param laserCam new value of laserCam
   */
  public void setLaserCam(LaserCam laserCam)
  {
    LaserCam oldLaserCam = this.laserCam;
    this.laserCam = laserCam;
    propertyChangeSupport.firePropertyChange(PROP_LASERCAM, oldLaserCam, laserCam);
  }

  protected LaserCutter laserCutter = null;
  public static final String PROP_LASERCUTTER = "laserCutter";

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
