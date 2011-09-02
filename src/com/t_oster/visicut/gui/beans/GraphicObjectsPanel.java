package com.t_oster.visicut.gui.beans;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JPanel;

/**
 * A JPanel with Support for rendering Graphic Objects.
 * This Panel supports Zoom etc.
 * 
 * @author thommy
 */
public class GraphicObjectsPanel extends JPanel
{

  protected boolean autoCenter = false;

  /**
   * Get the value of autoCenter
   *
   * @return the value of autoCenter
   */
  public boolean isAutoCenter()
  {
    return autoCenter;
  }

  /**
   * Set the value of autoCenter
   *
   * @param autoCenter new value of autoCenter
   */
  public void setAutoCenter(boolean autoCenter)
  {
    this.autoCenter = autoCenter;
  }

  protected Point center = null;
  public static final String PROP_CENTER = "center";

  /**
   * Get the value of center
   *
   * @return the value of center
   */
  public Point getCenter()
  {
    return center;
  }

  /**
   * Set the value of center
   *
   * @param center new value of center
   */
  public void setCenter(Point center)
  {
    Point oldCenter = this.center;
    this.center = center;
    this.repaint();
    firePropertyChange(PROP_CENTER, oldCenter, center);
  }
  protected int zoom = 1000;
  public static final String PROP_ZOOM = "zoom";

  /**
   * Get the value of zoom
   *
   * @return the value of zoom
   */
  public int getZoom()
  {
    return zoom;
  }

  /**
   * Set the value of zoom in %. 100 is one pixel per pixel.
   *
   * @param zoom new value of zoom
   */
  public void setZoom(int zoom)
  {
    int oldZoom = this.zoom;
    this.zoom = zoom;
    this.repaint();
    firePropertyChange(PROP_ZOOM, oldZoom, zoom);
  }

  @Override
  public void setSize(int w, int h)
  {
    super.setSize(w, h);
  }

  @Override
  public void setSize(Dimension d)
  {
    super.setSize(d);
  }

  private AffineTransform calculateTransform()
  {
    AffineTransform ownTransform = AffineTransform.getScaleInstance((double) zoom / 1000, (double) zoom / 1000);

    double w = this.getWidth();
    double h = this.getHeight();
    Point2D mp = new Point2D.Double(w / 2, h / 2);
    if (center != null)
    {
      Point2D drawnCenter = ownTransform.transform(center, null);
      AffineTransform trans = AffineTransform.getTranslateInstance(mp.getX() - drawnCenter.getX(), mp.getY() - drawnCenter.getY());
      trans.concatenate(ownTransform);
      ownTransform = trans;
    }
    else if (autoCenter)
    {
      try
      {
        ownTransform.createInverse().transform(mp, mp);
        this.setCenter(new Point((int) mp.getX(), (int) mp.getY()));
      }
      catch (NoninvertibleTransformException ex)
      {
        Logger.getLogger(GraphicObjectsPanel.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
    return ownTransform;
  }

  @Override
  protected void paintComponent(Graphics g)
  {
    super.paintComponent(g);
    if (g instanceof Graphics2D)
    {
      Graphics2D gg = (Graphics2D) g;
      AffineTransform at = gg.getTransform();
      at.concatenate(this.calculateTransform());
      gg.setTransform(at);
    }

  }
}
