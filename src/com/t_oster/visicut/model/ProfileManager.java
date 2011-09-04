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
    loadFromDirectory();
    if (materials.isEmpty())
    {
      generateDefault();
      writeAll();
    }
  }
  
  private void generateDefault()
  {
    //Finnpappe
    MaterialProfile profile = new MaterialProfile();
    profile.setName("Finnpappe");
    profile.setThumbnailPath(new File("materials/Finnpappe/profile.png").getAbsolutePath());
    profile.setDescription("A light paper based material.");
    profile.setColor(new Color(209,163,117));
    profile.setCutColor(Color.RED);
    profile.setDepth(3);
    List<LaserProfile> lprofiles = new LinkedList<LaserProfile>();
    VectorProfile vp = new VectorProfile();
    vp.setName("cut line");
    vp.setDescription("A completely cut throug line with small diameter");
    vp.setIsCut(true);
    vp.setWidth(1f);
    vp.setPreviewThumbnail(new File("materials/Finnpappe/cutline.png"));
    vp.setLaserProperties(new LaserProperty[]{new LaserProperty(50,100)});
    lprofiles.add(vp);
    vp = new VectorProfile();
    vp.setName("broad line");
    vp.setDescription("A broad line, which is not cut through.");
    vp.setWidth(3f);
    vp.setPreviewThumbnail(new File("materials/Finnpappe/bigline.png"));
    vp.setLaserProperties(new LaserProperty[]{new LaserProperty(50,100,5000,300)});
    lprofiles.add(vp);
    RasterProfile rp = new RasterProfile();
    rp.setColor(Color.black);
    rp.setName("Floyd Steinberg");
    rp.setDescription("The Floyd Steinberg Algorithm is good for Fotos.");
    rp.setPreviewThumbnail(new File("materials/Plexiglass/floydsteinberg.png"));
    rp.setDitherAlgorithm(DitherAlgorithm.FLOYD_STEINBERG);
    lprofiles.add(rp);
    rp = new RasterProfile();
    rp.setColor(Color.black);
    rp.setName("Ordered");
    rp.setDescription("The Ordered Algorithm adds a Pattern to the image");
    rp.setPreviewThumbnail(new File("materials/Plexiglass/rasterordered.png"));
    rp.setDitherAlgorithm(DitherAlgorithm.ORDERED);
    lprofiles.add(rp);
    rp = new RasterProfile();
    rp.setColor(Color.black);
    rp.setName("Average");
    rp.setDescription("The Average Algorithm makes a pixel black iff its darker than the average pixel");
    rp.setPreviewThumbnail(new File("materials/Plexiglass/rasterordered.png"));
    rp.setDitherAlgorithm(DitherAlgorithm.AVERAGE);
    lprofiles.add(rp);
    profile.setLaserProfiles(lprofiles.toArray(new LaserProfile[0]));
    this.materials.add(profile);
    //Filz
    profile = new MaterialProfile();
    profile.setName("Filz");
    profile.setThumbnailPath(new File("materials/Filz/profile.png").getAbsolutePath());
    profile.setDescription("A red Material");
    lprofiles = new LinkedList<LaserProfile>();
    vp = new VectorProfile();
    vp.setName("cut line");
    vp.setIsCut(true);
    vp.setWidth(2f);
    vp.setPreviewThumbnail(new File("materials/Filz/cutline.png"));
    lprofiles.add(vp);
    vp = new VectorProfile();
    vp.setName("broad cut line");
    vp.setWidth(6f);
    vp.setPreviewThumbnail(new File("materials/Filz/bigcut.png"));
    lprofiles.add(vp);
    profile.setLaserProfiles(lprofiles.toArray(new LaserProfile[0]));
    this.materials.add(profile);
    //Plexiglass
    profile = new MaterialProfile();
    profile.setColor(new Color(117,163,209));
    profile.setName("Plexiglass");
    profile.setDepth(1.5f);
    profile.setThumbnailPath(new File("materials/Plexiglass/profile.png").getAbsolutePath());
    lprofiles = new LinkedList<LaserProfile>();
    vp = new VectorProfile();
    vp.setName("cut line");
    vp.setIsCut(true);
    vp.setWidth(1f);
    vp.setColor(Color.white);
    vp.setPreviewThumbnail(new File("materials/Plexiglass/cutline.png"));
    lprofiles.add(vp);
    vp = new VectorProfile();
    vp.setColor(Color.white);
    vp.setName("broad line");
    vp.setWidth(2f);
    vp.setPreviewThumbnail(new File("materials/Plexiglass/bigline.png"));
    lprofiles.add(vp);
    rp = new RasterProfile();
    rp.setColor(Color.white);
    rp.setName("Floyd Steinberg");
    rp.setPreviewThumbnail(new File("materials/Plexiglass/floydsteinberg.png"));
    rp.setDitherAlgorithm(DitherAlgorithm.FLOYD_STEINBERG);
    lprofiles.add(rp);
    rp = new RasterProfile();
    rp.setColor(Color.white);
    rp.setName("Ordered");
    rp.setPreviewThumbnail(new File("materials/Plexiglass/rasterordered.png"));
    rp.setDitherAlgorithm(DitherAlgorithm.ORDERED);
    rp = new RasterProfile();
    rp.setColor(Color.white);
    rp.setName("Floyd Steinberg (inverted)");
    rp.setInvertColors(true);
    rp.setPreviewThumbnail(new File("materials/Plexiglass/floydsteinberg.png"));
    rp.setDitherAlgorithm(DitherAlgorithm.FLOYD_STEINBERG);
    lprofiles.add(rp);
    rp = new RasterProfile();
    rp.setColor(Color.white);
    rp.setName("Ordered (inverted)");
    rp.setPreviewThumbnail(new File("materials/Plexiglass/rasterordered.png"));
    rp.setDitherAlgorithm(DitherAlgorithm.ORDERED);
    rp.setInvertColors(true);
    lprofiles.add(rp);
    profile.setLaserProfiles(lprofiles.toArray(new LaserProfile[0]));
    this.materials.add(profile);
  }
  
  private void loadFromDirectory()
  {
    File dir = new File("materials");
    if (dir.isDirectory())
    {
      for (File f:dir.listFiles())
      {
        if (f.isFile() && f.getAbsolutePath().toLowerCase().endsWith(".xml"))
        {
          try
          {
            MaterialProfile prof = this.loadProfile(f);
            this.materials.add(prof);
          }
          catch (Exception ex)
          {
            Logger.getLogger(ProfileManager.class.getName()).log(Level.SEVERE, null, ex);
          }
        }
      }
    }
  }
  
  public void saveProfile(MaterialProfile mp, File f) throws FileNotFoundException
  {
    FileOutputStream out = new FileOutputStream(f);
    XMLEncoder enc = new XMLEncoder(out);
    enc.writeObject(mp);
    enc.close();
  }
  
  public MaterialProfile loadProfile(File f) throws FileNotFoundException, IOException
  {
    FileInputStream fin = new FileInputStream(f);
    MaterialProfile result = this.loadProfile(fin);
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
   * Writes all Profiles to a subdirectory "materials"
   */
  private void writeAll()
  {
    File dir = new File("materials");
    if (!dir.exists())
    {
      dir.mkdir();
    }
    for (MaterialProfile p:this.materials)
    {
      try
      {
        this.saveProfile(p, new File("materials/"+p.getName()+".xml"));
      }
      catch (FileNotFoundException ex)
      {
        Logger.getLogger(ProfileManager.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
  }

}
