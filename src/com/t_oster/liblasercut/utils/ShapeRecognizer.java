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
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.liblasercut.utils;

import com.t_oster.liblasercut.BlackWhiteRaster;
import com.t_oster.liblasercut.TimeIntensiveOperation;
import com.t_oster.liblasercut.platform.Point;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author oster
 */
public class ShapeRecognizer extends TimeIntensiveOperation
{

  private enum Direction
  {

    east,
    north_east,
    north,
    north_west,
    west,
    south_west,
    south,
    south_east
  }
  private boolean outerBlack = false;

  /**
   * Tries to find the outer Shape which is visible
   * on the given bwr. The list of Points returned
   * corresponds to Points on the Shape in order
   * to draw a line around.
   * 
   * This method calls the progressChanged Method
   * of all ProgressListeners with values from 0
   * to 100 indicating the progress
   * 
   * @param bwr
   * @return  a List of Points marking a line around the
   * recognized Shape
   */
  public List<Point> getOuterShape(BlackWhiteRaster bwr)
  {
    int w = bwr.getWidth();
    int h = bwr.getHeight();
    List<Point> result = new LinkedList<Point>();
    //if the upper left corner is black,
    //we assume a white shape on black background
    outerBlack = bwr.isBlack(0, 0);
    //find the first point with inner color
    Point current = null;
    for (int y = 0; y < h; y++)
    {
      for (int x = 0; x < h; x++)
      {
        if (bwr.isBlack(x, y) != outerBlack)
        {
          current = new Point(x, y);
        }
      }
    }
    if (current == null)
    {//picture completely black/white
      //return rectangle wrapping the picture
      result.add(new Point(0, 0));
      result.add(new Point(w, 0));
      result.add(new Point(w, h));
      result.add(new Point(0, h));
      result.add(new Point(0, 0));
      return result;
    }
    Point first = current.clone();
    Point before = null;
    //iterate along the point always taking the most outer position
    //until matching the first point?! or no more possibilities
    while (current != null && current != first)
    {
      result.add(current);
      Point next = getFollower(bwr, current, before);
      before = current;
      current = next;
    }
    return null;
  }
  
  private Direction previous = Direction.west;

  /**
   * Returns the best follower in the bwr or null if
   * only the before is an option This method has to be called
   * iteratively
   * 
   * @param bwr
   * @param c The cuttent point whose follower is to determine
   * @return 
   */
  private Point getFollower(BlackWhiteRaster bwr, Point c, Point p)
  {
    int xdiff = p == null ? 1 : c.x - p.x;
    int ydiff = p == null ? 0 : c.y - p.y;
    if (bwr.isBlack(c.x-ydiff, c.y+xdiff)==outerBlack){
      return (new Point(c.x-ydiff, c.y-xdiff));
    }
    if (bwr.isBlack(c.x+xdiff, c.y+xdiff)==outerBlack){
      return (new Point(c.x-ydiff, c.y-xdiff));
    }
    return null;
  }
}
