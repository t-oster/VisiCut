package com.t_oster.visicut.gui.beans;

import com.t_oster.liblasercut.platform.Util;
import com.t_oster.visicut.misc.Helper;
import com.t_oster.visicut.model.LaserProfile;
import com.t_oster.visicut.model.MaterialProfile;
import com.t_oster.visicut.model.VectorProfile;
import com.t_oster.visicut.model.mapping.Mapping;
import com.t_oster.visicut.model.graphicelements.GraphicObject;
import com.t_oster.visicut.model.graphicelements.GraphicSet;
import com.t_oster.visicut.model.graphicelements.ShapeObject;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class implements the Panel which provides the Preview
 * of the current LaserJob
 * 
 * @author thommy
 */
public class PreviewPanel extends GraphicObjectsPanel
{

  public PreviewPanel()
  {
    this.imageProcessingThread.start();
  }
  private final Thread imageProcessingThread = new Thread()
  {

    public boolean keepRunning = true;

    private BufferedImage renderMapping(Mapping m)
    {
      GraphicSet set = m.getFilterSet().getMatchingObjects(PreviewPanel.this.graphicObjects);
      Rectangle2D bb = set.getBoundingBox();
      BufferedImage buffer = null;
      if (bb != null && bb.getWidth() > 0 && bb.getHeight() > 0)
      {
        LaserProfile p = PreviewPanel.this.getMaterial().getLaserProfile(m.getProfileName());
        buffer = new BufferedImage((int) bb.getWidth(), (int) bb.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D gg = buffer.createGraphics();
        //Normalize Rendering to 0,0
        gg.setTransform(AffineTransform.getTranslateInstance(-bb.getX(), -bb.getY()));
        p.renderPreview(gg, set);
      }
      return buffer;
    }

    @Override
    public void run()
    {
      while (keepRunning)
      {
        Mapping toProcess = null;
        synchronized (PreviewPanel.this.renderBuffer)
        {
          for (Mapping m : PreviewPanel.this.renderBuffer.keySet())
          {
            if (PreviewPanel.this.renderBuffer.get(m) == null)
            {
              toProcess = m;
              break;
            }
          }
        }
        if (toProcess != null)
        {
          BufferedImage result = this.renderMapping(toProcess);
          synchronized (PreviewPanel.this.renderBuffer)
          {
            PreviewPanel.this.renderBuffer.put(toProcess, result);
          }
          PreviewPanel.this.repaint();
        }
        try
        {
          synchronized (this)
          {
            this.wait();
          }
        }
        catch (InterruptedException ex)
        {
          Logger.getLogger(PreviewPanel.class.getName()).log(Level.SEVERE, null, ex);
        }
      }
    }
  };
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
  protected boolean highlightCutLines = false;

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
  protected AffineTransform previewTransformation = AffineTransform.getTranslateInstance(40, 150);
  private AffineTransform lastDrawnTransform = null;

  public AffineTransform getLastDrawnTransform()
  {
    return lastDrawnTransform;
  }

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
  protected RenderedImage backgroundImage = null;

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
    if (this.backgroundImage != null)
    {
      this.setOuterBounds(new Dimension(backgroundImage.getWidth(), backgroundImage.getHeight()));
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
  private PropertyChangeListener materialObserver = new PropertyChangeListener()
  {

    public void propertyChange(PropertyChangeEvent pce)
    {
      PreviewPanel.this.setOuterBounds(new Dimension((int) Util.mm2px(PreviewPanel.this.material.getWidth(),500),(int) Util.mm2px(PreviewPanel.this.material.getHeight(), 500)));
      PreviewPanel.this.repaint();
    }
  };

  /**
   * Set the value of material
   *
   * @param material new value of material
   */
  public void setMaterial(MaterialProfile material)
  {
    if (this.material != null)
    {
      this.material.removePropertyChangeListener(materialObserver);
    }
    this.material = material;
    if (this.material != null)
    {
      this.material.addPropertyChangeListener(materialObserver);
      this.setOuterBounds(new Dimension((int) Util.mm2px(this.material.getWidth(),500),(int) Util.mm2px(this.material.getHeight(), 500)));
    }
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
  private final HashMap<Mapping, BufferedImage> renderBuffer = new LinkedHashMap<Mapping, BufferedImage>();

  @Override
  protected void paintComponent(Graphics g)
  {
    super.paintComponent(g);
    if (g instanceof Graphics2D)
    {
      Graphics2D gg = (Graphics2D) g;
      gg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      gg.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
      if (backgroundImage != null)
      {
        gg.drawRenderedImage(backgroundImage, null);
      }
      if (this.previewTransformation != null)
      {
        AffineTransform current = gg.getTransform();
        current.concatenate(this.getPreviewTransformation());
        gg.setTransform(current);
      }
      if (this.material != null)
      {
        Color c = this.material.getColor();
        if (this.backgroundImage != null)
        {
          gg.setColor(this.material.getCutColor() != null ? this.material.getCutColor() : Color.BLACK);
          gg.drawRect(0, 0, (int) Util.mm2px(material.getWidth(), 500), (int) Util.mm2px(material.getHeight(), 500));
        }
        else
        {
          gg.setColor(c == null ? Color.BLUE : c);
          gg.fillRect(0, 0, (int) Util.mm2px(material.getWidth(), 500), (int) Util.mm2px(material.getHeight(), 500));
        }
      }
      if (showGrid)
      {
        gg.setColor(Color.DARK_GRAY);
        drawGrid(gg);
      }
      if (this.getGraphicObjects() != null)
      {
        if (this.getMaterial() != null && this.getMappings() != null && this.getMappings().size() > 0)
        {
          GraphicSet rest = graphicObjects.copy();
          for (Mapping m : this.getMappings())
          {
            GraphicSet current = m.getFilterSet().getMatchingObjects(rest);
            rest.removeAll(current);
            Rectangle2D bb = current.getBoundingBox();
            if (bb != null && bb.getWidth() > 0 && bb.getHeight() > 0)
            {
              if (drawPreview)
              {
                synchronized (renderBuffer)
                {
                  BufferedImage img = this.renderBuffer.get(m);
                  if (img == null || bb.getWidth() != img.getWidth() || bb.getHeight() != img.getHeight())
                  {//Image not rendered or Size differs
                    if (!renderBuffer.containsKey(m) || img != null)
                    {//image not yet scheduled for rendering
                      this.renderBuffer.put(m, null);
                    }
                    synchronized (imageProcessingThread)
                    {
                      imageProcessingThread.notify();
                    }
                    gg.setColor(Color.GRAY);
                    Rectangle r = Helper.toRect(bb);
                    gg.fillRect(r.x, r.y, r.width, r.height);
                    gg.setColor(Color.BLACK);
                    gg.drawString("processing...", r.x + r.width / 2, r.y + r.height / 2);
                  }
                  else
                  {
                    gg.drawRenderedImage(img, AffineTransform.getTranslateInstance(bb.getX(), bb.getY()));
                  }
                }
              }
              if (highlightCutLines)
              {
                LaserProfile p = this.material.getLaserProfile(m.getProfileName());
                gg.setColor(material.getCutColor());
                if (p instanceof VectorProfile && ((VectorProfile) p).isIsCut())
                {
                  for (GraphicObject o : current)
                  {
                    if (o instanceof ShapeObject)
                    {
                      Shape s = ((ShapeObject) o).getShape();
                      if (current.getTransform() != null)
                      {
                        s = current.getTransform().createTransformedShape(s);
                      }
                      gg.draw(s);
                    }
                  }
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
        else
        {
          for (GraphicObject o : this.getGraphicObjects())
          {
            o.render(gg);
          }
        }
      }
      this.lastDrawnTransform = gg.getTransform();
      if (this.editRectangle != null)
      {
        this.editRectangle.render(gg);
      }
    }
  }
  protected List<Mapping> mappings = null;

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
    this.mappings = mappings;
    this.repaint();
  }

  public void mouseDragged(MouseEvent me)
  {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public void mouseMoved(MouseEvent me)
  {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  private void drawGrid(Graphics2D gg)
  {
    if (this.material != null)
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
      while (Util.mm2px(gridDst, 500) < minDrawDst)
      {
        gridDst *= 2;
        if (Util.mm2px(gridDst, 500) < minDrawDst)
        {
          gridDst *= 5;
        }
      }
      gg.setTransform(new AffineTransform());//we dont want the line width to scale with zoom etc
      double mmx = 0;
      for (int x = 0; x < Util.mm2px(this.material.getWidth(), 500); x += Util.mm2px(gridDst, 500))
      {
        Point a = new Point(x, 0);
        Point b = new Point(x, (int) Util.mm2px(this.material.getHeight(), 500));
        trans.transform(a, a);
        trans.transform(b, b);
        if (a.x > 0)//only draw if in viewing range
        {
          if (a.x > this.getWidth())
          {
            break;
          }
          gg.drawLine(a.x, a.y, b.x, b.y);
          String txt = ((float) Math.round((float) (10 * mmx))) / 10 + " mm";
          int w = gg.getFontMetrics().stringWidth(txt);
          int h = gg.getFontMetrics().getHeight();
          gg.drawString(txt, a.x - w / 2, 5 + h);
        }
        mmx += gridDst;
      }
      double mmy = 0;
      for (int y = 0; y < Util.mm2px(this.material.getHeight(), 500); y += Util.mm2px(gridDst, 500))
      {
        Point a = new Point(0, y);
        Point b = new Point((int) Util.mm2px(this.material.getWidth(), 500), y);
        trans.transform(a, a);
        trans.transform(b, b);
        if (a.y > 0)
        {
          if (a.y > this.getHeight())
          {
            break;
          }
          gg.drawLine(a.x, a.y, b.x, b.y);
          String txt = (float) (Math.round((float) (10 * mmy))) / 10 + " mm";
          int w = gg.getFontMetrics().stringWidth(txt);
          int h = gg.getFontMetrics().getHeight();
          gg.drawString(txt, 5, a.y);
        }
        mmy += gridDst;
      }
      gg.setTransform(trans);
    }
  }
}
