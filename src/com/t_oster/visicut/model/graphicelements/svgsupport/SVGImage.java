/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.visicut.model.graphicelements.svgsupport;

import com.kitfox.svg.ImageSVG;
import com.kitfox.svg.RenderableElement;
import java.awt.geom.Rectangle2D;
import java.util.List;

/**
 *
 * @author thommy
 */
public class SVGImage extends SVGObject
{

  private ImageSVG decoratee;

  public SVGImage(ImageSVG e)
  {
    decoratee = e;
  }

  @Override
  public RenderableElement getDecoratee()
  {
    return decoratee;
  }

  @Override
  public List<Object> getAttributeValues(String attribute)
  {
    List<Object> result = super.getAttributeValues(attribute);
    switch (Attribute.valueOf(attribute))
    {
      case ObjectType:
      {
        result.add("Image");
        break;
      }
    }
    return result;
  }
}
