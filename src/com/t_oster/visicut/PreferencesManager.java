/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.visicut;

import com.t_oster.liblasercut.drivers.EpilogCutter;
import java.awt.geom.AffineTransform;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.net.MalformedURLException;
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
      preferences = this.loadPreferences(new File(".VisiCut/settings.xml"));
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
    preferences.laserCutter = new EpilogCutter("137.226.56.228");
    preferences.laserCutter.setName("Epilog ZING @ Fablab Aachen");
    preferences.availableImporters = new String[]
    {
      "com.t_oster.visicut.model.graphicelements.svgsupport.SVGImporter",
      "com.t_oster.visicut.model.graphicelements.jpgpngsupport.JPGPNGImporter"
    };
    try
    {
      preferences.backgroundImageURL = new File(".VisiCut/defaultbackground.jpg").toURI().toURL().toString();
    }
    catch (MalformedURLException ex)
    {
      Logger.getLogger(PreferencesManager.class.getName()).log(Level.SEVERE, null, ex);
    }
    preferences.camCalibration = new AffineTransform(0.04572009144018288,0.0,0.0,0.043691786621507196,53.0,115.0);
  }

  public Preferences getPreferences()
  {
    return preferences;
  }

  public void savePreferences() throws FileNotFoundException
  {
    if (new File(".VisiCut").isDirectory())
    {
      this.savePreferences(preferences, new File(".VisiCut/settings.xml"));
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
