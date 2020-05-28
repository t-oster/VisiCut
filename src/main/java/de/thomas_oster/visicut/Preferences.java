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
package de.thomas_oster.visicut;

import de.thomas_oster.liblasercut.LaserCutter;
import de.thomas_oster.liblasercut.LibInfo;
import java.awt.Rectangle;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Set;

public class Preferences
{

  public Preferences()
  {
  }
  
  /**
   * Clear all preferences which are user-specific, e.g., recent files and window-size
   */
  public void anonymize()
  {
    setLastMaterial(null);
    setLastLaserDevice(null);
    setRecentFiles(null);
    setWindowBounds(null);
    lastAutoUpdateTime=0;
    setLastAutoUpdateLabName(null);
  }

  private String defaultMapping = null;

  public String getDefaultMapping()
  {
    return defaultMapping;
  }

  public void setDefaultMapping(String defaultMapping)
  {
    this.defaultMapping = defaultMapping;
  }
  
  private boolean disableSandbox = false;
  
  public boolean isDisableSandbox()
  {
    return disableSandbox;
  }
  
  public void setDisableSandbox(boolean disableSandbox)
  {
    this.disableSandbox = disableSandbox;
  }
  
  private boolean useFilenamesForJobs = false;

  public boolean isUseFilenamesForJobs()
  {
    return useFilenamesForJobs;
  }

  public void setUseFilenamesForJobs(boolean useFilenamesForJobs)
  {
    this.useFilenamesForJobs = useFilenamesForJobs;
  }
  
  private String labName = null;
  
  public String getLabName()
  {
    if(labName == null)
    {
      labName = "unknown lab";
    }
    return labName;
  }
  
  public void setLabName(String labName)
  {
    this.labName = labName;
  }
  
  private boolean enableQRCodes = false;

  public boolean isEnableQRCodes()
  {
    return enableQRCodes;
  }

  public void setEnableQRCodes(boolean enableQRCodes)
  {
    this.enableQRCodes = enableQRCodes;
  }
  
  private boolean fastQRCodes = false;

  public boolean isFastQRCodes()
  {
    return fastQRCodes;
  }

  public void setFastQRCodes(boolean fastQRCodes)
  {
    this.fastQRCodes = fastQRCodes;
  }
  
  private boolean fabqrActive = false;

  public boolean isFabqrActive()
  {
    return fabqrActive;
  }

  public void setFabqrActive(boolean fabqrActive)
  {
    this.fabqrActive = fabqrActive;
  }
  
  private String fabqrPrivateURL = "";
  
  public String getFabqrPrivateURL()
  {
    return fabqrPrivateURL;
  }

  public void setFabqrPrivateURL(String fabqrPrivateURL)
  {
    this.fabqrPrivateURL = fabqrPrivateURL;
  }
  
  private String fabqrPublicURL = "";
  
  public String getFabqrPublicURL()
  {
    return fabqrPublicURL;
  }

  public void setFabqrPublicURL(String fabqrPublicURL)
  {
    this.fabqrPublicURL = fabqrPublicURL;
  }
  
  private String fabqrPrivateUser = "";
  
  public String getFabqrPrivateUser()
  {
    return fabqrPrivateUser;
  }

  public void setFabqrPrivateUser(String fabqrPrivateUser)
  {
    this.fabqrPrivateUser = fabqrPrivateUser;
  }
  
  private String fabqrPrivatePassword = "";
  
  public String getFabqrPrivatePassword()
  {
    return fabqrPrivatePassword;
  }

  public void setFabqrPrivatePassword(String fabqrPrivatePassword)
  {
    this.fabqrPrivatePassword = fabqrPrivatePassword;
  }
  
  private String potracePath = null;

  public String getPotracePath()
  {
    if (potracePath == null)
    {
      potracePath = "potrace";
    }
    return potracePath;
  }

  public void setPotracePath(String potracePath)
  {
    this.potracePath = potracePath;
  }

    private String mkbitmapPath = null;

  public String getMkbitmapPath()
  {
    if (mkbitmapPath == null)
    {
      mkbitmapPath = "mkbitmap";
    }
    return mkbitmapPath;
  }

  public void setMkbitmapPath(String mkbitmapPath)
  {
    this.mkbitmapPath = mkbitmapPath;
  }
  
  private boolean autoUpdateSettingsDisabled = false;

  /**
   * automatically update settings every 14 days?
   */
  public boolean isAutoUpdateSettings() {
    return !autoUpdateSettingsDisabled;
  }

  public void setAutoUpdateSettings(boolean autoUpdateSettings) {
    this.autoUpdateSettingsDisabled = !autoUpdateSettings;
  }

  // Date.getTime() of last autoUpdate.
  private long lastAutoUpdateTime = 0;

  public long getDaysSinceLastAutoUpdate()
  {
    return (new Date().getTime() - lastAutoUpdateTime)/(24 * 60 * 60 * 1000);
  }

  public void resetLastAutoUpdateTime()
  {
    this.lastAutoUpdateTime = new Date().getTime();
  }

