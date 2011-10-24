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
package com.t_oster.liblasercut.dithering;

/**
 *
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class Random extends DitheringAlgorithm
{

  protected void doDithering()
  {
    int width = src.getWidth();
    int height = src.getHeight();
    int pixelcount = 0;
    java.util.Random r = new java.util.Random();

    for (int y = 0; y < height; y++)
    {
      for (int x = 0; x < width; x++)
      {
        this.setBlack(x, y, src.getGreyScale(x, y) < r.nextInt(256));
      }
      setProgress((100 * pixelcount++) / (height));
    }
  }
}
