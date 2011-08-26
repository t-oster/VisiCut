/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.visicut;

import com.t_oster.liblasercut.platform.Util;
import com.t_oster.visicut.model.Mapping;
import com.t_oster.visicut.model.MaterialProfile;
import com.t_oster.visicut.model.graphicelements.GraphicFileImporter;
import com.t_oster.visicut.model.graphicelements.GraphicObject;
import com.t_oster.visicut.model.graphicelements.ImportException;
import com.t_oster.visicut.model.graphicelements.svgsupport.SVGImporter;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.util.List;

/**
 * This class contains the state and business logic of the 
 * Application
 * 
 * @author thommy
 */
public class VisicutModel
{

  protected List<GraphicObject> graphicObjects = null;
  public static final String PROP_GRAPHICOBJECTS = "graphicObjects";

  /**
   * Get the value of graphicObjects
   *
   * @return the value of graphicObjects
   */
  public List<GraphicObject> getGraphicObjects()
  {
    return graphicObjects;
  }

  /**
   * Set the value of graphicObjects
   *
   * @param graphicObjects new value of graphicObjects
   */
  public void setGraphicObjects(List<GraphicObject> graphicObjects)
  {
    List<GraphicObject> oldGraphicObjects = this.graphicObjects;
    this.graphicObjects = graphicObjects;
    propertyChangeSupport.firePropertyChange(PROP_GRAPHICOBJECTS, oldGraphicObjects, graphicObjects);
  }
  private PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);
  protected File sourceFile = null;
  protected File graphicFile = null;
  public static final String PROP_GRAPHICFILE = "graphicFile";

  /**
   * Get the value of graphicFile
   *
   * @return the value of graphicFile
   */
  public File getGraphicFile()
  {
    return graphicFile;
  }

  /**
   * Set the value of graphicFile
   *
   * @param graphicFile new value of graphicFile
   */
  public void setGraphicFile(File graphicFile)
  {
    File oldGraphicFile = this.graphicFile;
    this.graphicFile = graphicFile;
    propertyChangeSupport.firePropertyChange(PROP_GRAPHICFILE, oldGraphicFile, graphicFile);
    if (Util.differ(graphicFile, oldGraphicFile))
    {
      this.loadGraphicFile(graphicFile);
    }
  }

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

  public void loadGraphicFile(File f)
  {
    try
    {
      GraphicFileImporter im = new GraphicFileImporter();
      this.setGraphicFile(f);
      this.setGraphicObjects(im.importFile(f));
    }
    catch (ImportException e)
    {

      this.setGraphicFile(null);
      this.setGraphicObjects(null);
    }
  }
  protected MaterialProfile material = new MaterialProfile();
  public static final String PROP_MATERIAL = "material";

  /**
   * Get the value of material
   *
   * @return the value of material
   */
  public MaterialProfile getMaterial()
  {
    return material;
  }

  /**
   * Set the value of material
   *
   * @param material new value of material
   */
  public void setMaterial(MaterialProfile material)
  {
    MaterialProfile oldMaterial = this.material;
    this.material = material;
    propertyChangeSupport.firePropertyChange(PROP_MATERIAL, oldMaterial, material);
  }
  protected List<Mapping> mappings = null;
  public static final String PROP_MAPPINGS = "mappings";

  /**
   * Get the value of mappings
   *
   * @return the value of mappings
   */
  public List<Mapping> getMappings()
  {
    return mappings;
  }

  /**
   * Set the value of mappings
   *
   * @param mappings new value of mappings
   */
  public void setMappings(List<Mapping> mappings)
  {
    List<Mapping> oldMappings = this.mappings;
    this.mappings = mappings;
    propertyChangeSupport.firePropertyChange(PROP_MAPPINGS, oldMappings, mappings);
  }
}
