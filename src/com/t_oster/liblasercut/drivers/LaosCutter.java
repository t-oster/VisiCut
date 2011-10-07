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
package com.t_oster.liblasercut.drivers;

import com.t_oster.liblasercut.IllegalJobException;
import com.t_oster.liblasercut.LaserCutter;
import com.t_oster.liblasercut.LaserJob;
import com.t_oster.liblasercut.VectorCommand;
import com.t_oster.liblasercut.VectorPart;
import com.t_oster.liblasercut.platform.Util;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

/**
 * This class implements a driver for the LAOS Lasercutter board.
 * Currently it supports the simple code and the G-Code, which may be used in
 * the future.
 * 
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class LaosCutter extends LaserCutter
{

  protected boolean simpleMode = true;

  /**
   * Get the value of simpleMode
   *
   * @return the value of simpleMode
   */
  public boolean isSimpleMode()
  {
    return simpleMode;
  }

  /**
   * Set the value of simpleMode
   *
   * @param simpleMode new value of simpleMode
   */
  public void setSimpleMode(boolean simpleMode)
  {
    this.simpleMode = simpleMode;
  }
  protected String hostname = "localhost";

  /**
   * Get the value of hostname
   *
   * @return the value of hostname
   */
  public String getHostname()
  {
    return hostname;
  }

  /**
   * Set the value of hostname
   *
   * @param hostname new value of hostname
   */
  public void setHostname(String hostname)
  {
    this.hostname = hostname;
  }
  protected int port = 515;

  /**
   * Get the value of port
   *
   * @return the value of port
   */
  public int getPort()
  {
    return port;
  }

  /**
   * Set the value of port
   *
   * @param port new value of port
   */
  public void setPort(int port)
  {
    this.port = port;
  }

  private byte[] generateVectorGCode(VectorPart vp, int resolution) throws UnsupportedEncodingException
  {
    ByteArrayOutputStream result = new ByteArrayOutputStream();
    PrintStream out = new PrintStream(result, true, "US-ASCII");
    if (!this.isSimpleMode())
    {
      out.print("G28\n");//move to origin
      out.print("G21\n");//units to mm
      out.print("M106\n");//ventilaton on
      out.print("M151 100\n");//air pressure on
    }
    
    int power = 100;
    int speed = 50;
    int frequency = 500;
    double focus = 0;
    for (VectorCommand cmd : vp.getCommandList())
    {
      switch (cmd.getType())
      {
        case MOVETO:
          if (this.isSimpleMode())
          {
            out.printf("0 %d %d\n", cmd.getX(), cmd.getY());
          }
          else
          {
            out.printf(Locale.US, "G0 X%f Y%f\n", Util.px2mm(cmd.getX(),resolution), Util.px2mm(cmd.getY(), resolution));
          }
          break;
        case LINETO:
          if (this.isSimpleMode())
          {
            out.printf("1 %d %d\n", cmd.getX(), cmd.getY());
          }
          else
          {//Frequency???
            out.printf(Locale.US, "G1 X%f Y%f E%d F%d\n", Util.px2mm(cmd.getX(),resolution), Util.px2mm(cmd.getY(),resolution), power, speed);
          }
          break;
        case SETPOWER:
          if (this.isSimpleMode())
          {
            out.printf("7 101 %d\n", cmd.getPower()*100);
          }
          else
          {
            power = cmd.getPower();
          }
          break;
        case SETFOCUS:
          if (this.isSimpleMode())
          {
            out.printf(Locale.US, "2 %d\n", Util.mm2px(cmd.getFocus(),resolution));
          }
          else
          {
            focus = cmd.getFocus();
          }
          break;
        case SETSPEED:
          if (this.isSimpleMode())
          {
            out.printf("7 100 %d\n", cmd.getSpeed()*100);
          }
          else
          {
            speed = cmd.getSpeed();
          }
          break;
        case SETFREQUENCY:
          if (this.isSimpleMode())
          {
            out.printf("7 102 %d\n", cmd.getFrequency());
          }
          else
          {
            frequency = cmd.getFrequency();
          }
          break;
      }
    }
    if (!this.isSimpleMode())
    {
      //back to origin and shutdown
      out.print("G28\n");
      out.print("M107\n");
      out.print("M151 0\n");
      out.print("M0\n");
    }
    return result.toByteArray();
  }

  @Override
  public void sendJob(LaserJob job) throws IllegalJobException, Exception
  {
    if (job.containsRaster() || job.contains3dRaster())
    {
      throw new IllegalJobException("The LAOS driver currently only supports Vector mode.");
    }
    Socket connection = new Socket();
    connection.connect(new InetSocketAddress(hostname, port), 3000);
    BufferedOutputStream out = new BufferedOutputStream(connection.getOutputStream());
    if (job.containsVector())
    {
      out.write(this.generateVectorGCode(job.getVectorPart(), job.getResolution()));
    }
    out.close();
  }
  private List<Integer> resolutions;

  @Override
  public List<Integer> getResolutions()
  {
    if (resolutions == null)
    {
      resolutions = Arrays.asList(new Integer[]
        {
          500
        });
    }
    return resolutions;
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
  private List<String> settingAttributes;

  @Override
  public List<String> getSettingAttributes()
  {
    if (settingAttributes == null)
    {
      settingAttributes = new LinkedList<String>();
      settingAttributes.add("Hostname");
      settingAttributes.add("Port");
      settingAttributes.add("GCode");
      settingAttributes.add("BedWidth");
      settingAttributes.add("BedHeight");
    }
    return settingAttributes;
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
    else if ("GCode".equals(attribute))
    {
      return this.isSimpleMode() ? "no" : "yes";
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
    else if ("GCode".equals(attribute))
    {
      this.setSimpleMode(!"yes".equals(value));
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

  @Override
  public int estimateJobDuration(LaserJob job)
  {
    return 10000;
  }

  @Override
  public LaserCutter clone()
  {
    LaosCutter clone = new LaosCutter();
    clone.hostname = hostname;
    clone.port = port;
    clone.simpleMode = simpleMode;
    clone.bedHeight = bedHeight;
    clone.bedWidth = bedWidth;
    return clone;
  }
}
