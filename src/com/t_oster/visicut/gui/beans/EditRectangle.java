/**
 * This file is part of VisiCut.
 * 
 *     VisiCut is free software: you can redistribute it and/or modify
 *     it under the terms of the Lesser GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *    VisiCut is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     Lesser GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with VisiCut.  If not, see <http://www.gnu.org/licenses/>.
 **/
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.visicut.gui.beans;

import com.t_oster.liblasercut.platform.Util;
import com.t_oster.visicut.misc.Helper;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

/**
 *
 * @author thommy
 */
public class EditRectangle extends Rectangle
{

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
    AffineTransform cur = gg.getTransform();
    gg.setTransform(AffineTransform.getRotateInstance(0));
    gg.setColor(new Color(104,146,255));
    gg.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 0, new float[]
      {
        10, 10
      }, 0));
    Rectangle tr = Helper.toRect(Helper.transform(this, cur));
    gg.drawRect(tr.x, tr.y, tr.width, tr.height);
    for (Button b : Button.values())
    {
      Rectangle r = this.getButton(b,tr);
      gg.fillRect(r.x, r.y, r.width, r.height);
    }
    gg.setTransform(cur);
  }
}
