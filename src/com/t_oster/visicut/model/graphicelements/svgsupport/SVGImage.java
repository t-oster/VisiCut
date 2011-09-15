/**
 * This file is part of VisiCut.
 * 
 *     VisiCut is free software: you can redistribute it and/or modify
 *     it under the terms of the Lesser GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *    VisiCut is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     Lesser GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with VisiCut.  If not, see <http://www.gnu.org/licenses/>.
 **/
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
