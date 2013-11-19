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
package com.t_oster.visicut.model.graphicelements.gcodesupport;
import com.t_oster.visicut.model.graphicelements.ShapeObject;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author thommy
 */
public class GCodeShape implements ShapeObject
{

  private Shape shape;
  private File scriptSource;
  
  public GCodeShape(Shape shape, File scriptSource)
  {
    this.shape = shape;
    this.scriptSource = scriptSource;
  }
  
  public File getScriptSource()
  {
    return scriptSource;
  }
  
  public Shape getShape()
  {
    return this.shape;
  }

  private Rectangle2D bbCache;
  public Rectangle2D getBoundingBox()
  {
    if (bbCache == null)
    {
      bbCache = this.shape.getBounds2D();
    }
    return bbCache;
  }

  public List<Object> getAttributeValues(String name)
  {
    if ("Type".equals(name))
    {
      return Arrays.asList(new Object[]{"GCode"});
    }
    return new LinkedList<Object>();
  }

  public List<String> getAttributes()
  {
    return Arrays.asList(new String[]{"Type"});
  }

  public void render(Graphics2D g)
  {
    g.setColor(Color.RED);
    g.setStroke(new BasicStroke());
    g.draw(shape);
  }
  
}
