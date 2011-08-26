/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.visicut.model.graphicelements.svgsupport;

import com.kitfox.svg.Group;
import com.kitfox.svg.RenderableElement;
import com.kitfox.svg.SVGElement;
import com.kitfox.svg.SVGException;
import com.kitfox.svg.xml.StyleAttribute;
import com.t_oster.visicut.model.graphicelements.GraphicObject;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
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
    Group,
    ID,
  }

  /**
   * Returns a List of SVGElements representing the Path
   * from the current Decorated Element to the root Node
   * @return 
   */
  public List<SVGElement> getPathToRoot()
  {
    List<SVGElement> result = new LinkedList<SVGElement>();
    SVGElement current = this.getDecoratee();
    while(current != null)
    {
      result.add(current);
      current=current.getParent();
    }
    return result;
  }
  
  public abstract RenderableElement getDecoratee();

  /**
   * This 
   * applies all transformations in the Path of the SVGShape
   * and returns the Transformed Shape, which can be displayed
   * or printed on the position it appears in the original image.
   * @param selectedSVGElement
   * @return 
   */
  public AffineTransform getAbsoluteTransformation() throws SVGException
  {
    if (this.getDecoratee() != null)
    {
      List first = this.getDecoratee().getPath(null);
      //Track all Transformations on the Path of the Elemenent
      AffineTransform tr = new AffineTransform();
      Object elem = first.get(first.size() - 1);
      for (Object o : first)
      {
        if (o instanceof SVGElement)
        {
          Object sty = ((SVGElement) o).getPresAbsolute("transform");
          if (sty != null && sty instanceof StyleAttribute)
          {
            StyleAttribute style = (StyleAttribute) sty;
            tr.concatenate(SVGElement.parseSingleTransform(style.getStringValue()));
          }
        }
      }
      return tr;
    }
    return null;
  }
  
  public void render(Graphics2D g)
  {
    try
    {
      g.setTransform(this.getAbsoluteTransformation());
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
      case ID:
      {
        result.add(this.getDecoratee().getId());
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
