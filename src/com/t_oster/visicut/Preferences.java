/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.visicut;

import com.t_oster.visicut.model.LaserDevice;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.List;

/**
 *
 * @author thommy
 */
public class Preferences
{

  public Preferences()
  {
  }
  
  protected List<LaserDevice> laserDevices = null;
  public static final String PROP_LASERDEVICES = "laserDevices";
  protected int defaultLaserDevice = 0;
  public static final String PROP_DEFAULTLASERDEVICE = "defaultLaserDevice";

  /**
   * Get the value of defaultLaserDevice
   *
   * @return the value of defaultLaserDevice
   */
  public int getDefaultLaserDevice()
  {
    return defaultLaserDevice;
  }

  /**
   * Set the value of defaultLaserDevice
   *
   * @param defaultLaserDevice new value of defaultLaserDevice
   */
  public void setDefaultLaserDevice(int defaultLaserDevice)
  {
    int oldDefaultLaserDevice = this.defaultLaserDevice;
    this.defaultLaserDevice = defaultLaserDevice;
    propertyChangeSupport.firePropertyChange(PROP_DEFAULTLASERDEVICE, oldDefaultLaserDevice, defaultLaserDevice);
  }

  /**
   * Get the value of laserDevices
   *
   * @return the value of laserDevices
   */
  public List<LaserDevice> getLaserDevices()
  {
    return laserDevices;
  }

  /**
   * Set the value of laserDevices
   *
   * @param laserDevices new value of laserDevices
   */
  public void setLaserDevices(List<LaserDevice> laserDevices)
  {
    List<LaserDevice> oldLaserDevices = this.laserDevices;
    this.laserDevices = laserDevices;
    propertyChangeSupport.firePropertyChange(PROP_LASERDEVICES, oldLaserDevices, laserDevices);
  }

  protected String[] availableLasercutterDrivers = null;

  /**
   * Get the value of availableLasercutterDrivers
   *
   * @return the value of availableLasercutterDrivers
   */
  public String[] getAvailableLasercutterDrivers()
  {
    return availableLasercutterDrivers;
  }

  /**
   * Set the value of availableLasercutterDrivers
   *
   * @param availableLasercutterDrivers new value of availableLasercutterDrivers
   */
  public void setAvailableLasercutterDrivers(String[] availableLasercutterDrivers)
  {
    this.availableLasercutterDrivers = availableLasercutterDrivers;
  }

  
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
