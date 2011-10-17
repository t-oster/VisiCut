/**
 * This file is part of VisiCut.
 * Copyright (C) 2011 Thomas Oster <thomas.oster@rwth-aachen.de>
 * RWTH Aachen University - 52062 Aachen, Germany
 * 
 *     VisiCut is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *    VisiCut is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 * 
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with VisiCut.  If not, see <http://www.gnu.org/licenses/>.
 **/
package com.t_oster.liblasercut.utils;

import com.t_oster.liblasercut.VectorPart;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;

/**
 * This class makes it possible to add java.awt.Shape Objects
 * to a VectorPart. The Shape will be converted to moveto and lineto
 * commands fitting as close as possible
 * 
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class ShapeConverter
{

  /**
   * Adds the given Shape to the given VectorPart by converting it to
   * lineto and moveto commands, whose lines differs not more than
   * 1 pixel from the original shape.
   * 
   * @param shape the Shape to be added
   * @param vectorpart the Vectorpart the shape shall be added to
   */
  public void addShape(Shape shape, VectorPart vectorpart)
  {
    AffineTransform scale = AffineTransform.getScaleInstance(1, 1);
    PathIterator iter = shape.getPathIterator(scale, 1);
    int startx = 0;
    int starty = 0;
    while (!iter.isDone())
    {
      double[] test = new double[8];
      int result = iter.currentSegment(test);
      if (result == PathIterator.SEG_MOVETO)
      {
        vectorpart.moveto((int) test[0], (int) test[1]);
        startx = (int) test[0];
        starty = (int) test[1];
      }
      else if (result == PathIterator.SEG_LINETO)
      {
        vectorpart.lineto((int) test[0], (int) test[1]);
      }
      else if (result == PathIterator.SEG_CLOSE)
      {
        vectorpart.lineto(startx, starty);
      }
      iter.next();
    }
  }
}
