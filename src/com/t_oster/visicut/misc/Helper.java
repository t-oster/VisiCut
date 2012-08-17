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
package com.t_oster.visicut.misc;

import com.kitfox.svg.xml.NumberWithUnits;
import com.t_oster.liblasercut.platform.Util;
import com.t_oster.visicut.VisicutModel;
import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLDecoder;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;

/**
 * This class contains frequently used conversion methods
 * 
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class Helper
{

  /**
   * Returns the distance between two Rectangles
   * @param r first rectangle
   * @param q second rectangle
   * @return Distance between two rectangles
   */
  public static double distance(Rectangle2D r, Rectangle2D q)
  {
    double qx0 = q.getX();
    double qy0 = q.getY();
    double qx1 = q.getX()+q.getWidth();
    double qy1 = q.getY()+q.getHeight();
    double rx0 = r.getX();
    double ry0 = r.getY();
    double rx1 = r.getX()+r.getWidth();
    double ry1 = r.getY()+r.getHeight();
    //Check for Overlap
    if (qx0 <= rx1 && qy0 <= ry1 && rx0 <= qx1 && ry0 <= qy1)
    {
      return 0;
    }
    double d = 0;
    if (rx0 > qx1)
    {
      d += (rx0 - qx1) * (rx0 - qx1);
    }
    else if (qx0 > rx1)
    {
      d += (qx0 - rx1) * (qx0 - rx1);
    }
    if (ry0 > qy1)
    {
      d += (ry0 - qy1) * (ry0 - qy1);
    }
    else if (qy0 > ry1)
    {
      d += (qy0 - ry1) * (qy0 - ry1);
    }
    return Math.sqrt(d);
  }

  public static boolean isMacOS()
  {
    return System.getProperty("os.name").toLowerCase().contains("mac");
  }
  
  public static File getVisiCutFolder()
  {
    try
    {
      String path = Helper.class.getProtectionDomain().getCodeSource().getLocation().getPath();
      if (path == null)
      {
        return null;
      }
      String decodedPath = URLDecoder.decode(path, "UTF-8");
      File folder = new File(decodedPath);
      return folder.isDirectory() ? folder : folder.getParentFile();
    }
    catch (UnsupportedEncodingException ex)
    {
      Logger.getLogger(Helper.class.getName()).log(Level.SEVERE, null, ex);
    }
    return null;
  }
  
  protected static File basePath;
  
  public static File getBasePath()
  {
    if (basePath == null)
    {
      basePath = new File(FileUtils.getUserDirectory(), ".visicut");
    }
    return basePath;
  }

  public static void setBasePath(File f)
  {
    basePath = f;
  }
  
  /**
   * If the given path is a successor of the parent-path,
   * only the relative path is given back.
   * Otherwise the path is not modified
   * @param path
   * @return 
   */
  public static String removeBasePath(String path)
  {
    File p = new File(path);
    File bp = getBasePath();
    String result = p.getName();
    while (p.getParentFile() != null)
    {
      p = p.getParentFile();
      if (p.getAbsolutePath().equals(bp.getAbsolutePath()))
      {
        return result;
      }
      result = p.getName() + "/" + result;
    }
    return path;
  }
  
  /**
   * If the given path is relative, the base-path is prepended
   * @param parent
   * @param path
   * @return 
   */
  public static String addBasePath(String path)
  {
    if (!(new File(path).isAbsolute()))
    {
      return new File(getBasePath(), path).getAbsolutePath();
    }
    return path;
  }
  
  /**
   * Returns how many mm correspont to the given length in pixels
   * with respect to the current resolution
   * @param mm
   * @return 
   */
  public static double px2mm(double px)
  {
    return Util.px2mm(px, VisicutModel.getInstance().getValidResolution());
  }

  /**
   * Returns how many pixels correspont to the given length in mm
   * with respect to the current resolution
   * @param mm
   * @return 
   */
  public static double mm2px(double mm)
  {
    return Util.mm2px(mm, VisicutModel.getInstance().getValidResolution());
  }

  /**
   * Generates an HTML img-Tag for the given file with given size
   * @param f
   * @param width
   * @param height
   * @return 
   */
  public static String imgTag(File f, int width, int height)
  {
    String size = width > 0 && height > 0 ? "width=" + width + " height=" + height : "";
    try
    {
      return "<img " + size + " src=\"" + f.toURI().toURL() + "\"/>";
    }
    catch (MalformedURLException ex)
    {
      Logger.getLogger(Helper.class.getName()).log(Level.SEVERE, null, ex);
      return "<img " + size + " src=\"file://" + f.getAbsolutePath() + "\"/>";
    }
  }

  /**
   * Returns an AffineTransform, which transformes src to dest
   * and constists of a scale and translate component
   * @param src
   * @param dest
   * @return 
   */
  public static AffineTransform getTransform(Rectangle2D src, Rectangle2D dest)
  {
    AffineTransform scale = AffineTransform.getScaleInstance(dest.getWidth() / src.getWidth(), dest.getHeight() / src.getHeight());
    Point2D scaled = scale.transform(new Point.Double(src.getX(), src.getY()), null);
    AffineTransform result = AffineTransform.getTranslateInstance(dest.getX() - scaled.getX(), dest.getY() - scaled.getY());
    result.concatenate(scale);
    return result;
  }

  public static Point toPoint(Point2D p)
  {
    return new Point((int) p.getX(), (int) p.getY());
  }

  public static Rectangle toRect(Rectangle2D src)
  {
    if (src == null)
    {
      return new Rectangle(0, 0, 0, 0);
    }
    return new Rectangle((int) src.getX(), (int) src.getY(), (int) src.getWidth(), (int) src.getHeight());
  }

  /**
   * Returns the smalles BoundingBox, which contains a number of Poins
   * @param points
   * @return 
   */
  public static Rectangle2D smallestBoundingBox(java.awt.Point.Double[] points)
  {
    double minX = points[0].x;
    double minY = points[0].y;
    double maxX = points[0].x;
    double maxY = points[0].y;
    for (java.awt.Point.Double p:points)
    {
      if (p.x < minX) {minX = p.x;}
      if (p.y < minY) {minY = p.y;}
      if (p.x > maxX) {maxX = p.x;}
      if (p.y > maxY) {maxY = p.y;}
    }
    return new Rectangle.Double(minX, minY, maxX-minX, maxY - minY);
  }
  
  /**
   * Returns a rectangle (parralel to x and y axis), which contains
   * the given rectangle after the given transform. If the transform
   * contains a rotation, the resulting rectangle is the smallest bounding-box
   * @param src
   * @param at
   * @return 
   */
  public static Rectangle2D transform(Rectangle2D src, AffineTransform at)
  {
    if (at == null)
    {
      return src;
    }
    else
    {
      java.awt.Point.Double[] points = new java.awt.Point.Double[4];
      points[0] = new java.awt.Point.Double(src.getX(), src.getY());
      points[1] = new java.awt.Point.Double(src.getX(), src.getY()+src.getHeight());
      points[2] = new java.awt.Point.Double(src.getX()+src.getWidth(), src.getY());
      points[3] = new java.awt.Point.Double(src.getX()+src.getWidth(), src.getY()+src.getHeight());
      for (int i=0;i<4;i++)
      {
        at.transform(points[i], points[i]);
      }
      return smallestBoundingBox(points);
    }
  }

  public static String toHtmlRGB(Color col)
  {
    String r = Integer.toHexString(col.getRed());
    String g = Integer.toHexString(col.getGreen());
    String b = Integer.toHexString(col.getBlue());
    return "#" + (r.length() == 1 ? "0" + r : r) + (g.length() == 1 ? "0" + g : g) + (b.length() == 1 ? "0" + b : b);
  }

  /**
   * Returns the given time in s as HH:MM:SS
   * @param estimateTime
   * @return 
   */
  public static String toHHMMSS(int estimateTime)
  {
    String result = "";
    int v = estimateTime / 3600;
    result += v < 10 ? "0" + v + ":" : "" + v + ":";
    estimateTime = estimateTime % 3600;
    v = estimateTime / 60;
    result += v < 10 ? "0" + v + ":" : "" + v + ":";
    estimateTime = estimateTime % 60;
    v = estimateTime;
    result += v < 10 ? "0" + v : "" + v;
    return result;
  }

  /**
   * Calculates the size in pixels (with repolution dpi)
   * of a NumberWithUnits element
   * @param n
   * @param dpi 
   */
  public static double numberWithUnitsToPx(NumberWithUnits n, int dpi)
  {
    switch (n.getUnits())
    {
      case NumberWithUnits.UT_MM:
        return Util.mm2px(n.getValue(), dpi);
      case NumberWithUnits.UT_CM:
        return Util.mm2px(10.0 * n.getValue(), dpi);
      case NumberWithUnits.UT_PX:
        return n.getValue() * dpi / 72;
      case NumberWithUnits.UT_IN:
        return Util.mm2px(Util.inch2mm(n.getValue()), dpi);
      case NumberWithUnits.UT_UNITLESS:
        return n.getValue();
      default:
        System.err.println("Unknown SVG unit!!!");
        return n.getValue();
    }
  }
}
