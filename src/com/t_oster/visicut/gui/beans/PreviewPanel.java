package com.t_oster.visicut.gui.beans;

import com.t_oster.visicut.model.Mapping;
import com.t_oster.visicut.model.graphicelements.GraphicObject;
import com.t_oster.visicut.model.graphicelements.GraphicSet;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.image.RenderedImage;
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
public class PreviewPanel extends FilesDropPanel
{
  
  protected AffineTransform previewTransformation = AffineTransform.getTranslateInstance(40, 150);

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
  }

  public void setBackgroundImageFile(File imageFile)
  {
    try
    {
      if (imageFile.exists())
      {
        this.setBackgroundImage(ImageIO.read(imageFile));
      }
    }
    catch (IOException ex)
    {
      Logger.getLogger(PreviewPanel.class.getName()).log(Level.SEVERE, null, ex);
    }
  }
  protected File droppedFile = null;
  public static final String PROP_DROPPEDFILE = "droppedFile";
  protected Color materialColor = null;

  /**
   * Get the value of materialColor
   *
   * @return the value of materialColor
   */
  public Color getMaterialColor()
  {
    return materialColor;
  }

  /**
   * Set the value of materialColor
   *
   * @param materialColor new value of materialColor
   */
  public void setMaterialColor(Color materialColor)
  {
    this.materialColor = materialColor;
    this.repaint();
  }

  /**
   * Get the value of droppedFile
   *
   * @return the value of droppedFile
   */
  public File getDroppedFile()
  {
    return droppedFile;
  }

  /**
   * Set the value of droppedFiles
   *
   * @param droppedFiles new value of droppedFiles
   */
  public void setDroppedFile(File droppedFile)
  {
    File oldDroppedFile = this.droppedFile;
    this.droppedFile = droppedFile;
    this.firePropertyChange(PROP_DROPPEDFILE, oldDroppedFile, droppedFile);
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
  public void filesDropped(List files)
  {
    if (files != null && !files.isEmpty())
    {
      Object o = files.get(0);
      File f = null;
      if (o instanceof File)
      {
        f = (File) o;
      }
      else if (o instanceof String)
      {
        f = new File((String) o);
      }
      if (f != null && f.exists())
      {
        this.setDroppedFile(f);
      }
    }
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
      else
      {
        gg.setColor(this.getMaterialColor());
        gg.fill(gg.getClip());
      }
      if (this.previewTransformation != null)
      {
        AffineTransform current = gg.getTransform();
        current.concatenate(this.getPreviewTransformation());
        gg.setTransform(current);
      }
      if (this.getGraphicObjects() != null)
      {
        if (this.getMappings() != null)
        {
          for (Mapping m : this.getMappings())
          {
            m.getB().renderPreview(gg, m.getA().getMatchingObjects(this.getGraphicObjects()));
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
