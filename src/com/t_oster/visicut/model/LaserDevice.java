package com.t_oster.visicut.model;

import com.t_oster.liblasercut.LaserCutter;
import com.t_oster.visicut.gui.ImageListable;
import java.awt.geom.AffineTransform;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * A wrapper for the LaserCutter class which adds some Attributes
 * 
 * @author thommy
 */
public class LaserDevice implements ImageListable
{

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

  protected AffineTransform cameraCalibration = null;
  public static final String PROP_CAMERACALIBRATION = "cameraCalibration";

  /**
   * Get the value of cameraCalibration
   *
   * @return the value of cameraCalibration
   */
  public AffineTransform getCameraCalibration()
  {
    return cameraCalibration;
  }

  /**
   * Set the value of cameraCalibration
   *
   * @param cameraCalibration new value of cameraCalibration
   */
  public void setCameraCalibration(AffineTransform cameraCalibration)
  {
    AffineTransform oldCameraCalibration = this.cameraCalibration;
    this.cameraCalibration = cameraCalibration;
    propertyChangeSupport.firePropertyChange(PROP_CAMERACALIBRATION, oldCameraCalibration, cameraCalibration);
  }

  protected String cameraURL = null;
  public static final String PROP_CAMERAURL = "cameraURL";

  /**
   * Get the value of cameraURL
   *
   * @return the value of cameraURL
   */
  public String getCameraURL()
  {
    return cameraURL;
  }

  /**
   * Set the value of cameraURL
   *
   * @param cameraURL new value of cameraURL
   */
  public void setCameraURL(String cameraURL)
  {
    String oldCameraURL = this.cameraURL;
    this.cameraURL = cameraURL;
    propertyChangeSupport.firePropertyChange(PROP_CAMERAURL, oldCameraURL, cameraURL);
  }

  protected String thumbnailPath = null;
  public static final String PROP_THUMBNAILPATH = "thumbnailPath";

  /**
   * Get the value of thumbnailPath
   *
   * @return the value of thumbnailPath
   */
  public String getThumbnailPath()
  {
    return thumbnailPath;
  }

  /**
   * Set the value of thumbnailPath
   *
   * @param thumbnailPath new value of thumbnailPath
   */
  public void setThumbnailPath(String thumbnailPath)
  {
    String oldThumbnailPath = this.thumbnailPath;
    this.thumbnailPath = thumbnailPath;
    propertyChangeSupport.firePropertyChange(PROP_THUMBNAILPATH, oldThumbnailPath, thumbnailPath);
  }

  protected String description = null;
  public static final String PROP_DESCRIPTION = "description";

  /**
   * Get the value of description
   *
   * @return the value of description
   */
  public String getDescription()
  {
    return description;
  }

  /**
   * Set the value of description
   *
   * @param description new value of description
   */
  public void setDescription(String description)
  {
    String oldDescription = this.description;
    this.description = description;
    propertyChangeSupport.firePropertyChange(PROP_DESCRIPTION, oldDescription, description);
  }

  protected String name = null;
  public static final String PROP_NAME = "name";

  /**
   * Get the value of name
   *
   * @return the value of name
   */
  public String getName()
  {
    return name;
  }

  /**
   * Set the value of name
   *
   * @param name new value of name
   */
  public void setName(String name)
  {
    String oldName = this.name;
    this.name = name;
    propertyChangeSupport.firePropertyChange(PROP_NAME, oldName, name);
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
