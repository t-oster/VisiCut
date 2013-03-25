/**
 * This file is part of VisiCut.
 * Copyright (C) 2011 - 2013 Thomas Oster <thomas.oster@rwth-aachen.de>
 * RWTH Aachen University - 52062 Aachen, Germany
 *
 *     VisiCut is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     VisiCut is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with VisiCut.  If not, see <http://www.gnu.org/licenses/>.
 **/
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
import com.t_oster.visicut.misc.Helper;
import com.t_oster.visicut.model.graphicelements.ShapeObject;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class SVGShape extends SVGObject implements ShapeObject
{

  private double svgResolution = 90;
  private ShapeElement decoratee;

  public SVGShape(ShapeElement s, double svgResolution)
  {
    this.svgResolution = svgResolution;
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
  private Map<String, List<Object>> attributeValues = new LinkedHashMap<String, List<Object>>();

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
        double width = 1;
        if (sa != null)
        {
          width = SVGImporter.numberWithUnitsToMm(sa.getNumberWithUnits(), this.svgResolution);
        }
        try
         {
           AffineTransform t = this.getAbsoluteTransformation();
           width *= (t.getScaleX()+t.getScaleY()) / 2;
         }
         catch (SVGException ex)
         {
           Logger.getLogger(SVGShape.class.getName()).log(Level.SEVERE, null, ex);
         }
         result.add((Double) width);
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
      case Color:
      {
        StyleAttribute sa = getStyleAttributeRecursive("stroke");
        if (sa != null && sa.getColorValue() != null)
        {
          result.add(sa.getColorValue());
        }
        else
        {
          result.add("none");
        }
        sa = getStyleAttributeRecursive("fill");
        if (sa != null && sa.getColorValue() != null)
        {
          result.add(sa.getColorValue());
        }
        else
        {
          result.add(Color.BLACK);
        }
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
          result.add(Color.BLACK);
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

  public Rectangle2D getShapeBoundingBox()
  {
    Rectangle2D bb = this.getShape().getBounds2D();
    return bb;
  }
  
  @Override
  public Rectangle2D getBoundingBox()
  {
    AffineTransform at;
    try
    {
      at = this.getAbsoluteTransformation();
    }
    catch (SVGException ex)
    {
      Logger.getLogger(SVGShape.class.getName()).log(Level.SEVERE, null, ex);
      at = new AffineTransform();
    }
    Rectangle2D bb = Helper.smallestBoundingBox(this.getDecoratee().getShape(), at);
    StyleAttribute sa = getStyleAttributeRecursive("stroke-width");
    if (sa != null)
    {
      double w = SVGImporter.numberWithUnitsToMm(sa.getNumberWithUnits(),svgResolution);
      //TODO: get Stroke width with unit and add it to width/height of BB
      bb.setRect(bb.getX()-w/2, bb.getY()-w/2, bb.getWidth()+w, bb.getHeight()+w);
    }
    return bb;
  }
  
  public Shape getShape()
  {
    try
    {
      AffineTransform at = this.getAbsoluteTransformation();
      StyleAttribute styleAttrib = new StyleAttribute();
      float[] strokeDashArray = null;
      if (getDecoratee().getStyle(styleAttrib.setName("stroke-dasharray")))
      {
          strokeDashArray = styleAttrib.getFloatList();
          if (strokeDashArray.length == 0)
        {
          strokeDashArray = null;
        }
      }
      float strokeDashOffset = 0f;
      if (getDecoratee().getStyle(styleAttrib.setName("stroke-dashoffset")))
      {
          strokeDashOffset = styleAttrib.getFloatValueWithUnits();
      }
      if (strokeDashArray == null)
      {
        return at.createTransformedShape(this.getDecoratee().getShape());
      }
      else
      {
        return at.createTransformedShape(new DashedShape(getDecoratee().getShape(), strokeDashArray, strokeDashOffset));
      }
    }
    catch (SVGException ex)
    {
      Logger.getLogger(SVGShape.class.getName()).log(Level.SEVERE, null, ex);
    }
    return null;
  }
}
