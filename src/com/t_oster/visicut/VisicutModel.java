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
package com.t_oster.visicut;

import com.t_oster.liblasercut.IllegalJobException;
import com.t_oster.liblasercut.LaserCutter;
import com.t_oster.liblasercut.LaserJob;
import com.t_oster.liblasercut.LaserProperty;
import com.t_oster.liblasercut.ProgressListener;
import com.t_oster.liblasercut.VectorPart;
import com.t_oster.liblasercut.platform.Util;
import com.t_oster.visicut.managers.LaserDeviceManager;
import com.t_oster.visicut.managers.MappingManager;
import com.t_oster.visicut.managers.MaterialManager;
import com.t_oster.visicut.managers.PreferencesManager;
import com.t_oster.visicut.misc.ExtensionFilter;
import com.t_oster.visicut.misc.FileUtils;
import com.t_oster.visicut.misc.Helper;
import com.t_oster.visicut.misc.MultiFilter;
import com.t_oster.visicut.model.LaserDevice;
import com.t_oster.visicut.model.LaserProfile;
import com.t_oster.visicut.model.MaterialProfile;
import com.t_oster.visicut.model.PlfFile;
import com.t_oster.visicut.model.PlfPart;
import com.t_oster.visicut.model.graphicelements.GraphicFileImporter;
import com.t_oster.visicut.model.graphicelements.GraphicSet;
import com.t_oster.visicut.model.graphicelements.ImportException;
import com.t_oster.visicut.model.graphicelements.psvgsupport.ParametricPlfPart;
import com.t_oster.visicut.model.mapping.Mapping;
import com.t_oster.visicut.model.mapping.MappingSet;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
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
import java.util.Arrays;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import javax.imageio.ImageIO;
import javax.swing.filechooser.FileFilter;

