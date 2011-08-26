/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.visicut.model.graphicelements.svgsupport;

import com.kitfox.svg.Group;
import com.kitfox.svg.RenderableElement;
import com.kitfox.svg.SVGElement;
import com.kitfox.svg.SVGException;
import com.t_oster.visicut.model.graphicelements.GraphicObject;
import java.awt.Graphics2D;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author thommy
 */
public abstract class SVGObject implements GraphicObject
{

  public enum Attribute
  {

    StrokeWidth,
    StrokeColor,
    FillColor,
    ObjectType,
    Group,}

  public abstract RenderableElement getDecoratee();

  public void render(Graphics2D g)
  {
    try
    {
      this.getDecoratee().render(g);
    }
    catch (SVGException ex)
    {
      Logger.getLogger(SVGShape.class.getName()).log(Level.SEVERE, null, ex);
    }
  }

  public List<Object> getAttributeValues(String name)
  {
    List<Object> result = new LinkedList<Object>();
    switch (Attribute.valueOf(name))
    {
      case Group:
      {
        for (SVGElement e : this.getPath(this.getDecoratee()))
        {
          if (e instanceof Group)
          {
            result.add(((Group) e).getId());
          }
        }
        break;
      }
    }
    return result;
  }

  public List<String> getAttributes()
  {
    List<String> result = new LinkedList<String>();
    for (Attribute a : Attribute.values())
    {
      if (this.getAttributeValues(a.toString()).size() > 0)
      {
        result.add(a.toString());
      }
    }
    return result;
  }

  /**
   * Returns the path from root to the given Element
   * @param e
   * @return 
   */
  protected List<SVGElement> getPath(SVGElement e)
  {
    List<SVGElement> result = new LinkedList<SVGElement>();
    while (e != null)
    {
      result.add(0, e);
      e = e.getParent();
    }
    return result;
  }
}
