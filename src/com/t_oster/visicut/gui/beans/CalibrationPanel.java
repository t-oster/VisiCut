package com.t_oster.visicut.gui.beans;

import com.t_oster.liblasercut.platform.Util;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.image.RenderedImage;
import java.util.logging.Level;
import java.util.logging.Logger;
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
  private Point lowerLeft = new Point((int) Util.mm2px(0, DPI), (int) Util.mm2px(300, DPI));
  private Point lowerRight = new Point((int) Util.mm2px(600, DPI), (int) Util.mm2px(300, DPI));
  protected AffineTransform previewTransformation = AffineTransform.getScaleInstance(0.02, 0.02);
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
    repaint();
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
      Point2D size = new Point(SIZE,SIZE);
      try
      {
        size = this.getPreviewTransformation().createInverse().deltaTransform(size, null);
      }
      catch (NoninvertibleTransformException ex)
      {
        Logger.getLogger(CalibrationPanel.class.getName()).log(Level.SEVERE, null, ex);
      }
      drawCross(gg, upperLeft, (int) size.getX());
      drawCross(gg, lowerLeft, (int) size.getX());
      drawCross(gg, lowerRight, (int) size.getX());
    }
  }

  private void drawCross(Graphics2D g, Point p, int size)
  {
    g.drawLine(p.x - size / 2, p.y, p.x + size / 2, p.y);
    g.drawLine(p.x, p.y - size / 2, p.x, p.y + size / 2);
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
        upperLeft, lowerLeft, lowerRight
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
      Point2D trUpperLeft = this.getPreviewTransformation().transform(upperLeft, null);
      Point2D trLowerLeft = this.getPreviewTransformation().transform(lowerLeft, null);
      Point2D trLowerRight = this.getPreviewTransformation().transform(lowerRight, null);
      Point transformed = me.getPoint();
      if (selectedPoint == upperLeft)
      {
        trUpperLeft = transformed;
      }
      else if (selectedPoint == lowerLeft)
      {
        trLowerLeft = transformed;
      }
      else
      {
        trLowerRight = transformed;
      }
      //TODO Calculate Transformation.
      AffineTransform scale = AffineTransform.getScaleInstance((trLowerRight.getX()-trLowerLeft.getX())/(lowerRight.x-lowerLeft.x), (trLowerLeft.getY()-trUpperLeft.getY())/(lowerLeft.y-upperLeft.y));
      AffineTransform move = AffineTransform.getTranslateInstance(trUpperLeft.getX(), trUpperLeft.getY());
      move.concatenate(scale);
      this.setPreviewTransformation(move);
    }
  }

  public void mouseEntered(MouseEvent me)
  {
  }

  public void mouseExited(MouseEvent me)
  {
  }
}