  /**
   * Which lab was selected (in MainView.jmDownloadSettingsActionPerformed)?
   * null = unknown - preferences are from older VisiCut version
   * "" = imported from file, not from web
   * "Germany, Berlin, ..." = imported from web
   */
  private String lastAutoUpdateLabName = null;

  public String getLastAutoUpdateLabName()
  {
    return lastAutoUpdateLabName;
  }

  public void setLastAutoUpdateLabName(String lastAutoUpdateLabName)
  {
    this.lastAutoUpdateLabName = lastAutoUpdateLabName;
  }




  private Rectangle windowBounds = null;

  /**
   * Get the value of windowBounds
   *
   * @return the value of windowBounds
   */
  public Rectangle getWindowBounds()
  {
    return windowBounds;
  }

  /**
   * Set the value of windowBounds
   *
   * @param windowBounds new value of windowBounds
   */
  public void setWindowBounds(Rectangle windowBounds)
  {
    this.windowBounds = windowBounds;
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

  protected LinkedList<String> recentFiles = new LinkedList<String>();

  /**
   * Get the value of recentFiles
   *
   * @return the value of recentFiles
   */
  public LinkedList<String> getRecentFiles()
  {
    if (recentFiles == null)
    {
      recentFiles = new LinkedList<String>();
    }
    return recentFiles;
  }

  /**
   * Set the value of recentFiles
   *
   * @param recentFiles new value of recentFiles
   */
  public void setRecentFiles(LinkedList<String> recentFiles)
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
    for (Class<? extends LaserCutter> c : LibInfo.getSupportedDrivers())
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
  
  protected static String[] builtinImporters = new String[]{
    "de.thomas_oster.visicut.model.graphicelements.psvgsupport.ParametricSVGImporter",
    "de.thomas_oster.visicut.model.graphicelements.psvgsupport.PSVGImporter",
    "de.thomas_oster.visicut.model.graphicelements.svgsupport.SVGImporter",
    "de.thomas_oster.visicut.model.graphicelements.jpgpngsupport.JPGPNGImporter",
    "de.thomas_oster.visicut.model.graphicelements.dxfsupport.DXFImporter",
    "de.thomas_oster.visicut.model.graphicelements.epssupport.EPSImporter",
    "de.thomas_oster.visicut.model.graphicelements.lssupport.LaserScriptImporter",
    "de.thomas_oster.visicut.model.graphicelements.gcodesupport.GCodeImporter"
  };
  
  protected String[] availableImporters = new String[0];

  /**
   * Get the value of availableImporters
   *
   * @return the value of availableImporters
   */
  public String[] getAvailableImporters()
  {
    Set<String> allImporters = new LinkedHashSet<String>();
    allImporters.addAll(Arrays.asList(builtinImporters));
    allImporters.addAll(Arrays.asList(availableImporters));
    return allImporters.toArray(new String[0]);
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

  //Deprecated. Just not removed in order to keep old XML files working
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
  
  // unused, keep only for compatibility with old settings
  private String fabLabLocationFacebookId = "UNUSED";
  private String laserCutterTags = "UNUSED";
  private String supportedExtensions = "UNUSED";

  @Override
  public Preferences clone()
  {
    Preferences result = new Preferences();
    if (availableImporters != null)
    {
      result.availableImporters = new String[availableImporters.length];
      System.arraycopy(availableImporters, 0, result.availableImporters, 0, availableImporters.length);
    }
    if (availableLasercutterDrivers != null)
    {
      result.availableLasercutterDrivers = new String[availableLasercutterDrivers.length];
      System.arraycopy(availableLasercutterDrivers, 0, result.availableLasercutterDrivers, 0, availableLasercutterDrivers.length);
    }
    result.defaultMapping = defaultMapping;
    result.disableSandbox = disableSandbox;
    result.editSettingsBeforeExecuting = editSettingsBeforeExecuting;
    result.lastLaserDevice = lastLaserDevice;
    result.lastMaterial = lastMaterial;
    result.mkbitmapPath = mkbitmapPath;
    result.potracePath = potracePath;
    result.recentFiles = new LinkedList<String>();
    result.recentFiles.addAll(recentFiles);
    result.useThicknessAsFocusOffset = useThicknessAsFocusOffset;
    result.windowBounds = windowBounds;
    result.useFilenamesForJobs = useFilenamesForJobs;
    result.labName = labName;
    result.enableQRCodes = enableQRCodes;
    result.fastQRCodes = fastQRCodes;
    result.fabqrActive = fabqrActive;
    result.fabqrPrivateURL = fabqrPrivateURL;
    result.fabqrPublicURL = fabqrPublicURL;
    result.fabqrPrivateUser = fabqrPrivateUser;
    result.fabqrPrivatePassword = fabqrPrivatePassword;
    result.fabLabLocationFacebookId = fabLabLocationFacebookId;
    result.supportedExtensions = supportedExtensions;
    result.laserCutterTags = laserCutterTags;
    result.autoUpdateSettingsDisabled = autoUpdateSettingsDisabled;
    result.lastAutoUpdateLabName = lastAutoUpdateLabName;
    result.lastAutoUpdateTime = lastAutoUpdateTime;
    return result;
  }
}