/**
 * This class contains the state and business logic of the
 * Application
 *
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class VisicutModel
{
  
  public void addScreenshotOfBackgroundImage(Rectangle crop, Rectangle2D target) throws IOException, FileNotFoundException, ImportException
  {
    File tmp = FileUtils.getNonexistingWritableFile("screenshot.png");
    tmp.deleteOnExit();
    BufferedImage cut = this.backgroundImage.getSubimage(crop.x, crop.y, crop.width, crop.height);
    ImageIO.write(cut, "png", tmp);
    PlfPart p = this.loadGraphicFile(tmp, new LinkedList<String>());
    if (target != null)
    {
      p.getGraphicObjects().setTransform(Helper.getTransform(p.getGraphicObjects().getOriginalBoundingBox(), target));
    }
    this.plfFile.add(p);
    this.propertyChangeSupport.firePropertyChange(PROP_PLF_PART_ADDED, null, p);
    this.setSelectedPart(p);
  }

  /**
   * Duplicates the given PlfPart and adds it to the
   * current PlfFile
   * @param p
   */
  public void duplicate(PlfPart p)
  {
    PlfPart dup = new PlfPart();
    dup.setSourceFile(p.getSourceFile() != null ? p.getSourceFile() : null);
    dup.setMapping(p.getMapping() != null ? p.getMapping().clone() : null);
    if (p.getGraphicObjects() != null)
    {
      dup.setGraphicObjects(p.getGraphicObjects().clone());
    }
    this.plfFile.add(dup);
    this.propertyChangeSupport.firePropertyChange(PROP_PLF_PART_ADDED, null, dup);
  }

  private Point2D.Double startPoint = null;
  public static final String PROP_STARTPOINT = "startPoint";

  public Point2D.Double getStartPoint()
  {
    return startPoint;
  }

  public void setStartPoint(Point2D.Double startPoint)
  {
    Point2D.Double oldStartPoint = this.startPoint;
    this.startPoint = startPoint;
    propertyChangeSupport.firePropertyChange(PROP_STARTPOINT, oldStartPoint, startPoint);
  }

  private PlfPart selectedPart = null;
  public static final String PROP_SELECTEDPART = "selectedPart";

  /**
   * Get the value of selectedPart
   *
   * @return the value of selectedPart
   */
  public PlfPart getSelectedPart()
  {
    return selectedPart;
  }

  public FileFilter getAllFileFilter()
  {
    List<FileFilter> filters = new LinkedList<FileFilter>();
    filters.add(PLFFilter);
    filters.addAll(Arrays.asList(this.getGraphicFileImporter().getFileFilters()));
    return new MultiFilter(filters);
  }
  
  /**
   * Set the value of selectedPart
   *
   * @param selectedPart new value of selectedPart
   */
  public void setSelectedPart(PlfPart selectedPart)
  {
    PlfPart oldSelectedPart = this.selectedPart;
    this.selectedPart = selectedPart;
    if (selectedPart == null && this.plfFile != null && !this.plfFile.isEmpty())
    {
      this.selectedPart = this.plfFile.get(0);
    }
    propertyChangeSupport.firePropertyChange(PROP_SELECTEDPART, oldSelectedPart, selectedPart);
  }

  private PlfFile plfFile = new PlfFile();

  /**
   * Get the value of plfFile
   *
   * @return the value of plfFile
   */
  public PlfFile getPlfFile()
  {
    return plfFile;
  }

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
      List<LaserDevice> devices = LaserDeviceManager.getInstance().getAll();
      if (this.preferences.lastLaserDevice != null)
      {
        for (LaserDevice ld : devices)
        {
          if (this.preferences.lastLaserDevice.equals(ld.getName()))
          {
            this.setSelectedLaserDevice(ld);
            break;
          }
        }
      }
      else if (!devices.isEmpty())
      {//select first laser-device by default
        this.setSelectedLaserDevice(devices.get(0));
      }
      List<MaterialProfile> materials = MaterialManager.getInstance().getAll();
      if (this.preferences.lastMaterial != null)
      {
        for (MaterialProfile mp : materials)
        {
          if (this.preferences.lastMaterial.equals(mp.getName()))
          {
            this.setMaterial(mp);
            break;
          }
        }
      }
      else if (!materials.isEmpty())
      {//use first material by default
        this.setMaterial(materials.get(0));
      }
      this.setUseThicknessAsFocusOffset(this.preferences.isUseThicknessAsFocusOffset());
    }
  }

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
  
  public void loadFile(MappingManager mm, File file, List<String> warnings, boolean discardCurrent) throws FileNotFoundException, IOException, ImportException
  {
    if (PLFFilter.accept(file))
    {
      PlfFile newFile = loadPlfFile(mm, file, warnings);
      if (newFile != null)
      {
        if (discardCurrent)
        {
          this.setPlfFile(newFile);
        }
        else
        {
          if (!newFile.isEmpty())
          {
            for (PlfPart p : newFile)
            {
              this.plfFile.add(p);
              this.propertyChangeSupport.firePropertyChange(PROP_PLF_PART_ADDED, null, p);
            }
            this.setSelectedPart(newFile.get(newFile.size() - 1));
          }
        }
      }
    }
    else
    {
      if (discardCurrent)
      {
        PlfFile nf = new PlfFile();
        nf.setFile(new File(file.getParentFile(), file.getName().substring(0, file.getName().lastIndexOf(".")) +".plf"));
        this.setPlfFile(nf);
      }
      PlfPart p = loadGraphicFile(file, warnings);
      if (this.preferences.getDefaultMapping() != null)
      {
        try
        {
          p.setMapping(MappingManager.getInstance().getItemByName(this.preferences.getDefaultMapping()));
        }
        catch (Exception e)
        {
          System.err.println("Could not load mapping '"+this.preferences.getDefaultMapping()+"'");
        }
      }
      this.plfFile.add(p);
      this.propertyChangeSupport.firePropertyChange(PROP_PLF_PART_ADDED, null, p);
      this.setSelectedPart(p);
    }
  }
  
  private PlfFile loadPlfFile(MappingManager mm, File f, List<String> warnings) throws FileNotFoundException, IOException, ImportException
  {
    ZipFile zip = new ZipFile(f);
    PlfFile resultingFile = new PlfFile();
    resultingFile.setFile(f);
    //Collect for each part the transform,mapping and sourceFile
    Map<Integer,AffineTransform> transforms = new LinkedHashMap<Integer,AffineTransform>();
    Map<Integer,MappingSet> mappings = new LinkedHashMap<Integer, MappingSet>();
    Map<Integer,File> sourceFiles = new LinkedHashMap<Integer, File>();
    Enumeration entries = zip.entries();
    while (entries.hasMoreElements())
    {
      ZipEntry entry = (ZipEntry) entries.nextElement();
      String name = entry.getName();
      Integer i = name.matches("[0-9]+/.*") ? Integer.parseInt(name.split("/")[0]) : 0;
      if (name.equals((i > 0 ? i+"/" : "")+"transform.xml"))
      {
        XMLDecoder decoder = new XMLDecoder(zip.getInputStream(entry));
        transforms.put(i, (AffineTransform) decoder.readObject());
      }
      else if (name.equals((i > 0 ? i+"/" : "")+"mappings.xml"))
      {
        MappingSet map = mm.loadFromFile(zip.getInputStream(entry));
        if (map != null)
        {
          mappings.put(i, map);
        }
        else
        {
          warnings.add("Could not load Mapping "+i+" from PLF File");
        }
      }
      else
      {
        //source files get extracted
        File tempFile = FileUtils.getNonexistingWritableFile(name.replace("/","_"));
        byte[] buf = new byte[1024];
        InputStream in = zip.getInputStream(entry);
        FileOutputStream out = new FileOutputStream(tempFile);
        // Transfer bytes from the file to the ZIP file
        int len;
        while ((len = in.read(buf)) > 0)
        {
          out.write(buf, 0, len);
        }
        out.close();
        in.close();
        tempFile.deleteOnExit();
        //Parameter files for parametric svg files are just extracted next
        //to the svg, but not counted as source file
        if (!name.toLowerCase().endsWith(".parameters"))
        {
          sourceFiles.put(i, tempFile);
        }
      }
    }
    
    for (Integer i : sourceFiles.keySet())
    {
      try
      {
        PlfPart p = this.loadGraphicFile(sourceFiles.get(i), warnings);
        if (p.getGraphicObjects() == null)
        {
          warnings.add("Corrupted input file "+i);
        }
        else
        {
          if (transforms.containsKey(i))
          {
            p.getGraphicObjects().setTransform(transforms.get(i));
          }
          else
          {
            warnings.add("Could not load Transform "+i+" from PLF File");
          }
          if (mappings.containsKey(i))
          {
            p.setMapping(mappings.get(i));
          }
          resultingFile.add(p);
        }
      }
      catch (ImportException e)
      {
        warnings.add("Error loading "+sourceFiles.get(i).getName()+": "+e.getMessage());
      }
    }
    return resultingFile;
  }

  public void saveToFile(MaterialManager pm, MappingManager mm, File f) throws FileNotFoundException, IOException
  {
    PlfFile plf = this.getPlfFile();
    FileInputStream in;
    byte[] buf = new byte[1024];
    int len;
    // Create the ZIP file
    ZipOutputStream out = new ZipOutputStream(new FileOutputStream(f));
    //find temporary file for xml
    int k = 0;
    File tmp = null;
    do
    {
      tmp = new File(Helper.getBasePath(), "tmp" + (k++) + ".xml");
    }
    while (tmp.exists());
    for(int i = 0; i < plf.size(); i++)
    {
      // Add source GraphicsFile to the Zip File
      out.putNextEntry(new ZipEntry((i > 0 ? i+"/" : "")+plf.get(i).getSourceFile().getName()));
      in = new FileInputStream(plf.get(i).getSourceFile());
      // Transfer bytes from the file to the ZIP file
      while ((len = in.read(buf)) > 0)
      {
        out.write(buf, 0, len);
      }
      in.close();
      // Complete the entry
      out.closeEntry();
      //If it's a Parametric PlfPart, write the parameters into an extra file
      if (plf.get(i) instanceof ParametricPlfPart)
      {
        // Add source GraphicsFile to the Zip File
        out.putNextEntry(new ZipEntry((i > 0 ? i+"/" : "")+plf.get(i).getSourceFile().getName()+".parameters"));
        ParametricPlfPart.serializeParameterValues(((ParametricPlfPart) plf.get(i)).getParameters(), out);
        // Complete the entry
        out.closeEntry(); 
      }
      AffineTransform at = plf.get(i).getGraphicObjects().getTransform();
      if (at != null)
      {
        out.putNextEntry(new ZipEntry((i > 0 ? i+"/" : "")+"transform.xml"));
        //Write xml to temp file
        //TODO: Why not directly write to zip output stream?
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
      if (plf.get(i).getMapping() != null)
      {
        out.putNextEntry(new ZipEntry((i > 0 ? i+"/" : "")+"mappings.xml"));
        mm.save(plf.get(i).getMapping(), tmp);
        in = new FileInputStream(tmp);
        // Transfer bytes from the file to the ZIP file
        while ((len = in.read(buf)) > 0)
        {
          out.write(buf, 0, len);
        }
        in.close();
        out.closeEntry();
      }
    }
    // Complete the ZIP file
    out.close();
    // Delete the tmp file
    tmp.delete();
  }
  private GraphicFileImporter graphicFileImporter = null;

  public GraphicFileImporter getGraphicFileImporter()
  {
    if (graphicFileImporter == null)
    {
      graphicFileImporter = new GraphicFileImporter(this.preferences.getAvailableImporters());
    }
    return graphicFileImporter;
  }

  private PlfPart loadGraphicFile(File f, List<String> warnings) throws ImportException
  {
    return this.getGraphicFileImporter().importFile(f, warnings);
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

  private LaserJob prepareJob(String name, Map<LaserProfile, List<LaserProperty>> propmap) throws FileNotFoundException, IOException
  {
    LaserJob job = new LaserJob(name, name, "visicut");
    if (this.startPoint != null)
    {
      job.setStartPoint(this.startPoint.x, this.startPoint.y);
    }
    float focusOffset = this.selectedLaserDevice.getLaserCutter().isAutoFocus() || !this.useThicknessAsFocusOffset ? 0 : this.materialThickness;

    for (PlfPart p : this.getPlfFile())
    {
      if (p.getMapping() == null)
      {
        continue;
      }
      for (Mapping m : p.getMapping())
      {
        GraphicSet set = m.getFilterSet() != null ? m.getFilterSet().getMatchingObjects(p.getGraphicObjects()) : p.getUnmatchedObjects();
        LaserProfile pr = m.getProfile();
        if (pr == null)//ignore-profile
        {
          continue;
        }
        List<LaserProperty> props = propmap.get(pr);
        pr.addToLaserJob(job, set, this.addFocusOffset(props, focusOffset));
      }
    }
    return job;
  }

  public void sendJob(String name, ProgressListener pl, Map<LaserProfile, List<LaserProperty>> props, List<String> warnings) throws IllegalJobException, SocketTimeoutException, Exception
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
      lasercutter.sendJob(job, pl, warnings);
    }
    else
    {
      lasercutter.sendJob(job, warnings);
    }
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

  //moves the laser head to the given position (in mm)
  public void moveHeadTo(Point2D.Double p)
  {
    try
    {
      LaserCutter lasercutter = this.getSelectedLaserDevice().getLaserCutter();
      LaserJob job = new LaserJob("move", "move", "visicut");
      if (this.startPoint != null)
      {
        job.setStartPoint(this.startPoint.x, this.startPoint.y);
      }
      double dpi = lasercutter.getResolutions().get(lasercutter.getResolutions().size()-1);
      double factor = Util.dpi2dpmm(dpi);
      AffineTransform mm2laserpx = AffineTransform.getScaleInstance(factor, factor);
      VectorPart part = new VectorPart(lasercutter.getLaserPropertyForVectorPart(), dpi);
      mm2laserpx.transform(p, p);
      part.moveto((int) p.x, (int) p.y);
      job.addPart(part);
      lasercutter.sendJob(job);
    }
    catch (IllegalJobException ex)
    {
      Logger.getLogger(VisicutModel.class.getName()).log(Level.SEVERE, null, ex);
    }
    catch (Exception ex)
    {
      Logger.getLogger(VisicutModel.class.getName()).log(Level.SEVERE, null, ex);
    }
  }

  public enum Modification
  {
    NONE,
    RESIZE,
    MOVE,
    ROTATE
  }
  
  /**
   * Adjusts the Transform of the Graphic-Objects such that the Objects
   * fit into the current Laser-Bed.
   * If modification was necessary, it returns true.
   * @return
   */
  public Modification fitObjectsIntoBed()
  {
    //TODO rotate file 90° if it would fit then
    Modification result = Modification.NONE;
    double bw = getSelectedLaserDevice() != null ? getSelectedLaserDevice().getLaserCutter().getBedWidth() : 600d;
    double bh = getSelectedLaserDevice() != null ? getSelectedLaserDevice().getLaserCutter().getBedHeight() : 300d;
    for(PlfPart p : this.plfFile)
    {
      boolean modified = false;
      Rectangle2D bb = p.getGraphicObjects().getBoundingBox();
      AffineTransform trans = p.getGraphicObjects().getTransform();
      //first try moving to origin, if not in range
      if (bb.getX() < 0 || bb.getX() + bb.getWidth() > bw)
      {
        trans.preConcatenate(AffineTransform.getTranslateInstance(-bb.getX(), 0));
        p.getGraphicObjects().setTransform(trans);
        bb = p.getGraphicObjects().getBoundingBox();
        modified = true;
        result = Modification.MOVE;
      }
      if (bb.getY() < 0 || bb.getY() + bb.getHeight() > bh)
      {
        trans.preConcatenate(AffineTransform.getTranslateInstance(0, -bb.getY()));
        p.getGraphicObjects().setTransform(trans);
        bb = p.getGraphicObjects().getBoundingBox();
        modified = true;
        result = Modification.MOVE;
      }
      //if still too big (we're in origin now) check if rotation is useful 
      if (bb.getX() + bb.getWidth() > bw || bb.getY() + bb.getHeight() > bh)
      {
        //check if laser-bed and graphic are not in the same orientation (landscape vs portrait)
        if ((bw / bh >= 1) != (bb.getWidth() / bb.getHeight() >= 1))
        {//if so, rotate the graphic 90 degrees, keeping the x and y value
          double oldX = bb.getX();
          double oldY = bb.getY();
          trans.preConcatenate(AffineTransform.getQuadrantRotateInstance(3));
          p.getGraphicObjects().setTransform(trans);
          bb = p.getGraphicObjects().getBoundingBox();
          //move to old position
          trans.preConcatenate(AffineTransform.getTranslateInstance(oldX-bb.getX(), oldY-bb.getY()));
          p.getGraphicObjects().setTransform(trans);
          bb = p.getGraphicObjects().getBoundingBox();
          result = Modification.ROTATE;
        }
      }
      //Do not scale the object, because this might be very confising for the user
      //if still too big (we're in origin now) we have to resize, but keeping 
      if (bb.getX() + bb.getWidth() > bw || bb.getY() + bb.getHeight() > bh)
      {
        double factor = Math.min(bw/bb.getWidth(), bh/bb.getHeight());
        trans.preConcatenate(AffineTransform.getScaleInstance(factor, factor));
        p.getGraphicObjects().setTransform(trans);
        modified = true;
        result = Modification.RESIZE;
      }
      if (modified)
      {
        this.firePartUpdated(p);
      }
    }
    return result;
  }

  public static final String PROP_PLF_FILE_CHANGED = "plf file changed";
  public static final String PROP_PLF_PART_ADDED = "plf part added";
  public static final String PROP_PLF_PART_REMOVED = "plf part removed";
  public static final String PROP_PLF_PART_UPDATED = "plf part updated";

  private void setPlfFile(PlfFile resultingFile)
  {
    this.plfFile = resultingFile;
    this.setSelectedPart(null);
    this.propertyChangeSupport.firePropertyChange(PROP_PLF_FILE_CHANGED, null, plfFile);
  }

  public void newPlfFile()
  {
    this.setPlfFile(new PlfFile());
  }

  public void reloadSelectedPart(LinkedList<String> warnings) throws ImportException
  {
    if (this.selectedPart != null)
    {
      AffineTransform tr = this.selectedPart.getGraphicObjects().getTransform();   
      PlfPart p = this.loadGraphicFile(this.selectedPart.getSourceFile(), warnings);
      p.getGraphicObjects().setTransform(tr);
      this.selectedPart.setGraphicObjects(p.getGraphicObjects());
      if (p instanceof ParametricPlfPart && this.selectedPart instanceof ParametricPlfPart)
      {
        ((ParametricPlfPart) this.selectedPart).setParameters(((ParametricPlfPart) p).getParameters());
      }
      this.propertyChangeSupport.firePropertyChange(PROP_PLF_PART_UPDATED, null, this.selectedPart);
    }
  }

  public void removePlfPart(PlfPart p)
  {
    if (p.equals(this.selectedPart))
    {
      this.setSelectedPart(null);
    }
    this.plfFile.remove(p);
    this.propertyChangeSupport.firePropertyChange(PROP_PLF_PART_REMOVED, p, null);
  }
  
  public void removeSelectedPart()
  {
    if (this.selectedPart != null)
    {
      this.removePlfPart(this.selectedPart);
    }
    this.setSelectedPart(this.plfFile != null && !this.plfFile.isEmpty() ? this.plfFile.get(0) : null);
  }

  public void firePartUpdated(PlfPart p)
  {
    this.propertyChangeSupport.firePropertyChange(PROP_PLF_PART_UPDATED, null, p);
  }
}
