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

import com.t_oster.visicut.misc.FileUtils;
import com.t_oster.visicut.misc.Helper;
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
public abstract class FilebasedManager<T>
{

  protected List<T> objects = null;
  

  private List<T> loadFromDirectory(File dir)
  {
    List<T> result = new LinkedList<T>();
    if (dir.isDirectory())
    {
      for (File f : dir.listFiles())
      {
        if (f.isFile() && f.getAbsolutePath().toLowerCase().endsWith(".xml"))
        {
          try
          {
            T prof = this.loadFromFile(f);
            //if file was wrongly named, correct the name
            if (!(f.getName().equals(this.getObjectPath(prof).getName())))
            {
              f.renameTo(new File(f.getParent(), this.getObjectPath(prof).getName()));
            }
            result.add(prof);
          }
          catch (Exception ex)
          {
            Logger.getLogger(FilebasedManager.class.getName()).log(Level.SEVERE, null, ex);
          }
        }
      }
    }
    Collections.sort(result, this.getComparator());
    return result;
  }

  protected abstract String getSubfolderName();
  
  public void reload()
  {
    this.objects = null;
  }
  
  private File getObjectsDirectory()
  {
    return new File(Helper.getBasePath(), getSubfolderName());
  }
  
  private File getObjectPath(T mp)
  {
    return new File(getObjectsDirectory(), Helper.toPathName(mp.toString())+".xml");
  }
  
  private File generateThumbnailPath(T o)
  {
    return new File(getObjectsDirectory(), Helper.toPathName(o.toString())+".png");
  }
  
  public abstract String getThumbnail(T o);
  public abstract void setThumbnail(T o, String f);
  
  public void remove(T mp)
  {
    this.objects.remove(mp);
    this.deleteObject(mp);
  }
  
  private void deleteObject(T mp)
  {
    File f = getObjectPath(mp);
    if (f.exists())
    {
      f.delete();
    }
    //f = new File(this.getThumbnail(mp));
    //if (f.exists())
    //{
    //  f.delete();
    //} 
  }
  
  protected abstract Comparator<T> getComparator();
  
  public void add(T mp) throws FileNotFoundException
  {
    this.getAll().add(mp);
    this.save(mp, this.getObjectPath(mp));
    Collections.sort(this.objects, getComparator());
  }
  
  
  
  public void save(T mp, File f) throws FileNotFoundException
  {
    if (!f.getParentFile().exists())
    {
      f.getParentFile().mkdirs();
    }
    if (this.getThumbnail(mp) != null)
    {
      //if thumbnail has not the right path, copy the referenced image
      File thumb = this.generateThumbnailPath(mp);
      File curThumb = new File(this.getThumbnail(mp));
      if (curThumb.exists() && !curThumb.getAbsolutePath().equals(thumb.getAbsolutePath()))
      {
        try
        {
          FileUtils.copyFile(curThumb, thumb, false);
          this.setThumbnail(mp, thumb.getAbsolutePath());
        }
        catch (IOException ex)
        {
          Logger.getLogger(FilebasedManager.class.getName()).log(Level.SEVERE, null, ex);
        }
      }
      this.setThumbnail(mp, Helper.removeParentPath(f.getParentFile(), this.getThumbnail(mp)));
    }
    FileOutputStream out = new FileOutputStream(f);
    XMLEncoder enc = new XMLEncoder(out);
    enc.writeObject(mp);
    enc.close();
    this.setThumbnail(mp, Helper.addParentPath(f.getParentFile(), this.getThumbnail(mp)));
  }
  
  public T loadFromFile(File f) throws FileNotFoundException, IOException
  {
    FileInputStream fin = new FileInputStream(f);
    T result = this.loadFromFile(fin);
    this.setThumbnail(result, Helper.addParentPath(f.getParentFile(), this.getThumbnail(result)));
    if (this.getThumbnail(result) == null && this.generateThumbnailPath(result).exists())
    {
      this.setThumbnail(result, this.generateThumbnailPath(result).getAbsolutePath());
    }
    fin.close();
    return result;
  }

  public T loadFromFile(InputStream in)
  {
    XMLDecoder dec = new XMLDecoder(in);
    T result = (T) dec.readObject();
    return result;
  }

  /**
   * Get the value of materials
   *
   * @return the value of materials
   */
  public List<T> getAll()
  {
    if (objects == null)
    {
      objects = this.loadFromDirectory(getObjectsDirectory());
    }
    return objects;
  }

  public void setAll(List<T> mats) throws FileNotFoundException
  {
    for(Object m:this.getAll().toArray())
    {
      this.remove((T) m);
    }
    for (T m:mats)
    {
      this.add(m);
    }
  }

}
