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
package de.thomas_oster.visicut.gui.beans;

import de.thomas_oster.visicut.VisicutModel;
import de.thomas_oster.visicut.misc.Helper;
import de.thomas_oster.visicut.model.LaserDevice;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

/**
 *
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class EditRectangle extends Rectangle2D.Double
{

  private double rotationAngle = 0;

  /**
   * Get the value of rotationAngle
   *
   * @return the value of rotationAngle
   */
  public double getRotationAngle()
  {
    return rotationAngle;
  }

  /**
   * Set the value of rotationAngle
   *
   * @param rotationAngle new value of rotationAngle
   */
  public void setRotationAngle(double rotationAngle)
  {
    this.rotationAngle = rotationAngle;
  }

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

  public EditRectangle(double x, double y, double width, double height)
  {
    super(x,y,width,height);
  }

  public EditRectangle(Rectangle2D r)
  {
    super(r.getX(),r.getY(),r.getWidth(),r.getHeight());
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
    BOTTOM_RIGHT,
    ROTATE_BUTTON
  }

  /**
   * Returns the Rectangle of the given Button
   * of an EditRectangle with the given Dimensions
   */
  private Rectangle2D getButton(Button b, Rectangle2D r, double buttonSize)
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

  public Button getButtonByPoint(Point2D.Double p, AffineTransform mm2px)
  {
    Rectangle2D tr = this;
    if (this.rotateMode)
    {
      double diagonal = Math.sqrt(tr.getWidth()*tr.getWidth()+tr.getHeight()*tr.getHeight());
      Point center = new Point((int) (tr.getX()+tr.getWidth()/2), (int) (tr.getY()+tr.getHeight()/2));
      if (p.distance((center.x + Math.cos(rotationAngle) * diagonal/2), (center.y + Math.sin(rotationAngle) * diagonal/2)) <= buttonSize)
      {
        return Button.ROTATE_BUTTON;
      }
    }
    else
    {
      double bs = this.buttonSize / mm2px.getScaleX();
      for (Button b : Button.values())
      {
        if (b != Button.ROTATE_BUTTON && this.getButton(b, tr, bs).contains(p))
        {
          return b;
        }
      }
    }
    return null;
  }

  private Color lineColor = new Color(104,146,255);
  private Color textColor = Color.BLACK;
  private Color textBackgroundColor = new Color(255, 255, 255, 160);
  private Color buttonColor = lineColor;

  public enum ParameterField
  {
    X,
    Y,
    WIDTH,
    HEIGHT,
    ANGLE
  }

  private Rectangle[] parameterFieldBounds = new Rectangle[]{
    new Rectangle(),
    new Rectangle(),
    new Rectangle(),
    new Rectangle(),
    new Rectangle()
  };
  /**
   * Returns the Bounds of the textFields
   * displaying the given parameter
   * in Panel Coordinates
   */
  public Rectangle getParameterFieldBounds(ParameterField pmf)
  {
    switch (pmf)
    {
      case X: return parameterFieldBounds[0];
      case Y: return parameterFieldBounds[1];
      case WIDTH: return parameterFieldBounds[2];
      case HEIGHT: return parameterFieldBounds[3];
      case ANGLE: return parameterFieldBounds[4];
    }
    return null;
  }

  /**
   * Renders the Edit Rectangle on the given Graphics2D.
   * The Rectangle coordinates are transformed according
   * the current Transform in the graphics. The Stroke width
   * and Button size, however are always in pixel.
   */
  public void render(Graphics2D gg, AffineTransform mm2px, boolean full)
  {
    class GraphicsHelper
    {
      /**
       * Draw text with filled background.
       * @param show actually draw something (True) or just compute the bounding box (False)
       * @param x position of text without background (as in Graphics2D.drawString)
       * @param y position without background (as in Graphics2D.drawString)
       * @return bounding box of text
       */
      public Rectangle drawTextWithBackground(boolean show, String text, int x, int y)
      {
        // determine bounding rectangle for drawString: https://stackoverflow.com/a/6416215
        Rectangle2D bounds = gg.getFontMetrics().getStringBounds(text, gg);
        bounds.setFrame(x, y - gg.getFontMetrics().getAscent(), bounds.getWidth(), bounds.getHeight());
        double R = 4;
        var fillRect = new RoundRectangle2D.Double(bounds.getX() - R/2, bounds.getY(), bounds.getWidth() + R, bounds.getHeight(), R, R);
        if (show)
        {
          gg.setColor(textBackgroundColor);
          gg.fill(fillRect);
          gg.setColor(textColor);
          gg.drawString(text, x, y);
        }
        return bounds.getBounds();
      }
    }
    var gh = new GraphicsHelper();
    //draw the rectangle
    gg.setColor(lineColor);
    gg.setStroke(new BasicStroke(full ? 2 : 1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]
      {
        10, 10
      }, 0));
    Rectangle tr = Helper.toRect(Helper.transform(this, mm2px));
    if (this.rotateMode)
    {
      double diagonal = Math.sqrt(tr.width*tr.width+tr.height*tr.height);
      Point center = new Point((int) (tr.x+tr.width/2d), (int) (tr.y+tr.height/2d));
      //draw a circle around the object
      gg.drawOval((int) (center.x-diagonal/2), (int) (center.y-diagonal/2), (int) diagonal, (int) diagonal);
      gg.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 0, null, 0));
      //draw the rotate button on the circle
      gg.drawOval((int) (center.x + Math.cos(rotationAngle) * diagonal/2 - buttonSize/2), (int) (center.y + Math.sin(rotationAngle) * diagonal/2 -buttonSize/2), buttonSize, buttonSize);
      //draw the angle
      gg.setColor(textColor);
      int w = (int) Math.round(10*Helper.angle2degree(this.rotationAngle));
      String txt = (w/10)+","+(w%10)+"°";
      int h = gg.getFontMetrics().getHeight();
      this.parameterFieldBounds[4] = gh.drawTextWithBackground(full, txt, 10+(int) (center.x+diagonal/2), center.y+h/2);
    }
    else
    {
      boolean originBottom = false;
      double bedHeight = 300;
      LaserDevice device = VisicutModel.getInstance().getSelectedLaserDevice();
      if (device != null) {
        bedHeight = device.getLaserCutter().getBedHeight();
        originBottom = device.isOriginBottomLeft();
      }
      
      gg.drawRect(tr.x, tr.y, tr.width, tr.height);
      if (full)
      {
        //draw the corner buttons
        gg.setColor(buttonColor);
        for (Button b : Button.values())
        {
          Rectangle r = Helper.toRect(this.getButton(b,tr,this.buttonSize));
          gg.fillRect(r.x, r.y, r.width, r.height);
        }
      }
      //draw the width
      gg.setColor(lineColor);
      gg.setColor(textColor);
      int w = (int) Math.round(this.width);
      String txt = "↔ " + (w/10)+","+(w%10)+" cm";
      w = gg.getFontMetrics().stringWidth(txt);
      int h = gg.getFontMetrics().getHeight();
      this.parameterFieldBounds[2] = gh.drawTextWithBackground(full, txt, tr.x+tr.width/2-w/2, originBottom ? tr.y-h : tr.y+tr.height+h);
      //draw the height
      w = (int) Math.round(this.height);
      txt = "↕ " + (w/10)+","+(w%10)+" cm";
      this.parameterFieldBounds[3] = gh.drawTextWithBackground(full, txt, tr.x+tr.width+10, tr.y+tr.height/2);
      //draw lines from the left and upper center
      gg.setColor(lineColor);
      Point zero = new Point(0, 0);
      if (full)
      {
        gg.drawLine(zero.x, tr.y+tr.height/2, tr.x, tr.y+tr.height/2);
        if (originBottom) {
          Point2D bh = mm2px.transform(new Point2D.Double(0, bedHeight), null);
          gg.drawLine(tr.x+tr.width/2, tr.y+tr.height, tr.x+tr.width/2, (int) bh.getY());
        }
        else {
          gg.drawLine(tr.x+tr.width/2, zero.y, tr.x+tr.width/2, tr.y);
        }
      }
      //draw the left
      gg.setColor(textColor);
      w = (int) Math.round(this.x);
      txt = (w/10)+","+(w%10)+" cm →";
      w = gg.getFontMetrics().stringWidth(txt);
      h = gg.getFontMetrics().getHeight();
      this.parameterFieldBounds[0] = gh.drawTextWithBackground(full, txt, tr.x-w-10, tr.y+tr.height/2+h);

      //draw the top offset
      
      
      
      w = (int) Math.round(originBottom ? bedHeight - this.y - this.height : this.y);
      txt = originBottom ? "↑ " : "↓ ";
      txt += (w/10)+","+(w%10)+" cm";
      w = gg.getFontMetrics().stringWidth(txt);
      this.parameterFieldBounds[1] = gh.drawTextWithBackground(full, txt, tr.x+tr.width/2+5, originBottom ? tr.y+tr.height+h : tr.y-h);
    }
  }
  
  @Override
  public EditRectangle clone()
  {
    return new EditRectangle(x, y, width, height);
  }
}
