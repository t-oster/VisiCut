/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.liblasercut.platform;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

/**
 *
 * @author oster
 */
public class Util
{

  public static Rectangle2D transform(Rectangle2D src, AffineTransform at)
  {
    if (at==null)
    {
      return src;
    }
    else
    {
      java.awt.Point p = new java.awt.Point((int) src.getX(),(int) src.getY());
      at.transform(p, p);
      java.awt.Point d = new java.awt.Point((int) src.getWidth(), (int) src.getHeight());
      at.deltaTransform(d, d);
      return new Rectangle(p.x,p.y,d.x,d.y);
    }
  }
  
  public static double inch2mm(double inch)
  {
    return inch * 25.4;
  }

  public static double mm2inch(double mm)
  {
    return mm / 25.4;
  }

  public static double px2mm(double px, double dpi)
  {
    return inch2mm(px / dpi);
  }

  public static double mm2px(double mm, double dpi)
  {
    return mm2inch(mm) * dpi;
  }

  /**
   * Returns true iff the given objects are not equal
   * This method is used to avoid null checks
   * @param a
   * @param b
   * @return 
   */
  public static boolean differ(Object a, Object b)
  {
    if (a == null ^ b == null)
    {
      return true;
    }
    else
    {
      if (a == null && b == null)
      {
        return false;
      }
      else
      {
        return !a.equals(b);
      }
    }
  }
}
