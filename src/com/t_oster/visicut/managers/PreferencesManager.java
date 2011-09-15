/**
 * This file is part of VisiCut.
 * 
 *     VisiCut is free software: you can redistribute it and/or modify
 *     it under the terms of the Lesser GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *    VisiCut is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     Lesser GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with VisiCut.  If not, see <http://www.gnu.org/licenses/>.
 **/
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.visicut.managers;

import com.t_oster.liblasercut.drivers.EpilogCutter;
import com.t_oster.visicut.Preferences;
import com.t_oster.visicut.model.LaserDevice;
import java.awt.geom.AffineTransform;
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
 * @author thommy
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
    preferences.setAvailableLasercutterDrivers(new String[]{"com.t_oster.liblasercut.drivers.EpilogCutter"});
    LaserDevice epilog = new LaserDevice();
    epilog.setLaserCutter(new EpilogCutter("137.226.56.228"));
    epilog.setName("Epilog ZING @ Fablab");
    epilog.setMaterialsPath("settings/cutters/epilog/materials");
    epilog.setDescription("The Epilog ZING 30W Laser which is in the Fablab");
    epilog.setCameraURL("http://137.226.56.115:8080/defaultbackground.jpg");
    epilog.setThumbnailPath("settings/cutters/epilog/epilog.png");
    epilog.setCameraCalibration(new AffineTransform(0.19630256844482077,0.0,0.0,0.19954840530623766,124.33334350585938,484.3333282470703));
    preferences.setLaserDevices(new LinkedList<LaserDevice>());
    preferences.getLaserDevices().add(epilog);
    preferences.setDefaultLaserDevice(0);
    LaserDevice trotec = new LaserDevice();
    trotec.setLaserCutter(new EpilogCutter("137.226.56.228"));
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
