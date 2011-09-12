/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.visicut.model.graphicelements.dxfsupport;

import com.t_oster.visicut.model.graphicelements.ShapeObject;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.kabeja.dxf.Bounds;
import org.kabeja.dxf.DXFPolyline;
import org.kabeja.dxf.DXFVertex;

/**
 *
 * @author thommy
 */
public class Polyline implements ShapeObject
{
  private DXFPolyline decoratee = null;
  public Polyline(DXFPolyline decoratee)
  {
    this.decoratee = decoratee;
  }

  public Shape getShape()
  {
    GeneralPath result = new GeneralPath();
    Iterator<DXFVertex> i = decoratee.getVertexIterator();
    while(i.hasNext())
    {
      DXFVertex v = i.next();
      result.lineTo(v.getPoint().getX(), v.getPoint().getY());
    }
    return result;
  }

  public Rectangle2D getBoundingBox()
  {
    Bounds b = decoratee.getBounds();
    return new Rectangle.Double(b.getMinimumX(), b.getMinimumY(), b.getWidth(), b.getHeight());
  }

  public List<Object> getAttributeValues(String name)
  {
    List<Object> result = new LinkedList<Object>();
    if ("Type".equals(name)){
      result.add("Shape");
      result.add("Polyline");
    }
    else if ("Stroke Width".equals(name)){
      result.add(decoratee.getLineWeight());
    }
    else if ("Line Type".equals(name)){
      result.add(decoratee.getLineType());
    }
    return result;
  }

  public List<String> getAttributes()
  {
    List<String> result = new LinkedList<String>();
    result.add("Type");
    result.add("Stroke Width");
    result.add("Line Type");
    return result;
  }

  public void render(Graphics2D g)
  {
    g.draw(this.getShape());
  }
}
