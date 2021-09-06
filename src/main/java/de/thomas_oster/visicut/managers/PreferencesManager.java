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
package de.thomas_oster.visicut.managers;

import com.thoughtworks.xstream.XStream;
import de.thomas_oster.liblasercut.LaserCutter;
import de.thomas_oster.liblasercut.LibInfo;
import de.thomas_oster.visicut.Preferences;
import de.thomas_oster.visicut.misc.FileUtils;
import de.thomas_oster.visicut.misc.Helper;
import de.thomas_oster.visicut.model.LaserDevice;
import de.thomas_oster.visicut.model.MaterialProfile;
import de.thomas_oster.visicut.model.Raster3dProfile;
import de.thomas_oster.visicut.model.RasterProfile;
import de.thomas_oster.visicut.model.VectorProfile;

import java.awt.Color;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

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
  
  /**
   * Get path of settings.xml, where all settings except some user-specific data (recent files, window size, etc.) are stored
   * @see getPrivatePreferencesPath()
   * @return File handle
   */
  private File getPreferencesPath()
  {
    return new File(new File(Helper.getBasePath(), "settings"), "settings.xml");
  }
  
  /**
   * Get path of settings.private.xml, where all settings including user-specific data (recent files, window size, etc.) are stored
   * @return File handle
   */
  private File getPrivatePreferencesPath()
  {
    return new File(new File(Helper.getBasePath(), "settings"), "settings.private.xml");
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
      for (Class<? extends LaserCutter> laserdriver : LibInfo.getSupportedDrivers())
      {
        try
        {
          LaserDevice dev = new LaserDevice();
          LaserCutter lc = laserdriver.newInstance();
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
      mp.setMaterialThicknesses(new LinkedList<>(Collections.singletonList(1f)));
      MaterialManager.getInstance().add(mp);
      mp = new MaterialProfile();
      mp.setName("Acrylic");
      mp.setColor(Color.BLUE);
      mp.setCutColor(Color.RED);
      mp.setEngraveColor(Color.WHITE);
      mp.setMaterialThicknesses(new LinkedList<>(Collections.singletonList(1f)));
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
      System.err.println("Generating some defaults if no examples exist...");
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
        // if it exists, load the local 'preferences.private.xml'. This file may be removed to remove sensitive information, e.g., recent files
          if (this.getPrivatePreferencesPath().lastModified() < this.getPreferencesPath().lastModified()) {
            System.err.println("Notice: settings.private.xml is older than settings.xml; deleting settings.private.xml.");
            this.getPrivatePreferencesPath().delete();
        }
        preferences = this.loadPreferences(this.getPrivatePreferencesPath());
      }
      catch (Exception ex)
      {
        System.err.println("Notice: Can't load settings.private.xml - loading settings.xml instead. This may be ignored. It is normal if the settings were imported from elsewhere or settings.private.xml file was deleted or settings.xml is newer than settings.private.xml.");
        try
        {
          // if for any reason preferences.private.xml cannot be loaded,
          // fall back to the public (anonymized) 'preferences.xml' file.

          preferences = this.loadPreferences(this.getPreferencesPath());
        }
        catch (Exception exx)
        {
          System.err.println("Error: Can't load settings. Something is wrong with the settings file, or it is missing.");
        }
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
      // save slightly anonymized file (settings.xml) with some information removed
      Preferences anonymizedPreferences = preferences.clone();
      anonymizedPreferences.anonymize();
      this.savePreferences(anonymizedPreferences, target);

      // save full preferences file (settings.private.xml), including sensitive information (e.g., recent files)
      // Note that this must be after saving the first file so that the modification date is newer.
      this.savePreferences(preferences, getPrivatePreferencesPath());
    }
    else
    {
      throw new FileNotFoundException("Can't create settings dir.");
    }
  }
  
  public void savePreferences(Preferences pref, File f) throws FileNotFoundException, IOException
  {
    FilebasedManager.writeObjectToXmlFile(pref, f, getXStream());
  }

  private XStream xstream = null;
  
  protected XStream getXStream()
  {
    if (xstream == null)
    {
      xstream = new VisiCutXStream();
      xstream.alias("visicutPreferences", Preferences.class);
    }
    return xstream;
  }
  
  public Preferences loadPreferences(File f) throws FileNotFoundException
  {
    return (Preferences) FilebasedManager.readObjectFromXmlFile(f, getXStream());
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
