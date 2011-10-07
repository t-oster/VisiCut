/**
 * This file is part of VisiCut.
 * Copyright (C) 2011 Thomas Oster <thomas.oster@rwth-aachen.de>
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

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * This class contains frequently used conversion methods
 * 
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class Helper
{

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
    Point2D scaled = scale.transform(new Point((int) src.getX(), (int) src.getY()), null);
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
    return new Rectangle((int) src.getX(), (int) src.getY(), (int) src.getWidth(), (int) src.getHeight());
  }

  public static Rectangle2D transform(Rectangle2D src, AffineTransform at)
  {
    if (at == null)
    {
      return src;
    }
    else
    {
      java.awt.Point p = new java.awt.Point((int) src.getX(), (int) src.getY());
      at.transform(p, p);
      java.awt.Point d = new java.awt.Point((int) (src.getX() + src.getWidth()), (int) (src.getY() + src.getHeight()));
      at.transform(d, d);
      return new Rectangle(p.x, p.y, d.x - p.x, d.y - p.y);
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
    int v = estimateTime/3600;
    result += v < 10 ? "0"+v+":" : ""+v+":";
    estimateTime=estimateTime%3600;
    v = estimateTime/60;
    result += v < 10 ? "0"+v+":" : ""+v+":";
    estimateTime=estimateTime%60;
    v = estimateTime;
    result += v < 10 ? "0"+v : ""+v;
    return result;
  }
}
