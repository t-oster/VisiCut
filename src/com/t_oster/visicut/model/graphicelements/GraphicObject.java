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
package com.t_oster.visicut.model.graphicelements;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

/**
 *
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public interface GraphicObject
{
  public Rectangle2D getBoundingBox();
  /**
   * Returns a list of attribute values for the given
   * Attribute.
   * @param name
   * @return 
   */
  public List<Object> getAttributeValues(String name);
  /**
   * Returns a List of Attributes where the Object
   * has values set
   * @return 
   */
  public List<String> getAttributes();
  
  /**
   * Renders the Object on the given Graphcis2D
   * @param g 
   */
  public void render(Graphics2D g);
}
