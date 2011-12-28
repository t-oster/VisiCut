/**
 * This file is part of VisiCut.
 * Copyright (C) 2011 Thomas Oster <thomas.oster@rwth-aachen.de>
 * RWTH Aachen University - 52062 Aachen, Germany
 * 
 *     VisiCut is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *    VisiCut is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 * 
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with VisiCut.  If not, see <http://www.gnu.org/licenses/>.
 **/
package com.t_oster.visicut;

import com.t_oster.visicut.model.LaserDevice;
import com.t_oster.visicut.model.MaterialProfile;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.LinkedList;
import java.util.List;

public class Preferences
{

  public Preferences()
  {
  }
  
  protected List<String> recentFiles = new LinkedList<String>();

  /**
   * Get the value of recentFiles
   *
   * @return the value of recentFiles
   */
  public List<String> getRecentFiles()
  {
    return recentFiles;
  }

  /**
   * Set the value of recentFiles
   *
   * @param recentFiles new value of recentFiles
   */
  public void setRecentFiles(List<String> recentFiles)
  {
    this.recentFiles = recentFiles;
  }

  
  protected MaterialProfile lastMaterial = null;

  /**
   * Get the value of lastMaterial
   *
   * @return the value of lastMaterial
   */
  public MaterialProfile getLastMaterial()
  {
    return lastMaterial;
  }

  /**
   * Set the value of lastMaterial
   *
   * @param lastMaterial new value of lastMaterial
   */
  public void setLastMaterial(MaterialProfile lastMaterial)
  {
    this.lastMaterial = lastMaterial;
  }

  protected Integer lastResolution = null;

  /**
   * Get the value of lastResolution
   *
   * @return the value of lastResolution
   */
  public Integer getLastResolution()
  {
    return lastResolution;
  }

  /**
   * Set the value of lastResolution
   *
   * @param lastResolution new value of lastResolution
   */
  public void setLastResolution(Integer lastResolution)
  {
    this.lastResolution = lastResolution;
  }

  protected LaserDevice lastLaserDevice = null;

  /**
   * Get the value of lastLaserDevice
   *
   * @return the value of lastLaserDevice
   */
  public LaserDevice getLastLaserDevice()
  {
    return lastLaserDevice;
  }

  /**
   * Set the value of lastLaserDevice
   *
   * @param lastLaserDevice new value of lastLaserDevice
   */
  public void setLastLaserDevice(LaserDevice lastLaserDevice)
  {
    this.lastLaserDevice = lastLaserDevice;
  }

  
  protected List<LaserDevice> laserDevices = null;
  public static final String PROP_LASERDEVICES = "laserDevices";

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
