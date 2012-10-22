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
package com.t_oster.visicut.model;

import com.t_oster.liblasercut.LaserJob;
import com.t_oster.liblasercut.LaserProperty;
import com.t_oster.liblasercut.VectorPart;
import com.t_oster.liblasercut.utils.ShapeConverter;
import com.t_oster.liblasercut.utils.VectorOptimizer;
import com.t_oster.visicut.misc.Helper;
import com.t_oster.liblasercut.utils.VectorOptimizer.OrderStrategy;
import com.t_oster.visicut.model.graphicelements.GraphicObject;
import com.t_oster.visicut.model.graphicelements.GraphicSet;
import com.t_oster.visicut.model.graphicelements.ShapeDecorator;
import com.t_oster.visicut.model.graphicelements.ShapeObject;
import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Area;
import java.util.List;

/**
 * This class represents a Line Profile,
 * which means a kind of line which can be
 * cut with the lasercutten in a specified
 * Material
 * 
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class VectorProfile extends LaserProfile
{

  public VectorProfile()
  {
    this.setName("cut");
  }
  
  protected OrderStrategy orderStrategy = OrderStrategy.NEAREST;

  /**
   * Get the value of orderStrategy
   *
   * @return the value of orderStrategy
   */
  public OrderStrategy getOrderStrategy()
  {
    return orderStrategy;
  }

  /**
   * Set the value of orderStrategy
   *
   * @param orderStrategy new value of orderStrategy
   */
  public void setOrderStrategy(OrderStrategy orderStrategy)
  {
    this.orderStrategy = orderStrategy;
  }

  
  protected boolean useOutline = false;

  /**
   * Get the value of useOutline
   *
   * @return the value of useOutline
   */
  public boolean isUseOutline()
  {
    return useOutline;
  }

  /**
   * Set the value of useOutline
   *
   * @param useOutline new value of useOutline
   */
  public void setUseOutline(boolean useOutline)
  {
    this.useOutline = useOutline;
  }
  
  protected boolean isCut = true;

  /**
   * Get the value of isCut
   *
   * @return the value of isCut
   */
  public boolean isIsCut()
  {
    return isCut;
  }

  /**
   * Set the value of isCut
   *
   * @param isCut new value of isCut
   */
  public void setIsCut(boolean isCut)
  {
    this.isCut = isCut;
  }
  protected float width = 1;

  /**
   * Get the value of width
   *
   * @return the value of width
   */
  public float getWidth()
  {
    return width;
  }

  /**
   * Set the value of width
   *
   * @param width new value of width
   */
  public void setWidth(float width)
  {
    this.width = width;
  }

  private GraphicSet calculateOuterShape(GraphicSet objects)
  {
    final Area outerShape = new Area();
    for (GraphicObject o : objects)
    {
      if (o instanceof ShapeObject)
      {
        outerShape.add(new Area(((ShapeObject) o).getShape()));
      }
      else
      {
        outerShape.add(new Area(o.getBoundingBox()));
      }
    }
    GraphicSet result = new GraphicSet();
    result.setBasicTransform(objects.getBasicTransform());
    result.setTransform(objects.getTransform());
    result.add(new ShapeDecorator(outerShape));
    return result;
  }
  
  @Override
  public void renderPreview(Graphics2D gg, GraphicSet objects, MaterialProfile material)
  {
    //TODO calculate outline
    gg.setColor(this.isCut ? material.getCutColor() : material.getEngraveColor());
    Stroke s = new BasicStroke((int) Helper.mm2px(this.getWidth()));
    gg.setStroke(s);
    if (this.isUseOutline())
    {
      objects = this.calculateOuterShape(objects);
    }
    for (GraphicObject e : objects)
    {
      Shape sh = (e instanceof ShapeObject) ? ((ShapeObject) e).getShape() : e.getBoundingBox();
      if (objects.getTransform() != null)
      {
        sh = objects.getTransform().createTransformedShape(sh);
      }
      if (sh == null)
      {
        //WTF??
        System.out.println("Error extracting Shape from: " + ((ShapeObject) e).toString());
      }
      else
      {
        gg.draw(sh);
      }
    }
  }

  @Override
  public void addToLaserJob(LaserJob job, GraphicSet objects, List<LaserProperty> laserProperties)
  {
    if (this.isUseOutline())
    {
      objects = this.calculateOuterShape(objects);
    }
    VectorPart part = new VectorPart(laserProperties.get(0));
    for (LaserProperty prop : laserProperties)
    {
      part.setProperty(prop);
      ShapeConverter conv = new ShapeConverter();
      for (GraphicObject e : objects)
      {
        Shape sh = (e instanceof ShapeObject) ? ((ShapeObject) e).getShape() : e.getBoundingBox();
        if (objects.getTransform() != null)
        {
          sh = objects.getTransform().createTransformedShape(sh);
        }
        conv.addShape(sh, part);
      }
    }
    VectorOptimizer vo = new VectorOptimizer(this.orderStrategy);
    job.addPart(vo.optimize(part));
  }

  @Override
  public LaserProfile clone()
  {
    VectorProfile cp = new VectorProfile();
    cp.description = description;
    cp.isCut = isCut;
    cp.name = name;
    cp.orderStrategy = orderStrategy;
    cp.thumbnailPath = thumbnailPath;
    cp.width = width;
    cp.useOutline = useOutline;
    return cp;
  }
}
