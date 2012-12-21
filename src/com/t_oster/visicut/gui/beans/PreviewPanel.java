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
package com.t_oster.visicut.gui.beans;

import com.t_oster.liblasercut.LaserCutter;
import com.t_oster.liblasercut.ProgressListener;
import com.t_oster.liblasercut.platform.Util;
import com.t_oster.visicut.VisicutModel;
import com.t_oster.visicut.misc.Helper;
import com.t_oster.visicut.model.LaserDevice;
import com.t_oster.visicut.model.LaserProfile;
import com.t_oster.visicut.model.MaterialProfile;
import com.t_oster.visicut.model.PlfPart;
import com.t_oster.visicut.model.Raster3dProfile;
import com.t_oster.visicut.model.RasterProfile;
import com.t_oster.visicut.model.VectorProfile;
import com.t_oster.visicut.model.graphicelements.GraphicObject;
import com.t_oster.visicut.model.graphicelements.GraphicSet;
import com.t_oster.visicut.model.mapping.FilterSet;
import com.t_oster.visicut.model.mapping.Mapping;
import com.t_oster.visicut.model.mapping.MappingSet;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
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
public class PreviewPanel extends ZoomablePanel implements PropertyChangeListener
{

  private double bedWidth = 600;
  private double bedHeight = 300;

  public PreviewPanel()
  {
    VisicutModel.getInstance().addPropertyChangeListener(this);
  }
  
  public void propertyChange(PropertyChangeEvent pce)
  {
    if (VisicutModel.PROP_SELECTEDLASERDEVICE.equals(pce.getPropertyName()))
    {
      if (pce.getNewValue() != null && pce.getNewValue() instanceof LaserDevice)
      {
        LaserCutter lc = ((LaserDevice) pce.getNewValue()).getLaserCutter();
        bedWidth = lc.getBedWidth();
        bedHeight = lc.getBedHeight();
        setAreaSize(new Point2D.Double(lc.getBedWidth(), lc.getBedHeight()));
        repaint();
      }
    }
    else if (VisicutModel.PROP_SELECTEDPART.equals(pce.getPropertyName()))
    {
      PlfPart p = VisicutModel.getInstance().getSelectedPart();
      this.setEditRectangle(p != null ? new EditRectangle(p.getGraphicObjects().getBoundingBox()) : null);
      //this.repaint();//setEditRectangle does already repaint
    }
    else if (VisicutModel.PROP_SELECTED_PART_CHANGED.equals(pce.getPropertyName()))
    {
      PlfPart p = VisicutModel.getInstance().getSelectedPart();
      this.clearCache(p);
      this.setEditRectangle(new EditRectangle(p.getGraphicObjects().getBoundingBox()));
    }
    else if (VisicutModel.PROP_MATERIAL.equals(pce.getPropertyName())
      || VisicutModel.PROP_PLF_FILE_CHANGED.equals(pce.getPropertyName()))
    {
      this.clearCache();
      repaint();
    }
    else if (VisicutModel.PROP_BACKGROUNDIMAGE.equals(pce.getPropertyName()))
    {
      repaint();
    }
  }
  
  private static final Logger logger = Logger.getLogger(PreviewPanel.class.getName());

  private class ImageProcessingThread extends Thread implements ProgressListener
  {

    private Logger logger = Logger.getLogger(ImageProcessingThread.class.getName());
    private BufferedImage buffer = null;
    private GraphicSet set;
    private AffineTransform mm2laserPx;
    private LaserProfile p;
    private Rectangle bb;
    private Rectangle2D bbInMm;
    private int progress = 0;
    private boolean isFinished = false;

    /**
     * Returns the bounding box of this preview image IN pixels
     * in LASERCUTTER-space
     * @return
     */
    public Rectangle getBoundingBox()
    {
      return bb;
    }

    public Rectangle2D getBoundingBoxInMm()
    {
      return bbInMm;
    }

    public BufferedImage getImage()
    {
      return buffer;
    }

