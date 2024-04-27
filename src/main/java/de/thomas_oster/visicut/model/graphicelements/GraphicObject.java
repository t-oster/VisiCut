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
package de.thomas_oster.visicut.model.graphicelements;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

/**
 *
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public interface GraphicObject
{
  /**
   * Get bounding box.
   *
   * The stroke width of lines is included in the bounding box (at least for SVG;
   * the implementation status for other formats is unclear.)
   * This may be done as a simplified approximation by adding half the stroke width at every boundary,
   * even if the rendered path behaves differently (e.g., ignoring the SVG stroke-linejoin setting).
   *
   * TODO: add a parameter to include/exclude stroke width in the bounding box calculation
   * (stroke width should be included for engrave but excluded for cutting)
   *
   * @return bounding rectangle in raw units (e.g., SVG pixels)
   */
  public Rectangle2D getBoundingBox();
  /**
   * Returns a list of attribute values for the given
   * Attribute.
   */
  public List<Object> getAttributeValues(String name);
  /**
   * Returns a List of Attributes where the Object
   * has values set
   */
  public List<String> getAttributes();
  
  /**
   * Renders the Object on the given Graphcis2D
   */
  public void render(Graphics2D g);
}
