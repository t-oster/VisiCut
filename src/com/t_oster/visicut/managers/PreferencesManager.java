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
package com.t_oster.visicut.managers;

import com.t_oster.liblasercut.drivers.EpilogHelix;
import com.t_oster.liblasercut.drivers.EpilogZing;
import com.t_oster.visicut.Preferences;
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
    try
    {
      preferences = this.loadPreferences(new File("settings/settings.xml"));
    }
    catch (FileNotFoundException ex)
    {
      this.generateDefault();
      try
      {
        this.savePreferences();
      }
      catch (FileNotFoundException ex1)
      {
        Logger.getLogger(PreferencesManager.class.getName()).log(Level.SEVERE, null, ex1);
      }
    }
  }

  private void generateDefault()
  {
    preferences = new Preferences();
    preferences.setAvailableImporters( new String[]
    {
      "com.t_oster.visicut.model.graphicelements.svgsupport.SVGImporter",
      "com.t_oster.visicut.model.graphicelements.jpgpngsupport.JPGPNGImporter",
      "com.t_oster.visicut.model.graphicelements.dxfsupport.DXFImporter",
      "com.t_oster.visicut.model.graphicelements.epssupport.EPSImporter"
    });
    preferences.setAvailableLasercutterDrivers(new String[]{});
    LaserDevice epilog = new LaserDevice();
    epilog.setLaserCutter(new EpilogZing("137.226.56.228"));
    epilog.setName("Epilog ZING @ Fablab");
    epilog.setMaterialsPath("cutters/epilog/materials");
    epilog.setDescription("The Epilog ZING 30W Laser which is in the Fablab");
    epilog.setCameraURL("http://137.226.56.115:8080/defaultbackground.jpg");
    epilog.setThumbnailPath("cutters/epilog/epilog.png");
    epilog.setCameraCalibration(new AffineTransform(0.19630256844482077,0.0,0.0,0.19954840530623766,124.33334350585938,484.3333282470703));
    preferences.setLaserDevices(new LinkedList<LaserDevice>());
    preferences.getLaserDevices().add(epilog);
    preferences.setLastLaserDevice(epilog);
    LaserDevice legend = new LaserDevice();
    legend.setLaserCutter(new EpilogHelix("137.226.56.228"));
    legend.setName("Epilog HELIX");
    legend.setMaterialsPath("settings/cutters/trotec/materials");
    legend.setDescription("The Trotec SP 1500 Laser which is not in the Fablab");
    legend.setCameraURL("http://137.226.56.115:8080/defaultbackground.jpg");
    legend.setThumbnailPath("settings/cutters/trotec/trotec.png");
    legend.setCameraCalibration(new AffineTransform(0.19630256844482077,0.0,0.0,0.19954840530623766,124.33334350585938,484.3333282470703));
    preferences.getLaserDevices().add(legend);
  }

  public Preferences getPreferences()
  {
    return preferences;
  }

  public void savePreferences() throws FileNotFoundException
  {
    if (new File("settings").isDirectory())
    {
      this.savePreferences(preferences, new File("settings/settings.xml"));
    }
  }

  public void savePreferences(Preferences pref, File f) throws FileNotFoundException
  {
    for(LaserDevice ld:pref.getLaserDevices())
    {
      ld.setThumbnailPath(Helper.getRelativePath(f, ld.getThumbnailPath()));
      ld.setMaterialsPath(Helper.getRelativePath(f, ld.getMaterialsPath()));
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
    for(LaserDevice ld:pref.getLaserDevices())
    {
      ld.setThumbnailPath(Helper.getAbsolutePath(f, ld.getThumbnailPath()));
      ld.setMaterialsPath(Helper.getAbsolutePath(f, ld.getMaterialsPath()));
    }
  }

  public Preferences loadPreferences(File f) throws FileNotFoundException
  {
    FileInputStream os = new FileInputStream(f);
    XMLDecoder decoder = new XMLDecoder(os);
    Preferences p = (Preferences) decoder.readObject();
    decoder.close();
    for(LaserDevice ld:p.getLaserDevices())
    {
      ld.setThumbnailPath(Helper.getAbsolutePath(f, ld.getThumbnailPath()));
      ld.setMaterialsPath(Helper.getAbsolutePath(f, ld.getMaterialsPath()));
    }
    return p;
  }
}
