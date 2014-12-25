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
package com.t_oster.visicut.managers;

import com.t_oster.liblasercut.LaserCutter;
import com.t_oster.liblasercut.LibInfo;
import com.t_oster.visicut.Preferences;
import com.t_oster.visicut.misc.FileUtils;
import com.t_oster.visicut.misc.Helper;
import com.t_oster.visicut.model.LaserDevice;
import com.t_oster.visicut.model.MaterialProfile;
import com.t_oster.visicut.model.Raster3dProfile;
import com.t_oster.visicut.model.RasterProfile;
import com.t_oster.visicut.model.VectorProfile;
import com.thoughtworks.xstream.XStream;
import java.awt.Color;
import java.beans.XMLDecoder;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipException;

/**
 *
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public final class PreferencesManager
{

  private static PreferencesManager instance;

  public static PreferencesManager getInstance()
  {
    if (instance == null)
    {
      instance = new PreferencesManager();
    }
    return instance;
  }
  private Preferences preferences;
  
  private File getPreferencesPath()
  {
    return new File(new File(Helper.getBasePath(), "settings"), "settings.xml");
  }
  
  private PreferencesManager()
  {
  }

  private void generateDefault() throws FileNotFoundException, IOException
  {
    preferences = new Preferences();
    if (LaserDeviceManager.getInstance().getAll().isEmpty())
    {
      //Create a Laserdevice for each known driver
      for (Class laserdriver : LibInfo.getSupportedDrivers())
      {
        try
        {
          LaserDevice dev = new LaserDevice();
          LaserCutter lc = (LaserCutter) laserdriver.newInstance();
          dev.setLaserCutter(lc);
          dev.setName(lc.getModelName());
          dev.setThumbnailPath(new File(Helper.getBasePath(), "devices/"+lc.getModelName()+".png").getAbsolutePath());
          try
          {
            LaserDeviceManager.getInstance().add(dev);
          }
          catch (IOException ex)
          {
            Logger.getLogger(PreferencesManager.class.getName()).log(Level.SEVERE, null, ex);
          }
          if (preferences.getLastLaserDevice() == null)
          {
            preferences.setLastLaserDevice(dev.getName());
          }
        }
        catch (InstantiationException ex)
        {
          Logger.getLogger(PreferencesManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        catch (IllegalAccessException ex)
        {
          Logger.getLogger(PreferencesManager.class.getName()).log(Level.SEVERE, null, ex);
        }
      }
    }
    //generate default materials
    //only if no materials found
    if (MaterialManager.getInstance().getAll().isEmpty())
    {
      MaterialProfile mp = new MaterialProfile();
      mp.setName("Paper");
      mp.setColor(Color.WHITE);
      mp.setCutColor(Color.RED);
      mp.setEngraveColor(Color.DARK_GRAY);
      MaterialManager.getInstance().add(mp);
      mp = new MaterialProfile();
      mp.setName("Acrylic");
      mp.setColor(Color.BLUE);
      mp.setCutColor(Color.RED);
      mp.setEngraveColor(Color.WHITE);
      MaterialManager.getInstance().add(mp);
      preferences.setLastMaterial(mp.getName());
    }
    
    //generate default Profiles
    //only if none found
    if (ProfileManager.getInstance().getAll().isEmpty())
    {
      VectorProfile cut = new VectorProfile();
      cut.setName("cut");
      cut.setDescription("Cut through the material");
      cut.setIsCut(true);
      cut.setWidth(1f);
      ProfileManager.getInstance().add(cut);
      VectorProfile mark = new VectorProfile();
      mark.setName("mark");
      mark.setDescription("Cut through the material");
      mark.setIsCut(true);
      mark.setWidth(1f);
      ProfileManager.getInstance().add(mark);
      RasterProfile engrave = new RasterProfile();
      engrave.setName("engrave");
      ProfileManager.getInstance().add(engrave);
      Raster3dProfile engrave3d = new Raster3dProfile();
      engrave3d.setName("engrave 3d");
      ProfileManager.getInstance().add(engrave3d);
    }
  }

  private void initializeSettingDirectory() {
    this.initializeSettingDirectory(false);
  }
  
  /**
   * create the settings directory
   * @param generateDefaults : fill it with demo settings
   */
  private void initializeSettingDirectory(boolean generateDefaults)
  {
    File bp = Helper.getBasePath();
    System.out.println("'" + bp.getAbsolutePath() + "' doesn't exist. We create it.");
    if (!bp.mkdirs())
    {
      System.err.println("Can't create directory: '" + bp.getAbsolutePath() + "'. VisiCut won't save any settings");
      return;
    }
    
    if (!generateDefaults) {
      try
      {
        this.savePreferences();
      }
      catch (Exception ex)
      {
        Logger.getLogger(PreferencesManager.class.getName()).log(Level.SEVERE, null, ex);
      }
      return;
    }
    //Try to copy skeleton from VisiCut's program folder
    File vc = Helper.getVisiCutFolder();
    if (vc != null && vc.isDirectory())
    {
      for (String folder : new String[]{"profiles", "materials", "mappings", "devices", "laserprofiles"})
      {
        if (new File(vc, folder).isDirectory())
        {
          try
          {
            System.out.println("Copying default settings...");
            FileUtils.copyDirectoryToDirectory(new File(vc, folder), new File(bp, folder));
            System.out.println("done.");
            return;
          }
          catch (Exception ex)
          {
            Logger.getLogger(PreferencesManager.class.getName()).log(Level.SEVERE, null, ex);
            System.err.println("Can't copy default settings.");
          }
        } else {
          System.err.println("cannot find default settings folder for "+folder);
        }
      }
    }
    try
    {
      System.err.println("No default settings found. Generating some...");
      this.generateDefault();
      System.out.println("Saving generated settings...");
      this.savePreferences();
    }
    catch (Exception ex)
    {
      Logger.getLogger(PreferencesManager.class.getName()).log(Level.SEVERE, null, ex);
      System.err.println("Couldn't save preferences");
    }
  }
  private Map<String, Object> listDirectory(File dir)
  {
    Map<String, Object> result = new LinkedHashMap<String, Object>();
    if (dir.exists() && dir.isDirectory())
    {
      for(File f : dir.listFiles())
      {
        if (f.isFile())
        {
          result.put(f.getName(), f);
        }
        else if (f.isDirectory())
        {
          result.put(f.getName(), this.listDirectory(f));
        }
      }
    }
    return result;
  }
  
  /*
   * A Map either containing a name and a file each
   * or a name and another list of the same form
   * (aka the example files tree)
   */
  private Map<String, Object> exampleFiles = null;
  public Map<String, Object> getExampleFiles()
  {
    if (exampleFiles == null)
    {
      exampleFiles = this.listDirectory(
        new File(Helper.getBasePath(), "examples")
        );
    }
    return exampleFiles;
  }
  
  private Map<String, Object> builtinExampleFiles = null;
  public Map<String, Object> getBuiltinExampleFiles()
  {
    if (builtinExampleFiles == null)
    {
      builtinExampleFiles = this.listDirectory(
        new File(Helper.getVisiCutFolder(), "examples"));
    }
    return builtinExampleFiles;
  }
  
  public Preferences getPreferences()
  {
    if (preferences == null)
    {
      File bp = Helper.getBasePath();
      if (!bp.exists())
      {
        this.initializeSettingDirectory();
      }
      try
      {
        preferences = this.loadPreferences(this.getPreferencesPath());
      }
      catch (Exception ex)
      {
        System.err.println("Can't load settings.");
      }
      if (preferences == null)
      {
        preferences = new Preferences();
      }
    }
    return preferences;
  }

  public void savePreferences(Preferences pref) throws FileNotFoundException, IOException
  {
    this.preferences = pref;
    savePreferences();
  }
  
  public void savePreferences() throws FileNotFoundException, IOException
  {
    File target = this.getPreferencesPath();
    File settingsDir = target.getParentFile();
    if (settingsDir.isDirectory() || settingsDir.mkdirs())
    {
      this.savePreferences(preferences, target);
    }
    else
    {
      throw new FileNotFoundException("Can't create settings dir.");
    }
  }
  
  public void savePreferences(Preferences pref, File f) throws FileNotFoundException, IOException
  {
    FileOutputStream os = new FileOutputStream(f);
    getXStream().toXML(pref, os);
    os.close();
  }

  private XStream xstream = null;
  
  protected XStream getXStream()
  {
    if (xstream == null)
    {
      xstream = new XStream();
      xstream.alias("visicutPreferences", Preferences.class);
    }
    return xstream;
  }
  
  public Preferences loadPreferences(File f) throws FileNotFoundException
  {
    try
    {
      return (Preferences) getXStream().fromXML(f);
    }
    catch (Exception e)
    {
      FileInputStream os = new FileInputStream(f);
      XMLDecoder decoder = new XMLDecoder(os);
      Preferences p = (Preferences) decoder.readObject();
      decoder.close();
      return p;
    }
  }

  public void exportSettings(File file) throws FileNotFoundException, IOException
  {
    FileUtils.zipDirectory(Helper.getBasePath(), file);
  }

  /**
   * import settings from file created by exportSettings
   * @param file, or null to load the example settings, or File("__EMPTY__") to delete everything
   * @throws Exception when the file is invalid
   */
  public void importSettings(File file) throws Exception
  {
    try
    {
      FileUtils.cleanDirectory(Helper.getBasePath());
      this.exampleFiles = null;
      if (file == null || "__EMPTY__".equals(file.getName())) {
        Helper.getBasePath().delete();
        LaserDeviceManager.getInstance().reload();
        MappingManager.getInstance().reload();
        MaterialManager.getInstance().reload();
        ProfileManager.getInstance().reload();
        if (file==null) {
          // file==null: load example settings (generateDefault=true)
          this.initializeSettingDirectory(true);
        } else {
          // file==__EMPTY__: clear settings (generateDefault=false)
          this.initializeSettingDirectory(false);
        }
        this.preferences = new Preferences();
      } else {
        FileUtils.unzipSettingsToDirectory(file, Helper.getBasePath());
      }
      this.preferences = this.loadPreferences(this.getPreferencesPath());
    }
    catch (Exception e)
    {
      this.preferences = new Preferences();
      throw new Exception("Error importing settings",e);
    }
    finally {
      LaserDeviceManager.getInstance().reload();
      MappingManager.getInstance().reload();
      MaterialManager.getInstance().reload();
      ProfileManager.getInstance().reload();
    }
  }
}
