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
 * @author thommy
 */
public class BufferedImageAdapter implements GreyscaleRaster
{

  private BufferedImage img;
  private int colorShift = 0;

  public BufferedImageAdapter(BufferedImage img)
  {
    this.img = img;
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
    return Math.max(Math.min(value, 255), 0);
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
