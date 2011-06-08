package com.t_oster.util;

/**
 * This Class is the replacement of the java.awt.Point and android.graphics.Point
 * because the library wants to run on both platforms without modification
 * 
 * @author thommy
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
}
