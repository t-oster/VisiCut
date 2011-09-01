package com.t_oster.visicut.gui.beans;

import com.t_oster.liblasercut.platform.Util;
import com.t_oster.visicut.Helper;
import com.t_oster.visicut.model.LaserProfile;
import com.t_oster.visicut.model.MaterialProfile;
import com.t_oster.visicut.model.mapping.Mapping;
import com.t_oster.visicut.model.graphicelements.GraphicObject;
import com.t_oster.visicut.model.graphicelements.GraphicSet;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
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
          gg.setColor(Color.BLACK);
          gg.drawRect(0, 0, (int) Util.mm2px(material.getWidth(), 500), (int) Util.mm2px(material.getHeight(), 500));
        }
        else
        {
          gg.setColor(c == null ? Color.BLUE : c);
          gg.fillRect(0, 0, (int) Util.mm2px(material.getWidth(), 500), (int) Util.mm2px(material.getHeight(), 500));
        }
      }
      if (this.getGraphicObjects() != null)
      {
        if (this.getMaterial() != null && this.getMappings() != null && this.getMappings().size() > 0)
        {
          for (Mapping m : this.getMappings())
          {
            Rectangle2D bb = m.getFilterSet().getMatchingObjects(graphicObjects).getBoundingBox();
            if (bb != null && bb.getWidth() > 0 && bb.getHeight() > 0)
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
                  gg.drawString("processing...", r.x+r.width/2, r.y+r.height/2);
                }
                else
                {
                  gg.drawRenderedImage(img, AffineTransform.getTranslateInstance(bb.getX(), bb.getY()));
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
}
