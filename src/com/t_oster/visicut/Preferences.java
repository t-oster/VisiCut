/**
 * This file is part of VisiCut.
 * Copyright (C) 2012 Thomas Oster <thomas.oster@rwth-aachen.de>
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

import com.t_oster.liblasercut.LibInfo;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class Preferences
{

  public Preferences()
  {
  }
  
  protected boolean useThicknessAsFocusOffset = true;

  /**
   * Get the value of useThicknessAsFocusOffset
   *
   * @return the value of useThicknessAsFocusOffset
   */
  public boolean isUseThicknessAsFocusOffset()
  {
    return useThicknessAsFocusOffset;
  }

  /**
   * Set the value of useThicknessAsFocusOffset
   *
   * @param useThicknessAsFocusOffset new value of useThicknessAsFocusOffset
   */
  public void setUseThicknessAsFocusOffset(boolean useThicknessAsFocusOffset)
  {
    this.useThicknessAsFocusOffset = useThicknessAsFocusOffset;
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

  
  protected String lastMaterial = null;

  /**
   * Get the value of lastMaterial
   *
   * @return the value of lastMaterial
   */
  public String getLastMaterial()
  {
    return lastMaterial;
  }

  /**
   * Set the value of lastMaterial
   *
   * @param lastMaterial new value of lastMaterial
   */
  public void setLastMaterial(String lastMaterial)
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

  protected String lastLaserDevice = null;

  /**
   * Get the value of lastLaserDevice
   *
   * @return the value of lastLaserDevice
   */
  public String getLastLaserDevice()
  {
    return lastLaserDevice;
  }

  /**
   * Set the value of lastLaserDevice
   *
   * @param lastLaserDevice new value of lastLaserDevice
   */
  public void setLastLaserDevice(String lastLaserDevice)
  {
    this.lastLaserDevice = lastLaserDevice;
  }

  protected String[] availableLasercutterDrivers = null;

  /**
   * Get the value of availableLasercutterDrivers
   * and adds all Builtin classes of LibLaserCut
   *
   * @return the value of availableLasercutterDrivers
   */
  public String[] getAvailableLasercutterDrivers()
  {
    Set<String> result = new LinkedHashSet<String>();
    for (Class c : LibInfo.getSupportedDrivers())
    {
      result.add(c.getCanonicalName());
    }
    if (availableLasercutterDrivers!=null)
    {
      result.addAll(Arrays.asList(availableLasercutterDrivers));
    }
    return result.toArray(new String[0]);
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

  
  protected String[] availableImporters = new String[]{
    "com.t_oster.visicut.model.graphicelements.svgsupport.SVGImporter",
    "com.t_oster.visicut.model.graphicelements.jpgpngsupport.JPGPNGImporter",
    "com.t_oster.visicut.model.graphicelements.dxfsupport.DXFImporter",
    "com.t_oster.visicut.model.graphicelements.epssupport.EPSImporter"
  };

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
    if (availableImporters != null)
    {
      this.availableImporters = availableImporters;
    }
  }

    private boolean editSettingsBeforeExecuting = false;

  /**
   * Get the value of editSettingsBeforeExecuting
   *
   * @return the value of editSettingsBeforeExecuting
   */
  public boolean isEditSettingsBeforeExecuting()
  {
    return editSettingsBeforeExecuting;
  }

  /**
   * Set the value of editSettingsBeforeExecuting
   *
   * @param editSettingsBeforeExecuting new value of editSettingsBeforeExecuting
   */
  public void setEditSettingsBeforeExecuting(boolean editSettingsBeforeExecuting)
  {
    this.editSettingsBeforeExecuting = editSettingsBeforeExecuting;
  }

}
