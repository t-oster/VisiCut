/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.jepilog.model;

import com.kitfox.svg.RenderableElement;
import com.t_oster.liblasercut.LaserProperty;
import com.t_oster.liblasercut.platform.Tuple;
import com.t_oster.liblasercut.platform.Point;
import java.util.List;

/**
 *
 * @author thommy
 */
public class EngravingImage extends Tuple<List<RenderableElement>, LaserProperty>
{

  private Point startPoint = new Point(0, 0);

  public void setStartPoint(Point p)
  {
    this.startPoint = p;
  }

  public Point getStartPoint()
  {
    return this.startPoint;
  }

  public LaserProperty getProperty()
  {
    return this.getB();
  }

  public List<RenderableElement> getElements()
  {
    return this.getA();
  }
}
