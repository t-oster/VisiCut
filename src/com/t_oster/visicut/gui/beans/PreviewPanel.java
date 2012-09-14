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
package com.t_oster.visicut.gui.beans;

import com.t_oster.liblasercut.LaserCutter;
import com.t_oster.liblasercut.ProgressListener;
import com.t_oster.visicut.VisicutModel;
import com.t_oster.visicut.misc.Helper;
import com.t_oster.visicut.model.LaserDevice;
import com.t_oster.visicut.model.LaserProfile;
import com.t_oster.visicut.model.MaterialProfile;
import com.t_oster.visicut.model.RasterProfile;
import com.t_oster.visicut.model.Raster3dProfile;
import com.t_oster.visicut.model.VectorProfile;
import com.t_oster.visicut.model.mapping.Mapping;
import com.t_oster.visicut.model.graphicelements.GraphicObject;
import com.t_oster.visicut.model.graphicelements.GraphicSet;
import com.t_oster.visicut.model.mapping.FilterSet;
import com.t_oster.visicut.model.mapping.MappingSet;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;

/**
 * This class implements the Panel which provides the Preview
 * of the current LaserJob
 * 
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class PreviewPanel extends ZoomablePanel
{

  private double bedWidth = 600;
  private double bedHeight = 300;
  
  public PreviewPanel()
  {
    VisicutModel.getInstance().addPropertyChangeListener(new PropertyChangeListener(){

      public void propertyChange(PropertyChangeEvent pce)
      {
        if (VisicutModel.PROP_SELECTEDLASERDEVICE.equals(pce.getPropertyName()))
        {
          if (pce.getNewValue() != null && pce.getNewValue() instanceof LaserDevice)
          {
            LaserCutter lc = ((LaserDevice) pce.getNewValue()).getLaserCutter();
            PreviewPanel.this.bedWidth = lc.getBedWidth();
            PreviewPanel.this.bedHeight = lc.getBedHeight();
          }
        }
      }
    });
  }
  
  /**
   * This transform is for mapping lasercutter
   * coordinates on the image from the camera
   */
  private AffineTransform previewTransformation;
  /**
   * Get the value of previewTransformation
   *
   * @return the value of previewTransformation
   */
  public AffineTransform getPreviewTransformation()
  {
    return previewTransformation;
  }

  /**
   * Set the value of previewTransformation
   *
   * @param previewTransformation new value of previewTransformation
   */
  public void setPreviewTransformation(AffineTransform previewTransformation)
  {
    this.previewTransformation = previewTransformation;
    this.repaint();
  }
  
  private Logger logger = Logger.getLogger(PreviewPanel.class.getName());
  
  private class ImageProcessingThread extends Thread implements ProgressListener
  {

    private Logger logger = Logger.getLogger(ImageProcessingThread.class.getName());
    private BufferedImage buffer = null;
    private GraphicSet set;
    private LaserProfile p;
    private Rectangle2D bb;
    private int progress = 0;
    private boolean isFinished = false;

    public Rectangle2D getBoundingBox()
    {
      return bb;
    }

    public BufferedImage getImage()
    {
      return buffer;
    }

    public ImageProcessingThread(GraphicSet objects, LaserProfile p) 
    {
      this.set = objects;
      this.p = p;
      bb = set.getBoundingBox();
      if (bb == null || bb.getWidth() == 0 || bb.getHeight() == 0)
      {
        logger.log(Level.SEVERE, "invalid BoundingBox");
        throw new IllegalArgumentException("Boundingbox zero");
      }
    }

    public synchronized boolean isFinished()
    {
      return isFinished;
    }

    private synchronized void setFinished(boolean finished)
    {
      this.isFinished = finished;
    }

    private void render()
    {
      if (p instanceof RasterProfile)
      {
        RasterProfile rp = (RasterProfile) p;
        buffer = rp.getRenderedPreview(set, material, this);
      }
      else
      {
        buffer = new BufferedImage((int) bb.getWidth(), (int) bb.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D gg = buffer.createGraphics();
        //Normalize Rendering to 0,0
        gg.setTransform(AffineTransform.getTranslateInstance(-bb.getX(), -bb.getY()));
        if (p instanceof Raster3dProfile)
        {
          ((Raster3dProfile) p).renderPreview(gg, set, PreviewPanel.this.getMaterial(), this);
        }
        else
        {
          p.renderPreview(gg, set, PreviewPanel.this.getMaterial());
        }
      }
    }

    @Override
    public void run()
    {
      try
      {
        logger.log(Level.FINE, "Rendering started");
        long start = System.currentTimeMillis();
        render();
        long stop = System.currentTimeMillis();
        logger.log(Level.FINE, "Rendering finished. Took: "+(stop-start)+" ms");
      }
      catch (OutOfMemoryError e)
      {
        logger.log(Level.FINE, "Out of memory during rendering. Staring garbage collection");
        System.gc();
        try
        {
          logger.log(Level.FINE, "Re started Rendering");
          long start = System.currentTimeMillis();
          render();
          long stop = System.currentTimeMillis();
          logger.log(Level.FINE, "2nd Rendering took "+(stop-start)+" ms");
        }
        catch (OutOfMemoryError ee)
        {
          JOptionPane.showMessageDialog(PreviewPanel.this, "Error: Not enough Memory\nPlease start the Program from the provided shell scripts instead of running the .jar file", "Error: Out of Memory", JOptionPane.ERROR_MESSAGE);
        }
      }
      this.setFinished(true);
      PreviewPanel.this.repaint();
      logger.log(Level.FINE, "Thread finished");
    }

    private void cancel()
    {
      logger.log(Level.FINE, "Canceling Thread");
      this.stop();
      this.buffer = null;
      this.set = null;
      logger.log(Level.FINE, "Thread canceled");
    }

    public int getProgress()
    {
      return this.progress;
    }
    
    public void progressChanged(Object source, int percent)
    {
      this.progress = percent;
      PreviewPanel.this.repaint();
    }

    public void taskChanged(Object source, String taskName)
    {
      
    }
  }
  protected Integer resolution = 500;

  /**
   * Get the value of resolution
   *
   * @return the value of resolution
   */
  public Integer getResolution()
  {
    return resolution;
  }

  /**
   * Set the value of resolution
   *
   * @param resolution new value of resolution
   */
  public void setResolution(Integer resolution)
  {
    int oldResolution = this.resolution == null ? 500 : this.resolution;
    this.resolution = resolution == null ? 500 : resolution;
    Point ctr = this.getCenter();
    if (ctr != null)
    {
      AffineTransform.getScaleInstance((double) this.resolution/oldResolution, (double) this.resolution/oldResolution).transform(ctr, ctr);
      this.setCenter(ctr);
    }
    this.setMaterial(this.getMaterial());
    if (this.editRectangle != null)
    {
      this.setEditRectangle(new EditRectangle(getGraphicObjects().getBoundingBox()));
    }
  }

  
  protected boolean drawPreview = true;
  public static final String PROP_DRAWPREVIEW = "drawPreview";

  /**
   * Get the value of drawPreview
   *
   * @return the value of drawPreview
   */
  public boolean isDrawPreview()
  {
    return drawPreview;
  }

  /**
   * Set the value of drawPreview
   *
   * @param drawPreview new value of drawPreview
   */
  public void setDrawPreview(boolean drawPreview)
  {
    boolean oldDrawPreview = this.drawPreview;
    this.drawPreview = drawPreview;
    firePropertyChange(PROP_DRAWPREVIEW, oldDrawPreview, drawPreview);
    repaint();
  }
  protected boolean highlightCutLines = true;

  /**
   * Get the value of highlightCutLines
   *
   * @return the value of highlightCutLines
   */
  public boolean isHighlightCutLines()
  {
    return highlightCutLines;
  }

  /**
   * Set the value of highlightCutLines
   *
   * @param highlightCutLines new value of highlightCutLines
   */
  public void setHighlightCutLines(boolean highlightCutLines)
  {
    boolean oldHighlightCutLines = this.highlightCutLines;
    this.highlightCutLines = highlightCutLines;
    this.firePropertyChange("highlightCutLines", oldHighlightCutLines, highlightCutLines);
    this.repaint();
  }
  protected boolean showGrid = false;

  /**
   * Get the value of showGrid
   *
   * @return the value of showGrid
   */
  public boolean isShowGrid()
  {
    return showGrid;
  }

  /**
   * Set the value of showGrid
   *
   * @param showGrid new value of showGrid
   */
  public void setShowGrid(boolean showGrid)
  {
    boolean oldShowGrid = this.showGrid;
    this.showGrid = showGrid;
    this.firePropertyChange("showGrid", oldShowGrid, showGrid);
    this.repaint();
  }
  private AffineTransform lastDrawnTransform = null;

  public AffineTransform getLastDrawnTransform()
  {
    return lastDrawnTransform;
  }

  protected RenderedImage backgroundImage = null;

  public void ClearCache()
  {
    synchronized(this.renderBuffer)
    {
      for (ImageProcessingThread thr : this.renderBuffer.values())
      {
        if (!thr.isFinished())
        {
          thr.cancel();
        }
      }
    }
    this.renderBuffer.clear();
  }
  protected boolean showBackgroundImage = true;

  /**
   * Get the value of showBackgroundImage
   *
   * @return the value of showBackgroundImage
   */
  public boolean isShowBackgroundImage()
  {
    return showBackgroundImage;
  }

  /**
   * Set the value of showBackgroundImage
   *
   * @param showBackgroundImage new value of showBackgroundImage
   */
  public void setShowBackgroundImage(boolean showBackgroundImage)
  {
    this.showBackgroundImage = showBackgroundImage;
    if (this.backgroundImage != null && this.showBackgroundImage)
    {
      this.setOuterBounds(new Dimension(backgroundImage.getWidth(), backgroundImage.getHeight()));
    }
    else
    {
      this.setOuterBounds(new Dimension((int) Helper.mm2px(this.bedWidth), (int) Helper.mm2px(this.bedHeight)));
    }
    this.repaint();
  }

  /**
   * Get the value of backgroundImage
   *
   * @return the value of backgroundImage
   */
  public RenderedImage getBackgroundImage()
  {
    return backgroundImage;
  }

  /**
   * Set the value of backgroundImage
   *
   * @param backgroundImage new value of backgroundImage
   */
  public void setBackgroundImage(RenderedImage backgroundImage)
  {
    this.backgroundImage = backgroundImage;
    if (this.backgroundImage != null && this.showBackgroundImage)
    {
      this.setOuterBounds(new Dimension(backgroundImage.getWidth(), backgroundImage.getHeight()));
    }
    else
    {
      this.setOuterBounds(new Dimension((int) Helper.mm2px(this.bedWidth), (int) Helper.mm2px(this.bedHeight)));
    }
    this.repaint();
  }
  protected MaterialProfile material = new MaterialProfile();

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
    this.material = material;
    this.renderBuffer.clear();
    this.repaint();
  }
  protected EditRectangle editRectangle = null;

  /**
   * Get the value of editRectangle
   *
   * @return the value of editRectangle
   */
  public EditRectangle getEditRectangle()
  {
    return editRectangle;
  }

  /**
   * Set the value of editRectangle
   * The EditRectangele is drawn if
   * The Values of the Rectangle are exspected to be
   * in LaserCutter Coordinate Space.
   *
   * @param editRectangle new value of editRectangle
   */
  public void setEditRectangle(EditRectangle editRectangle)
  {
    this.editRectangle = editRectangle;
    this.repaint();
  }
  protected GraphicSet graphicObjects = null;

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
    this.graphicObjects = graphicObjects;
    if (graphicObjects != null && (this.backgroundImage == null || !this.showBackgroundImage) && this.material == null)
    {
      Rectangle bb = Helper.toRect(this.graphicObjects.getBoundingBox());
      this.setOuterBounds(new Dimension(bb.x + bb.width, bb.y + bb.height));
    }
    this.renderBuffer.clear();
    this.repaint();
  }
  /**
   * The renderBuffer contains a BufferedImage for each Mapping.
   * On refreshRenderBuffer, the Images are created by rendering
   * the GraphicElements matching the mapping onto an Image,
   * with the size of the BoundingBox.
   * When drawn the offset of the BoundingBox has to be used as Upper Left
   * Corner
   */
  private final HashMap<Mapping, ImageProcessingThread> renderBuffer = new LinkedHashMap<Mapping, ImageProcessingThread>();

  @Override
  protected void paintComponent(Graphics g)
  {
    super.paintComponent(g);
    if (g instanceof Graphics2D)
    {
      Graphics2D gg = (Graphics2D) g;
      gg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
      gg.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
      if (backgroundImage != null && showBackgroundImage)
      {
        gg.drawRenderedImage(backgroundImage, null);
        if (this.previewTransformation != null)
        {
          AffineTransform current = gg.getTransform();
          current.concatenate(this.getPreviewTransformation());
          gg.setTransform(current);
        }
      }
      if (this.backgroundImage != null && showBackgroundImage)
      {
        gg.setColor(Color.BLACK);
        gg.drawRect(0, 0, (int) Helper.mm2px(this.bedWidth), (int) Helper.mm2px(this.bedHeight));
      }
      else
      {
        gg.setColor(this.material != null && this.material.getColor() != null ? this.material.getColor() : Color.WHITE);
        gg.fillRect(0, 0, (int) Helper.mm2px(this.bedWidth), (int) Helper.mm2px(this.bedHeight));
      }
      if (showGrid)
      {
        gg.setColor(Color.DARK_GRAY);
        drawGrid(gg);
      }
      if (this.getGraphicObjects() != null)
      {
        MappingSet mappingsToDraw = this.getMappings();
        if (this.getMaterial() == null || mappingsToDraw == null || mappingsToDraw.isEmpty())
        {//Just draw the original Image
          mappingsToDraw = new MappingSet();
          mappingsToDraw.add(new Mapping(new FilterSet(), null));
        }
        boolean somethingMatched = false;
        for (Mapping m : mappingsToDraw)
        {//Render Original Image
          if (m.getProfileName() == null)
          {
            AffineTransform bak = gg.getTransform();
            if (this.graphicObjects.getTransform() != null)
            {
              AffineTransform trans = gg.getTransform();
              trans.concatenate(this.graphicObjects.getTransform());
              gg.setTransform(trans);
            }
            for (GraphicObject o : m.getFilterSet().getMatchingObjects(this.getGraphicObjects()))
            {
              somethingMatched = true;
              o.render(gg);
            }
            gg.setTransform(bak);
          }
          else if (this.getMaterial().getLaserProfile(m.getProfileName()) != null)
          {//Render only parts the material supports, or where Profile = null
            LaserProfile p = m.getProfileName() == null ? null : this.material.getLaserProfile(m.getProfileName());
            GraphicSet current = m.getFilterSet().getMatchingObjects(this.graphicObjects);
            Rectangle2D bb = current.getBoundingBox();
            if (bb != null && bb.getWidth() > 0 && bb.getHeight() > 0)
            {
              somethingMatched = true;
              if (drawPreview && !(p instanceof VectorProfile))
              {
                synchronized (renderBuffer)
                {
                  ImageProcessingThread procThread = this.renderBuffer.get(m);
                  if (procThread == null || !procThread.isFinished() || (int) bb.getWidth() != (int) procThread.getBoundingBox().getWidth() || (int) bb.getHeight() != (int) procThread.getBoundingBox().getHeight())
                  {//Image not rendered or Size differs
                    if (!renderBuffer.containsKey(m))
                    {//image not yet scheduled for rendering
                      logger.log(Level.FINE, "Starting ImageProcessing Thread for "+m);
                      procThread = new ImageProcessingThread(current, this.getMaterial().getLaserProfile(m.getProfileName()));
                      this.renderBuffer.put(m, procThread);
                      procThread.start();//start processing thread
                    }
                    else if ((int) bb.getWidth() != (int) procThread.getBoundingBox().getWidth() || (int) bb.getHeight() != (int) procThread.getBoundingBox().getHeight())
                    {//Image is (being) rendered with wrong size
                      logger.log(Level.FINE, "Image has wrong size");
                      if (!procThread.isFinished())
                      {//stop the old thread if still running
                        procThread.cancel();
                      }
                      logger.log(Level.FINE, "Starting ImageProcessingThread for"+m);
                      procThread = new ImageProcessingThread(current, this.getMaterial().getLaserProfile(m.getProfileName()));
                      this.renderBuffer.put(m, procThread);
                      procThread.start();//start processing thread
                    }
                    gg.setColor(Color.GRAY);
                    Rectangle r = Helper.toRect(bb);
                    gg.fillRect(r.x, r.y, r.width, r.height);
                    gg.setColor(Color.BLACK);
                    AffineTransform tmp = gg.getTransform();
                    gg.setTransform(new AffineTransform());
                    Point po = new Point(r.x + r.width / 2, r.y + r.height / 2);
                    tmp.transform(po, po);
                    String txt = "please wait ("+procThread.getProgress()+"%)";
                    int w = gg.getFontMetrics().stringWidth(txt);
                    gg.drawString(txt, po.x - w / 2, po.y);
                    gg.setTransform(tmp);
                  }
                  else
                  {
                    gg.drawRenderedImage(procThread.getImage(), AffineTransform.getTranslateInstance(bb.getX(), bb.getY()));
                  }
                }
              }
              else if (p instanceof VectorProfile)
              {
                if ((highlightCutLines && ((VectorProfile) p).isIsCut()) || (drawPreview && !((VectorProfile) p).isIsCut()))
                {
                  p.renderPreview(gg, current, this.material);
                }
              }
            }
            else
            {//Mapping is Empty or BoundingBox zero
              synchronized (renderBuffer)
              {
                renderBuffer.remove(m);
              }
            }
          }
        }
        if (!somethingMatched)
        {//Nothing drawn because of no Matching mapping
          AffineTransform trans = gg.getTransform();
          gg.setTransform(new AffineTransform());
          gg.drawString("No matching parts for the current Mapping found.", 10, this.getHeight() / 2);
          gg.setTransform(trans);
        }
      }
      if (this.editRectangle != null)
      {
        this.editRectangle.render(gg);
      }
      this.lastDrawnTransform = gg.getTransform();
    }
  }
  protected MappingSet mappings = null;

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
    this.mappings = mappings;
    this.ClearCache();
    this.repaint();
  }

  private void drawGrid(Graphics2D gg)
  {
    /**
     * The minimal distance of 2 grid lines in Pixel
     */
    int minPixelDst = 40;
    AffineTransform trans = gg.getTransform();
    double minDrawDst = minPixelDst / trans.getScaleX();
    /**
     * The grid distance in mm
     */
    //todo calculate gridDst from Transform
    double gridDst = 0.1;
    int smalllines = 2;
    while (Helper.mm2px(gridDst) < minDrawDst)
    {
      gridDst *= 2;
      if (Helper.mm2px(gridDst) < minDrawDst)
      {
        gridDst *= 5;
      }
    }
    gg.setTransform(new AffineTransform());//we dont want the line width to scale with zoom etc
    double mmx = 0;
    int count = 0;
    for (int x = 0; x < Helper.mm2px(this.bedWidth); x += Helper.mm2px(gridDst))
    {
      Point a = new Point(x, 0);
      Point b = new Point(x, (int) Helper.mm2px(this.bedHeight));
      trans.transform(a, a);
      trans.transform(b, b);
      if (a.x > 0)//only draw if in viewing range
      {
        if (a.x > this.getWidth())
        {
          break;
        }
        gg.drawLine(a.x, a.y, b.x, b.y);
        if (++count >= smalllines)
        {
          String txt;
          float mm = ((float) Math.round((float) (10 * mmx))) / 10;
          if ((int) mm == mm)
          {
            txt = "" + (int) mm;
          }
          else
          {
            txt = "" + mm;
          }
          int w = gg.getFontMetrics().stringWidth(txt);
          int h = gg.getFontMetrics().getHeight();
          gg.drawString(txt, a.x - w / 2, 5 + h);
          count = 0;
        }
      }
      mmx += gridDst;
    }
    double mmy = 0;
    count = 0;
    for (int y = 0; y < Helper.mm2px(this.bedHeight); y += Helper.mm2px(gridDst))
    {
      Point a = new Point(0, y);
      Point b = new Point((int) Helper.mm2px(this.bedWidth), y);
      trans.transform(a, a);
      trans.transform(b, b);
      if (a.y > 0)
      {
        if (a.y > this.getHeight())
        {
          break;
        }
        gg.drawLine(a.x, a.y, b.x, b.y);
        if (++count >= smalllines)
        {
          String txt;
          float mm = ((float) Math.round((float) (10 * mmy))) / 10;
          if ((int) mm == mm)
          {
            txt = "" + (int) mm;
          }
          else
          {
            txt = "" + mm;
          }
          gg.drawString(txt, 5, a.y);
          count = 0;
        }
      }
      mmy += gridDst;
    }
    gg.setTransform(trans);
  }
}
