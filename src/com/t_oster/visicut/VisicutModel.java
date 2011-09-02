/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.visicut;

import com.t_oster.liblasercut.IllegalJobException;
import com.t_oster.liblasercut.LaserJob;
import com.t_oster.liblasercut.LaserProperty;
import com.t_oster.liblasercut.RasterPart;
import com.t_oster.liblasercut.VectorPart;
import com.t_oster.liblasercut.drivers.EpilogCutter;
import com.t_oster.visicut.model.LaserProfile;
import com.t_oster.visicut.model.mapping.Mapping;
import com.t_oster.visicut.model.MaterialProfile;
import com.t_oster.visicut.model.graphicelements.GraphicFileImporter;
import com.t_oster.visicut.model.graphicelements.GraphicSet;
import com.t_oster.visicut.model.graphicelements.ImportException;
import com.t_oster.visicut.model.mapping.MappingSet;
import java.awt.geom.AffineTransform;
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
import java.net.SocketTimeoutException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * This class contains the state and business logic of the 
 * Application
 * 
 * @author thommy
 */
public class VisicutModel
{

  protected Preferences preferences = new Preferences();
  public static final String PROP_PREFERENCES = "preferences";
  public static final String PROP_LOADEDFILE = "loadedFile";

  /**
   * Get the value of preferences
   *
   * @return the value of preferences
   */
  public Preferences getPreferences()
  {
    return preferences;
  }

  /**
   * Set the value of preferences
   *
   * @param preferences new value of preferences
   */
  public void setPreferences(Preferences preferences)
  {
    Preferences oldPreferences = this.preferences;
    this.preferences = preferences;
    propertyChangeSupport.firePropertyChange(PROP_PREFERENCES, oldPreferences, preferences);
  }
  protected GraphicSet graphicObjects = null;
  public static final String PROP_GRAPHICOBJECTS = "graphicObjects";

  /**
   * Get the value of graphicObjects
   *
   * @return the value of graphicObjects
   */
  public GraphicSet getGraphicObjects()
  {
    return graphicObjects;
  }

