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
public class ZoomablePanel extends JPanel
{

  protected Dimension outerBounds = new Dimension(100, 100);

  /**
   * Get the value of outerBounds
   *
   * @return the value of outerBounds
   */
  public Dimension getOuterBounds()
  {
    return outerBounds;
  }

  /**
   * Set the value of outerBounds
   * The Component can't zoom more out than the size of the outer bounds.
   *
   * @param outerBounds new value of outerBounds
   */
  public void setOuterBounds(Dimension outerBounds)
  {
    this.outerBounds = outerBounds;
    if (this.outerBounds == null)
    {
      outerBounds = this.getSize();
    }
    this.setZoom(this.getZoom());
  }
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
    if (center == null)
    {
      center = new Point(outerBounds.width/2, outerBounds.height/2);
    }
    double minCenterX = (this.outerBounds.width / 2) / (zoom / 100d);
    if (center.x < minCenterX)
    {
      center.x = (int) minCenterX;
    }
    double minCenterY = (this.outerBounds.height / 2) / (zoom / 100d);
    if (center.y < minCenterY)
    {
      center.y = (int) minCenterY;
    }
    double maxCenterX = this.outerBounds.width - minCenterX;
    if (center.x > maxCenterX)
    {
      center.x = (int) maxCenterX;
    }
    double maxCenterY = this.outerBounds.height - minCenterY;
    if (center.y > maxCenterY)
    {
      center.y = (int) maxCenterY;
    }

    Point oldCenter = this.center;
    this.center = center;
    firePropertyChange(PROP_CENTER, oldCenter, center);
    this.repaint();
  }
  protected int zoom = 100;
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
   * Set the value of zoom in %. 100 
   *
   * @param zoom new value of zoom
   */
  public void setZoom(int zoom)
  {
    if (zoom < 100)
    {
      zoom = 100;
    }
    int oldZoom = this.zoom;
    this.zoom = zoom;
    firePropertyChange(PROP_ZOOM, oldZoom, zoom);
    this.setCenter(this.getCenter());
  }

  public AffineTransform getZoomTransform()
  {
    if (this.getWidth() > 0 && this.getHeight() > 0 && zoom > 0)
    {
      double sh = (double) this.getWidth() / (double) this.outerBounds.width;
      double sv = (double) this.getHeight() / (double) this.outerBounds.height;
      double scale = Math.min(sh, sv);
      AffineTransform ownTransform = AffineTransform.getScaleInstance((double) scale * zoom / 100, (double) scale * zoom / 100);

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
      else
      {
        if (autoCenter)
        {
          try
          {
            ownTransform.createInverse().transform(mp, mp);
            this.setCenter(new Point((int) mp.getX(), (int) mp.getY()));
          }
          catch (NoninvertibleTransformException ex)
          {
            Logger.getLogger(ZoomablePanel.class.getName()).log(Level.SEVERE, null, ex);
          }
        }
      }
      return ownTransform;
    }
    return new AffineTransform();
  }

  @Override
  protected void paintComponent(Graphics g)
  {
    super.paintComponent(g);
    if (g instanceof Graphics2D)
    {
      Graphics2D gg = (Graphics2D) g;
      AffineTransform at = gg.getTransform();
      at.concatenate(this.getZoomTransform());
      gg.setTransform(at);
    }

  }
}
