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
    // Also clear deprecated/unused preferences variables
    labName = null;
    enableQRCodes = false;
    fastQRCodes = false;
    fabqrActive = false;
    fabqrPrivateURL = null;
    fabqrPublicURL = null;
    fabqrPrivateUser = null;
    fabqrPrivatePassword = null;
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
  
  
  // Preferences from the removed FabQR feature. Keep here for compatibility with old preferences files.
  @Deprecated private transient String labName = null;
  @Deprecated private transient boolean enableQRCodes = false;
  @Deprecated private transient boolean fastQRCodes = false;
  @Deprecated private transient boolean fabqrActive = false;
  @Deprecated private transient String fabqrPrivateURL = "";
  @Deprecated private transient String fabqrPublicURL = "";
  @Deprecated private transient String fabqrPrivateUser = "";
  @Deprecated private transient String fabqrPrivatePassword = "";
  
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

  // Deprecated. Just not removed in order to keep old XML files working
  @Deprecated protected transient String[] availableLasercutterDrivers = null;

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
    return result.toArray(new String[0]);
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

  //Deprecated. Just not removed in order to keep old XML files working
  @Deprecated protected transient String[] availableImporters = new String[0];

  /**
   * Get the list of available importers
   *
   * @return class names of available importers
   */
  public String[] getAvailableImporters()
  {
    Set<String> allImporters = new LinkedHashSet<String>();
    allImporters.addAll(Arrays.asList(builtinImporters));
    return allImporters.toArray(new String[0]);
  }

  //Deprecated. Just not removed in order to keep old XML files working
  @Deprecated private transient boolean editSettingsBeforeExecuting = false;
  @Deprecated private transient String fabLabLocationFacebookId = "UNUSED";
  @Deprecated private transient String laserCutterTags = "UNUSED";
  @Deprecated private transient String supportedExtensions = "UNUSED";

  @Override
  public Preferences clone()
  {
    Preferences result = new Preferences();
    result.defaultMapping = defaultMapping;
    result.disableSandbox = disableSandbox;
    result.lastLaserDevice = lastLaserDevice;
    result.lastMaterial = lastMaterial;
    result.mkbitmapPath = mkbitmapPath;
    result.potracePath = potracePath;
    result.recentFiles = new LinkedList<String>();
    result.recentFiles.addAll(getRecentFiles());
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
    result.autoUpdateSettingsDisabled = autoUpdateSettingsDisabled;
    result.lastAutoUpdateLabName = lastAutoUpdateLabName;
    result.lastAutoUpdateTime = lastAutoUpdateTime;
    return result;
  }
}
