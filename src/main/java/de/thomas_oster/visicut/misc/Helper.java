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
package de.thomas_oster.visicut.misc;

import org.apache.commons.net.util.Base64;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
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
import java.io.InputStreamReader;
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
      else if (isMacOS()) {
        trg = new File(FileUtils.getUserDirectory(), "Library/Application Support/org.inkscape.inkscape/config");
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
          if ("VISICUTDIR = \"\"".equals(line))
          {
            line = "VISICUTDIR = r\""+getVisiCutFolder().getAbsolutePath()+"\"";
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
    StringBuilder errors = new StringBuilder();
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
            errors.append("Can't copy to ").append(d.getAbsolutePath()).append("\n");
          }
        }
      }
    }
    if (!"".equals(errors.toString()))
    {
      throw new IOException(errors.toString());
    }
  }

  public static File getVisiCutFolder()
  {
    Logger logger = Logger.getLogger(Helper.class.getName());

    // being pessimistic and assuming the directory can't be found
    // rest of this function, please feel free to prove the opposite!
    File visicutDir = null;

    try
    {
      // otherwise, we can try to find the location relative to the classes directory resp. the built JAR file
      // (in a development environment)
      File classPath = new File(Helper.class.getProtectionDomain().getCodeSource().getLocation().getPath());
      visicutDir = new File(classPath.getParent(), "../distribute/files");

      // TODO: it is unknown why we need to decode the path
      String decodedPath = URLDecoder.decode(visicutDir.getPath(), "UTF-8");
      visicutDir = new File(decodedPath);

      if (!visicutDir.isDirectory()) {
        visicutDir = visicutDir.getParentFile();
      }

      // detect and return the path in which the example folder exists
      File examplesFolder = new File(visicutDir, "examples");

      // if it can't be found, it likely means we're on macOS, so we try that path next
      if (!examplesFolder.exists()) {
        File macosExamplesFolder = new File(visicutDir.getParentFile(), "Resources/Java/examples");
        if (macosExamplesFolder.exists()) {
          visicutDir = macosExamplesFolder.getParentFile();
        }
      }
    }
    catch (UnsupportedEncodingException ex)
    {
      logger.log(Level.SEVERE, "Unsupported Encoding Exception", ex);
    }

    if (visicutDir == null) {
      logger.log(Level.SEVERE, "Could not detect visicut directory. Please report this bug.");
    }

    return visicutDir;
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
    StringBuilder result = new StringBuilder(p.getName());
    while (p.getParentFile() != null)
    {
      p = p.getParentFile();
      if (p.getAbsolutePath().equals(parent.getAbsolutePath()))
      {
        return result.toString();
      }
      result.insert(0, p.getName() + "/");
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
   */
  public static String removeBasePath(String path)
  {
    return removeParentPath(getBasePath(), path);
  }

  /**
   * If the given path is relative, the base-path is prepended
   */
  public static String addBasePath(String path)
  {
    return addParentPath(getBasePath(), path);
  }

  /**
   * Is basePath (the settings directory) controlled by a Version Control system
   * such as git?
   */
  public static boolean basePathIsVersionControlled()
  {
    String[] vcsDirs = {".git", ".svn", ".hg"};
    for (String vcsDir : vcsDirs)
    {
      if (new File(Helper.addBasePath(vcsDir)).exists()) {
        return true;
      }
    }
    return false;
  }

  /**
   * Generates an HTML img-Tag for the given file with given size
   */
  public static String imgTag(URL u, int width, int height)
  {
    String size = width > 0 && height > 0 ? "width=" + width + " height=" + height : "";
    return "<img " + size + " src=\"" + u + "\"/>";
  }

  /**
   * Generates an HTML img-Tag for the given file with given size
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

    int x = (int) src.getX();
    int y = (int) src.getY();
    int w = (int) src.getWidth();
    int h = (int) src.getHeight();

    // Normalize negative width and height to positive values, adjust x,y accordingly.
    if (w < 0)
    {
      w = -w;
      x = x - w;
    }
    if (h < 0)
    {
      h = -h;
      y = y - h;
    }

    return new Rectangle(x, y, w, h);
  }

  /**
   * Returns the smalles BoundingBox, which contains a number of Poins
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
    double minX = Double.POSITIVE_INFINITY;
    double maxX = Double.NEGATIVE_INFINITY;
    double minY = Double.POSITIVE_INFINITY;
    double maxY = Double.NEGATIVE_INFINITY;
    PathIterator pi = s.getPathIterator(t, 1);
    double[] lastMoveTo = null;
    while (!pi.isDone())
    {
      double[] d = new double[8];
      switch(pi.currentSegment(d))
      {
        case PathIterator.SEG_LINETO:
        {
          if (lastMoveTo != null)
          {
            if (lastMoveTo[0] < minX) { minX = lastMoveTo[0]; }
            if (lastMoveTo[0] > maxX) { maxX = lastMoveTo[0]; }
            if (lastMoveTo[1] < minY) { minY = lastMoveTo[1]; }
            if (lastMoveTo[1] > maxY) { maxY = lastMoveTo[1]; }
          }
          if (d[0] < minX) { minX = d[0]; }
          if (d[0] > maxX) { maxX = d[0]; }
          if (d[1] < minY) { minY = d[1]; }
          if (d[1] > maxY) { maxY = d[1]; }
          break;
        }
        case PathIterator.SEG_MOVETO:
        {
          lastMoveTo = d;
          break;
        }
      }
      pi.next();
    }
    if (Double.isInfinite(minX)) {
      // edge case: path contains no LINETO segments, only MOVETO
      if (lastMoveTo != null) {
        // path only consists of MOVETO parts
        Logger.getLogger(Helper.class.getName()).log(Level.WARNING, "cannot compute useful bounding box -- path contains only MOVETO elements");
        minX = lastMoveTo[0];
        maxX = lastMoveTo[0];
        minY = lastMoveTo[1];
        maxY = lastMoveTo[1];
      } else {
        // the path contains no points
        // There is no sensible return value as it is expected to be != null.
        // Just output 0,0.
        Logger.getLogger(Helper.class.getName()).log(Level.WARNING, "cannot compute bounding box -- path contains no points");
        minX = 0;
        maxX = 0;
        minY = 0;
        maxY = 0;
      }
    }
    return new Rectangle.Double(minX, minY, maxX-minX, maxY - minY);
  }

  /**
   * Returns a rectangle (parralel to x and y axis), which contains
   * the given rectangle after the given transform. If the transform
   * contains a rotation, the resulting rectangle is the smallest bounding-box
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

  /**
   * Test if given path is empty (i.e., can be ignored for drawing or lasercutting)
   * @return True if the path contains zero points or only MOVETO points, False otherwise.
   */
  public static boolean shapeIsEmpty(Shape path) {
    double[] d = new double[8];
    var pi = path.getPathIterator(null);
    while (!pi.isDone()) {
      if (pi.currentSegment(d) != PathIterator.SEG_MOVETO) {
        return false;
      }
      pi.next();
    }
    return true;
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
   */
  public static String toPathName(String name)
  {
    StringBuilder result = new StringBuilder();
    for (char c : name.toCharArray())
    {
      if (!allowedChars.contains(""+c))
      {
        result.append("_").append((int) c).append("_");
      }
      else
      {
        result.append(c);
      }
    }
    return result.toString();
  }
  
  /**
   * Converts a converted filename from toPathName()
   * back to the original string
   */
  public static String fromPathName(String name)
  {
    StringBuilder result = new StringBuilder();
    StringBuilder index = null;
    for (char c : name.toCharArray())
    {
      if (index == null)
      {
        if (c == '_')
        {
          index = new StringBuilder();
        }
        else
        {
          result.append(c);
        }
      }
      else
      {
        if (c == '_')
        {
          result.append((char) Integer.parseInt(index.toString()));
          index = null;
        }
        else
        {
          index.append(c);
        }
      }
    }
    return result.toString();
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

  /**
   * Attempt to get the wifi ssid, returning empty string if not found.
   * This attempts to call system specific shell commands to find wifi info.
   */
  public static String getWifiSSID() {
    if (isLinux()) {
      try {
        // This command works for ubuntu linux
        Process p = Runtime.getRuntime().exec("iwgetid -r");
        p.waitFor();
        if (p.exitValue() == 0) {
          return new BufferedReader(new InputStreamReader(p.getInputStream())).readLine();
        }
      } catch (Exception e) {
        // Ignore all the expections, it's just a failure to get the SSID.
      }
    }
    if (isWindows()) {
      try {
        Process p = Runtime.getRuntime().exec("NETSH WLAN SHOW INTERFACE | findstr /r \"^....SSID\"");
        p.waitFor();
        if (p.exitValue() == 0) {
          return new BufferedReader(new InputStreamReader(p.getInputStream())).readLine();
        }
      } catch (Exception e) {
        // Ignore all the expections, it's just a failure to get the SSID.
      }
    }
    if (isMacOS()) {
      try {
        Process p = Runtime.getRuntime().exec("/System/Library/PrivateFrameworks/Apple80211.framework/Versions/Current/Resources/airport -I | awk '/ SSID/ {print substr($0, index($0, $2))}'");
        p.waitFor();
        if (p.exitValue() == 0) {
          return new BufferedReader(new InputStreamReader(p.getInputStream())).readLine();
        }
      } catch (Exception e) {
        // Ignore all the expections, it's just a failure to get the SSID.
      }
    }
    return "";
  }

}
