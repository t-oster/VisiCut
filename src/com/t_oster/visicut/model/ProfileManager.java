/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.visicut.model;

import com.t_oster.liblasercut.BlackWhiteRaster.DitherAlgorithm;
import com.t_oster.liblasercut.LaserProperty;
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
    //Finnpappe
    this.materials = new LinkedList<MaterialProfile>();
    MaterialProfile profile = new MaterialProfile();
    profile.setName("Finnpappe");
    profile.setColor(new Color(209,163,117));
    profile.setHeight(3);
    List<LaserProfile> lprofiles = new LinkedList<LaserProfile>();
    VectorProfile vp = new VectorProfile();
    vp.setName("cut line");
    vp.setWidth(1f);
    vp.setPreviewThumbnail(new File("/home/thommy/NetBeansProjects/Visicut/materials/Finnpappe/cutline.png"));
    vp.setCuttingProperty(new LaserProperty(50,100));
    lprofiles.add(vp);
    vp = new VectorProfile();
    vp.setName("broad line");
    vp.setWidth(3f);
    vp.setPreviewThumbnail(new File("/home/thommy/NetBeansProjects/Visicut/materials/Finnpappe/bigline.png"));
    vp.setCuttingProperty(new LaserProperty(50,100,5000,300));
    lprofiles.add(vp);
    RasterProfile rp = new RasterProfile();
    rp.setColor(Color.black);
    rp.setName("Floyd Steinberg");
    rp.setPreviewThumbnail(new File("/home/thommy/NetBeansProjects/Visicut/materials/Plexiglass/floydsteinberg.png"));
    rp.setDitherAlgorithm(DitherAlgorithm.FLOYD_STEINBERG);
    lprofiles.add(rp);
    rp = new RasterProfile();
    rp.setColor(Color.black);
    rp.setName("Ordered");
    rp.setPreviewThumbnail(new File("/home/thommy/NetBeansProjects/Visicut/materials/Plexiglass/rasterordered.png"));
    rp.setDitherAlgorithm(DitherAlgorithm.ORDERED);
    lprofiles.add(rp);
    profile.setLaserProfiles(lprofiles.toArray(new LaserProfile[0]));
    this.materials.add(profile);
    //Filz
    profile = new MaterialProfile();
    profile.setName("Filz");
    lprofiles = new LinkedList<LaserProfile>();
    vp = new VectorProfile();
    vp.setName("cut line");
    vp.setWidth(2f);
    vp.setPreviewThumbnail(new File("/home/thommy/NetBeansProjects/Visicut/materials/Filz/cutline.png"));
    lprofiles.add(vp);
    vp = new VectorProfile();
    vp.setName("broad cut line");
    vp.setWidth(6f);
    vp.setPreviewThumbnail(new File("/home/thommy/NetBeansProjects/Visicut/materials/Filz/bigcut.png"));
    lprofiles.add(vp);
    profile.setLaserProfiles(lprofiles.toArray(new LaserProfile[0]));
    this.materials.add(profile);
    //Plexiglass
    profile = new MaterialProfile();
    profile.setColor(new Color(117,163,209));
    profile.setName("Plexiglass");
    profile.setHeight(3);
    lprofiles = new LinkedList<LaserProfile>();
    vp = new VectorProfile();
    vp.setName("cut line");
    vp.setWidth(1f);
    vp.setColor(Color.white);
    vp.setPreviewThumbnail(new File("/home/thommy/NetBeansProjects/Visicut/materials/Plexiglass/cutline.png"));
    lprofiles.add(vp);
    vp = new VectorProfile();
    vp.setColor(Color.white);
    vp.setName("broad line");
    vp.setWidth(2f);
    vp.setPreviewThumbnail(new File("/home/thommy/NetBeansProjects/Visicut/materials/Plexiglass/bigline.png"));
    lprofiles.add(vp);
    rp = new RasterProfile();
    rp.setColor(Color.white);
    rp.setName("Floyd Steinberg");
    rp.setPreviewThumbnail(new File("/home/thommy/NetBeansProjects/Visicut/materials/Plexiglass/floydsteinberg.png"));
    rp.setDitherAlgorithm(DitherAlgorithm.FLOYD_STEINBERG);
    lprofiles.add(rp);
    rp = new RasterProfile();
    rp.setColor(Color.white);
    rp.setName("Ordered");
    rp.setPreviewThumbnail(new File("/home/thommy/NetBeansProjects/Visicut/materials/Plexiglass/rasterordered.png"));
    rp.setDitherAlgorithm(DitherAlgorithm.ORDERED);
    lprofiles.add(rp);
    profile.setLaserProfiles(lprofiles.toArray(new LaserProfile[0]));
    this.materials.add(profile);
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
