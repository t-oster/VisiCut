/**
 * This file is part of VisiCut.
 * Copyright (C) 2011 Thomas Oster <thomas.oster@rwth-aachen.de>
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

import com.t_oster.liblasercut.platform.Util;
import com.t_oster.visicut.misc.Helper;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 *
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class EditRectangle extends Rectangle
{

  protected boolean rotateMode = false;

  /**
   * Get the value of rotateMode
   *
   * @return the value of rotateMode
   */
  public boolean isRotateMode()
  {
    return rotateMode;
  }

  /**
   * Set the value of rotateMode
   *
   * @param rotateMode new value of rotateMode
   */
  public void setRotateMode(boolean rotateMode)
  {
    this.rotateMode = rotateMode;
  }

  
  private int buttonSize = 10;

  public EditRectangle(int x, int y, int width, int height)
  {
    super(x,y,width,height);
  }
  
  public EditRectangle(Rectangle r)
  {
    super(r.x,r.y,r.width,r.height);
  }

  public EditRectangle(Rectangle2D r)
  {
    this(Helper.toRect(r));
  }
  
  public enum Button
  {
    TOP_LEFT,
    TOP_CENTER,
    TOP_RIGHT,
    CENTER_LEFT,
    CENTER_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_CENTER,
    BOTTOM_RIGHT,}

  public Button[] rotateButtons = new Button[]{Button.TOP_LEFT,Button.TOP_RIGHT,Button.BOTTOM_LEFT, Button.BOTTOM_RIGHT};
  /**
   * Returns the Rectangle of the given Button
   * of an EditRectangle with the given Dimensions
   * @param b
   * @return 
   */
  public Rectangle getButton(Button b, Rectangle r)
  {
    switch (b)
    {
      case TOP_LEFT:
        return new Rectangle(r.x - buttonSize / 2, r.y - buttonSize / 2, buttonSize, buttonSize);
      case TOP_CENTER:
        return new Rectangle(r.x + r.width / 2 - buttonSize / 2, r.y - buttonSize / 2, buttonSize, buttonSize);
      case TOP_RIGHT:
        return new Rectangle(r.x + r.width - buttonSize / 2, r.y - buttonSize / 2, buttonSize, buttonSize);
      case CENTER_LEFT:
        return new Rectangle(r.x - buttonSize / 2, r.y + r.height / 2 - buttonSize / 2, buttonSize, buttonSize);
      case CENTER_RIGHT:
        return new Rectangle(r.x + r.width - buttonSize / 2, r.y + r.height / 2 - buttonSize / 2, buttonSize, buttonSize);
      case BOTTOM_LEFT:
        return new Rectangle(r.x - buttonSize / 2, r.y + r.height - buttonSize / 2, buttonSize, buttonSize);
      case BOTTOM_CENTER:
        return new Rectangle(r.x + r.width / 2 - buttonSize / 2, r.y + r.height - buttonSize / 2, buttonSize, buttonSize);
      case BOTTOM_RIGHT:
        return new Rectangle(r.x + r.width - buttonSize / 2, r.y + r.height - buttonSize / 2, buttonSize, buttonSize);
    }
    return null;
  }

  public Button getButtonByPoint(Point p, AffineTransform atr)
  {
    Rectangle tr = atr == null ? this :Helper.toRect(Helper.transform(this, atr));
    for (Button b : Button.values())
    {
      if (this.getButton(b, tr).contains(p))
      {
        return b;
      }
    }
    return null;
  }
  
  private Color lineColor = new Color(104,146,255);
  private Color textColor = Color.BLACK;
  private Color buttonColor = lineColor;
  
  /**
   * Renders the Edit Rectangle on the given Graphics2D.
   * The Rectangle coordinates are transformed according
   * the current Transform in the graphics. The Stroke width
   * and Button size, however are always in pixel.
   * @param gg
   * @param transform 
   */
  public void render(Graphics2D gg)
  {
    //reset transform for drawing same (text and line) size on every zoom level
    AffineTransform cur = gg.getTransform();
    gg.setTransform(AffineTransform.getRotateInstance(0));
    //draw the rectangle
    gg.setColor(lineColor);
    gg.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 0, new float[]
      {
        10, 10
      }, 0));
    Rectangle tr = Helper.toRect(Helper.transform(this, cur));
    gg.drawRect(tr.x, tr.y, tr.width, tr.height);
    if (this.rotateMode)
    {
      gg.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 0, null, 0));
      for (Button b: rotateButtons)
      {
        Rectangle r = this.getButton(b,tr);
        gg.drawOval(r.x, r.y, r.width, r.height);
      }
    }
    else
    {
      //draw the corner buttons
      gg.setColor(buttonColor);
      for (Button b : Button.values())
      {
        Rectangle r = this.getButton(b,tr);
        gg.fillRect(r.x, r.y, r.width, r.height);
      }
    }
    //draw the width
    gg.setColor(textColor);
    int w = (int) Helper.px2mm(this.width);
    String txt = (w/10)+","+(w%10)+" cm";
    w = gg.getFontMetrics().stringWidth(txt);
    int h = gg.getFontMetrics().getHeight();
    gg.drawString(txt, tr.x+tr.width/2-w/2, tr.y+tr.height+h);
    //draw the height
    w = (int) Helper.px2mm(this.height);
    txt = (w/10)+","+(w%10)+" cm";
    w = gg.getFontMetrics().stringWidth(txt);
    gg.drawString(txt, tr.x+tr.width+5, tr.y+tr.height/2);
    //draw lines from the left and upper center
    gg.setColor(lineColor);
    Point zero = new Point(0, 0);
    cur.transform(zero, zero);
    gg.drawLine(zero.x, tr.y+tr.height/2, tr.x, tr.y+tr.height/2);
    gg.drawLine(tr.x+tr.width/2, zero.y, tr.x+tr.width/2, tr.y);
    //draw the left
    gg.setColor(textColor);
    w = (int) Helper.px2mm(this.x);
    txt = (w/10)+","+(w%10)+" cm";
    w = gg.getFontMetrics().stringWidth(txt);
    h = gg.getFontMetrics().getHeight();
    gg.drawString(txt, tr.x-w-10, tr.y+tr.height/2+h);
    //draw the top offset
    w = (int) Helper.px2mm(this.y);
    txt = (w/10)+","+(w%10)+" cm";
    w = gg.getFontMetrics().stringWidth(txt);
    gg.drawString(txt, tr.x+tr.width/2+5, tr.y-h);
    //reset transform
    gg.setTransform(cur);
  }
}
