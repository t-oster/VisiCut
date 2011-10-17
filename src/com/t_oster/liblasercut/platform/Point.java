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
package com.t_oster.liblasercut.platform;

/**
 * This Class is the replacement of the java.awt.Point and android.graphics.Point
 * because the library wants to run on both platforms without modification
 * 
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class Point
{

  public int x;
  public int y;

  public Point(int x, int y)
  {
    this.x = x;
    this.y = y;
  }

  @Override
  public Point clone()
  {
    return new Point(x, y);
  }

  @Override
  public boolean equals(Object o)
  {
    if (o instanceof Point)
    {
      Point p = (Point) o;
      return p.x == x && p.y == y;
    }
    return o == this;
  }

  @Override
  public int hashCode()
  {
    int hash = 7;
    hash = 29 * hash + this.x;
    hash = 29 * hash + this.y;
    return hash;
  }
}
