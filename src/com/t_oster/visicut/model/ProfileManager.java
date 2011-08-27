/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.visicut.model;

import com.t_oster.liblasercut.BlackWhiteRaster.DitherAlgorithm;
import java.awt.Color;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.util.LinkedList;
import java.util.List;

/**
 * This class manages the available Material Profiles
 * 
 * @author thommy
 */
public class ProfileManager
{

  protected List<MaterialProfile> materials;
  public static final String PROP_MATERIALS = "materials";

  public ProfileManager()
  {
    this.materials = new LinkedList<MaterialProfile>();
    MaterialProfile filz = new MaterialProfile();
    filz.setName("Filz");
    VectorProfile[] lines = new VectorProfile[2];
    lines [0] = new VectorProfile();
    lines[0].setName("cut line");
    lines[0].setWidth(2f);
    lines[0].setPreviewThumbnail(new File("/home/thommy/NetBeansProjects/Visicut/materials/Filz/cutline.png"));
    lines[1] = new VectorProfile();
    lines[1].setName("broad cut line");
    lines[1].setWidth(6f);
    lines[1].setPreviewThumbnail(new File("/home/thommy/NetBeansProjects/Visicut/materials/Filz/bigcut.png"));
    filz.setLaserProfiles(lines);
    this.materials.add(filz);
    MaterialProfile finnpappe = new MaterialProfile();
    finnpappe.setColor(new Color(209,163,117));
    finnpappe.setName("Finnpappe");
    finnpappe.setHeight(2);
    lines = new VectorProfile[2];
    lines [0] = new VectorProfile();
    lines[0].setName("cut line");
    lines[0].setWidth(1f);
    lines[0].setPreviewThumbnail(new File("/home/thommy/NetBeansProjects/Visicut/materials/Finnpappe/cutline.png"));
    lines[1] = new VectorProfile();
    lines[1].setName("broad line");
    lines[1].setWidth(3f);
    lines[1].setPreviewThumbnail(new File("/home/thommy/NetBeansProjects/Visicut/materials/Finnpappe/bigline.png"));
    finnpappe.setLaserProfiles(lines);
    this.materials.add(finnpappe);
    MaterialProfile plexiglass = new MaterialProfile();
    plexiglass.setColor(new Color(117,163,209));
    plexiglass.setName("Plexiglass");
    plexiglass.setHeight(3);
    lines = new VectorProfile[2];
    lines [0] = new VectorProfile();
    lines[0].setName("cut line");
    lines[0].setWidth(1f);
    lines[0].setColor(Color.white);
    lines[0].setPreviewThumbnail(new File("/home/thommy/NetBeansProjects/Visicut/materials/Plexiglass/cutline.png"));
    lines[1] = new VectorProfile();
    lines[1].setColor(Color.white);
    lines[1].setName("broad line");
    lines[1].setWidth(2f);
    lines[1].setPreviewThumbnail(new File("/home/thommy/NetBeansProjects/Visicut/materials/Plexiglass/bigline.png"));
    RasterProfile raster = new RasterProfile();
    raster.setColor(Color.white);
    raster.setName("Floyd Steinberg");
    raster.setPreviewThumbnail(new File("/home/thommy/NetBeansProjects/Visicut/materials/Plexiglass/floydsteinberg.png"));
    raster.setDitherAlgorithm(DitherAlgorithm.FLOYD_STEINBERG);
    LaserProfile[] profiles = new LaserProfile[4];
    profiles[0] = lines[0];
    profiles[1] = lines[1];
    profiles[2] = raster;
    raster = new RasterProfile();
    raster.setColor(Color.white);
    raster.setName("Ordered");
    raster.setPreviewThumbnail(new File("/home/thommy/NetBeansProjects/Visicut/materials/Plexiglass/rasterordered.png"));
    raster.setDitherAlgorithm(DitherAlgorithm.ORDERED);
    profiles[3] = raster;
    plexiglass.setLaserProfiles(profiles);
    this.materials.add(plexiglass);
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

}
