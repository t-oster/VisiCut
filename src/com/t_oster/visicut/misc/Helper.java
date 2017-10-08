/**
 * This file is part of VisiCut.
 * Copyright (C) 2011 - 2013 Thomas Oster <thomas.oster@rwth-aachen.de>
 * RWTH Aachen University - 52062 Aachen, Germany
 *
 *     VisiCut is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     VisiCut is distributed in the hope that it will be useful,
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
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import org.apache.commons.net.util.Base64;

/**
 * This class contains frequently used conversion methods
 *
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class Helper
{

  public static List<String> findVisiCamInstances()
  {
    List<String> result = new LinkedList<String>();
    // Find the server using UDP broadcast
    try {
      //Open a random port to send the package
      DatagramSocket c = new DatagramSocket();
      c.setBroadcast(true);

      byte[] sendData = "VisiCamDiscover".getBytes();

      //Try the 255.255.255.255 first
      try 
      {
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName("255.255.255.255"), 8888);
        c.send(sendPacket);
      } 
      catch (Exception e) 
      {
      }

      // Broadcast the message over all the network interfaces
      Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
      while (interfaces.hasMoreElements()) 
      {
        NetworkInterface networkInterface = interfaces.nextElement();
        if (networkInterface.isLoopback() || !networkInterface.isUp()) 
        {
          continue; // Don't want to broadcast to the loopback interface
        }
        for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) 
        {
          InetAddress broadcast = interfaceAddress.getBroadcast();
          if (broadcast == null) {
            continue;
          }
          // Send the broadcast package!
          try 
          {
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, broadcast, 8888);
            c.send(sendPacket);
          } 
          catch (Exception e) 
          {
          }
        }
      }
      //Wait for a response
      byte[] recvBuf = new byte[15000];
      c.setSoTimeout(3000);
      while(true)
      {
        DatagramPacket receivePacket = new DatagramPacket(recvBuf, recvBuf.length);
        try
        {
          c.receive(receivePacket);
          //Check if the message is correct
          String message = new String(receivePacket.getData()).trim();
          //Close the port!
          c.close();
          if (message.startsWith("http")) {
            result.add(message);
          }
        }
        catch (SocketTimeoutException e)
        {
          break;
        }
      }
    } catch (IOException ex) 
    {
    }
    return result;
  }
  
  public static Double evaluateExpression(String expr)
  {
    expr = expr.replace(",", ".");
    try
    {
      ScriptEngineManager mgr = new ScriptEngineManager();
      ScriptEngine engine = mgr.getEngineByName("JavaScript");
      expr = engine.eval(expr).toString();
    }
    catch (Exception e)
    {
      //e.printStackTrace();
    }
    return Double.parseDouble(expr);
  }

  public static double angle2degree(double angle)
  {
    double w = -(angle * 180d / Math.PI);
    if (w < 0)
    {
      w+=360;
    }
    return w;
  }

  public static double degree2angle(double degree)
  {
    return -Math.PI*degree/180d;
  }

  /**
  * Compute the rotation angle of an affine transformation.
  * Counter-clockwise rotation is considered positive.
  *
  * method taken from http://javagraphics.blogspot.com/
  *
  * @return rotation angle in radians (beween -pi and pi),
  *  or NaN if the transformation is bogus.
  */
  public static double getRotationAngle(AffineTransform transform) {
    transform = (AffineTransform) transform.clone();
    // Eliminate any post-translation
    transform.preConcatenate(AffineTransform.getTranslateInstance(
    -transform.getTranslateX(), -transform.getTranslateY()));
    Point2D p1 = new Point2D.Double(1,0);
    p1 = transform.transform(p1,p1);
    return Math.atan2(p1.getY(),p1.getX());
  }

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

  public static boolean isWindows()
  {
    return System.getProperty("os.name").toLowerCase().contains("windows");
  }
  
  public static boolean isLinux()
  {
    return System.getProperty("os.name").toLowerCase().contains("linux");
  }

  public static boolean isWindowsXP()
  {
    return isWindows() && System.getProperty("os.name").toLowerCase().contains("xp");
  }

  public static void installInkscapeExtension() throws FileNotFoundException, IOException
  {
    File src = new File(getVisiCutFolder(), "inkscape_extension");
    if (!src.exists() || !src.isDirectory())
    {
      throw new FileNotFoundException("Not a directory: "+src);
    }
    
    String profile_path = System.getenv("INKSCAPE_PORTABLE_PROFILE_DIR");

    if (profile_path == null)
    {
      profile_path = System.getenv("INKSCAPE_PROFILE_DIR");
    }

    File trg;

    if (profile_path != null)
    {
      trg = new File(profile_path);
    }
    else
    {
      if (isWindows())
      {
        trg = new File(System.getenv("AppData"));
      }
      else
      {
        trg = new File(FileUtils.getUserDirectory(), ".config");
      }
    }
    
    trg = new File(new File(trg, "inkscape"), "extensions");      
    
    if (!trg.exists() && !trg.mkdirs())
    {
      throw new FileNotFoundException("Can't create directory: "+trg);
    }
    for (File f :src.listFiles())
    {
      if ("visicut_export.py".equals(f.getName()))
      {
        File target = new File(trg, "visicut_export.py");
        BufferedReader r = new BufferedReader(new FileReader(f));
        BufferedWriter w = new BufferedWriter(new FileWriter(target));
        String line = r.readLine();
        while (line != null)
        {
          if ("VISICUTDIR=\"\"".equals(line))
          {
            line = "VISICUTDIR=r\""+getVisiCutFolder().getAbsolutePath()+"\"";
          }
          w.write(line);
          w.newLine();
          line = r.readLine();
        }
        w.flush();
        w.close();
        r.close();
      }
      else if (f.getName().toLowerCase().endsWith("inx") || f.getName().toLowerCase().endsWith("py"))
      {
        FileUtils.copyFileToDirectory(f, trg);
      }
    }
  }

  public static void installIllustratorScript() throws IOException
  {
    String errors = "";
    for (File dir : new File[]{
      new File("/Applications/Adobe Illustrator CS3/Presets"),
      new File("/Applications/Adobe Illustrator CS4/Presets"),
      new File("/Applications/Adobe Illustrator CS5/Presets"),
      new File("/Applications/Adobe Illustrator CS6/Presets"),      
      new File("/Applications/Adobe Illustrator CS4/Presets.localized"),
      new File("/Applications/Adobe Illustrator CS5/Presets.localized"),
      new File("/Applications/Adobe Illustrator CS6/Presets.localized")
    })
    {
      if (dir.exists() && dir.isDirectory())
      {
        for (File d : dir.listFiles())
        {
          if (!d.isDirectory())
          {
            continue;
          }
          if (!"Scripts".equals(d.getName()))
          {
            d = new File(d, "Scripts");
            if (!d.exists() || !d.isDirectory())
            {
              continue;
            }
          }
          try
          {
            FileUtils.copyFileToDirectory(getIllustratorScript(), d);
          }
          catch (IOException ex)
          {
            errors += "Can't copy to "+d.getAbsolutePath()+"\n";
          }
        }
      }
    }
    if (!"".equals(errors))
    {
      throw new IOException(errors);
    }
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

  public static String removeParentPath(File parent, String path)
  {
    if (path == null)
    {
      return null;
    }
    File p = new File(path);
    File bp = parent;
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

  public static String addParentPath(File parent, String path)
  {
    if (path == null)
    {
      return null;
    }
    if (!(new File(path).isAbsolute()))
    {
      return new File(parent, path).getAbsolutePath();
    }
    return path;
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
    return removeParentPath(getBasePath(), path);
  }

  /**
   * If the given path is relative, the base-path is prepended
   * @param parent
   * @param path
   * @return
   */
  public static String addBasePath(String path)
  {
    return addParentPath(getBasePath(), path);
  }

  /**
   * Generates an HTML img-Tag for the given file with given size
   * @param f
   * @param width
   * @param height
   * @return
   */
  public static String imgTag(URL u, int width, int height)
  {
    String size = width > 0 && height > 0 ? "width=" + width + " height=" + height : "";
    return "<img " + size + " src=\"" + u + "\"/>";
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
    try
    {
     return imgTag(f.toURI().toURL(), width, height);
    }
    catch (MalformedURLException ex)
    {
      Logger.getLogger(Helper.class.getName()).log(Level.SEVERE, null, ex);
      String size = width > 0 && height > 0 ? "width=" + width + " height=" + height : "";
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
  
  public static Rectangle2D smallestBoundingBox(Shape s, AffineTransform t)
  {
    double minX = 0;
    double maxX = 0;
    double minY = 0;
    double maxY = 0;
    PathIterator pi = s.getPathIterator(t, 1);
    double[] last = null;
    boolean first = true;
    while (!pi.isDone())
    {
      double[] d = new double[8];
      switch(pi.currentSegment(d))
      {
        case PathIterator.SEG_LINETO:
        {
          if (last != null)
          {
            if (first)
            {
              minX = last[0];
              maxX = last[0];
              minY = last[1];
              maxY = last[1];
              first = false;
            }
            else
            {
              if (last[0] < minX) { minX = last[0]; }
              if (last[0] > maxX) { maxX = last[0]; }
              if (last[1] < minY) { minY = last[1]; }
              if (last[1] > maxY) { maxY = last[1]; }
            }
          }
          if (first)
          {
            minX = d[0];
            maxX = d[0];
            minY = d[1];
            maxY = d[1];
            first = false;
          }
          else
          {
            if (d[0] < minX) { minX = d[0]; }
            if (d[0] > maxX) { maxX = d[0]; }
            if (d[1] < minY) { minY = d[1]; }
            if (d[1] > maxY) { maxY = d[1]; }
          }
          break;
        }
        case PathIterator.SEG_MOVETO:
        {
          last = d;
          break;
        }
      }
      pi.next();
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

  public static Color fromHtmlRGB(String rgb)
  {
    int r = Integer.parseInt(rgb.substring(1, 3), 16);
    int g = Integer.parseInt(rgb.substring(3, 5), 16);
    int b = Integer.parseInt(rgb.substring(5, 7), 16);
    return new Color(r,g,b);
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

  public static boolean isInkscapeExtensionInstallable()
  {
    File is = new File(getVisiCutFolder(), "inkscape_extension");
    return is.exists() && is.isDirectory();
  }

  private static File getIllustratorScript()
  {
    return new File(new File(getVisiCutFolder(), "illustrator_script"), "OpenWithVisiCut.scpt");
  }

  public static boolean isIllustratorScriptInstallable()
  {
    return isMacOS() && getIllustratorScript().exists();
  }

  public static String allowedChars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789.-";
  
  /**
   * Converts a string into a valid path name
   * @param name
   * @return
   */
  public static String toPathName(String name)
  {
    String result = "";
    for (char c : name.toCharArray())
    {
      if (!allowedChars.contains(""+c))
      {
        result += "_"+((int) c)+"_";
      }
      else
      {
        result += c;
      }
    }
    return result;
  }
  
  /**
   * Converts a converted filename from toPathName()
   * back to the original string
   * @param name
   * @return 
   */
  public static String fromPathName(String name)
  {
    String result = "";
    String index = null;
    for (char c : name.toCharArray())
    {
      if (index == null)
      {
        if (c == '_')
        {
          index = "";
        }
        else
        {
          result += c;
        }
      }
      else
      {
        if (c == '_')
        {
          result += (char) Integer.parseInt(index);
          index = null;
        }
        else
        {
          index += c;
        }
      }
    }
    return result;
  }
  
  public static String getEncodedCredentials(String user, String password)
  {
    String result = "";

    if (user != null && !user.isEmpty() && password != null && !password.isEmpty())
    {
      String credentials = user + ":" + password;

      try
      {
        result = Base64.encodeBase64String(credentials.getBytes("UTF-8"));
      }
      catch (UnsupportedEncodingException e)
      {
        result = Base64.encodeBase64String(credentials.getBytes());
      }

      // Remove line breaks in result, old versions of Base64.encodeBase64String add wrong line breaks
      result = result.replace("\n", "");
      result = result.replace("\r", "");
    }

    return result;
  }
}
