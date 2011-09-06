/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.visicut.model.graphicelements;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author thommy
 */
public class ShapeDecorator implements ShapeObject
{

  private Shape decoratee;

  public ShapeDecorator(Shape s)
  {
    this.decoratee = s;
  }
  
  public Shape getShape()
  {
    return decoratee;
  }

  public Rectangle2D getBoundingBox()
  {
    return decoratee.getBounds2D();
  }

  public List<Object> getAttributeValues(String name)
  {
    List<Object> result = new LinkedList<Object>();
    if ("Type".equals(name))
    {
      result.add("Shape");
    }
    return result;
  }

  public List<String> getAttributes()
  {
    List<String> result = new LinkedList<String>();
    result.add("Type");
    return result;
  }

  public void render(Graphics2D g)
  {
    g.draw(decoratee);
  }
}
