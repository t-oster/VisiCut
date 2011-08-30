/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.visicut.model;

import com.t_oster.visicut.model.mapping.FilterSet;
import com.t_oster.visicut.model.mapping.Mapping;
import com.t_oster.visicut.model.mapping.MappingFilter;
import com.t_oster.visicut.model.mapping.MappingSet;
import java.awt.Color;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author thommy
 */
public class MappingManager
{
  public MappingManager()
  {
    mappingSets = new LinkedList<MappingSet>();
    MappingSet ms;
    FilterSet fs;
    ms = new MappingSet();
    ms.setName("Cut all Lines");
    fs = new FilterSet();
    fs.add(new MappingFilter("ObjectType", "Shape"));
    ms.add(new Mapping(fs,"cut line"));
    fs = new FilterSet();
    fs.add(new MappingFilter("ObjectType", "Image"));
    ms.add(new Mapping(fs, "FloydSteinberg"));
    mappingSets.add(ms);
    ms = new MappingSet();
    ms.setName("Epilog");
    fs = new FilterSet();
    fs.add(new MappingFilter("ObjectType", "Shape"));
    fs.add(new MappingFilter("StrokeColor", Color.RED));
    fs.add(new MappingFilter("FillColor", "none"));
    ms.add(new Mapping(fs,"cut line"));
    fs = new FilterSet();//Empty Filter matches everything
    ms.add(new Mapping(fs, "FloydSteinberg"));
    mappingSets.add(ms);
    //TODO: Refactor Mapping to Contain just the Name of the LaserProfile
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

}
