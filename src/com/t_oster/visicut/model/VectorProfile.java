package com.t_oster.visicut.model;

import com.t_oster.liblasercut.LaserJob;
import com.t_oster.liblasercut.LaserProperty;
import com.t_oster.liblasercut.platform.Util;
import com.t_oster.liblasercut.utils.ShapeConverter;
import com.t_oster.visicut.model.graphicelements.GraphicObject;
import com.t_oster.visicut.model.graphicelements.GraphicSet;
import com.t_oster.visicut.model.graphicelements.ShapeObject;
import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;

/**
 * This class represents a Line Profile,
 * which means a kind of line which can be
 * cut with the lasercutten in a specified
 * Material
 * 
 * @author thommy
 */
public class VectorProfile extends LaserProfile
{

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
  public void renderPreview(Graphics2D gg, GraphicSet objects)
  {
    for (GraphicObject e : objects)
    {
      if (e instanceof ShapeObject)
      {
        gg.setColor(this.getColor());
        Stroke s = new BasicStroke((int) Util.mm2px(this.getWidth(),500));
        gg.setStroke(s);
        Shape sh = ((ShapeObject) e).getShape();
        if (objects.getTransform()!=null)
        {
          sh = objects.getTransform().createTransformedShape(sh);
        }
        if (sh == null)
        {
          //WTF??
          System.out.println("Error extracting Shape from: "+((ShapeObject) e).toString());
        }
        else
        {
          gg.draw(sh);
        }
      }
    }
  }

  @Override
  public void addToLaserJob(LaserJob job, GraphicSet objects)
  {
    job.getVectorPart().setCurrentCuttingProperty(this.getCuttingProperty());
    ShapeConverter conv = new ShapeConverter();
    for (GraphicObject e : objects)
    {
      if (e instanceof ShapeObject)
      {
        Shape sh = ((ShapeObject) e).getShape();
        if (objects.getTransform() != null)
        {
          sh = objects.getTransform().createTransformedShape(sh);
        }
        conv.addShape(sh, job.getVectorPart());
      }
    }
  }
}
