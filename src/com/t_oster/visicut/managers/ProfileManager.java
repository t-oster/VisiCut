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

import com.t_oster.visicut.misc.Helper;
import com.t_oster.visicut.model.LaserDevice;
import com.t_oster.visicut.model.LaserProfile;
import com.t_oster.visicut.model.MaterialProfile;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class manages the available Material Profiles
 * 
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class ProfileManager
{

  private static ProfileManager instance;
  
  public static ProfileManager getInstance()
  {
    if (instance == null)
    {
      instance = new ProfileManager();
    }
    return instance;
  }
  
  protected List<MaterialProfile> materials;
  public static final String PROP_MATERIALS = "materials";
  private File dir;

  /*
   * Need a public constructior for UI manager
   * Do not use. Use getInstance instead
   */
  public ProfileManager()
  {
    if (instance != null)
    {
      System.err.println("ProfileManager should not be instanciated directly");
    }
    this.materials = new LinkedList<MaterialProfile>();
  }

  public void loadMaterials(LaserDevice l)
  {
    this.materials = this.getMaterials(l);
  }

  private List<MaterialProfile> loadFromDirectory(File dir)
  {
    List<MaterialProfile> result = new LinkedList<MaterialProfile>();
    if (dir.isDirectory())
    {
      for (File f : dir.listFiles())
      {
        if (f.isFile() && f.getAbsolutePath().toLowerCase().endsWith(".xml"))
        {
          try
          {
            MaterialProfile prof = this.loadProfile(f);
            result.add(prof);
          }
          catch (Exception ex)
          {
            Logger.getLogger(ProfileManager.class.getName()).log(Level.SEVERE, null, ex);
          }
        }
      }
    }
    return result;
  }

  public void saveProfile(MaterialProfile mp, File f) throws FileNotFoundException
  {
    mp.setThumbnailPath(Helper.removeBasePath(mp.getThumbnailPath()));
    for (LaserProfile lp:mp.getLaserProfiles())
    {
      lp.setThumbnailPath(Helper.removeBasePath(lp.getThumbnailPath()));
    }
    FileOutputStream out = new FileOutputStream(f);
    XMLEncoder enc = new XMLEncoder(out);
    enc.writeObject(mp);
    enc.close();
    mp.setThumbnailPath(Helper.addBasePath(mp.getThumbnailPath()));
    for (LaserProfile lp:mp.getLaserProfiles())
    {
      lp.setThumbnailPath(Helper.addBasePath(lp.getThumbnailPath()));
    }
  }
  
  public MaterialProfile loadProfile(File f) throws FileNotFoundException, IOException
  {
    FileInputStream fin = new FileInputStream(f);
    MaterialProfile result = this.loadProfile(fin);
    fin.close();
    result.setThumbnailPath(Helper.addBasePath(result.getThumbnailPath()));
    for (LaserProfile lp:result.getLaserProfiles())
    {
      lp.setThumbnailPath(Helper.addBasePath(lp.getThumbnailPath()));
    }
    return result;
  }

  public MaterialProfile loadProfile(InputStream in)
  {
    XMLDecoder dec = new XMLDecoder(in);
    MaterialProfile result = (MaterialProfile) dec.readObject();
    return result;
  }

  /**
   * Get the value of materials
   *
   * @return the value of materials
   */
  public List<MaterialProfile> getMaterials()
  {
    return materials;
  }

  /**
   * Set the value of materials
   *
   * @param materials new value of materials
   */
  public void setMaterials(List<MaterialProfile> materials)
  {
    List<MaterialProfile> oldMaterials = this.materials;
    this.materials = materials;
    propertyChangeSupport.firePropertyChange(PROP_MATERIALS, oldMaterials, materials);
  }
  private PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

  /**
   * Add PropertyChangeListener.
   *
   * @param listener
   */
  public void addPropertyChangeListener(PropertyChangeListener listener)
  {
    propertyChangeSupport.addPropertyChangeListener(listener);
  }

  /**
   * Remove PropertyChangeListener.
   *
   * @param listener
   */
  public void removePropertyChangeListener(PropertyChangeListener listener)
  {
    propertyChangeSupport.removePropertyChangeListener(listener);
  }

  /**
   * Saves the Material for the given LaserDevice
   * @param result
   * @param selectedLaserDevice 
   */
  public void saveProfile(MaterialProfile result, LaserDevice selectedLaserDevice) throws FileNotFoundException
  {
    if (materialsCache.containsKey(selectedLaserDevice))
    {
      materialsCache.get(selectedLaserDevice).add(result);
    }
    this.saveProfile(result, new File(selectedLaserDevice.getMaterialsPath(), result.toString() + ".xml"));
  }

  public void deleteProfile(MaterialProfile m, LaserDevice selectedLaserDevice)
  {
    if (materialsCache.containsKey(selectedLaserDevice))
    {
      materialsCache.get(selectedLaserDevice).remove(m);
    }
    new File(selectedLaserDevice.getMaterialsPath(), m.toString() + ".xml").delete();
  }

  private Map<LaserDevice,List<MaterialProfile>> materialsCache = new LinkedHashMap<LaserDevice,List<MaterialProfile>>();
  /**
   * Returns all Materials this Lasercutter supports
   * (without loading anything)
   * @param ld
   * @return 
   */
  public List<MaterialProfile> getMaterials(LaserDevice ld)
  {
    if (this.materialsCache.containsKey(ld))
    {
      return this.materialsCache.get(ld);
    }
    this.dir = ld.getMaterialsPath() != null ? new File(ld.getMaterialsPath()) : new File(Helper.getBasePath(), "settings/materials");
    List<MaterialProfile> result = this.loadFromDirectory(dir);
    Collections.sort(result);
    this.materialsCache.put(ld, result);
    return result;
  }
}
