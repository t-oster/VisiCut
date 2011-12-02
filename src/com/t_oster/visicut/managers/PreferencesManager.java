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

import com.t_oster.liblasercut.drivers.EpilogCutter;
import com.t_oster.visicut.Preferences;
import com.t_oster.visicut.model.LaserDevice;
import com.t_oster.visicut.model.CONSTANT; 
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
    preferences.setAvailableLasercutterDrivers(new String[]{
      "com.t_oster.liblasercut.drivers.EpilogCutter",
      "com.t_oster.liblasercut.drivers.LaosCutter"
    });
    LaserDevice epilog = new LaserDevice();
    epilog.setLaserCutter(new EpilogCutter(CONSTANT.PROP_CUTTER_STATICIP));
    epilog.setName("Epilog ZING @ Fablab");
    epilog.setMaterialsPath("settings/cutters/epilog/materials");
    epilog.setDescription("The Epilog ZING 30W Laser which is in the Fablab");
    epilog.setCameraURL("http://137.226.56.115:8080/defaultbackground.jpg");
    epilog.setThumbnailPath("settings/cutters/epilog/epilog.png");
    // the following calls are now in the constructor of the EpilogCutter
    //epilog.setDpi(500);
    //epilog.setHostname(CONSTANT.PROP_CUTTER_STATICIP);
    //epilog.setPort(515);
    //epilog.setModel("ZING");
    epilog.setCameraCalibration(new AffineTransform(0.19630256844482077,0.0,0.0,0.19954840530623766,124.33334350585938,484.3333282470703));
    preferences.setLaserDevices(new LinkedList<LaserDevice>());
    preferences.getLaserDevices().add(epilog);
    preferences.setDefaultLaserDevice(0);
    LaserDevice trotec = new LaserDevice();
    trotec.setLaserCutter(new EpilogCutter(CONSTANT.PROP_CUTTER_STATICIP));
    trotec.setName("Trotec SP 1500 !@ Fablab");
    trotec.setMaterialsPath("settings/cutters/trotec/materials");
    trotec.setDescription("The Trotec SP 1500 Laser which is not in the Fablab");
    trotec.setCameraURL("http://137.226.56.115:8080/defaultbackground.jpg");
    trotec.setThumbnailPath("settings/cutters/trotec/trotec.png");
    trotec.setCameraCalibration(new AffineTransform(0.19630256844482077,0.0,0.0,0.19954840530623766,124.33334350585938,484.3333282470703));
    preferences.getLaserDevices().add(trotec);
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
}
