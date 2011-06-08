/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.jepilog.model;

import com.kitfox.svg.RenderableElement;
import com.t_oster.liblasercut.EngravingProperty;
import com.t_oster.util.Tuple;
import com.t_oster.util.Point;
import java.util.List;

/**
 *
 * @author thommy
 */
public class EngravingImage extends Tuple<List<RenderableElement>, EngravingProperty>
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

  public EngravingProperty getProperty()
  {
    return this.getB();
  }

  public List<RenderableElement> getElements()
  {
    return this.getA();
  }
}
