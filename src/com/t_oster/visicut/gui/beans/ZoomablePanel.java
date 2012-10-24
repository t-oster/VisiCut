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

import java.awt.Dimension;
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
    this.mm2pxCache = null;
  }

  
  public ZoomablePanel()
  {
    this.addMouseWheelListener(this);
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
    if (oldZoom != zoom)
    {
      this.mm2pxCache = null;
      this.setPreferredSize(new Dimension((int) (this.getParent().getWidth()*(zoom/100d)), (int) (this.getParent().getHeight()*(zoom/100d))));
      this.revalidate();
      //this.repaint();
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
      double factor = Math.min(this.getParent().getWidth()/this.areaSize.x, this.getParent().getHeight()/this.areaSize.y);
      factor *= this.getZoom()/100d;
      //TODO translate to offset
      mm2pxCache = AffineTransform.getScaleInstance(factor, factor);
    }
    return mm2pxCache;
  }
  
  public void mouseWheelMoved(MouseWheelEvent mwe)
  {
    if (mwe.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL)
    {
      this.setZoom(this.getZoom() - (mwe.getUnitsToScroll() * this.getZoom() / 32));
    }
  }
}
