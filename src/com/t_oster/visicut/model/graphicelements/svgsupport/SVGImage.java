/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.visicut.model.graphicelements.svgsupport;

import com.kitfox.svg.ImageSVG;
import com.kitfox.svg.RenderableElement;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

  private Map<String,List<Object>> attributeValues = new LinkedHashMap<String,List<Object>>();
  @Override
  public List<Object> getAttributeValues(String attribute)
  {
    if (attributeValues.containsKey(attribute))
    {
      return attributeValues.get(attribute);
    }
    List<Object> result = super.getAttributeValues(attribute);
    switch (Attribute.valueOf(attribute.replace(" ", "_")))
    {
      case Type:
      {
        result.add("Image");
        break;
      }
    }
    attributeValues.put(attribute, result);
    return result;
  }
}
