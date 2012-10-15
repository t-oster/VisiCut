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
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class EditRectangle extends Rectangle2D.Double
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
  public Rectangle2D getButton(Button b, Rectangle2D r)
  {
    switch (b)
    {
      case TOP_LEFT:
        return new Rectangle2D.Double(r.getX() - buttonSize / 2, r.getY() - buttonSize / 2, buttonSize, buttonSize);
      case TOP_CENTER:
        return new Rectangle2D.Double(r.getX() + r.getWidth() / 2 - buttonSize / 2, r.getY() - buttonSize / 2, buttonSize, buttonSize);
      case TOP_RIGHT:
        return new Rectangle2D.Double(r.getX() + r.getWidth() - buttonSize / 2, r.getY() - buttonSize / 2, buttonSize, buttonSize);
      case CENTER_LEFT:
        return new Rectangle2D.Double(r.getX() - buttonSize / 2, r.getY() + r.getHeight() / 2 - buttonSize / 2, buttonSize, buttonSize);
      case CENTER_RIGHT:
        return new Rectangle2D.Double(r.getX() + r.getWidth() - buttonSize / 2, r.getY() + r.getHeight() / 2 - buttonSize / 2, buttonSize, buttonSize);
      case BOTTOM_LEFT:
        return new Rectangle2D.Double(r.getX() - buttonSize / 2, r.getY() + r.getHeight() - buttonSize / 2, buttonSize, buttonSize);
      case BOTTOM_CENTER:
        return new Rectangle2D.Double(r.getX() + r.getWidth() / 2 - buttonSize / 2, r.getY() + r.getHeight() - buttonSize / 2, buttonSize, buttonSize);
      case BOTTOM_RIGHT:
        return new Rectangle2D.Double(r.getX() + r.getWidth() - buttonSize / 2, r.getY() + r.getHeight() - buttonSize / 2, buttonSize, buttonSize);
    }
    return null;
  }

  public Button getButtonByPoint(Point p, AffineTransform atr)
  {
    Rectangle2D tr = atr == null ? this : Helper.transform(this, atr);
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
  
  public enum ParameterField
  {
    X,
    Y,
    WIDTH,
    HEIGHT
  }
  
  private Rectangle[] parameterFieldBounds = new Rectangle[]{
    new Rectangle(),
    new Rectangle(),
    new Rectangle(),
    new Rectangle()
  };
  /**
   * Returns the Bounds of the textFields
   * displaying the given parameter
   * in Panel Coordinates
   * @param pmf
   * @return 
   */
  public Rectangle getParameterFieldBounds(ParameterField pmf)
  {
    switch (pmf)
    {
      case X: return parameterFieldBounds[0];
      case Y: return parameterFieldBounds[1];
      case WIDTH: return parameterFieldBounds[2];
      case HEIGHT: return parameterFieldBounds[3];
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
  public void render(Graphics2D gg, AffineTransform mm2px)
  {
    //draw the rectangle
    gg.setColor(lineColor);
    gg.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 0, new float[]
      {
        10, 10
      }, 0));
    Rectangle tr = Helper.toRect(Helper.transform(this, mm2px));
    gg.drawRect(tr.x, tr.y, tr.width, tr.height);
    if (this.rotateMode)
    {
      gg.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 0, null, 0));
      for (Button b: rotateButtons)
      {
        Rectangle r = Helper.toRect(this.getButton(b,tr));
        gg.drawOval(r.x, r.y, r.width, r.height);
      }
    }
    else
    {
      //draw the corner buttons
      gg.setColor(buttonColor);
      for (Button b : Button.values())
      {
        Rectangle r = Helper.toRect(this.getButton(b,tr));
        gg.fillRect(r.x, r.y, r.width, r.height);
      }
    }
    //draw the width
    gg.setColor(textColor);
    int w = (int) Math.round(this.width);
    String txt = (w/10)+","+(w%10)+" cm";
    w = gg.getFontMetrics().stringWidth(txt);
    int ascend = gg.getFontMetrics().getAscent();
    int h = gg.getFontMetrics().getHeight();
    gg.drawString(txt, tr.x+tr.width/2-w/2, tr.y+tr.height+h);
    this.parameterFieldBounds[2].setBounds(tr.x+tr.width/2-w/2, tr.y+tr.height+h-ascend, w, h);
    //draw the height
    w = (int) Math.round(this.height);
    txt = (w/10)+","+(w%10)+" cm";
    w = gg.getFontMetrics().stringWidth(txt);
    gg.drawString(txt, tr.x+tr.width+5, tr.y+tr.height/2);
    this.parameterFieldBounds[3].setBounds(tr.x+tr.width+5, tr.y+tr.height/2-ascend, w, h);
    //draw lines from the left and upper center
    gg.setColor(lineColor);
    Point zero = new Point(0, 0);
    gg.drawLine(zero.x, tr.y+tr.height/2, tr.x, tr.y+tr.height/2);
    gg.drawLine(tr.x+tr.width/2, zero.y, tr.x+tr.width/2, tr.y);
    //draw the left
    gg.setColor(textColor);
    w = (int) Math.round(this.x);
    txt = (w/10)+","+(w%10)+" cm";
    w = gg.getFontMetrics().stringWidth(txt);
    h = gg.getFontMetrics().getHeight();
    gg.drawString(txt, tr.x-w-10, tr.y+tr.height/2+h);
    this.parameterFieldBounds[0].setBounds(tr.x-w-10, tr.y+tr.height/2+h-ascend, w, h);
    //draw the top offset
    w = (int) Math.round(this.y);
    txt = (w/10)+","+(w%10)+" cm";
    w = gg.getFontMetrics().stringWidth(txt);
    gg.drawString(txt, tr.x+tr.width/2+5, tr.y-h);
    this.parameterFieldBounds[1].setBounds(tr.x+tr.width/2+5, tr.y-h-ascend, w, h);
  }
}
