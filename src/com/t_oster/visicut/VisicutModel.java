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
package com.t_oster.visicut;

import com.t_oster.visicut.misc.ExtensionFilter;
import com.t_oster.liblasercut.IllegalJobException;
import com.t_oster.liblasercut.LaserCutter;
import com.t_oster.liblasercut.LaserJob;
import com.t_oster.liblasercut.LaserProperty;
import com.t_oster.liblasercut.RasterPart;
import com.t_oster.liblasercut.Raster3dPart;
import com.t_oster.liblasercut.VectorPart;
import com.t_oster.liblasercut.platform.Util;
import com.t_oster.visicut.model.LaserProfile;
import com.t_oster.visicut.managers.MappingManager;
import com.t_oster.visicut.model.graphicelements.GraphicObject;
import com.t_oster.visicut.model.mapping.Mapping;
import com.t_oster.visicut.model.MaterialProfile;
import com.t_oster.visicut.managers.ProfileManager;
import com.t_oster.visicut.model.LaserDevice;
import com.t_oster.visicut.model.graphicelements.GraphicFileImporter;
import com.t_oster.visicut.model.graphicelements.GraphicSet;
import com.t_oster.visicut.model.graphicelements.ImportException;
import com.t_oster.visicut.model.mapping.MappingSet;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.beans.Encoder;
import java.beans.Expression;
import java.beans.PersistenceDelegate;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import javax.swing.filechooser.FileFilter;

/**
 * This class contains the state and business logic of the 
 * Application
 * 
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class VisicutModel
{

  public static final FileFilter PLFFilter = new ExtensionFilter(".plf", "VisiCut Portable Laser Format (*.plf)");
  protected LaserDevice selectedLaserDevice = null;
  public static final String PROP_SELECTEDLASERDEVICE = "selectedLaserDevice";

  /**
   * Get the value of selectedLaserDevice
   *
   * @return the value of selectedLaserDevice
   */
  public LaserDevice getSelectedLaserDevice()
  {
    return selectedLaserDevice;
  }

  /**
   * Set the value of selectedLaserDevice
   *
   * @param selectedLaserDevice new value of selectedLaserDevice
   */
  public void setSelectedLaserDevice(LaserDevice selectedLaserDevice)
  {
    LaserDevice oldSelectedLaserDevice = this.selectedLaserDevice;
    this.selectedLaserDevice = selectedLaserDevice;
    propertyChangeSupport.firePropertyChange(PROP_SELECTEDLASERDEVICE, oldSelectedLaserDevice, selectedLaserDevice);
  }
  protected BufferedImage backgroundImage = null;
  public static final String PROP_BACKGROUNDIMAGE = "backgroundImage";

  /**
   * Get the value of backgroundImage
   *
   * @return the value of backgroundImage
   */
  public BufferedImage getBackgroundImage()
  {
    return backgroundImage;
  }

  /**
   * Set the value of backgroundImage
   *
   * @param backgroundImage new value of backgroundImage
   */
  public void setBackgroundImage(BufferedImage backgroundImage)
  {
    BufferedImage oldBackgroundImage = this.backgroundImage;
    this.backgroundImage = backgroundImage;
    propertyChangeSupport.firePropertyChange(PROP_BACKGROUNDIMAGE, oldBackgroundImage, backgroundImage);
  }
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
    this.graphicFileImporter = null;
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
    this.fitObjectsIntoMaterial();
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

  public void loadFromFile(MappingManager mm, File f) throws FileNotFoundException, IOException, ImportException
  {
    ZipFile zip = new ZipFile(f);
    Enumeration entries = zip.entries();
    AffineTransform transform = null;
    MappingSet loadedMappings = null;
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
        loadedMappings = mm.loadMappingSet(zip.getInputStream(entry));
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
    if (loadedMappings == null || transform == null || inputFile == null || !inputFile.exists())
    {
      throw new ImportException("Corrupted Input File");
    }
    this.setMappings(loadedMappings);
    GraphicSet gs = new GraphicSet();
    inputFile.deleteOnExit();
    gs = this.loadSetFromFile(inputFile);
    if (gs != null)
    {
      gs.setTransform(transform);
      this.setGraphicObjects(gs);
      this.setLoadedFile(f);
    }
    else
    {
      throw new ImportException("Can not load included graphic File");
    }
  }

  public void saveToFile(ProfileManager pm, MappingManager mm, File f) throws FileNotFoundException, IOException
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
    AffineTransform at = this.getGraphicObjects().getTransform();
    if (at != null)
    {
      out.putNextEntry(new ZipEntry("transform.xml"));
      //Write xml to temp file
      XMLEncoder encoder = new XMLEncoder(new FileOutputStream(tmp));
      encoder.setPersistenceDelegate(AffineTransform.class, new PersistenceDelegate()
      {//Fix for older java versions
        protected Expression instantiate(Object oldInstance, Encoder out)
        {
          AffineTransform tx = (AffineTransform) oldInstance;
          double[] coeffs = new double[6];
          tx.getMatrix(coeffs);
          return new Expression(oldInstance,
            oldInstance.getClass(),
            "new",
            new Object[]
            {
              coeffs
            });
        }
      });
      encoder.writeObject(at);
      encoder.close();
      in = new FileInputStream(tmp);
      // Transfer bytes from the file to the ZIP file
      while ((len = in.read(buf)) > 0)
      {
        out.write(buf, 0, len);
      }
      in.close();
      out.closeEntry();
    }
    out.putNextEntry(new ZipEntry("mappings.xml"));
    mm.saveMappingSet(this.getMappings(), tmp);
    in = new FileInputStream(tmp);
    // Transfer bytes from the file to the ZIP file
    while ((len = in.read(buf)) > 0)
    {
      out.write(buf, 0, len);
    }
    in.close();
    out.closeEntry();
