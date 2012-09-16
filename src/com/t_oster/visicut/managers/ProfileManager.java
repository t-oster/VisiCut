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
import com.t_oster.visicut.model.LaserProfile;
import com.t_oster.visicut.model.MaterialProfile;
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
  
  protected Map<String, LaserProfile> profiles;
  
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
  }

  private List<LaserProfile> loadFromDirectory(File dir)
  {
    List<LaserProfile> result = new LinkedList<LaserProfile>();
    if (dir.isDirectory())
    {
      for (File f : dir.listFiles())
      {
        if (f.isFile() && f.getAbsolutePath().toLowerCase().endsWith(".xml"))
        {
          try
          {
            LaserProfile prof = this.loadProfile(f);
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

  private File getProfilePath(LaserProfile mp)
  {
    return new File(this.getProfilesDircetory(), Helper.toPathName(mp.getName())+".xml");
  }
  
  public void removeProfile(LaserProfile mp)
  {
    this.getMap().remove(mp.getName());
    this.deleteProfile(mp);
  }
  
  private void deleteProfile(LaserProfile mp)
  {
    File f = getProfilePath(mp);
    if (f.exists())
    {
      f.delete();
    }
  }
  
  public void addProfile(LaserProfile mp) throws FileNotFoundException
  {
    this.saveProfile(mp, this.getProfilePath(mp));
    this.getMap().put(mp.getName(), mp);
  }
  
  public void saveProfile(LaserProfile mp, File f) throws FileNotFoundException
  {
    if (!f.getParentFile().exists())
    {
      f.getParentFile().mkdirs();
    }
    mp.setThumbnailPath(Helper.removeParentPath(f.getParentFile(), mp.getThumbnailPath()));
    FileOutputStream out = new FileOutputStream(f);
    XMLEncoder enc = new XMLEncoder(out);
    enc.writeObject(mp);
    enc.close();
    mp.setThumbnailPath(Helper.addParentPath(f.getParentFile(), mp.getThumbnailPath()));
  }
  
  public LaserProfile loadProfile(File f) throws FileNotFoundException, IOException
  {
    FileInputStream fin = new FileInputStream(f);
    LaserProfile result = this.loadProfile(fin);
    result.setThumbnailPath(Helper.addParentPath(f.getParentFile(), result.getThumbnailPath()));
    fin.close();
    return result;
  }

  public LaserProfile loadProfile(InputStream in)
  {
    XMLDecoder dec = new XMLDecoder(in);
    LaserProfile result = (LaserProfile) dec.readObject();
    return result;
  }

  private Map<String, LaserProfile> getMap()
  {
    if (profiles == null)
    {
      profiles = new LinkedHashMap<String, LaserProfile>();
      for(LaserProfile lp:this.loadFromDirectory(this.getProfilesDircetory()))
      {
        profiles.put(lp.getName(), lp);
      }
    }
    return profiles;
  }
  
  /**
   * Get the value of materials
   *
   * @return the value of materials
   */
  public Collection<LaserProfile> getProfiles()
  {
    return getMap().values();
  }

  public LaserProfile getProfileByName(String profileName)
  {
    return getMap().get(profileName);
  }

  private File getProfilesDircetory()
  {
    return new File(Helper.getBasePath(), "profiles");
  }

  public void setProfiles(List<LaserProfile> result) throws FileNotFoundException
  {
    for (LaserProfile lp:this.getProfiles().toArray(new LaserProfile[0]))
    {
      this.removeProfile(lp);
    }
    for (LaserProfile lp:result)
    {
      this.addProfile(lp);
    }
  }

}
