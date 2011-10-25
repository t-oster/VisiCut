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
public class FloydSteinberg extends DitheringAlgorithm
{

  protected void doDithering()
  {
    int pixelcount = 0;
    /**
     * We have to copy the input image, because we will
     * alter the pixels during dither process and don't want
     * to destroy the input image
     */
    int[][] input = new int[src.getWidth()][2];
    for (int x = 0; x < src.getWidth(); x++)
    {
      input[x][1] = src.getGreyScale(x, 0);
    }
    for (int y = 0; y < src.getHeight(); y++)
    {
      // copy lower line to upper line
      // and read in next line from picture
      for (int x = 0; x < input.length; x++)
      {
        input[x][0] = input[x][1];
        if (y + 1 < src.getHeight())
        {
          input[x][1] = (src.getGreyScale(x, y + 1));
        }
      }

      for (int x = 0; x < input.length; x++)
      {
        this.setBlack(x, y, input[x][0] <= 127);
        int error = input[x][0] - ((input[x][0] <= 127) ? 0 : 255);
        if (x + 1 < input.length)
        {
          input[x + 1][0] = (input[x + 1][0] + 7 * error / 16);
          if (y + 1 < src.getHeight())
          {
            input[x + 1][1] = (input[x + 1][1] + 1 * error / 16);
          }
        }
        if (y + 1 < src.getHeight())
        {
          input[x][1] = (input[x][1] + 5 * error / 16);
          if (x > 0)
          {
            input[x - 1][1] = (input[x - 1][1] + 3 * error / 16);
          }
        }
      }
      setProgress((100 * pixelcount++) / (src.getHeight()));
    }
  }
}