    public ImageProcessingThread(GraphicSet objects, LaserProfile p)
    {
      this.set = objects;
      this.p = p;
      double factor = Util.dpi2dpmm(p.getDPI());
      this.mm2laserPx = AffineTransform.getScaleInstance(factor, factor);
      bbInMm = set.getBoundingBox();
      bb = Helper.toRect(Helper.transform(bbInMm, mm2laserPx));
      if (bb == null || bb.width == 0 || bb.height == 0)
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
        buffer = rp.getRenderedPreview(set, VisicutModel.getInstance().getMaterial(), mm2laserPx, this);
      }
      else if (p instanceof Raster3dProfile)
      {
        Raster3dProfile rp = (Raster3dProfile) p;
        buffer = rp.getRenderedPreview(set, VisicutModel.getInstance().getMaterial(), mm2laserPx, this);
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
        logger.log(Level.FINE, "Rendering finished. Took: {0} ms", (stop-start));
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
          logger.log(Level.FINE, "2nd Rendering took {0} ms", (stop-start));
        }
        catch (OutOfMemoryError ee)
        {
          JOptionPane.showMessageDialog(PreviewPanel.this, java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/beans/resources/PreviewPanel").getString("ERROR: NOT ENOUGH MEMORY PLEASE START THE PROGRAM FROM THE PROVIDED SHELL SCRIPTS INSTEAD OF RUNNING THE .JAR FILE"), java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/beans/resources/PreviewPanel").getString("ERROR: OUT OF MEMORY"), JOptionPane.ERROR_MESSAGE);
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

  private boolean fastPreview = false;

  /**
   * Get the value of fastPreview
   *
   * @return the value of fastPreview
   */
  public boolean isFastPreview()
  {
    return fastPreview;
  }

