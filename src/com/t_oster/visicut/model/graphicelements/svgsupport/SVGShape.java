/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.visicut.model.graphicelements.svgsupport;

import com.kitfox.svg.Circle;
import com.kitfox.svg.Line;
import com.kitfox.svg.Path;
import com.kitfox.svg.Rect;
import com.kitfox.svg.SVGException;
import com.kitfox.svg.ShapeElement;
import com.kitfox.svg.Text;
import com.kitfox.svg.Tspan;
import com.kitfox.svg.xml.StyleAttribute;
import com.t_oster.visicut.model.graphicelements.ShapeObject;
import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author thommy
 */
public class SVGShape extends SVGObject implements ShapeObject
{

  private ShapeElement decoratee;

  public SVGShape(ShapeElement s)
  {
    this.decoratee = s;
  }

  /**
   * Returns the first StyleAttribute with the given name in the
   * Path from the current Node to the Root node
   * @param name
   * @return 
   */
  private StyleAttribute getStyleAttributeRecursive(String name)
  {
    StyleAttribute sa = new StyleAttribute(name);
    try
    {
      if (this.getDecoratee().getStyle(sa, true))
      {
        return sa;
      }
    }
    catch (Exception ex)
    {
      Logger.getLogger(SVGShape.class.getName()).log(Level.SEVERE, null, ex);
    }
    return null;
  }

  private Map<String,List<Object>> attributeValues = new LinkedHashMap<String,List<Object>>();
  @Override
  public List<Object> getAttributeValues(String a)
  {
    if (attributeValues.containsKey(a))
    {
      return attributeValues.get(a);
    }
    List<Object> result = super.getAttributeValues(a);
    switch (Attribute.valueOf(a.replace(" ", "_")))
    {
      case Stroke_Width:
      {
        StyleAttribute sa = getStyleAttributeRecursive("stroke-width");
        if (sa != null)
        {
          result.add("" + sa.getFloatValue());
        }
        else
        {
          result.add("none");
        }
        break;
      }
      case Type:
      {
        if (this.getDecoratee() instanceof Tspan)
        {
          result.add("Tspan");
        }
        if (this.getDecoratee() instanceof Circle)
        {
          result.add("Circle");
        }
        if (this.getDecoratee() instanceof Rect)
        {
          result.add("Rect");
        }
        if (this.getDecoratee() instanceof Text)
        {
          result.add("Text");
        }
        if (this.getDecoratee() instanceof com.kitfox.svg.Ellipse)
        {
          result.add("Ellipse");
        }
        if (this.getDecoratee() instanceof Line)
        {
          result.add("Line");
        }
        if (this.getDecoratee() instanceof Path)
        {
          result.add("Path");
        }
        result.add("Shape");
        break;
      }
      case Stroke_Color:
      {
        StyleAttribute sa = getStyleAttributeRecursive("stroke");
        if (sa != null)
        {
          Color c = sa.getColorValue();
          result.add(c == null ? "none" : c);
        }
        else
        {
          result.add("none");
        }
        break;
      }
      case Fill_Color:
      {
        StyleAttribute sa = getStyleAttributeRecursive("fill");
        if (sa != null)
        {
          Color c = sa.getColorValue();
          result.add(c == null ? "none" : c);
        }
        else
        {
          result.add("none");
        }
        break;
      }
    }
    attributeValues.put(a, result);
    return result;
  }

  public ShapeElement getDecoratee()
  {
    return this.decoratee;
  }

  private static final int MAXOVERSHOOT = 10;
  @Override
  public Rectangle2D getBoundingBox()
  {
    Rectangle2D bb = super.getBoundingBox();
    //Add overshoot
    bb.setRect(bb.getX()-MAXOVERSHOOT, bb.getY()-MAXOVERSHOOT, bb.getWidth()+2*MAXOVERSHOOT, bb.getHeight()+2*MAXOVERSHOOT);
    return bb;
  }
  
  public Shape getShape()
  {
    if (false && this.getDecoratee() instanceof Path)
    {
      try
      {
        /**
         * Seems as if the Transformations for non-Path elements
         * are already handled correct by Kitfox, but Path has to
         * be transformed manually
         */
        AffineTransform at = this.getAbsoluteTransformation();
        return at.createTransformedShape(this.getDecoratee().getShape());
      }
      catch (SVGException ex)
      {
        Logger.getLogger(SVGShape.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
    return this.getDecoratee().getShape();
  }
}
