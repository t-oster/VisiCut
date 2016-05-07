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
package com.t_oster.visicut.gui.beans;

import com.t_oster.uicomponents.ZoomablePanel;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.image.RenderedImage;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

/**
 * This Panel displays a Set of points which are draggable
 * with the mouse
 *
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class CalibrationPanel extends ZoomablePanel implements MouseListener, MouseMotionListener
{

  //The size of the Points in Pixel
  private static int SIZE = 10;

  public CalibrationPanel()
  {
    this.addMouseListener(this);
    this.addMouseMotionListener(this);
    getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("LEFT"), "left");
    getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("KP_LEFT"), "left");
    getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("RIGHT"), "right");
    getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("KP_RIGHT"), "right");
    getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("UP"), "up");
    getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("KP_UP"), "up");
    getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("DOWN"), "down");
    getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("KP_DOWN"), "down");
    getActionMap().put("left", new MoveAction(-1,0));
    getActionMap().put("right", new MoveAction(1,0));
    getActionMap().put("up", new MoveAction(0,-1));
    getActionMap().put("down", new MoveAction(0,1));
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
    if (this.backgroundImage != null)
    {
      this.setAreaSize(new Point2D.Double(this.backgroundImage.getWidth(), this.backgroundImage.getHeight()));
    }
    this.repaint();
  }
  protected Point2D.Double[] pointList = new Point2D.Double[0];

  /**
   * Get the value of pointList
   *
   * @return the value of pointList
   */
  public Point2D.Double[] getPointList()
  {
    return pointList;
  }

  /**
   * Set the value of pointList
   *
   * @param pointList new value of pointList
   */
  public void setPointList(Point2D.Double[] pointList)
  {
    this.pointList = pointList;
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
        gg.drawRenderedImage(backgroundImage, this.getMmToPxTransform());
      }
      gg.setColor(Color.red);
      for (int i = 0; i < this.pointList.length; i++)
      {
        Point2D p = this.pointList[i];
        Point2D.Double sp = new Point2D.Double();
        AffineTransform trans = this.getMmToPxTransform();
        trans.transform(p, sp);
        drawCrossAndTicks(gg, sp, SIZE, i);
        if (p == selectedPoint)
        {
          gg.drawOval((int) (sp.x - SIZE / 2), (int) (sp.y - SIZE / 2), (int) SIZE, (int) SIZE);
        }
      }
    }
  }

  private void drawCrossAndTicks(Graphics2D g, Point2D.Double p, int size, int ticks)
  {
    g.drawLine((int) (p.x - size / 2), (int) p.y, (int) (p.x + size / 2), (int) p.y);
    g.drawLine((int) p.x, (int) (p.y - size / 2), (int) p.x, (int) (p.y + size / 2));
    for (int j = 0; j <= ticks; j++) {
          // label counts with ticks underneath
      if ((j+1) % 5 == 0) {
        g.drawLine((int)p.x - ticks * 2 + (j-5) * 4, (int)p.y + size+1, (int)p.x - ticks * 2 + j * 4, (int)p.y +size+ 4);
      } else {
        g.drawLine((int)p.x - ticks * 2 + j * 4, (int)p.y + size+1, (int)p.x - ticks * 2 + j * 4, (int)p.y +size+ 4);
      }
    }
  }

  public void mouseClicked(MouseEvent me)
  {
  }
  public Point2D.Double selectedPoint = null;

  public void mousePressed(MouseEvent me)
  {
    Point p = me.getPoint();
    selectedPoint = null;
    for (Point2D.Double source : this.getPointList())
    {
      Point2D target = this.getMmToPxTransform().transform(source, null);
      if (p.distance(target) < SIZE)
      {
        selectedPoint = source;
        repaint();
        return;
      }
    }
  }

  public void mouseReleased(MouseEvent me)
  {
  }

  public void mouseEntered(MouseEvent me)
  {
  }

  public void mouseExited(MouseEvent me)
  {
  }

  public void mouseDragged(MouseEvent me)
  {
    if (selectedPoint != null)
    {
      try
      {
        Point p = me.getPoint();
        this.getMmToPxTransform().createInverse().transform(p, p);
        selectedPoint.x = p.x;
        selectedPoint.y = p.y;
      }
      catch (NoninvertibleTransformException ex)
      {
        Logger.getLogger(CalibrationPanel.class.getName()).log(Level.SEVERE, null, ex);
      }
      this.repaint();
    }
  }

  public void mouseMoved(MouseEvent me)
  {
  }

  public class MoveAction extends AbstractAction {
    int x;
    int y;
    public MoveAction(int x, int y) {
      this.x = x;
      this.y = y;
    }
    public void actionPerformed(ActionEvent e) {
      if (selectedPoint != null) {
      selectedPoint.x += x;
      selectedPoint.y += y;
      repaint();
      }
    }
  }

}
