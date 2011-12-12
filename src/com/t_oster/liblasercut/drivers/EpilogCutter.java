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
/**
 * Known Limitations:
 * - If there is Raster and Raster3d Part in one job, the speed from 3d raster
 * is taken for both and eventually other side effects:
 * IT IS NOT RECOMMENDED TO USE 3D-Raster and Raster in the same Job
 */
package com.t_oster.liblasercut.drivers;

import com.t_oster.liblasercut.*;
import com.t_oster.liblasercut.platform.Util;
import com.t_oster.liblasercut.platform.Point;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.LinkedList;

/**
 *
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class EpilogCutter extends LaserCutter
{

  public static boolean SIMULATE_COMMUNICATION = false;
  public static final int NETWORK_TIMEOUT = 3000;
  /* Resolutions in DPI */
  private static final int[] RESOLUTIONS = new int[]
  {
    300, 500, 600, 1000
  };
  private static final int MINFOCUS = -500;//Minimal focus value (not mm)
  private static final int MAXFOCUS = 500;//Maximal focus value (not mm)
  private static final double FOCUSWIDTH = 0.0252;//How much mm/unit the focus values are
  private String hostname;
  private int port = 515;
  private Socket connection;
  private InputStream in;
  private OutputStream out;

  private int mm2focus(float mm)
  {
    return (int) (mm / FOCUSWIDTH);
  }

  private float focus2mm(int focus)
  {
    return (float) (focus * FOCUSWIDTH);
  }

  public EpilogCutter()
  {
    this("localhost");
  }

  public EpilogCutter(String hostname)
  {
    this.hostname = hostname;
  }

  public String getHostname()
  {
    return this.hostname;
  }

  public void setHostname(String hostname)
  {
    this.hostname = hostname;
  }

  private void waitForResponse(int expected) throws IOException, Exception
  {
    waitForResponse(expected, 3);
  }

  private void waitForResponse(int expected, int timeout) throws IOException, Exception
  {
    if (SIMULATE_COMMUNICATION)
    {
      return;
    }
    int result = -1;
    out.flush();
    for (int i = 0; i < timeout * 10; i++)
    {
      if (in.available() > 0)
      {
        result = in.read();
        if (result == -1)
        {
          throw new IOException("End of Stream");
        }
        if (result != expected)
        {
          throw new Exception("unexpected Response: " + result);
        }
        return;
      }
      else
      {
        Thread.sleep(100 * timeout);
      }
    }
    throw new Exception("Timeout");

  }

  private byte[] generatePjlHeader(LaserJob job) throws UnsupportedEncodingException
  {
    ByteArrayOutputStream result = new ByteArrayOutputStream();
    PrintStream out = new PrintStream(result, true, "US-ASCII");
    /* Print the printer job language header. */
    out.printf("\033%%-12345X@PJL JOB NAME=%s\r\n", job.getTitle());
    out.printf("\033E@PJL ENTER LANGUAGE=PCL\r\n");
    /* Set autofocus off. */
    out.printf("\033&y0A");
    /* Set focus to 0. */
    out.printf("\033&y0C");
    /* UNKNOWN */
    out.printf("\033&y0Z");
    /* Left (long-edge) offset registration.  Adjusts the position of the
     * logical page across the width of the page.
     */
    out.printf("\033&l0U");
    /* Top (short-edge) offset registration.  Adjusts the position of the
     * logical page across the length of the page.
     */
    out.printf("\033&l0Z");
    /* Resolution of the print. Number of Units/Inch*/
    out.printf("\033&u%dD", job.getResolution());
    /* X position = 0 */
    out.printf("\033*p0X");
    /* Y position = 0 */
    out.printf("\033*p0Y");
    /* PCL/RasterGraphics resolution. */
    out.printf("\033*t%dR", job.getResolution());
    return result.toByteArray();
  }

  private byte[] generatePjlFooter() throws UnsupportedEncodingException
  {
    ByteArrayOutputStream result = new ByteArrayOutputStream();
    PrintStream out = new PrintStream(result, true, "US-ASCII");

    /* Footer for printer job language. */
    /* Reset */
    out.printf("\033E");
    /* Exit language. */
    out.printf("\033%%-12345X");
    /* End job. */
    out.printf("@PJL EOJ \r\n");
    return result.toByteArray();
  }

  private void sendPjlJob(LaserJob job, byte[] pjlData) throws UnknownHostException, UnsupportedEncodingException, IOException, Exception
  {
    String localhost = null;
    try
    {
      localhost = java.net.InetAddress.getLocalHost().getHostName();
    }
    catch (UnknownHostException e)
    {
      localhost = "unknown";
    }
    PrintStream out = new PrintStream(this.out, true, "US-ASCII");
    out.print("\002\n");
    waitForResponse(0);
    ByteArrayOutputStream tmp = new ByteArrayOutputStream();
    PrintStream stmp = new PrintStream(tmp, true, "US-ASCII");
    stmp.printf("H%s\n", localhost);
    stmp.printf("P%s\n", job.getUser());
    stmp.printf("J%s\n", job.getTitle());
    stmp.printf("ldfA%s%s\n", job.getName(), localhost);
    stmp.printf("UdfA%s%s\n", job.getName(), localhost);
    stmp.printf("N%s\n", job.getTitle());
    out.printf("\002%d cfA%s%s\n", tmp.toByteArray().length, job.getName(), localhost);
    waitForResponse(0);
    out.write(tmp.toByteArray());
    out.append((char) 0);
    waitForResponse(0);
    /* Send the Job length and name to the queue */
    out.printf("\003%d dfA%s%s\n", pjlData.length, job.getName(), localhost);
    waitForResponse(0);
    /* Send the real PJL Job */
    out.write(pjlData);
    waitForResponse(0);
  }

  private void connect() throws IOException, SocketTimeoutException
  {
    if (SIMULATE_COMMUNICATION)
    {
      out = System.out;
    }
    else
    {
      connection = new Socket();
      connection.connect(new InetSocketAddress(hostname, port), NETWORK_TIMEOUT);
      in = new BufferedInputStream(connection.getInputStream());
      out = new BufferedOutputStream(connection.getOutputStream());
    }
  }

  private void disconnect() throws IOException
  {
    if (!SIMULATE_COMMUNICATION)
    {
      in.close();
      out.close();
    }
  }

  private void checkJob(LaserJob job) throws IllegalJobException
  {
    boolean pass = false;
    for (int i : this.getResolutions())
    {
      if (i == job.getResolution())
      {
        pass = true;
        break;
      }
    }
    if (!pass)
    {
      throw new IllegalJobException("Resoluiton of " + job.getResolution() + " is not supported");
    }
    if (job.containsVector())
    {
      double w = Util.px2mm(job.getVectorPart().getWidth(), job.getResolution());
      double h = Util.px2mm(job.getVectorPart().getHeight(), job.getResolution());

      if (w > this.getBedWidth() || h > this.getBedHeight())
      {
        throw new IllegalJobException("The Job is too big (" + w + "x" + h + ") for the Laser bed (" + this.getBedHeight() + "x" + this.getBedHeight() + ")");
      }

      for (VectorCommand cmd : job.getVectorPart().getCommandList())
      {
        if (cmd.getType() == VectorCommand.CmdType.SETFOCUS)
        {
          if (mm2focus(cmd.getFocus()) > MAXFOCUS || (mm2focus(cmd.getFocus())) < MINFOCUS)
          {
            throw new IllegalJobException("Illegal Focus value. This Lasercutter supports values between"
              + focus2mm(MINFOCUS) + "mm to " + focus2mm(MAXFOCUS) + "mm.");
          }
        }
      }
    }
    if (job.containsRaster())
    {
      double w = Util.px2mm(job.getRasterPart().getWidth(), job.getResolution());
      double h = Util.px2mm(job.getRasterPart().getHeight(), job.getResolution());

      if (w > this.getBedWidth() || h > this.getBedHeight())
      {
        throw new IllegalJobException("The Job is too big (" + w + "mm x" + h + "mm) for the Laser bed (" + this.getBedWidth() + "mm x" + this.getBedHeight() + "mm)");
      }
      for (int i = 0; i < job.getRasterPart().getRasterCount(); i++)
      {
        float focus = job.getRasterPart().getLaserProperty(i) == null ? 0 : job.getRasterPart().getLaserProperty(i).getFocus();
        if (mm2focus(focus) > MAXFOCUS || (mm2focus(focus)) < MINFOCUS)
        {
          throw new IllegalJobException("Illegal Focus value. This Lasercutter supports values between"
            + focus2mm(MINFOCUS) + "mm to " + focus2mm(MAXFOCUS) + "mm.");
        }
      }
    }
    if (job.contains3dRaster())
    {
      double w = Util.px2mm(job.getRaster3dPart().getWidth(), job.getResolution());
      double h = Util.px2mm(job.getRaster3dPart().getHeight(), job.getResolution());

      if (w > this.getBedWidth() || h > this.getBedHeight())
      {
        throw new IllegalJobException("The Job is too big (" + w + "x" + h + ") for the Laser bed (" + this.getBedHeight() + "x" + this.getBedHeight() + ")");
      }
      for (int i = 0; i < job.getRaster3dPart().getRasterCount(); i++)
      {
        float focus = job.getRaster3dPart().getLaserProperty(i) == null ? 0 : job.getRaster3dPart().getLaserProperty(i).getFocus();
        if (mm2focus(focus) > MAXFOCUS || (mm2focus(focus)) < MINFOCUS)
        {
          throw new IllegalJobException("Illegal Focus value. This Lasercutter supports values between"
            + focus2mm(MINFOCUS) + "mm to " + focus2mm(MAXFOCUS) + "mm.");
        }
      }
    }
  }

  public void sendJob(LaserJob job) throws IllegalJobException, SocketTimeoutException, UnsupportedEncodingException, IOException, UnknownHostException, Exception
  {
    //Perform santiy checks
    checkJob(job);
    if (job.contains3dRaster() && job.containsRaster())
    {//Raster and 3d Raster may not be in the same job. Send 2
      this.realSendJob(new LaserJob("(1/2)" + job.getTitle(), job.getName(), job.getUser(), job.getResolution(), job.getRaster3dPart(), null, null));
      this.realSendJob(new LaserJob("(2/2)" + job.getTitle(), job.getName(), job.getUser(), job.getResolution(), null, job.getVectorPart(), job.getRasterPart()));
    }
    else
    {
      this.realSendJob(job);
    }
  }

  private void realSendJob(LaserJob job) throws UnsupportedEncodingException, IOException, UnknownHostException, Exception
  {
    //Generate all the data
    byte[] pjlData = generatePjlData(job);
    //connect to lasercutter
    connect();
    //send job
    sendPjlJob(job, pjlData);
    //disconnect
    disconnect();
  }

  public List<Integer> getResolutions()
  {
    List<Integer> result = new LinkedList();
    for (int r : RESOLUTIONS)
    {
      result.add(r);
    }
    return result;
  }

  /**
   * Encodes the given line of the given image in TIFF Packbyte encoding
   */
  public List<Byte> encode(List<Byte> line)
  {
    int idx = 0;
    int r = line.size();
    List<Byte> result = new LinkedList<Byte>();
    while (idx < r)
    {
      int p;
      p = idx + 1;
      while (p < r && p < idx + 128 && line.get(p) == line.get(idx))
      {
        p++;
      }
      if (p - idx >= 2)
      {
        // run length
        result.add((byte) (1 - (p - idx)));
        result.add((byte) line.get(idx));
        idx = p;
      }
      else
      {
        p = idx;
        while (p < r && p < idx + 127
          && (p + 1 == r || line.get(p)
          != line.get(p + 1)))
        {
          p++;
        }
        result.add((byte) (p - idx - 1));
        while (idx < p)
        {
          result.add((byte) (line.get(idx++)));
        }
      }
    }
    return result;
  }

  private byte[] generateRaster3dPCL(LaserJob job, Raster3dPart rp) throws UnsupportedEncodingException, IOException
  {
    ByteArrayOutputStream result = new ByteArrayOutputStream();
    PrintStream out = new PrintStream(result, true, "US-ASCII");
    LaserProperty curprop = new LaserProperty();
    if (rp != null && rp.getRasterCount() > 0)
    {
      if (rp.getRasterCount() > 0)
      {
        curprop = rp.getLaserProperty(0);
      }
      /* Raster Orientation: Printed in current direction */
      out.printf("\033*r0F");
      /* Raster power */
      out.printf("\033&y%dP", curprop.getPower());
      /* Raster speed */
      out.printf("\033&z%dS", curprop.getSpeed());
      /* Focus */
      out.printf("\033&y%dA", mm2focus(curprop.getFocus()));

      out.printf("\033*r%dT", rp != null ? rp.getHeight() : 10);//height);
      out.printf("\033*r%dS", rp != null ? rp.getWidth() : 10);//width);
            /* Raster compression:
       *  2 = TIFF encoding
       *  7 = TIFF encoding, 3d-mode,
       *
       * Wahrscheinlich:
       * 2M = Bitweise, also 1=dot 0=nodot (standard raster)
       * 7MLT = Byteweise 0= no power 100=full power (3d raster)
       */
      out.printf("\033*b%dMLT", 7);
      /* Raster direction (1 = up, 0=down) */
      out.printf("\033&y%dO", 0);
      /* start at current position */
      out.printf("\033*r1A");

      for (int i = 0; rp != null && i < rp.getRasterCount(); i++)
      {
        LaserProperty newprop = rp.getLaserProperty(i);
        if (newprop.getPower() != curprop.getPower())
        {
          /* Raster power */
          out.printf("\033&y%dP", newprop.getPower());
        }
        if (newprop.getSpeed() != curprop.getSpeed())
        {
          /* Raster speed */
          out.printf("\033&z%dS", newprop.getSpeed());
        }
        if (newprop.getFocus() != curprop.getFocus())
        {
          /* Focus  */
          out.printf("\033&y%dA", mm2focus(newprop.getFocus()));
        }
        curprop = newprop;
        Point sp = rp.getRasterStart(i);
        boolean leftToRight = true;
        for (int y = 0; y < rp.getRasterHeight(i); y++)
        {

          List<Byte> line = rp.getInvertedRasterLine(i, y);
          for (int n = 0; n < line.size(); n++)
          {//Apperantly the other power settings are ignored, so we have to scale
            int x = line.get(n);
            x = x >= 0 ? x : 256 + x;
            int scalex = x * curprop.getPower() / 100;
            byte bx = (byte) (scalex < 128 ? scalex : scalex - 256);
            line.set(n, bx);
          }
          //Remove leading zeroes, but keep track of the offset
          int jump = 0;

          while (line.size() > 0 && line.get(0) == 0)
          {
            line.remove(0);
            jump++;
          }
          if (line.size() > 0)
          {
            out.printf("\033*p%dX", sp.x + jump);
            out.printf("\033*p%dY", sp.y + y);
            if (leftToRight)
            {
              out.printf("\033*b%dA", line.size());
            }
            else
            {
              out.printf("\033*b%dA", -line.size());
              Collections.reverse(line);
            }
            line = encode(line);
            int len = line.size();
            int pcks = len / 8;
            if (len % 8 > 0)
            {
              pcks++;
            }
            out.printf("\033*b%dW", pcks * 8);
            for (byte s : line)
            {
              out.write(s);
            }
            for (int k = 0; k < 8 - (len % 8); k++)
            {
              out.write((byte) 128);
            }
            leftToRight = !leftToRight;
          }
        }

      }
      out.printf("\033*rC");       // end raster
    }
    return result.toByteArray();
  }

  private byte[] generateRasterPCL(LaserJob job, RasterPart rp) throws UnsupportedEncodingException, IOException
  {

    LaserProperty curprop = null;
    if (rp != null && rp.getRasterCount() > 0)
    {
      curprop = rp.getLaserProperty(0);
    }
    if (curprop == null)
    {
      curprop = new LaserProperty();
    }
    ByteArrayOutputStream result = new ByteArrayOutputStream();
    PrintStream out = new PrintStream(result, true, "US-ASCII");
    /* Raster Orientation: Printed in current direction */
    out.printf("\033*r0F");
    /* Raster power */
    out.printf("\033&y%dP", curprop.getPower());
    /* Raster speed */
    out.printf("\033&z%dS", curprop.getSpeed());
    /* Focus */
    out.printf("\033&y%dA", mm2focus(curprop.getFocus()));

    out.printf("\033*r%dT", rp != null ? rp.getHeight() : 10);//height);
    out.printf("\033*r%dS", rp != null ? rp.getWidth() : 10);//width);
        /* Raster compression:
     *  2 = TIFF encoding
     *  7 = TIFF encoding, 3d-mode,
     *
     * Wahrscheinlich:
     * 2M = Bitweise, also 1=dot 0=nodot (standard raster)
     * 7MLT = Byteweise 0= no power 100=full power (3d raster)
     */
    out.printf("\033*b2M");
    /* Raster direction (1 = up, 0=down) */
    out.printf("\033&y%dO", 0);
    /* start at current position */
    out.printf("\033*r1A");

    for (int i = 0; rp != null && i < rp.getRasterCount(); i++)
    {
      //TODO: Test if new Settings are applied
      LaserProperty newprop = rp.getLaserProperty(i);
      if (newprop.getPower() != curprop.getPower())
      {
        /* Raster power */
        out.printf("\033&y%dP", newprop.getPower());
      }
      if (newprop.getSpeed() != curprop.getSpeed())
      {
        /* Raster speed */
        out.printf("\033&z%dS", newprop.getSpeed());
      }
      if (newprop.getFocus() != curprop.getFocus())
      {
        /* Focus  */
        out.printf("\033&y%dA", mm2focus(newprop.getFocus()));
      }
      curprop = newprop;
      Point sp = rp.getRasterStart(i);
      boolean leftToRight = true;
      for (int y = 0; y < rp.getRasterHeight(i); y++)
      {

        List<Byte> line = rp.getRasterLine(i, y);
        //Remove leading zeroes, but keep track of the offset
        int jump = 0;

        while (line.size() > 0 && line.get(0) == 0)
        {
          line.remove(0);
          jump++;
        }
        //Remove trailing zeroes
        while (line.size() > 0 && line.get(line.size()-1) == 0)
        {
          line.remove(line.size()-1);
        }
        if (line.size() > 0)
        {
          out.printf("\033*p%dX", sp.x + jump * 8);
          out.printf("\033*p%dY", sp.y + y);
          if (leftToRight)
          {
            out.printf("\033*b%dA", line.size());
          }
          else
          {
            out.printf("\033*b%dA", -line.size());
            Collections.reverse(line);
          }
          line = encode(line);
          int len = line.size();
          int pcks = len / 8;
          if (len % 8 > 0)
          {
            pcks++;
          }
          /**
           * Number of Pixels in a row??
           * or b2m%dW for TIFF encoding?
           * Or number of Bytes in a row? who knows
           * in ctrl-cut its number of packed bytes
           */
          out.printf("\033*b%dW", pcks * 8);
          for (byte s : line)
          {
            out.write(s);
          }
          for (int k = 0; k < 8 - (len % 8); k++)
          {
            out.write((byte) 128);
          }
          leftToRight = !leftToRight;
        }
      }

    }
    out.printf("\033*rC");       // end raster
    return result.toByteArray();
  }

  private byte[] generateVectorPCL(LaserJob job, VectorPart vp) throws UnsupportedEncodingException
  {

    ByteArrayOutputStream result = new ByteArrayOutputStream();
    PrintStream out = new PrintStream(result, true, "US-ASCII");

    out.printf("\033*r0F");
    out.printf("\033*r%dT", vp == null ? 500 : vp.getHeight());// if not dummy, then job.getHeight());
    out.printf("\033*r%dS", vp == null ? 500 : vp.getWidth());// if not dummy then job.getWidth());
    out.printf("\033*r1A");
    out.printf("\033*rC");
    out.printf("\033%%1B");// Start HLGL
    out.printf("IN;PU0,0;");

    if (vp != null)
    {
      int sx = job.getStartX();
      int sy = job.getStartY();
      VectorCommand.CmdType lastType = null;
      for (VectorCommand cmd : vp.getCommandList())
      {
        if (lastType != null && lastType == VectorCommand.CmdType.LINETO && cmd.getType() != VectorCommand.CmdType.LINETO)
        {
          out.print(";");
        }
        switch (cmd.getType())
        {
          case SETFOCUS:
          {
            out.printf("WF%d;", mm2focus(cmd.getFocus()));
            break;
          }
          case SETFREQUENCY:
          {
            out.printf("XR%04d;", cmd.getFrequency());
            break;
          }
          case SETPOWER:
          {
            out.printf("YP%03d;", cmd.getPower());
            break;
          }
          case SETSPEED:
          {
            out.printf("ZS%03d;", cmd.getSpeed());
            break;
          }
          case MOVETO:
          {
            out.printf("PU%d,%d;", cmd.getX() - sx, cmd.getY() - sy);
            break;
          }
          case LINETO:
          {
            if (lastType == null || lastType != VectorCommand.CmdType.LINETO)
            {
              out.printf("PD%d,%d", cmd.getX() - sx, cmd.getY() - sy);
            }
            else
            {
              out.printf(",%d,%d", cmd.getX() - sx, cmd.getY() - sy);
            }
            break;
          }
        }
        lastType = cmd.getType();
      }
    }
    //Reset Focus to 0
    //out.printf("WF%d;", 0);
    return result.toByteArray();
  }

  private byte[] generatePjlData(LaserJob job) throws UnsupportedEncodingException, IOException
  {
    /* Generate complete PJL Job */
    ByteArrayOutputStream pjlJob = new ByteArrayOutputStream();
    PrintStream wrt = new PrintStream(pjlJob, true, "US-ASCII");

    wrt.write(generatePjlHeader(job));
    wrt.write(generateRasterPCL(job, job.getRasterPart()));
    wrt.write(generateRaster3dPCL(job, job.getRaster3dPart()));
    wrt.write(generateVectorPCL(job, job.getVectorPart()));
    wrt.write(generatePjlFooter());
    /* Pad out the remainder of the file with 0 characters. */
    for (int i = 0; i < 4096; i++)
    {
      wrt.append((char) 0);
    }
    wrt.flush();
    return pjlJob.toByteArray();
  }

  public int getPort()
  {
    return this.port;
  }

  public void setPort(int Port)
  {
    this.port = Port;
  }

  @Override
  public EpilogCutter clone()
  {
    EpilogCutter result = new EpilogCutter();
    result.hostname = hostname;
    result.port = port;
    result.bedHeight = bedHeight;
    result.bedWidth = bedWidth;
    return result;
  }

  @Override
  public String getSettingValue(String attribute)
  {
    if ("Hostname".equals(attribute))
    {
      return this.getHostname();
    }
    else if ("Port".equals(attribute))
    {
      return "" + this.getPort();
    }
    else if ("BedWidth".equals(attribute))
    {
      return "" + this.getBedWidth();
    }
    else if ("BedHeight".equals(attribute))
    {
      return "" + this.getBedHeight();
    }
    return null;
  }
  protected double bedWidth = 600;

  /**
   * Get the value of bedWidth
   *
   * @return the value of bedWidth
   */
  public double getBedWidth()
  {
    return bedWidth;
  }

  /**
   * Set the value of bedWidth
   *
   * @param bedWidth new value of bedWidth
   */
  public void setBedWidth(double bedWidth)
  {
    this.bedWidth = bedWidth;
  }
  protected double bedHeight = 300;

  /**
   * Get the value of bedHeight
   *
   * @return the value of bedHeight
   */
  public double getBedHeight()
  {
    return bedHeight;
  }

  /**
   * Set the value of bedHeight
   *
   * @param bedHeight new value of bedHeight
   */
  public void setBedHeight(double bedHeight)
  {
    this.bedHeight = bedHeight;
  }

  @Override
  public void setSettingValue(String attribute, String value)
  {
    if ("Hostname".equals(attribute))
    {
      this.setHostname(value);
    }
    else if ("Port".equals(attribute))
    {
      this.setPort(Integer.parseInt(value));
    }
    else if ("BedWidth".equals(attribute))
    {
      this.setBedWidth(Double.parseDouble(value));
    }
    else if ("BedHeight".equals(attribute))
    {
      this.setBedHeight(Double.parseDouble(value));
    }
  }
  private String[] attributes = new String[]
  {
    "Hostname", "Port", "BedWidth", "BedHeight"
  };

  @Override
  public List<String> getSettingAttributes()
  {
    return Arrays.asList(attributes);
  }

  @Override
  public int estimateJobDuration(LaserJob job)
  {
    double VECTOR_MOVESPEED_X = 20000d / 4.5;
    double VECTOR_MOVESPEED_Y = 10000d / 2.5;
    double VECTOR_LINESPEED = 20000d / 36.8;
    double RASTER_LINEOFFSET = 0.08d;
    double RASTER_LINESPEED = 100000d / ((268d / 50) - RASTER_LINEOFFSET);
    //TODO: The Raster3d values are not tested yet, theyre just copies
    double RASTER3D_LINEOFFSET = 0.08;
    double RASTER3D_LINESPEED = 100000d / ((268d / 50) - RASTER3D_LINEOFFSET);

    //Holds the current Laser Head position in Pixels
    Point p = new Point(0, 0);

    double result = 0;//usual offset
    if (job.containsRaster())
    {
      RasterPart rp = job.getRasterPart();
      for (int i = 0; i < rp.getRasterCount(); i++)
      {//Time to move to Start Position
        Point sp = rp.getRasterStart(i);
        result += Math.max((double) (p.x - sp.x) / VECTOR_MOVESPEED_X,
          (double) (p.y - sp.y) / VECTOR_MOVESPEED_Y);
        double linespeed = ((double) RASTER_LINESPEED * rp.getLaserProperty(i).getSpeed()) / 100;
        BlackWhiteRaster bwr = rp.getImages()[i];
        for (int y = 0; y < bwr.getHeight(); y++)
        {//Find any black point
          boolean lineEmpty = true;
          for (int x = 0; x < bwr.getWidth(); x++)
          {
            if (bwr.isBlack(x, y))
            {
              lineEmpty = false;
              break;
            }
          }
          if (!lineEmpty)
          {
            int w = bwr.getWidth();
            result += (double) RASTER_LINEOFFSET + (double) w / linespeed;
            p.x = sp.y % 2 == 0 ? sp.x + w : sp.x;
            p.y = sp.y + y;
          }
          else
          {
            result += RASTER_LINEOFFSET;
          }
        }
      }
    }
    if (job.contains3dRaster())
    {
      Raster3dPart rp = job.getRaster3dPart();
      for (int i = 0; i < rp.getRasterCount(); i++)
      {//Time to move to Start Position
        Point sp = rp.getRasterStart(i);
        result += Math.max((double) (p.x - sp.x) / VECTOR_MOVESPEED_X,
          (double) (p.y - sp.y) / VECTOR_MOVESPEED_Y);
        double linespeed = ((double) RASTER3D_LINESPEED * rp.getLaserProperty(i).getSpeed()) / 100;
        GreyscaleRaster gsr = rp.getImages()[i];
        for (int y = 0; y < gsr.getHeight(); y++)
        {//Check if
          boolean lineEmpty = true;
          for (int x = 0; x < gsr.getWidth(); x++)
          {
            if (gsr.getGreyScale(x, y) != 0)
            {
              lineEmpty = false;
              break;
            }
          }
          if (!lineEmpty)
          {
            int w = gsr.getWidth();
            result += (double) RASTER3D_LINEOFFSET + (double) w / linespeed;
            p.x = sp.y % 2 == 0 ? sp.x + w : sp.x;
            p.y = sp.y + y;
          }
        }
      }
    }
    if (job.containsVector())
    {
      double speed = VECTOR_LINESPEED;
      VectorPart vp = job.getVectorPart();
      for (VectorCommand cmd : vp.getCommandList())
      {
        switch (cmd.getType())
        {
          case SETSPEED:
          {
            speed = VECTOR_LINESPEED * cmd.getSpeed() / 100;
            break;
          }
          case MOVETO:
            result += Math.max((double) (p.x - cmd.getX()) / VECTOR_MOVESPEED_X,
              (double) (p.y - cmd.getY()) / VECTOR_MOVESPEED_Y);
            p = new Point(cmd.getX(), cmd.getY());
            break;
          case LINETO:
            double dist = distance(cmd.getX(), cmd.getY(), p);
            p = new Point(cmd.getX(), cmd.getY());
            result += dist / speed;
            break;
        }
      }
    }
    return (int) result;
  }

  private double distance(int x, int y, Point p)
  {
    return Math.sqrt(Math.pow(p.x - x, 2) + Math.pow(p.y - y, 2));
  }
}