//    out.putNextEntry(new ZipEntry("material.xml"));
//    pm.saveProfile(this.getMaterial(), tmp);
//    in = new FileInputStream(tmp);
//    // Transfer bytes from the file to the ZIP file
//    while ((len = in.read(buf)) > 0)
//    {
//      out.write(buf, 0, len);
//    }
//    in.close();
//    out.closeEntry();
    // Complete the ZIP file
    out.close();
    // Delete the tmp file
    tmp.delete();
  }
  private File sourceFile = null;
  private GraphicFileImporter graphicFileImporter = null;

  public GraphicFileImporter getGraphicFileImporter()
  {
    if (graphicFileImporter == null)
    {
      graphicFileImporter = new GraphicFileImporter(this.preferences.availableImporters);
    }
    return graphicFileImporter;
  }

  private GraphicSet loadSetFromFile(File f)
  {
    try
    {
      GraphicFileImporter im = this.getGraphicFileImporter();
      return im.importFile(f);
    }
    catch (ImportException e)
    {
      return null;
    }
  }

  public void loadGraphicFile(File f)
  {
    GraphicSet gs = this.loadSetFromFile(f);
    if (gs != null)
    {
      this.setGraphicObjects(gs);
      this.sourceFile = f;
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
    fitObjectsIntoMaterial();
    propertyChangeSupport.firePropertyChange(PROP_MATERIAL, oldMaterial, material);
  }

  /**
   * First moves the Objects into the top left corner, if their origin
   * is negative. (no matter if material selected)
   * Then checks if the Objects fit into the material.
   * If not, it adapts their Transform.
   */
  private void fitObjectsIntoMaterial()
  {
    if (this.graphicObjects != null)
    {
      Rectangle2D bb = this.graphicObjects.getBoundingBox();
      if (bb != null)
      {
        if (bb.getX() < 0 || bb.getY() < 0)
        {//Move Object's origin to be positive
          AffineTransform tr = this.graphicObjects.getTransform() == null ? new AffineTransform() : this.graphicObjects.getTransform();
          tr.translate(bb.getX() < 0 ? -bb.getX(): 0, bb.getY() < 0 ? -bb.getY() : 0);
          this.graphicObjects.setTransform(tr);
        }
        if (this.material != null)
        {
          double w = bb.getX() + bb.getWidth();
          double h = bb.getY() + bb.getHeight();
          double mw = Util.mm2px(this.material.getWidth(), 500);
          double mh = Util.mm2px(this.material.getHeight(), 500);
          if (w > mw || h > mh)
          {//scale Object to fit material
            double dw = mw / w;
            double dh = mh / h;
            AffineTransform tr = this.graphicObjects.getTransform() == null ? new AffineTransform() : this.graphicObjects.getTransform();
            tr.scale(Math.min(dw, dh), Math.min(dw, dh));
            this.graphicObjects.setTransform(tr);
          }
        }
      }
    }
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

  private LaserJob prepareJob(String name)
  {
    RasterPart rp = new RasterPart(new LaserProperty());
    Raster3dPart r3dp = new Raster3dPart(new LaserProperty());
    VectorPart vp = new VectorPart(new LaserProperty());

    LaserJob job = new LaserJob(name, "123", "unk", 500, r3dp, vp, rp);
    //Aggregate all Mappings per LaserProfile
    HashMap<LaserProfile, GraphicSet> parts = new LinkedHashMap<LaserProfile, GraphicSet>();
    for (Mapping m : this.getMappings())
    {
      GraphicSet set = m.getA().getMatchingObjects(this.getGraphicObjects());
      LaserProfile p = material.getLaserProfile(m.getProfileName());
      if (parts.containsKey(p))
      {
        for (GraphicObject e : set)
        {
          if (!parts.get(p).contains(e))
          {
            parts.get(p).add(e);
          }
        }
      }
      else
      {
        parts.put(p, set);
      }
    }
    for (Entry<LaserProfile, GraphicSet> e : parts.entrySet())
    {
      e.getKey().addToLaserJob(job, e.getValue(), material.getDepth());
    }
    job.getVectorPart().setFocus(0);
    return job;
  }

  public void sendJob(String name) throws IllegalJobException, SocketTimeoutException, Exception
  {
    LaserCutter instance = this.getSelectedLaserDevice().getLaserCutter();
    LaserJob job = this.prepareJob(name);
    instance.sendJob(job);
  }

  private void setLoadedFile(File f)
  {
    File oldLoadedFile = this.loadedFile;
    this.loadedFile = f;
    this.propertyChangeSupport.firePropertyChange(PROP_LOADEDFILE, oldLoadedFile, f);
  }

  public int estimateTime()
  {
    LaserCutter instance = this.getSelectedLaserDevice().getLaserCutter();
    LaserJob job = this.prepareJob("calc");
    return instance.estimateJobDuration(job);
  }
}
