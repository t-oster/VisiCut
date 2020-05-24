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

import de.thomas_oster.liblasercut.LaserProperty;
import de.thomas_oster.liblasercut.PowerSpeedFocusProperty;
import de.thomas_oster.visicut.misc.Helper;
import de.thomas_oster.visicut.model.LaserDevice;
import de.thomas_oster.visicut.model.LaserProfile;
import de.thomas_oster.visicut.model.MaterialProfile;
import de.thomas_oster.visicut.model.Raster3dProfile;
import de.thomas_oster.visicut.model.RasterProfile;
import de.thomas_oster.visicut.model.VectorProfile;
import com.thoughtworks.xstream.XStream;

import java.io.*;
import java.util.*;

/**
 * This class manages the available Material Profiles
 *
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class LaserPropertyManager
{

  private static LaserPropertyManager instance;

  public static LaserPropertyManager getInstance()
  {
    if (instance == null)
    {
      instance = new LaserPropertyManager();
    }
    return instance;
  }

  /*
   * Need a public constructior for UI manager
   * Do not use. Use getInstance instead
   */
  public LaserPropertyManager()
  {
    if (instance != null)
    {
      System.err.println("ProfileManager should not be instanciated directly");
    }
  }
  
  /**
   * Used to determine the path of propery files before VisiCut 1.5-99
   */
  private String oldToPathName(String name)
  {
    return name.replace("?", "_").replace("/", "_").replace("\\", "_");
  }
  
  /**
   * Used to determine the path of propery files before VisiCut 1.5-99
   */
  private File getOldLaserPropertiesFile(LaserDevice ld, MaterialProfile mp, LaserProfile lp, float materialThickness)
  {
    File laserprofiles = new File(Helper.getBasePath(), "laserprofiles");
    File deviceprofiles = new File(laserprofiles, oldToPathName(ld.getName()));
    File material = new File(new File(deviceprofiles, oldToPathName(mp.getName())), materialThickness + "mm");
    File profile = new File(material, oldToPathName(lp.getName()) + ".xml");
    return profile;
  }
  
  private File getLaserPropertiesFile(LaserDevice ld, MaterialProfile mp, LaserProfile lp, float materialThickness)
  {
    File laserprofiles = new File(Helper.getBasePath(), "laserprofiles");
    File deviceprofiles = new File(laserprofiles, Helper.toPathName(ld.getName()));
    File material = new File(new File(deviceprofiles, Helper.toPathName(mp.getName())), materialThickness + "mm");
    File profile = new File(material, Helper.toPathName(lp.getName()) + ".xml");
    return profile;
  }

  public List<LaserProperty> getLaserProperties(LaserDevice ld, MaterialProfile mp, LaserProfile lp, float materialThickness) throws FileNotFoundException, IOException
  {
    File f = getLaserPropertiesFile(ld, mp, lp, materialThickness);
    if (!f.exists() && getOldLaserPropertiesFile(ld, mp, lp, materialThickness).exists())
    {
      File old = getOldLaserPropertiesFile(ld, mp, lp, materialThickness);
      f.getParentFile().mkdirs();
      old.renameTo(f);
    }
    if (f.exists())
    {
      List<LaserProperty> result = this.loadProperties(f);
      if (result != null)
      {
        //check if it is still the correct type for the laser-cutter
        for (LaserProperty p : result)
        {
          LaserProperty expected = null;
          if (lp instanceof RasterProfile)
          {
            expected = ld.getLaserCutter().getLaserPropertyForRasterPart();
          }
          else if (lp instanceof VectorProfile)
          {
            expected = ld.getLaserCutter().getLaserPropertyForVectorPart();
          }
          else if (lp instanceof Raster3dProfile)
          {
            expected = ld.getLaserCutter().getLaserPropertyForRaster3dPart();
          }
          if (!expected.getClass().isAssignableFrom(p.getClass()))
          {
            System.err.println("Tried to load a laser-property of class " + p.getClass().toString() + ", but lasercutter expects " + expected.toString());
            System.err.println("Trying to copy most values");
            for (String k : p.getPropertyKeys())
            {
              try
              {
                expected.setProperty(k, p.getProperty(k));
              }
              catch (Exception e)
              {
                System.err.println("Could not transfer property: "+k);
              }
            }
            result.set(result.indexOf(p), expected);
          }
          // Some laser properties have a "hideFocus" setting which is really a machine property but unfortunately gets serialized into the material properties.
          // To get around this we copy the "hideFocus" value from the expected one from the machine over anything read from the config.
          if (expected instanceof PowerSpeedFocusProperty && p instanceof PowerSpeedFocusProperty) {
            ((PowerSpeedFocusProperty)p).setHideFocus(((PowerSpeedFocusProperty)expected).isHideFocus());
          }
        }
      }
      return result;
    }
    return null;
  }

  public void deleteLaserProperties(LaserDevice ld, MaterialProfile mp, LaserProfile lp, float materialThickness)
  {
    File f = getLaserPropertiesFile(ld, mp, lp, materialThickness);
    if (f.exists())
    {
      f.delete();
    }
  }
  private XStream xstream = null;

  protected XStream getXStream()
  {
    if (xstream == null)
    {
      xstream = new XStream();
      //fix old class references
      xstream.aliasPackage("com.t_oster", "de.thomas_oster");
      xstream.alias("LaosCutterProperty", de.thomas_oster.liblasercut.drivers.LaosCutterProperty.class);
      xstream.alias("FloatPowerSpeedFocusFrequencyProperty", de.thomas_oster.liblasercut.FloatPowerSpeedFocusFrequencyProperty.class);
      xstream.alias("PowerSpeedFocusFrequencyProperty", de.thomas_oster.liblasercut.PowerSpeedFocusFrequencyProperty.class);
      xstream.alias("PowerSpeedFocusProperty", de.thomas_oster.liblasercut.PowerSpeedFocusProperty.class);
    }
    return xstream;
  }

  public void saveLaserProperties(LaserDevice ld, MaterialProfile mp, LaserProfile lp, float materialThickness, List<LaserProperty> lps) throws FileNotFoundException, IOException
  {
    File f = getLaserPropertiesFile(ld, mp, lp, materialThickness);
    if (!f.getParentFile().exists())
    {
      f.getParentFile().mkdirs();
    }
    FilebasedManager.writeObjectToXmlFile(lps, f, getXStream());
  }

  public List<LaserProperty> loadProperties(File f) throws FileNotFoundException, IOException
  {
    try
    {
      return (List<LaserProperty>) FilebasedManager.readObjectFromXmlFile(f, getXStream());
    }
    catch (Exception e)
    {
      return null;
    }
  }
}
