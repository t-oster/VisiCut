package com.t_oster.liblasercut;

/**
 * This class represents a defined Raster of greyscale pixels
 * which means it can be seen as 2 dimensional array of bytes.
 * 
 * It has a subset of the routines of the java.awt.BufferedImage
 * so this could easy be used with an adapter.
 * It was chosen to use this Interface rather than the BufferdImage
 * because there are some plattfroms (ie Android) where BufferedImage
 * is not available but other Image classes which can be used.
 * 
 * @author thommy
 */
public interface GreyscaleRaster
{

  public int getWidth();

  public Byte getGreyScale(int x, int y);

  public void setGreyScale(int x, int y, Byte grey);

  public int getHeight();
}