  /**
   * Set the value of fastPreview
   * if true, profiles won't be rendered as they look on
   * the laser-cutter, but just as they look in the image
   *
   * @param fastPreview new value of fastPreview
   */
  public void setFastPreview(boolean fastPreview)
  {
    this.fastPreview = fastPreview;
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

  protected RenderedImage backgroundImage = null;

  public void clearCache(PlfPart p)
  {
    synchronized(this.renderBuffers)
    {
      for (ImageProcessingThread thr : this.renderBuffers.get(p).values())
      {
        if (!thr.isFinished())
        {
          thr.cancel();
        }
      }
      this.renderBuffers.get(p).clear();
    }
  }
  
  public void clearCache()
  {
    synchronized(this.renderBuffers)
    {
      for(PlfPart p : this.renderBuffers.keySet())
      {
        clearCache(p);
      }
      this.renderBuffers.clear();
    }
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
  
  /**
   * The renderBuffer contains a BufferedImage for each Mapping of each PlfPart.
   * On refreshRenderBuffer, the Images are created by rendering
   * the GraphicElements matching the mapping onto an Image,
   * with the size of the BoundingBox.
   * When drawn the offset of the BoundingBox has to be used as Upper Left
   * Corner
   */
  private final HashMap<PlfPart,HashMap<Mapping, ImageProcessingThread>> renderBuffers = new LinkedHashMap<PlfPart,HashMap<Mapping, ImageProcessingThread>>();

  private boolean renderOriginalImage(Graphics2D gg, Mapping m, PlfPart p)
  {
    boolean somethingMatched = false;
    AffineTransform bak = gg.getTransform();
    AffineTransform tr = gg.getTransform();
    tr.concatenate(this.getMmToPxTransform());
    if (p.getGraphicObjects().getTransform() != null)
    {
      tr.concatenate(p.getGraphicObjects().getTransform());
    }
    gg.setTransform(tr);
    for (GraphicObject o : m.getFilterSet().getMatchingObjects(p.getGraphicObjects()))
    {
      somethingMatched = true;
      o.render(gg);
    }
    gg.setTransform(bak);
    return somethingMatched;
  }

  @Override
  protected void paintComponent(Graphics g)
  {
    super.paintComponent(g);
    if (g instanceof Graphics2D)
    {
      Graphics2D gg = (Graphics2D) g;
      Point2D dim = this.getMmToPxTransform().transform(this.getAreaSize(), null);
      Rectangle r = this.getVisibleRect();
      r=r.intersection(new Rectangle(0, 0, (int) dim.getX(), (int) dim.getY()));
      gg.setClip(r.x, r.y, r.width, r.height);
      gg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      gg.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
      if (backgroundImage != null && showBackgroundImage && VisicutModel.getInstance().getSelectedLaserDevice() != null)
      {
        AffineTransform img2px = new AffineTransform(this.getMmToPxTransform());
        if (VisicutModel.getInstance().getSelectedLaserDevice().getCameraCalibration() != null)
        {
          img2px.concatenate(VisicutModel.getInstance().getSelectedLaserDevice().getCameraCalibration());
        }
        gg.drawRenderedImage(backgroundImage, img2px);
      }
      Rectangle box = Helper.toRect(Helper.transform(
          new Rectangle2D.Double(0, 0, this.bedWidth, this.bedHeight),
          this.getMmToPxTransform()
          ));
      if (this.backgroundImage != null && showBackgroundImage)
      {
        gg.setColor(Color.BLACK);
        gg.drawRect(box.x, box.y, box.width, box.height);
      }
      else
      {
        MaterialProfile m = VisicutModel.getInstance().getMaterial();
        gg.setColor(m != null && m.getColor() != null ? m.getColor() : Color.WHITE);
        gg.fillRect(box.x, box.y, box.width, box.height);
      }
      if (showGrid)
      {
        gg.setColor(Color.DARK_GRAY);
        drawGrid(gg);
      }
      for (PlfPart part : VisicutModel.getInstance().getPlfFile())
      {
        HashMap<Mapping,ImageProcessingThread> renderBuffer = renderBuffers.get(part);
        if (renderBuffer == null)
        {
          renderBuffer = new LinkedHashMap<Mapping,ImageProcessingThread>();
          renderBuffers.put(part,renderBuffer);
        }
        if (part.getGraphicObjects() != null)
        {
          MappingSet mappingsToDraw = part.getMapping();
          if (VisicutModel.getInstance().getMaterial() == null || mappingsToDraw == null || mappingsToDraw.isEmpty())
          {//Just draw the original Image
            mappingsToDraw = new MappingSet();
            mappingsToDraw.add(new Mapping(new FilterSet(), null));
          }
          boolean somethingMatched = false;
          for (Mapping m : mappingsToDraw)
          {//Render Original Image
            if (m.getProfile() == null || (this.fastPreview && part.equals(VisicutModel.getInstance().getSelectedPart())))
            {
              somethingMatched = this.renderOriginalImage(gg, m, part);
            }
            else if (m.getProfile() != null)
            {//Render only parts the material supports, or where Profile = null
              LaserProfile p = m.getProfile();
              GraphicSet current = m.getFilterSet().getMatchingObjects(part.getGraphicObjects());
              Rectangle2D bbInMm = current.getBoundingBox();
              Rectangle bbInPx = Helper.toRect(Helper.transform(bbInMm, this.getMmToPxTransform()));
              if (bbInPx != null && bbInPx.getWidth() > 0 && bbInPx.getHeight() > 0)
              {
                somethingMatched = true;
                if (!(p instanceof VectorProfile))
                {
                  synchronized (renderBuffer)
                  {
                    ImageProcessingThread procThread = renderBuffer.get(m);
                    if (procThread == null || !procThread.isFinished() || Math.abs(bbInMm.getWidth()-procThread.getBoundingBoxInMm().getWidth()) > 0.01 || Math.abs(bbInMm.getHeight()-procThread.getBoundingBoxInMm().getHeight()) > 0.01)
                    {//Image not rendered or Size differs
                      if (!renderBuffer.containsKey(m))
                      {//image not yet scheduled for rendering
                        logger.log(Level.FINE, "Starting ImageProcessing Thread for {0}", m);
                        procThread = new ImageProcessingThread(current, p);
                        renderBuffer.put(m, procThread);
                        procThread.start();//start processing thread
                      }
                      else if (bbInMm.getWidth() != procThread.getBoundingBoxInMm().getWidth() || bbInMm.getHeight() != procThread.getBoundingBoxInMm().getHeight())
                      {//Image is (being) rendered with wrong size
                        logger.log(Level.FINE, "Image has wrong size");
                        if (!procThread.isFinished())
                        {//stop the old thread if still running
                          procThread.cancel();
                        }
                        logger.log(Level.FINE, "Starting ImageProcessingThread for{0}", m);
                        procThread = new ImageProcessingThread(current, p);
                        renderBuffer.put(m, procThread);
                        procThread.start();//start processing thread
                      }
                      this.renderOriginalImage(gg, m, part);
                      Composite o = gg.getComposite();
                      gg.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
                      Point po = new Point(bbInPx.x + bbInPx.width / 2, bbInPx.y + bbInPx.height / 2);
                      String txt = java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/beans/resources/PreviewPanel").getString("PLEASE WAIT (")+procThread.getProgress()+java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/beans/resources/PreviewPanel").getString("%)");
                      int w = gg.getFontMetrics().stringWidth(txt);
                      int h = gg.getFontMetrics().getHeight();
                      gg.setColor(Color.GRAY);
                      gg.fillRoundRect(po.x -w /2 -5, po.y-h, w+10, (int) (1.5d*h), 5, 5);
                      gg.setComposite(o);
                      gg.setColor(Color.BLACK);
                      gg.drawString(txt, po.x - w / 2, po.y);
                    }
                    else
                    {
                      AffineTransform laserPxToPreviewPx = Helper.getTransform(procThread.getBoundingBox(), bbInPx);
                      laserPxToPreviewPx.translate(procThread.getBoundingBox().x, procThread.getBoundingBox().y);
                      gg.drawRenderedImage(procThread.getImage(), laserPxToPreviewPx);
                    }
                  }
                }
                else if (p instanceof VectorProfile)
                {
                  p.renderPreview(gg, current, VisicutModel.getInstance().getMaterial(), this.getMmToPxTransform());
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
            gg.drawString(java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/beans/resources/PreviewPanel").getString("NO MATCHING PARTS FOR THE CURRENT MAPPING FOUND."), 10, this.getHeight() / 2);
          }
        }
      }
      if (this.editRectangle != null)
      {
        this.editRectangle.render(gg, this.getMmToPxTransform());
      }
    }
  }
  
  private void drawGrid(Graphics2D gg)
  {
    /**
     * The minimal distance of 2 grid lines in Pixel
     */
    int minPixelDst = 40;
    /**
     * The minimal distance of 2 grid lines in mm
     */
    double minDrawDst = minPixelDst / this.getMmToPxTransform().getScaleX();
    /**
     * The grid distance in mm
     */
    double gridDst = 0.1;
    while (gridDst < minDrawDst)
    {
      gridDst *= 2;
      if (gridDst < minDrawDst)
      {
        gridDst *= 5;
      }
    }
    for (double x = 0; x < this.bedWidth; x += gridDst)
    {
      Point a = Helper.toPoint(this.getMmToPxTransform().transform(new Point2D.Double(x, 0), null));
      Point b = Helper.toPoint(this.getMmToPxTransform().transform(new Point2D.Double(x, this.bedHeight), null));
      if (a.getX() > 0)//only draw if in viewing range
      {
        if (a.getX() > this.getWidth())
        {
          break;
        }
        gg.drawLine(a.x, a.y, b.x, b.y);
      }
    }
    for (double y = 0; y < this.bedHeight; y += gridDst)
    {
      Point a = Helper.toPoint(this.getMmToPxTransform().transform(new Point2D.Double(0, y), null));
      Point b = Helper.toPoint(this.getMmToPxTransform().transform(new Point2D.Double(bedWidth, y), null));
      if (a.y > 0)
      {
        if (a.y > this.getHeight())
        {
          break;
        }
        gg.drawLine(a.x, a.y, b.x, b.y);
      }
    }
  }
}
