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
