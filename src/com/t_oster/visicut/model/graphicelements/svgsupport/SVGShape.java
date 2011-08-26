/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.visicut.model.graphicelements.svgsupport;

import com.kitfox.svg.SVGElementException;
import com.kitfox.svg.ShapeElement;
import com.kitfox.svg.animation.AnimationElement;
import com.kitfox.svg.xml.StyleAttribute;
import com.t_oster.visicut.model.graphicelements.ShapeObject;
import java.awt.Shape;
import java.util.List;
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

  @Override
  public List<Object> getAttributeValues(String a)
  {
    List<Object> result = super.getAttributeValues(a);
    try
    {
      switch (Attribute.valueOf(a))
      {
        case StrokeWidth:
        {
          if (decoratee.hasAttribute("stroke-width", AnimationElement.AT_CSS))
          {
            StyleAttribute sa = decoratee.getStyleAbsolute("stroke-width");
            result.add("" + sa.getFloatValue());
          }
          break;
        }
        case ObjectType:
        {
          result.add("Shape");
          break;
        }
        case StrokeColor:
        {
          if (decoratee.hasAttribute("stroke", AnimationElement.AT_CSS))
          {
            StyleAttribute sa = decoratee.getStyleAbsolute("stroke");
            result.add(sa.getColorValue());
          }
          break;
        }
        case FillColor:
        {
          if (decoratee.hasAttribute("fill", AnimationElement.AT_CSS))
          {
            StyleAttribute sa = decoratee.getStyleAbsolute("fill");
            result.add(sa.getColorValue());
          }
          break;
        }
      }
    }
    catch (SVGElementException ex)
    {
      Logger.getLogger(SVGShape.class.getName()).log(Level.SEVERE, null, ex);
    }
    return result;
  }

  public ShapeElement getDecoratee()
  {
    return this.decoratee;
  }

  public Shape getShape()
  {
    return this.getDecoratee().getShape();
  }
}
