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

import com.t_oster.liblasercut.GreyscaleRaster;
import java.awt.Color;
import java.awt.image.BufferedImage;

/**
 *
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class BufferedImageAdapter implements GreyscaleRaster
{

  private BufferedImage img;
  private int colorShift = 0;
  private boolean invertColors = false;

  public BufferedImageAdapter(BufferedImage img)
  {
    this(img, false);
  }
  
  public BufferedImageAdapter(BufferedImage img, boolean invertColors)
  {
    this.img = img;
    this.invertColors = invertColors;
  }

  public void setColorShift(int cs){
      this.colorShift = cs;
  }

  public int getColorShift(){
      return this.colorShift;
  }

  public int getGreyScale(int x, int line)
  {
    Color c = new Color(img.getRGB(x, line));
    int value = colorShift+(int) (0.3 * c.getRed() + 0.59 * c.getGreen() + 0.11 * c.getBlue());
    return invertColors ? 255-Math.max(Math.min(value, 255), 0) : Math.max(Math.min(value, 255), 0);
  }

  public void setGreyScale(int x, int y, int grey)
  {
    Color c = new Color(grey, grey, grey);
    img.setRGB(x, y, c.getRGB());
  }

  public int getWidth()
  {
    return img.getWidth();
  }

  public int getHeight()
  {
    return img.getHeight();
  }
}
