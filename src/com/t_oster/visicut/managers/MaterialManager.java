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
public class MaterialManager
{

  private static MaterialManager instance;
  
  public static MaterialManager getInstance()
  {
    if (instance == null)
    {
      instance = new MaterialManager();
    }
    return instance;
  }
  
  protected List<MaterialProfile> materials = null;
  
  /*
   * Need a public constructior for UI manager
   * Do not use. Use getInstance instead
   */
  public MaterialManager()
  {
    if (instance != null)
    {
      System.err.println("ProfileManager should not be instanciated directly");
    }
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
            //if file was wrongly named, correct the name
            if (!(f.getName().equals(this.getMaterialPath(prof).getName())))
            {
              f.renameTo(new File(f.getParent(), this.getMaterialPath(prof).getName()));
            }
            result.add(prof);
          }
          catch (Exception ex)
          {
            Logger.getLogger(MaterialManager.class.getName()).log(Level.SEVERE, null, ex);
          }
        }
      }
    }
    Collections.sort(result, new Comparator<MaterialProfile>(){

      public int compare(MaterialProfile t, MaterialProfile t1)
      {
        return t.getName().compareTo(t1.getName());
      }
    });
    return result;
  }

  private File getMaterialsDirectory()
  {
    return new File(Helper.getBasePath(), "materials");
  }
  
  private File getMaterialPath(MaterialProfile mp)
  {
    return new File(getMaterialsDirectory(), Helper.toPathName(mp.toString())+".xml");
  }
  
  public void removeProfile(MaterialProfile mp)
  {
    this.materials.remove(mp);
    this.deleteProfile(mp);
  }
  
  private void deleteProfile(MaterialProfile mp)
  {
    File f = getMaterialPath(mp);
    if (f.exists())
    {
      f.delete();
    }
  }
  
  public void addProfile(MaterialProfile mp) throws FileNotFoundException
  {
    this.saveProfile(mp, this.getMaterialPath(mp));
    this.materials.add(mp);
  }
  
  public void saveProfile(MaterialProfile mp, File f) throws FileNotFoundException
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
  
  public MaterialProfile loadProfile(File f) throws FileNotFoundException, IOException
  {
    FileInputStream fin = new FileInputStream(f);
    MaterialProfile result = this.loadProfile(fin);
    result.setThumbnailPath(Helper.addParentPath(f.getParentFile(), result.getThumbnailPath()));
    fin.close();
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
    if (materials == null)
    {
      materials = this.loadFromDirectory(getMaterialsDirectory());
    }
    return materials;
  }

  public void setMaterials(List<MaterialProfile> mats) throws FileNotFoundException
  {
    for(MaterialProfile m:this.getMaterials().toArray(new MaterialProfile[0]))
    {
      this.deleteProfile(m);
    }
    for (MaterialProfile m:mats)
    {
      this.addProfile(m);
    }
  }

}
