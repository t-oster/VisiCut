package com.t_oster.visicut.gui.beans;

import com.t_oster.liblasercut.platform.Util;
import com.t_oster.visicut.model.Mapping;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.JPanel;

/**
 * This class implements the Panel which provides the Preview
 * of the current LaserJob
 * 
 * @author thommy
 */
public class CalibrationPanel extends JPanel implements MouseListener
{

  public CalibrationPanel()
  {
    this.addMouseListener(this);
  }
  private int DPI = 500;
  private int SIZE = 10;
  private Point upperLeft = new Point((int) Util.mm2px(0, DPI), (int) Util.mm2px(0, DPI));
  private Point upperRight = new Point((int) Util.mm2px(600, DPI), (int) Util.mm2px(0, DPI));
  private Point lowerLeft = new Point((int) Util.mm2px(0, DPI), (int) Util.mm2px(300, DPI));
  private Point lowerRight = new Point((int) Util.mm2px(600, DPI), (int) Util.mm2px(300, DPI));
  protected AffineTransform previewTransformation = null;
  public static final String PROP_PREVIEWTRANSFORMATION = "previewTransformation";

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
    AffineTransform oldPreviewTransformation = this.previewTransformation;
    this.previewTransformation = previewTransformation;
    firePropertyChange(PROP_PREVIEWTRANSFORMATION, oldPreviewTransformation, previewTransformation);
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
        AffineTransform curr = gg.getTransform();
        curr.concatenate(this.getPreviewTransformation());
        gg.setTransform(curr);
      }
      gg.setColor(Color.red);
      drawCross(gg, upperLeft, SIZE);
      drawCross(gg, upperRight, SIZE);
      drawCross(gg, lowerLeft, SIZE);
      drawCross(gg, lowerRight, SIZE);
    }
  }

  private void drawCross(Graphics2D g, Point p, int size)
  {
    g.drawLine(p.x - size / 2, p.y, p.x + size / 2, p.y);
    g.drawLine(p.x, p.y - size / 2, p.x, p.y + size / 2);
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

  public void mouseClicked(MouseEvent me)
  {
  }
  
  public Point selectedPoint = null;
  public void mousePressed(MouseEvent me)
  {
    Point p = me.getPoint();
    for (Point source : new Point[]
      {
        upperLeft, upperRight, lowerLeft, lowerRight
      })
    {
      Point2D target = this.getPreviewTransformation().transform(source, null);
      if (p.distance(target) < SIZE)
      {
        selectedPoint = source;
        return;
      }
    }
    selectedPoint = null;
  }

  public void mouseReleased(MouseEvent me)
  {
    if (selectedPoint != null)
    {
      Point transformed = me.getPoint();
      if (selectedPoint == upperLeft)
      {
        //TODO...
      }
    }
  }

  public void mouseEntered(MouseEvent me)
  {
  }

  public void mouseExited(MouseEvent me)
  {
  }
}
