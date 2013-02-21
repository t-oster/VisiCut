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
package com.t_oster.visicut.managers;

import com.t_oster.visicut.misc.FileUtils;
import com.t_oster.visicut.misc.Helper;
import com.thoughtworks.xstream.XStream;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.beans.XMLDecoder;
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

  private XStream xstream = null;
  protected final XStream getXStream()
  {
    if (xstream == null)
    {
      xstream = createXStream(); 
    }
    return xstream;
  }
  protected XStream createXStream()
  {
    XStream xs = new XStream();
    return xs;
  }
  
  protected List<T> objects = null;
  protected PropertyChangeSupport pcs = new PropertyChangeSupport(this);
  public void addPropertyChangeListener(PropertyChangeListener l)
  {
    pcs.addPropertyChangeListener(l);
  }
  public void removePropertyChangeListener(PropertyChangeListener l)
  {
    pcs.removePropertyChangeListener(l);
  }

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
            if (prof != null)
            {
              //if file was wrongly named, correct the name
              if (!(f.getName().equals(this.getObjectPath(prof).getName())))
              {
                f.renameTo(new File(f.getParent(), this.getObjectPath(prof).getName()));
              }
              result.add(prof);
            }
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
    pcs.firePropertyChange("removed", mp, objects);
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
  
  public void add(T mp) throws FileNotFoundException, IOException
  {
    if (mp==null) {
      return;
    }
    
    // objects with the same storage path as the newly added one are overwritten - delete them
    // otherwise getAll() would return the old and new version of an item, both with the same name
    // with this, Manager.getAll() should always equal Manager.reload().getAll()  (except for special cases in subclasses, like temporary profiles)
    List<T> objectsToRemove=new LinkedList<T>();
    for (T object: this.getAll()) {
      if (this.getObjectPath(object).equals(this.getObjectPath(mp))) {
        objectsToRemove.add(object);
      }
    }
    for (T objectToRemove: objectsToRemove) {
      this.objects.remove(objectToRemove);
    }
    
    this.getAll().add(mp);
    this.save(mp, this.getObjectPath(mp));
    Collections.sort(this.objects, getComparator());
    pcs.firePropertyChange("add", null, mp);
  }
  
  public void save(T mp) throws FileNotFoundException, IOException
  {
    this.save(mp, this.getObjectPath(mp));
  }
  
  public void save(T mp, File f) throws FileNotFoundException, IOException
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
    this.getXStream().toXML(mp, out);
    out.close();
    this.setThumbnail(mp, Helper.addParentPath(f.getParentFile(), this.getThumbnail(mp)));
  }
  
  private T loadFromOldFile(File f)
  {
    try
    {
      XMLDecoder dec = new XMLDecoder(new FileInputStream(f));
      T result = (T) dec.readObject();
      dec.close();
      return result;
    }
    catch (Exception e)
    {
      return null;
    }
  }
  
  public T loadFromFile(File f) throws FileNotFoundException, IOException
  {
    FileInputStream fin = new FileInputStream(f);
    T result = this.loadFromFile(fin);
    fin.close();
    if (result == null)
    {
      result = this.loadFromOldFile(f);
    }
    if (result == null)
    {
      System.err.println("Error reading: "+f.getAbsolutePath()+". Invalid File Format (created with old VisiCut version?)");    
    }
    else
    {
      this.setThumbnail(result, Helper.addParentPath(f.getParentFile(), this.getThumbnail(result)));
      if (this.getThumbnail(result) == null && this.generateThumbnailPath(result).exists())
      {
        this.setThumbnail(result, this.generateThumbnailPath(result).getAbsolutePath());
      }
    }
    return result;
  }

  public T loadFromFile(InputStream in)
  {
    try
    {
      return (T) this.getXStream().fromXML(in);
    }
    catch (Exception e)
    {
      return null;
    }
    catch (java.lang.InstantiationError e)
    {
      return null;
    }
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
  
  /**
   * Find an object with the given name
   * @param string
   * @return object where (object.toString()==string), null if not found
   */
  public T getItemFromString(String string) {
    for (T obj: this.getAll()) {
      if (obj.toString().equals(string)) {
        return obj;
      }
    }
    return null;
  }

  public void setAll(List<T> mats) throws FileNotFoundException, IOException
  {
    for(Object m:this.getAll().toArray())
    {
      this.deleteObject((T) m);
    }
    this.getAll().clear();
    for (T m:mats)
    {
      this.getAll().add(m);
      this.save(m, this.getObjectPath(m));
    }
    Collections.sort(this.getAll(), getComparator());
    pcs.firePropertyChange("all", null, this.objects);
  }

}
