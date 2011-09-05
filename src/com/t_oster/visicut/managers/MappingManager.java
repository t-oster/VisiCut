/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.visicut.managers;

import com.t_oster.visicut.model.mapping.FilterSet;
import com.t_oster.visicut.model.mapping.Mapping;
import com.t_oster.visicut.model.mapping.MappingFilter;
import com.t_oster.visicut.model.mapping.MappingSet;
import java.awt.Color;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author thommy
 */
public class MappingManager
{
  public MappingManager()
  {
    mappingSets = new LinkedList<MappingSet>();
    loadFromDirectory();
    if (mappingSets.isEmpty())
    {
      generateDefault();
      saveMappings();
    }
  }
  
  private void loadFromDirectory()
  {
    File dir = new File(".VisiCut/mappings");
    if (dir.isDirectory())
    {
      for (File f:dir.listFiles())
      {
        if (f.isFile() && f.getAbsolutePath().toLowerCase().endsWith(".xml"))
        {
          try
          {
            MappingSet s = this.loadMappingSet(f);
            this.mappingSets.add(s);
          }
          catch (Exception ex)
          {
            Logger.getLogger(MappingManager.class.getName()).log(Level.SEVERE, null, ex);
          }
        }
      }
    }
  }
  
  private void generateDefault()
  {
    MappingSet ms;
    FilterSet fs;
    ms = new MappingSet();
    ms.setName("Cut");
    fs = new FilterSet();
    fs.add(new MappingFilter());
    ms.add(new Mapping(fs,"cut line"));
    mappingSets.add(ms);
    ms = new MappingSet();
    ms.setName("Engrave");
    fs = new FilterSet();//Empty Filter matches everything
    ms.add(new Mapping(fs, "Floyd Steinberg"));
    mappingSets.add(ms);
    ms = new MappingSet();
    ms.setName("Cut + Engrave");
    fs = new FilterSet();
    fs.add(new MappingFilter("ObjectType", "Shape"));
    fs.add(new MappingFilter("FillColor", "none"));
    ms.add(new Mapping(fs,"cut line"));
    fs = new FilterSet();//Empty Filter matches everything
    ms.add(new Mapping(fs, "Floyd Steinberg"));
    mappingSets.add(ms);
  }
  
  protected List<MappingSet> mappingSets = null;
  public static final String PROP_MAPPINGSETS = "mappingSets";

  /**
   * Get the value of mappingSets
   *
   * @return the value of mappingSets
   */
  public List<MappingSet> getMappingSets()
  {
    return mappingSets;
  }

  /**
   * Set the value of mappingSets
   *
   * @param mappingSets new value of mappingSets
   */
  public void setMappingSets(List<MappingSet> mappingSets)
  {
    List<MappingSet> oldMappingSets = this.mappingSets;
    this.mappingSets = mappingSets;
    propertyChangeSupport.firePropertyChange(PROP_MAPPINGSETS, oldMappingSets, mappingSets);
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

  public void saveMappingSet(MappingSet pref, File f) throws FileNotFoundException
  {
    FileOutputStream os = new FileOutputStream(f);
    XMLEncoder encoder = new XMLEncoder(os);
    encoder.writeObject(pref);
    encoder.close();
  }

  public MappingSet loadMappingSet(File f) throws FileNotFoundException, IOException
  {
    FileInputStream in = new FileInputStream(f);
    MappingSet p = this.loadMappingSet(in);
    in.close();
    return p;
  }
  
  public MappingSet loadMappingSet(InputStream in) throws FileNotFoundException
  {
    XMLDecoder decoder = new XMLDecoder(in);
    MappingSet p = (MappingSet) decoder.readObject();
    return p;
  }

  private void saveMappings()
  {
    if (new File(".VisiCut").isDirectory())
    {
      File dir = new File(".VisiCut/mappings");
      if (!dir.exists())
      {
        dir.mkdir();
      }
      for (MappingSet s:this.mappingSets)
      {
        try
        {
          this.saveMappingSet(s, new File(dir, s.getName()+".xml"));
        }
        catch (FileNotFoundException ex)
        {
          Logger.getLogger(MappingManager.class.getName()).log(Level.SEVERE, null, ex);
        }
      }
    }
  }

}
