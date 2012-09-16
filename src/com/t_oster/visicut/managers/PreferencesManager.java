/**
 * This file is part of VisiCut. Copyright (C) 2011 Thomas Oster
 * <thomas.oster@rwth-aachen.de> RWTH Aachen University - 52062 Aachen, Germany
 *
 * VisiCut is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * VisiCut is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with VisiCut. If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.t_oster.visicut.managers;

import com.t_oster.liblasercut.LaserCutter;
import com.t_oster.liblasercut.LibInfo;
import com.t_oster.liblasercut.drivers.EpilogZing;
import com.t_oster.liblasercut.drivers.LaosCutter;
import com.t_oster.visicut.Preferences;
import com.t_oster.visicut.misc.FileUtils;
import com.t_oster.visicut.misc.Helper;
import com.t_oster.visicut.model.LaserDevice;
import com.t_oster.visicut.model.MaterialProfile;
import com.t_oster.visicut.model.Raster3dProfile;
import com.t_oster.visicut.model.RasterProfile;
import com.t_oster.visicut.model.VectorProfile;
import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.beans.Encoder;
import java.beans.Expression;
import java.beans.PersistenceDelegate;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
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
  
  private PreferencesManager()
  {
  }

  private void generateDefault() throws FileNotFoundException
  {
    preferences = new Preferences();
    preferences.setAvailableImporters(new String[]
      {
        "com.t_oster.visicut.model.graphicelements.svgsupport.SVGImporter",
        "com.t_oster.visicut.model.graphicelements.jpgpngsupport.JPGPNGImporter",
        "com.t_oster.visicut.model.graphicelements.dxfsupport.DXFImporter",
        "com.t_oster.visicut.model.graphicelements.epssupport.EPSImporter"
      });
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
        LaserDeviceManager.getInstance().add(dev);
        if (preferences.getLastLaserDevice() == null)
        {
          preferences.setLastLaserDevice(dev);
          preferences.setLastResolution(lc.getResolutions().get(lc.getResolutions().size()-1));
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
    //generate default materials
    MaterialProfile mp = new MaterialProfile();
    mp.setName("Paper");
    mp.setColor(Color.WHITE);
    mp.setCutColor(Color.RED);
    mp.setEngraveColor(Color.DARK_GRAY);
    mp.setDepth(0.1f);
    MaterialManager.getInstance().add(mp);
    mp = new MaterialProfile();
    mp.setName("Acrylic");
    mp.setColor(Color.BLUE);
    mp.setCutColor(Color.RED);
    mp.setEngraveColor(Color.WHITE);
    mp.setDepth(2f);
    MaterialManager.getInstance().add(mp);
    preferences.setLastMaterial(mp);
    
    //generate default Profiles
    VectorProfile cut = new VectorProfile();
    cut.setName("cut");
    cut.setDescription("Cut through the material");
    cut.setIsCut(true);
    cut.setWidth(1f);
    ProfileManager.getInstance().addProfile(cut);
    VectorProfile mark = new VectorProfile();
    mark.setName("mark");
    mark.setDescription("Cut through the material");
    mark.setIsCut(true);
    mark.setWidth(1f);
    ProfileManager.getInstance().addProfile(mark);
    RasterProfile engrave = new RasterProfile();
    engrave.setName("engrave");
    ProfileManager.getInstance().addProfile(engrave);
    Raster3dProfile engrave3d = new Raster3dProfile();
    engrave3d.setName("engrave 3d");
    ProfileManager.getInstance().addProfile(engrave3d);
    
  }

  private void initializeSettingDirectory()
  {
    File bp = Helper.getBasePath();
    System.out.println("'" + bp.getAbsolutePath() + "' doesn't exist. We create it.");
    if (!bp.mkdirs())
    {
      System.err.println("Can't create directory: '" + bp.getAbsolutePath() + "'. VisiCut won't save any settings");
      return;
    }
    //Try to copy skeleton from VisiCut's program folder
    File vc = Helper.getVisiCutFolder();
    if (vc != null && vc.isDirectory())
    {
      for (String folder : new String[]{"examples", "profiles", "materials", "mappings", "devices", "laserprofiles"})
      {
      if (new File(vc, "examples").isDirectory())
        {
          try
          {
            System.out.println("Copying "+folder+"...");
            FileUtils.copyDirectoryToDirectory(new File(vc, folder), new File(bp, folder));
            System.out.println("done.");
          }
          catch (Exception ex)
          {
            Logger.getLogger(PreferencesManager.class.getName()).log(Level.SEVERE, null, ex);
            System.err.println("Can't copy default settings.");
          }
        }
      }
      if (new File(vc, "settings").isDirectory())
      {
        try
        {
          System.out.println("Copying default settings...");
          FileUtils.copyDirectoryToDirectory(new File(vc, "settings"), new File(bp, "settings"));
          System.out.println("done.");
          return;
        }
        catch (Exception ex)
        {
          Logger.getLogger(PreferencesManager.class.getName()).log(Level.SEVERE, null, ex);
          System.err.println("Can't copy default settings.");
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
    catch (FileNotFoundException ex)
    {
      Logger.getLogger(PreferencesManager.class.getName()).log(Level.SEVERE, null, ex);
      System.err.println("Couldn't save preferences");
    }
  }

  private List<String> exampleFilenames = null;
  public List<String> getExampleFilenames()
  {
    if (exampleFilenames == null)
    {
      exampleFilenames = new LinkedList<String>();
      File dir = new File(Helper.getBasePath(), "examples");
      if (dir.exists() && dir.isDirectory())
      {
        for(File f : dir.listFiles())
        {
          if (f.isFile())
          {
            exampleFilenames.add(f.getName());
          }
        }
      }
    }
    return exampleFilenames;
  }
  
  public File getExampleFile(String name)
  {
    return new File(new File(Helper.getBasePath(), "examples"), name);
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
        preferences = this.loadPreferences(new File(Helper.getBasePath(), "settings/settings.xml"));
      }
      catch (FileNotFoundException ex)
      {
        Logger.getLogger(PreferencesManager.class.getName()).log(Level.SEVERE, null, ex);
        System.err.println("Can't load settings. Using default ones...");
        try
        {
          this.generateDefault();
        }
        catch (FileNotFoundException ex1)
        {
          Logger.getLogger(PreferencesManager.class.getName()).log(Level.SEVERE, null, ex1);
        }
      }
    }
    return preferences;
  }

  public void savePreferences() throws FileNotFoundException
  {
    File settingsDir = new File(Helper.getBasePath(), "settings");
    if (settingsDir.isDirectory() || settingsDir.mkdirs())
    {
      this.savePreferences(preferences, new File(settingsDir, "settings.xml"));
    }
    else
    {
      throw new FileNotFoundException("Can't create settings dir.");
    }
  }
  
  public void savePreferences(Preferences pref, File f) throws FileNotFoundException
  {
    FileOutputStream os = new FileOutputStream(f);
    XMLEncoder encoder = new XMLEncoder(os);
    encoder.setPersistenceDelegate(AffineTransform.class, new PersistenceDelegate()
    {//Fix for older java versions
      protected Expression instantiate(Object oldInstance, Encoder out)
      {
        AffineTransform tx = (AffineTransform) oldInstance;
        double[] coeffs = new double[6];
        tx.getMatrix(coeffs);
        return new Expression(oldInstance,
          oldInstance.getClass(),
          "new",
          new Object[]
          {
            coeffs
          });
      }
    });
    encoder.writeObject(pref);
    encoder.close();
  }

  public Preferences loadPreferences(File f) throws FileNotFoundException
  {
    FileInputStream os = new FileInputStream(f);
    XMLDecoder decoder = new XMLDecoder(os);
    Preferences p = (Preferences) decoder.readObject();
    decoder.close();
    return p;
  }

  public void exportSettings(File file) throws FileNotFoundException, IOException
  {
    FileUtils.zipDirectory(Helper.getBasePath(), file);
  }

  public void importSettings(File file) throws ZipException, IOException
  {
    FileUtils.cleanDirectory(Helper.getBasePath());
    FileUtils.unzipToDirectory(file, Helper.getBasePath());
    this.exampleFilenames = null;
    this.preferences = null;
  }
}
