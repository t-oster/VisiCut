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

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.geom.AffineTransform;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.JPanel;

/**
 *
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class Ruler extends JPanel implements PropertyChangeListener, ComponentListener
{
  public static int HORIZONTAL = 1;
  public static int VERTICAL = 2;
  private int size = 25;
  private ZoomablePanel target;
  private int align = HORIZONTAL;
  public Ruler(ZoomablePanel target, int align)
  {
    this.align = align;
    this.target = target;
    target.addComponentListener(this);
    target.addPropertyChangeListener(this);
    this.setPreferredSize(new Dimension(align == HORIZONTAL ? target.getWidth() : size, align == VERTICAL ? target.getHeight() : size));
  }

  @Override
  public void paintComponent(Graphics g)
  {
    Rectangle vr = this.getVisibleRect();
    g.setColor(this.getBackground());
    g.fillRect(vr.x, vr.y, vr.width, vr.height);
    g.setColor(this.getForeground());
    AffineTransform mm2px = target.getMmToPxTransform();
    if (align == HORIZONTAL)
    {
      double dst = mm2px.getScaleX();
      for (int mm = 0; mm < target.getAreaSize().x; mm+= dst > 5 ? 1 : dst > 2 ? 5 : 10)
      {
        boolean drawText = mm % (dst > 3 ? 10 : 100) == 0;
        g.drawLine((int) (dst*mm), (drawText ? 5 : 6)*size/8, (int) (dst*mm), size);
        if (drawText && mm != 0)
        {
          String txt = ""+mm/10;
          int w = g.getFontMetrics().stringWidth(txt);
          g.drawString(txt, (int) (dst*mm)-w/2, size/2);
        }
      }
    }
    else
    {
      double dst = mm2px.getScaleY();
      for (int mm = 0; mm < target.getAreaSize().y; mm+= dst > 5 ? 1 : dst > 2 ? 5 : 10)
      {
        boolean drawText = mm % (dst > 3 ? 10 : 100) == 0;
        g.drawLine((drawText ? 5 : 6)*size/8, (int) (dst*mm), size, (int) (dst*mm));
        if (drawText && mm != 0)
        {
          int h = g.getFontMetrics().getHeight();
          g.drawString(""+mm/10, 0, (int) (dst*mm)+h/2);
        }
      }
    }
  }

  public void propertyChange(PropertyChangeEvent pce)
  {
    //throw new UnsupportedOperationException("Not supported yet.");
  }

  public void componentResized(ComponentEvent ce)
  {
    this.setPreferredSize(new Dimension(align == HORIZONTAL ? target.getWidth() : 25, align == VERTICAL ? target.getHeight() : 25));
    this.repaint();
  }

  public void componentMoved(ComponentEvent ce)
  {
  }

  public void componentShown(ComponentEvent ce)
  {
  }

  public void componentHidden(ComponentEvent ce)
  {
  }
}
