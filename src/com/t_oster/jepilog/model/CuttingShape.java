/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.jepilog.model;

import com.kitfox.svg.SVGElement;
import com.kitfox.svg.SVGException;
import com.kitfox.svg.ShapeElement;
import com.kitfox.svg.xml.StyleAttribute;
import com.t_oster.liblasercut.CuttingProperty;
import com.t_oster.util.Tuple;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.util.List;

/**
 *
 * @author thommy
 */
public class CuttingShape extends Tuple<ShapeElement, CuttingProperty>
{

  /**
   * This 
   * applies all transformations in the Path of the SVGShape
   * and returns the Transformed Shape, which can be displayed
   * or printed on the position it appears in the original image.
   * @param selectedSVGElement
   * @return 
   */
  public static Shape extractTransformedShape(ShapeElement s) throws SVGException
  {
    if (s != null)
    {
      List first = s.getPath(null);
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
      return tr.createTransformedShape(s.getShape());
    }
    return null;
  }

  public CuttingShape()
  {
    this(null, null);
  }

  public CuttingShape(ShapeElement s)
  {
    this(s, null);
  }

  public CuttingShape(ShapeElement s, CuttingProperty p)
  {
    super(s, p);
  }

  public CuttingProperty getProperty()
  {
    return this.getB();
  }

  public ShapeElement getShapeElement()
  {
    return this.getA();
  }

  public Shape getTransformedShape() throws SVGException
  {
    return extractTransformedShape(this.getA());
  }

  public void setProperty(CuttingProperty p)
  {
    this.setB(p);
  }

  public void setShapeElement(ShapeElement s)
  {
    this.setA(s);
  }
}
