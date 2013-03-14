/**
 * This file is part of VisiCut.
 * Copyright (C) 2011 - 2013 Thomas Oster <thomas.oster@rwth-aachen.de>
 * RWTH Aachen University - 52062 Aachen, Germany
 *
 *     VisiCut is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     VisiCut is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with VisiCut.  If not, see <http://www.gnu.org/licenses/>.
 **/
package com.t_oster.uicomponents;

import com.t_oster.liblasercut.platform.Util;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import javax.swing.JPanel;

/**
 * A JPanel with Support for rendering Graphic Objects.
 * This Panel supports Zoom etc.
 *
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class ZoomablePanel extends JPanel implements MouseWheelListener
{

    private Point2D.Double areaSize = new Point2D.Double(600d, 300d);

  /**
   * Get the value of AreaSize
   *
   * @return the value of AreaSize
   */
  public Point2D.Double getAreaSize()
  {
    return areaSize;
  }

  public void setOneToOneZoom()
  {
    double dpmm = Util.dpi2dpmm(this.getToolkit().getScreenResolution());
    double w = this.getAreaSize().x;
    double h = this.getAreaSize().y;
    double pw = this.getParent().getWidth();
    double ph = this.getParent().getHeight();
    this.setZoom(pw/ph <= w/h ? 100 * w/pw * dpmm : 100*h/ph*dpmm);
  }

  public void setZoomToFillParent()
  {
    double w = this.getAreaSize().x;
    double h = this.getAreaSize().y;
    double pw = this.getParent().getWidth();
    double ph = this.getParent().getHeight();
    this.setZoom(w/h > pw/ph ? 100*ph/pw*w/h : 100*pw/ph*h/w);
  }

  /**
   * Set the value of AreaSize
   * This ist the area in mm, which will be visible
   * when the Zoom is 100%
   *
   * @param AreaSize new value of AreaSize
   */
  public void setAreaSize(Point2D.Double AreaSize)
  {
    this.areaSize = AreaSize;
    this.resizeToFitZoomAndArea();
  }


  public ZoomablePanel()
  {
    this.addMouseWheelListener(this);
  }

  protected double zoom = 100;
  public static final String PROP_ZOOM = "zoom";

  /**
   * Get the value of zoom
   *
   * @return the value of zoom
   */
  public double getZoom()
  {
    return zoom;
  }

   /**
   * Set the value of zoom in %. 100
   *
   * @param zoom new value of zoom
   */
  public void setZoom(double zoom)
  {
    this.setZoom(zoom, null);
  }

  public void resizeToFitZoomAndArea()
  {
    this.mm2pxCache = null;
    if (this.getParent() != null)
    {
      double w = this.getAreaSize().x;
      double h = this.getAreaSize().y;
      double pw = this.getParent().getWidth();
      double ph = this.getParent().getHeight();
      double fullw = pw/ph <= w/h ? pw*zoom/100d : w*ph*zoom/100d/h;
      double fullh = pw/ph > w/h ? ph*zoom/100d : h*fullw/w;
      this.setPreferredSize(new Dimension((int) (fullw), (int) (fullh)));
      this.revalidate();
    }
  }

  public void setZoom(double zoom, Point stablePoint)
  {
    if (zoom < 100)
    {
      zoom = 100;
    }
    double oldZoom = this.zoom;
    this.zoom = zoom;
    if (oldZoom != zoom)
    {
      this.resizeToFitZoomAndArea();
      if (stablePoint != null)
      {
        double factor = (double) zoom/ (double) oldZoom;
        Point loc = this.getLocation();
        loc.setLocation(loc.x-(stablePoint.x*factor - stablePoint.x), loc.y-(stablePoint.y*factor - stablePoint.y));
        this.setLocation(loc);
      }
    }
    firePropertyChange(PROP_ZOOM, oldZoom, zoom);
  }

  private AffineTransform mm2pxCache = null;
  /**
   * returns the transformation, which transforms coordinates in mm
   * of the AreaSize to pixels of the preview panel. Taking into account
   * zoom and position.
   * @return
   */
  public AffineTransform getMmToPxTransform()
  {
    if (mm2pxCache == null)
    {
      Dimension d = this.getPreferredSize();
      double factor = Math.min(d.width/this.areaSize.x, d.height/this.areaSize.y);
      mm2pxCache = AffineTransform.getScaleInstance(factor, factor);
    }
    return mm2pxCache;
  }

  public void mouseWheelMoved(MouseWheelEvent mwe)
  {
    if (mwe.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL)
    {
      this.setZoom(this.getZoom() - (mwe.getUnitsToScroll() * this.getZoom() / 32), mwe.getPoint());
    }
  }
}