  /**
   * Set the value of graphicObjects
   *
   * @param graphicObjects new value of graphicObjects
   */
  public void setGraphicObjects(GraphicSet graphicObjects)
  {
    GraphicSet oldGraphicObjects = this.graphicObjects;
    this.graphicObjects = graphicObjects;
    propertyChangeSupport.firePropertyChange(PROP_GRAPHICOBJECTS, oldGraphicObjects, graphicObjects);
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
  private File loadedFile = null;

  /**
   * Returns the loaded plf file, if this was the last file
   * loaded. Otherwise it returns null
   * @return 
   */
  public File getLoadedFile()
  {
    return loadedFile;
  }

  public void loadFromFile(File f) throws FileNotFoundException, IOException, ImportException
  {
    ZipFile zip = new ZipFile(f);
    Enumeration entries = zip.entries();
    AffineTransform transform = null;
    MappingSet mappings = null;
    MaterialProfile material = null;
    File inputFile = null;
    while (entries.hasMoreElements())
    {
      ZipEntry entry = (ZipEntry) entries.nextElement();
      String name = entry.getName();
      if (name.equals("transform.xml"))
      {
        XMLDecoder decoder = new XMLDecoder(zip.getInputStream(entry));
        transform = (AffineTransform) decoder.readObject();
      }
      else if (name.equals("mappings.xml"))
      {
        XMLDecoder decoder = new XMLDecoder(zip.getInputStream(entry));
        mappings = (MappingSet) decoder.readObject();
      }
      else if (name.equals("material.xml"))
      {
        XMLDecoder decoder = new XMLDecoder(zip.getInputStream(entry));
        material = (MaterialProfile) decoder.readObject();
      }
      else
      {
        int i = 0;
        inputFile = new File(name);
        while (inputFile.exists())
        {//Find next nonexisting file
          inputFile = new File((i++) + name);
        }
        byte[] buf = new byte[1024];
        InputStream in = zip.getInputStream(entry);
        FileOutputStream out = new FileOutputStream(inputFile);
        // Transfer bytes from the file to the ZIP file
        int len;
        while ((len = in.read(buf)) > 0)
        {
          out.write(buf, 0, len);
        }
        out.close();
        in.close();
      }
    }
    if (mappings == null || transform == null || material == null || inputFile == null || !inputFile.exists())
    {
      throw new ImportException("Corrupted Input File");
    }
    this.setMaterial(material);
    this.setMappings(mappings);
    if (inputFile != null && inputFile.exists())
    {
      this.loadGraphicFile(inputFile);
    }
    else
    {
      this.setGraphicObjects(new GraphicSet());
    }
    this.getGraphicObjects().setTransform(transform);
    this.setLoadedFile(f);
  }

  public void saveToFile(File f) throws FileNotFoundException, IOException
  {
    FileInputStream in;
    byte[] buf = new byte[1024];
    int len;
    // Create the ZIP file
    ZipOutputStream out = new ZipOutputStream(new FileOutputStream(f));
    if (this.sourceFile != null)
    {
      // Add source GraphicsFile to the Zip File
      out.putNextEntry(new ZipEntry(this.sourceFile.getName()));
      in = new FileInputStream(this.sourceFile);
      // Transfer bytes from the file to the ZIP file
      while ((len = in.read(buf)) > 0)
      {
        out.write(buf, 0, len);
      }
      in.close();
      // Complete the entry
      out.closeEntry();
    }
    //find temporary file for xml
    int i = 0;
    File tmp = null;
    do
    {
      tmp = new File("tmp" + (i++) + ".xml");
    }
    while (tmp.exists());

    out.putNextEntry(new ZipEntry("transform.xml"));
    //Write xml to temp file
    XMLEncoder encoder = new XMLEncoder(new FileOutputStream(tmp));
    encoder.writeObject(this.getGraphicObjects().getTransform());
    encoder.close();
    in = new FileInputStream(tmp);
    // Transfer bytes from the file to the ZIP file
    while ((len = in.read(buf)) > 0)
    {
      out.write(buf, 0, len);
    }
    in.close();
    out.closeEntry();
    out.putNextEntry(new ZipEntry("mappings.xml"));
    encoder = new XMLEncoder(new FileOutputStream(tmp));
    encoder.writeObject(this.getMappings());
    encoder.close();
    in = new FileInputStream(tmp);
    // Transfer bytes from the file to the ZIP file
    while ((len = in.read(buf)) > 0)
    {
      out.write(buf, 0, len);
    }
    in.close();
    out.closeEntry();
    out.putNextEntry(new ZipEntry("material.xml"));
    encoder = new XMLEncoder(new FileOutputStream(tmp));
    encoder.writeObject(this.getMaterial());
    encoder.close();
    in = new FileInputStream(tmp);
    // Transfer bytes from the file to the ZIP file
    while ((len = in.read(buf)) > 0)
    {
      out.write(buf, 0, len);
    }
    in.close();
    out.closeEntry();
    // Complete the ZIP file
    out.close();
    // Delete the tmp file
    tmp.delete();
  }
  private File sourceFile = null;

  public void loadGraphicFile(File f)
  {
    try
    {
      GraphicFileImporter im = new GraphicFileImporter();
      this.setGraphicObjects(im.importFile(f));
      sourceFile = f;
    }
    catch (ImportException e)
    {
      this.setGraphicObjects(null);
    }
  }
  protected MaterialProfile material = null;
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
  protected MappingSet mappings = null;
  public static final String PROP_MAPPINGS = "mappings";

  /**
   * Get the value of mappings
   *
   * @return the value of mappings
   */
  public MappingSet getMappings()
  {
    return mappings;
  }

  /**
   * Set the value of mappings
   *
   * @param mappings new value of mappings
   */
  public void setMappings(MappingSet mappings)
  {
    MappingSet oldMappings = this.mappings;
    this.mappings = mappings;
    propertyChangeSupport.firePropertyChange(PROP_MAPPINGS, oldMappings, mappings);
  }

  public void sendJob() throws IllegalJobException, SocketTimeoutException, Exception
  {
    RasterPart rp = new RasterPart(new LaserProperty());
    VectorPart vp = new VectorPart(new LaserProperty());
    EpilogCutter instance = new EpilogCutter("137.226.56.228");
    LaserJob job = new LaserJob("VisiCut", "666", "bla", 500, null, vp, rp);
    for (Mapping m : this.getMappings())
    {
      GraphicSet set = m.getA().getMatchingObjects(this.getGraphicObjects());
      LaserProfile p = material.getLaserProfile(m.getProfileName());
      p.addToLaserJob(job, set, material.getDepth());
    }
    job.getVectorPart().setFocus(0);
    instance.sendJob(job);
  }

  private void setLoadedFile(File f)
  {
    File oldLoadedFile = this.loadedFile;
    this.loadedFile = f;
    this.propertyChangeSupport.firePropertyChange(PROP_LOADEDFILE, oldLoadedFile, f);
  }
}
