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
package com.t_oster.visicut.model.graphicelements.jpgpngsupport;

import com.t_oster.visicut.model.graphicelements.GraphicObject;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.image.RenderedImage;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class JPGPNGImage implements GraphicObject
{

  private RenderedImage decoratee;
  public JPGPNGImage(RenderedImage img)
  {
    decoratee = img;
  }
  public List<Object> getAttributeValues(String name)
  {
    List<Object> result = new LinkedList<Object>();
    if (name.equals("Type"))
    {
      result.add("Image");
    }
    if (name.equals("Color"))
    {
      result.add("Bitmap");
    }
    return result;
  }

  public List<String> getAttributes()
  {
    List<String> result = new LinkedList<String>();
    result.add("Type");
    result.add("Color");
    return result;
  }

  public void render(Graphics2D g)
  {
    g.drawRenderedImage(decoratee, null);
  }

  public Rectangle2D getBoundingBox()
  {
    return new Rectangle(decoratee.getMinX(), decoratee.getMinY(), decoratee.getWidth(), decoratee.getHeight());
  }
  
}
