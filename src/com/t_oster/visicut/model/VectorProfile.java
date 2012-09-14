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
package com.t_oster.visicut.model;

import com.t_oster.liblasercut.LaserJob;
import com.t_oster.liblasercut.LaserProperty;
import com.t_oster.visicut.misc.Helper;
import com.t_oster.liblasercut.utils.ShapeConverter;
import com.t_oster.visicut.model.graphicelements.GraphicObject;
import com.t_oster.visicut.model.graphicelements.GraphicSet;
import com.t_oster.visicut.model.graphicelements.ShapeObject;
import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;
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
    this.setName("Cut Line");
  }
  protected boolean isCut = false;

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

  @Override
  public void renderPreview(Graphics2D gg, GraphicSet objects, MaterialProfile material)
  {
    for (GraphicObject e : objects)
    {
      gg.setColor(this.getColor());
      Stroke s = new BasicStroke((int) Helper.mm2px(this.getWidth()));
      gg.setStroke(s);
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
    for (LaserProperty prop : laserProperties)
    {
      job.getVectorPart().setCurrentCuttingProperty(prop);
      ShapeConverter conv = new ShapeConverter();
      for (GraphicObject e : objects)
      {
        Shape sh = (e instanceof ShapeObject) ? ((ShapeObject) e).getShape() : e.getBoundingBox();
        if (objects.getTransform() != null)
        {
          sh = objects.getTransform().createTransformedShape(sh);
        }
        conv.addShape(sh, job.getVectorPart());
      }
    }
  }

  @Override
  public LaserProfile clone()
  {
    VectorProfile cp = new VectorProfile();
    cp.color = color;
    cp.description = description;
    cp.isCut = isCut;
    cp.name = name;
    cp.thumbnailPath = thumbnailPath;
    cp.width = width;
    return cp;
  }
}
