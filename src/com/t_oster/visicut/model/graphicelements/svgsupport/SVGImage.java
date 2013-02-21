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

import com.kitfox.svg.ImageSVG;
import com.kitfox.svg.RenderableElement;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
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
      case Color:
      {
        result.add("Bitmap");
        break;
      }
    }
    attributeValues.put(attribute, result);
    return result;
  }
}
