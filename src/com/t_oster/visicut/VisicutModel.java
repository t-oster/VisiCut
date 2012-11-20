/**
 * This file is part of VisiCut.
 * Copyright (C) 2012 Thomas Oster <thomas.oster@rwth-aachen.de>
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

import com.t_oster.liblasercut.IllegalJobException;
import com.t_oster.liblasercut.LaserCutter;
import com.t_oster.liblasercut.LaserJob;
import com.t_oster.liblasercut.LaserProperty;
import com.t_oster.liblasercut.ProgressListener;
import com.t_oster.liblasercut.platform.Util;
import com.t_oster.visicut.managers.LaserDeviceManager;
import com.t_oster.visicut.managers.MappingManager;
import com.t_oster.visicut.managers.MaterialManager;
import com.t_oster.visicut.managers.PreferencesManager;
import com.t_oster.visicut.managers.ProfileManager;
import com.t_oster.visicut.misc.ExtensionFilter;
import com.t_oster.visicut.misc.Helper;
import com.t_oster.visicut.model.LaserDevice;
import com.t_oster.visicut.model.LaserProfile;
import com.t_oster.visicut.model.MaterialProfile;
import com.t_oster.visicut.model.graphicelements.GraphicFileImporter;
import com.t_oster.visicut.model.graphicelements.GraphicSet;
import com.t_oster.visicut.model.graphicelements.ImportException;
import com.t_oster.visicut.model.mapping.FilterSet;
import com.t_oster.visicut.model.mapping.Mapping;
import com.t_oster.visicut.model.mapping.MappingSet;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.beans.Encoder;
import java.beans.Expression;
import java.beans.PersistenceDelegate;
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
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

/**
 * This class contains the state and business logic of the
 * Application
 *
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class VisicutModel
{

  protected float materialThickness = 0;
  public static final String PROP_MATERIALTHICKNESS = "materialThickness";

  /**
   * Get the value of materialThickness
   *
   * @return the value of materialThickness
   */
  public float getMaterialThickness()
  {
    return materialThickness;
  }

  /**
   * Set the value of materialThickness
   *
   * @param materialThickness new value of materialThickness
   */
  public void setMaterialThickness(float materialThickness)
  {
    float oldMaterialThickness = this.materialThickness;
    this.materialThickness = materialThickness;
    propertyChangeSupport.firePropertyChange(PROP_MATERIALTHICKNESS, oldMaterialThickness, materialThickness);
  }

  protected boolean useThicknessAsFocusOffset = true;
  public static final String PROP_USETHICKNESSASFOCUSOFFSET = "useThicknessAsFocusOffset";

  /**
   * Get the value of useThicknessAsFocusOffset
   *
   * @return the value of useThicknessAsFocusOffset
   */
  public boolean isUseThicknessAsFocusOffset()
  {
    return useThicknessAsFocusOffset;
  }

  /**
   * Set the value of useThicknessAsFocusOffset
   *
   * @param useThicknessAsFocusOffset new value of useThicknessAsFocusOffset
   */
  public void setUseThicknessAsFocusOffset(boolean useThicknessAsFocusOffset)
  {
    boolean oldUseThicknessAsFocusOffset = this.useThicknessAsFocusOffset;
    this.useThicknessAsFocusOffset = useThicknessAsFocusOffset;
    propertyChangeSupport.firePropertyChange(PROP_USETHICKNESSASFOCUSOFFSET, oldUseThicknessAsFocusOffset, useThicknessAsFocusOffset);
  }


  public static final FileFilter PLFFilter = new ExtensionFilter(".plf", "VisiCut Portable Laser Format (*.plf)");

  private static VisicutModel instance;

  public static VisicutModel getInstance()
  {
    if (instance == null){
      instance = new VisicutModel();
    }
    return instance;
  }

  /**
   * This Constructor is only for the UI Editor to run properly
   * Do not use. use getInstance() instead.
   */
  public VisicutModel()
  {
    if (instance != null)
    {
      System.err.println("Should not use public Constructor of VisicutModel");
    }
  }

  /**
     * Generates an Everything=> Profile mapping for every
     * Occuring MaterialProfile
     * @return
     */
  public List<MappingSet> generateDefaultMappings()
  {
    List<MappingSet> result = new LinkedList<MappingSet>();
    Set<String> profiles = new LinkedHashSet<String>();
    for (LaserProfile lp:ProfileManager.getInstance().getAll())
    {
      if (!profiles.contains(lp.getName()))
      {
        profiles.add(lp.getName());
        MappingSet set = new MappingSet();
        set.add(new Mapping(new FilterSet(), lp));
        set.setName(java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/beans/resources/CustomMappingPanel").getString("EVERYTHING")+"=>"+lp.getName());
        set.setDescription("An auto-generated mapping");
        result.add(set);
      }
    }
    return result;
  }

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
    if (this.preferences != null)
    {
      this.graphicFileImporter = null;
      if (this.preferences.lastLaserDevice != null)
      {
        for (LaserDevice ld : LaserDeviceManager.getInstance().getAll())
        {
          if (this.preferences.lastLaserDevice.equals(ld.getName()))
          {
            this.setSelectedLaserDevice(ld);
            break;
          }
        }
      }
      if (this.preferences.lastMaterial != null)
      {
        for (MaterialProfile mp : MaterialManager.getInstance().getAll())
        {
          if (this.preferences.lastMaterial.equals(mp.getName()))
          {
            this.setMaterial(mp);
            break;
          }
        }
      }
      this.setUseThicknessAsFocusOffset(this.preferences.isUseThicknessAsFocusOffset());
    }
  }
  protected GraphicSet graphicObjects = null;
  public static final String PROP_GRAPHICOBJECTS = "graphicObjects";

  public void updatePreferences()
  {
    this.preferences.setLastLaserDevice(this.selectedLaserDevice == null ? null : selectedLaserDevice.getName());
    this.preferences.setLastMaterial(this.material == null ? null : material.getName());
    this.preferences.setUseThicknessAsFocusOffset(this.useThicknessAsFocusOffset);
    try
    {
      PreferencesManager.getInstance().savePreferences();
    }
    catch (Exception ex)
    {
      Logger.getLogger(VisicutModel.class.getName()).log(Level.SEVERE, null, ex);
    }
  }

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

  public void loadFromFile(MappingManager mm, File f, List<String> warnings) throws FileNotFoundException, IOException, ImportException
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
        loadedMappings = mm.loadFromFile(zip.getInputStream(entry));
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
    if (loadedMappings == null)
    {
      //TODO: i10n
      JOptionPane.showMessageDialog(null, "Could not load Mapping from PLF File");
    }
    else
    {
      this.setMappings(loadedMappings);
    }
    if (transform == null)
    {
      //TODO: i10n
      JOptionPane.showMessageDialog(null, "Could not load Transform from PLF File");
    }
    if (inputFile == null || !inputFile.exists())
    {
      //TODO: i10n
      throw new ImportException("Corrupted Input File");
    }
    inputFile.deleteOnExit();
    GraphicSet gs = this.loadSetFromFile(inputFile, warnings);
    this.setSourceFile(inputFile);
    if (gs != null)
    {
      if (transform != null)
      {
        gs.setTransform(transform);
      }
      this.setGraphicObjects(gs);
      this.setLoadedFile(f);
    }
    else
    {
      throw new ImportException("Can not load included graphic File");
    }
  }

  public void saveToFile(MaterialManager pm, MappingManager mm, File f) throws FileNotFoundException, IOException
  {
    FileInputStream in;
    byte[] buf = new byte[1024];
    int len;
    // Create the ZIP file
    ZipOutputStream out = new ZipOutputStream(new FileOutputStream(f));
    if (this.getSourceFile() != null)
    {
      // Add source GraphicsFile to the Zip File
      out.putNextEntry(new ZipEntry(this.getSourceFile().getName()));
      in = new FileInputStream(this.getSourceFile());
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
      tmp = new File(Helper.getBasePath(), "tmp" + (i++) + ".xml");
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
    mm.save(this.getMappings(), tmp);
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
      graphicFileImporter = new GraphicFileImporter(this.preferences.getAvailableImporters());
    }
    return graphicFileImporter;
  }

  private GraphicSet loadSetFromFile(File f, List<String> warnings) throws ImportException
  {
    GraphicFileImporter im = this.getGraphicFileImporter();
    GraphicSet set = im.importFile(f, warnings);
    return set;
  }

  public void loadGraphicFile(File f, List<String> warnings) throws ImportException
  {
    this.loadGraphicFile(f, warnings, false);
  }

  public static final String PROP_SOURCEFILE = "sourceFile";

  /**
   * Returns the source file of the current Image.
   * In case of PLF this is a temporary file extracted from the plf
   * @return
   */
  public File getSourceFile()
  {
    return this.sourceFile;
  }

  private void setSourceFile(File f)
  {
    if (Util.differ(this.sourceFile, f))
    {
      File oldValue = this.sourceFile;
      this.sourceFile = f;
      this.propertyChangeSupport.firePropertyChange(PROP_SOURCEFILE, oldValue, f);
    }
  }

  public void loadGraphicFile(File f, List<String> warnings, boolean keepTransform) throws ImportException
  {
    AffineTransform at = null;
    if (keepTransform && this.getGraphicObjects() != null)
    {
      at = this.getGraphicObjects().getTransform();
    }
    GraphicSet gs = this.loadSetFromFile(f, warnings);
    if (gs != null)
    {
      if (at != null)
      {
        gs.setTransform(at);
      }
      this.setGraphicObjects(gs);
      this.setSourceFile(f);
    }
    this.setLoadedFile(null);
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
          tr.preConcatenate(AffineTransform.getTranslateInstance(bb.getX() < 0 ? -bb.getX(): 0, bb.getY() < 0 ? -bb.getY() : 0));
          this.graphicObjects.setTransform(tr);
          bb = this.graphicObjects.getBoundingBox();
        }
        if (this.selectedLaserDevice != null)
        {
          double w = bb.getX() + bb.getWidth();
          double h = bb.getY() + bb.getHeight();
          double mw = this.selectedLaserDevice.getLaserCutter().getBedWidth();
          double mh = this.selectedLaserDevice.getLaserCutter().getBedHeight();
          if (w > mw || h > mh)
          {//scale Object to fit laser-bed
            double dw = mw / w;
            double dh = mh / h;
            AffineTransform tr = this.graphicObjects.getTransform() == null ? new AffineTransform() : this.graphicObjects.getTransform();
            tr.preConcatenate(AffineTransform.getScaleInstance(Math.min(dw, dh), Math.min(dw, dh))  );
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

  private LaserJob prepareJob(String name, Map<LaserProfile, List<LaserProperty>> propmap) throws FileNotFoundException, IOException
  {
    LaserJob job = new LaserJob(name, name, "visicut");

    float focusOffset = this.selectedLaserDevice.getLaserCutter().isAutoFocus() || !this.useThicknessAsFocusOffset ? 0 : this.materialThickness;

    for (Mapping m : this.getMappings())
    {
      GraphicSet set = m.getFilterSet().getMatchingObjects(this.getGraphicObjects());
      LaserProfile p = m.getProfile();
      List<LaserProperty> props = propmap.get(p);
      p.addToLaserJob(job, set, this.addFocusOffset(props, focusOffset));
    }

    return job;
  }

  public void sendJob(String name, ProgressListener pl, Map<LaserProfile, List<LaserProperty>> props) throws IllegalJobException, SocketTimeoutException, Exception
  {
    LaserCutter lasercutter = this.getSelectedLaserDevice().getLaserCutter();
    if (pl != null)
    {
      pl.taskChanged(this, "preparing job");
    }
    LaserJob job = this.prepareJob(name, props);
    if (pl != null)
    {
      pl.taskChanged(this, "sending job");
      lasercutter.sendJob(job, pl);
    }
    else
    {
      lasercutter.sendJob(job);
    }
  }

  private void setLoadedFile(File f)
  {
    File oldLoadedFile = this.loadedFile;
    this.loadedFile = f;
    this.propertyChangeSupport.firePropertyChange(PROP_LOADEDFILE, oldLoadedFile, f);
  }

  public int estimateTime(Map<LaserProfile, List<LaserProperty>> propmap) throws FileNotFoundException, IOException
  {
    LaserCutter lc = this.getSelectedLaserDevice().getLaserCutter();
    LaserJob job = this.prepareJob("calc", propmap);
    return lc.estimateJobDuration(job);
  }

  private List<LaserProperty> addFocusOffset(List<LaserProperty> props, float focusOffset)
  {
    if (focusOffset == 0 || props.isEmpty() || props.get(0).getProperty("focus") == null)
    {
      return props;
    }
    List<LaserProperty> result = new LinkedList<LaserProperty>();
    for(LaserProperty p:props)
    {
      LaserProperty c = p.clone();
      Object foc = c.getProperty("focus");
      if (foc instanceof Integer)
      {
        c.setProperty("focus", (Integer) (((Integer) foc)+ (int) focusOffset));
      }
      else if (foc instanceof Float)
      {
        c.setProperty("focus", (Float) foc + focusOffset);
      }
      else if (foc instanceof Double)
      {
        c.setProperty("focus", (Double) (((Double) foc)+ (double) focusOffset));
      }
      result.add(c);
    }
    return result;
  }

  /**
   * Adjusts the Transform of the Graphic-Objects such that the Objects
   * fit into the current Laser-Bed.
   * If modification was necessary, it returns true.
   * @return
   */
  public boolean fitMaterialIntoBed()
  {
    boolean modified = false;
    double bw = getSelectedLaserDevice() != null ? getSelectedLaserDevice().getLaserCutter().getBedWidth() : 600d;
    double bh = getSelectedLaserDevice() != null ? getSelectedLaserDevice().getLaserCutter().getBedHeight() : 300d;
    if (getGraphicObjects() != null)
    {
      Rectangle2D bb = this.getGraphicObjects().getBoundingBox();
      AffineTransform trans = getGraphicObjects().getTransform();
      //first try moving to origin, if not in range
      if (bb.getX() < 0 || bb.getX() + bb.getWidth() > bw)
      {
        trans.preConcatenate(AffineTransform.getTranslateInstance(-bb.getX(), 0));
        getGraphicObjects().setTransform(trans);
        bb = this.getGraphicObjects().getBoundingBox();
        modified = true;
      }
      if (bb.getY() < 0 || bb.getY() + bb.getHeight() > bh)
      {
        trans.preConcatenate(AffineTransform.getTranslateInstance(0, -bb.getY()));
        getGraphicObjects().setTransform(trans);
        bb = this.getGraphicObjects().getBoundingBox();
        modified = true;
      }
      //if still too big (we're in origin now) we have to resize
      if (bb.getX() + bb.getWidth() > bw)
      {
        trans.preConcatenate(AffineTransform.getScaleInstance(bw/bb.getWidth(), bw/bb.getWidth()));
        getGraphicObjects().setTransform(trans);
        bb = this.getGraphicObjects().getBoundingBox();
        modified = true;
      }
      if (bb.getY() + bb.getHeight() > bh)
      {
        trans.preConcatenate(AffineTransform.getScaleInstance(bh/bb.getWidth(), bh/bb.getHeight()));
        getGraphicObjects().setTransform(trans);
        modified = true;
      }
    }
    return modified;
  }
}
