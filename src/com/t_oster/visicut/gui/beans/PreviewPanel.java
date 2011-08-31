package com.t_oster.visicut.gui.beans;

import com.t_oster.liblasercut.platform.Util;
import com.t_oster.visicut.model.LaserProfile;
import com.t_oster.visicut.model.MaterialProfile;
import com.t_oster.visicut.model.mapping.Mapping;
import com.t_oster.visicut.model.graphicelements.GraphicObject;
import com.t_oster.visicut.model.graphicelements.GraphicSet;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.RenderedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

/**
 * This class implements the Panel which provides the Preview
 * of the current LaserJob
 * 
 * @author thommy
 */
public class PreviewPanel extends GraphicObjectsPanel
{

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
    this.repaint();
  }

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
            GraphicSet set = m.getFilterSet().getMatchingObjects(this.graphicObjects);
            LaserProfile p = this.getMaterial().getLaserProfile(m.getProfileName());
            p.renderPreview(gg, set);
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
