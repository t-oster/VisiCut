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

import com.t_oster.liblasercut.drivers.EpilogZing;
import com.t_oster.liblasercut.drivers.LaosCutter;
import com.t_oster.visicut.Preferences;
import com.t_oster.visicut.misc.FileUtils;
import com.t_oster.visicut.misc.Helper;
import com.t_oster.visicut.model.LaserDevice;
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
import java.util.LinkedList;
import java.util.List;
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
  
  private PreferencesManager()
  {
  }

  private void generateDefault()
  {
    preferences = new Preferences();
    preferences.setAvailableImporters(new String[]
      {
        "com.t_oster.visicut.model.graphicelements.svgsupport.SVGImporter",
        "com.t_oster.visicut.model.graphicelements.jpgpngsupport.JPGPNGImporter",
        "com.t_oster.visicut.model.graphicelements.dxfsupport.DXFImporter",
        "com.t_oster.visicut.model.graphicelements.epssupport.EPSImporter"
      });
    LaserDevice epilog = new LaserDevice();
    epilog.setLaserCutter(new EpilogZing("137.226.56.228"));
    epilog.setName("Epilog ZING");
    epilog.setMaterialsPath("settings/materials/epilog");
    epilog.setDescription("The Epilog ZING 30W Laser which is in the Fablab");
    //epilog.setCameraURL("http://137.226.56.115:8080/defaultbackground.jpg");
    //epilog.setThumbnailPath("settings/epilogcutter.png");
    epilog.setCameraCalibration(new AffineTransform(0.19630256844482077, 0.0, 0.0, 0.19954840530623766, 124.33334350585938, 484.3333282470703));
    preferences.setLaserDevices(new LinkedList<LaserDevice>());
    preferences.getLaserDevices().add(epilog);
    preferences.setLastLaserDevice(epilog);
    LaserDevice laos = new LaserDevice();
    laos.setLaserCutter(new LaosCutter());
    laos.setName("Laos HPC");
    laos.setMaterialsPath("settings/materials/laos");
    //laos.setThumbnailPath("settings/laoscutter.png");
    laos.setCameraCalibration(new AffineTransform(0.19630256844482077, 0.0, 0.0, 0.19954840530623766, 124.33334350585938, 484.3333282470703));
    preferences.getLaserDevices().add(laos);
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
      if (new File(vc, "examples").isDirectory())
      {
        try
        {
          System.out.println("Copying examples...");
          FileUtils.copyDirectoryToDirectory(new File(vc, "examples"), new File(bp, "examples"));
          System.out.println("done.");
        }
        catch (Exception ex)
        {
          Logger.getLogger(PreferencesManager.class.getName()).log(Level.SEVERE, null, ex);
          System.err.println("Can't copy default settings.");
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
    System.err.println("No default settings found. Generating some...");
    this.generateDefault();
    System.out.println("Saving generated settings...");
    try
    {
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
        this.generateDefault();
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
    for (LaserDevice ld : pref.getLaserDevices())
    {
      ld.setThumbnailPath(Helper.removeBasePath(ld.getThumbnailPath()));
      ld.setMaterialsPath(Helper.removeBasePath(ld.getMaterialsPath()));
    }
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
    for (LaserDevice ld : pref.getLaserDevices())
    {
      ld.setThumbnailPath(Helper.addBasePath(ld.getThumbnailPath()));
      ld.setMaterialsPath(Helper.addBasePath(ld.getMaterialsPath()));
    }
  }

  public Preferences loadPreferences(File f) throws FileNotFoundException
  {
    FileInputStream os = new FileInputStream(f);
    XMLDecoder decoder = new XMLDecoder(os);
    Preferences p = (Preferences) decoder.readObject();
    decoder.close();
    for (LaserDevice ld : p.getLaserDevices())
    {
      ld.setThumbnailPath(Helper.addBasePath(ld.getThumbnailPath()));
      ld.setMaterialsPath(Helper.addBasePath(ld.getMaterialsPath()));
    }
    return p;
  }
}
