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
package com.t_oster.liblasercut.drivers;

import com.t_oster.liblasercut.BlackWhiteRaster;
import com.t_oster.liblasercut.IllegalJobException;
import com.t_oster.liblasercut.LaserCutter;
import com.t_oster.liblasercut.LaserJob;
import com.t_oster.liblasercut.LaserProperty;
import com.t_oster.liblasercut.RasterPart;
import com.t_oster.liblasercut.VectorCommand;
import com.t_oster.liblasercut.VectorPart;
import com.t_oster.liblasercut.platform.Point;
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

  private static final String SETTING_HOSTNAME = "Hostname / IP";
  private static final String SETTING_PORT = "Port";
  private static final String SETTING_GCODE = "Use GCode (yes/no)";
  private static final String SETTING_BEDWIDTH = "Laserbed width";
  private static final String SETTING_BEDHEIGHT = "Laserbed height";
  private static final String SETTING_FLIPX = "X axis goes right to left (yes/no)";
  private static final String SETTING_MMPERSTEP = "mm per Step (for SimpleMode)";
  
  protected boolean flipXaxis = false;

  /**
   * Get the value of flipXaxis
   *
   * @return the value of flipXaxis
   */
  public boolean isFlipXaxis()
  {
    return flipXaxis;
  }

  /**
   * Set the value of flipXaxis
   *
   * @param flipXaxis new value of flipXaxis
   */
  public void setFlipXaxis(boolean flipXaxis)
  {
    this.flipXaxis = flipXaxis;
  }
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

  protected double mmPerStep = 0.006323126711476225;

  /**
   * Get the value of mmPerStep
   *
   * @return the value of mmPerStep
   */
  public double getMmPerStep()
  {
    return mmPerStep;
  }

  /**
   * Set the value of mmPerStep
   *
   * @param mmPerStep new value of mmPerStep
   */
  public void setMmPerStep(double mmPerStep)
  {
    this.mmPerStep = mmPerStep;
  }

  
  //TOD Add property "steps per mm" or "stepwidth" and adapt px2steps
  //to be resolution dependent
  private int px2steps(double px, double dpi)
  {
    return (int) (Util.px2mm(px, dpi)/this.mmPerStep);
    //return (int) (8.034 * px);
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
          move(out, cmd.getX(), cmd.getY(), resolution);
          break;
        case LINETO:
          line(out, cmd.getX(), cmd.getY(), power, speed, frequency, resolution);
          break;
        case SETPOWER:
          if (this.isSimpleMode())
          {
            out.printf("7 101 %d\n", cmd.getPower() * 100);
          }
          else
          {
            power = cmd.getPower();
          }
          break;
        case SETFOCUS:
          if (this.isSimpleMode())
          {
            out.printf(Locale.US, "2 %d\n", (int) Util.mm2px(cmd.getFocus(), resolution));
          }
          else
          {
            focus = cmd.getFocus();
          }
          break;
        case SETSPEED:
          if (this.isSimpleMode())
          {
            out.printf("7 100 %d\n", cmd.getSpeed() * 100);
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
    if (this.isSimpleMode())
    {
      out.printf("0 0 0\n");
    }
    else
    {
      out.printf(Locale.US, "G0 X%f Y%f\n", 0, 0);
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

  private void move(PrintStream out, int x, int y, int resolution)
  {
    if (this.isSimpleMode())
    {
      out.printf("0 %d %d\n", px2steps(isFlipXaxis() ? Util.mm2px(bedWidth, resolution) - x : x, resolution), px2steps(y, resolution));
    } 
    else
    {
      out.printf(Locale.US, "G0 X%f Y%f\n", Util.px2mm(isFlipXaxis() ? Util.mm2px(bedWidth, resolution) - x : x, resolution), Util.px2mm(y, resolution));
    }
  }

  private void line(PrintStream out, int x, int y, int power, int speed, int frequency, int resolution)
  {
    if (this.isSimpleMode())
    {
      out.printf("1 %d %d\n", px2steps(isFlipXaxis() ? Util.mm2px(bedWidth, resolution) - x : x, resolution), px2steps(y, resolution));
    }
    else
    {//Frequency???
      out.printf(Locale.US, "G1 X%f Y%f E%d F%d\n", Util.px2mm(isFlipXaxis() ? Util.mm2px(bedWidth, resolution) - x : x, resolution), Util.px2mm(y, resolution), power, speed);
    }
  }

  private byte[] generatePseudoRasterGCode(RasterPart rp, int resolution) throws UnsupportedEncodingException
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
    int speed = 100;
    int frequency = 500;
    double focus = 0;
    for (int i = 0; i < rp.getRasterCount(); i++)
    {
      Point off = rp.getRasterStart(i);
      LaserProperty lp = rp.getLaserProperty(i);
      if (this.isSimpleMode())
      {
        out.printf("7 101 %d\n", lp.getPower() * 100);
        out.printf(Locale.US, "2 %d\n", (int) Util.mm2px(lp.getFocus(), resolution));
        out.printf("7 100 %d\n", lp.getSpeed() * 100);
      }
      else
      {
        power = lp.getPower();
        focus = lp.getFocus();
        speed = lp.getSpeed();
      }
      boolean dirLeft = true;
      BlackWhiteRaster r = rp.getImages()[i];
      for (int y = off.y; y < off.y + r.getHeight(); y++)
      {
        if (dirLeft)
        {
          int startx = off.x;
          while (startx < off.x + r.getWidth() && !r.isBlack(startx - off.x, y - off.y))
          {//skip empty beginning
            startx++;
          }
          //move to start of the line
          move(out, startx, y, resolution);
          boolean on = false;
          for (int x = startx; x < off.x + r.getWidth(); x++)
          {
            if (on && !r.isBlack(x - off.x, y - off.y))
            {
              line(out, x - 1, y, power, speed, frequency, resolution);
              on = false;
            }
            else if (!on && r.isBlack(x - off.x, y - off.y))
            {
              move(out, x - 1, y, resolution);
              on = true;
            }
          }
          if (on)
          {
            line(out, off.x + r.getWidth(), y, power, speed, frequency, resolution);
          }
          else
          {
            move(out, off.x + r.getWidth(), y, resolution);
          }
        }
        else
        {
          int startx = off.x + r.getWidth();
          while (startx >= off.x && !r.isBlack(startx - off.x, y - off.y))
          {//skip empty beginning
            startx--;
          }
          //move to start of the line
          move(out, startx, y, resolution);
          boolean on = false;
          for (int x = startx; x >= off.x; x--)
          {
            if (on && !r.isBlack(x - off.x, y - off.y))
            {
              line(out, x + 1, y, power, speed, frequency, resolution);
              on = false;
            }
            else if (!on && r.isBlack(x - off.x, y - off.y))
            {
              move(out, x + 1, y, resolution);
              on = true;
            }
          }
          if (on)
          {
            line(out, off.x, y, power, speed, frequency, resolution);
          }
          else
          {
            move(out, off.x, y, resolution);
          }
        }
        dirLeft = !dirLeft;
      }
    }
    if (this.isSimpleMode())
    {
      out.printf("0 0 0\n");
    }
    else
    {
      out.printf(Locale.US, "G0 X%f Y%f\n", 0, 0);
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
    if (job.contains3dRaster())
    {
      throw new IllegalJobException("The LAOS driver currently only supports Vector and Raster mode.");
    }
    Socket connection = new Socket();
    connection.connect(new InetSocketAddress(hostname, port), 3000);
    BufferedOutputStream out = new BufferedOutputStream(connection.getOutputStream());
    if (job.containsRaster())
    {
      out.write(this.generatePseudoRasterGCode(job.getRasterPart(), job.getResolution()));
    }
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
      settingAttributes.add(SETTING_HOSTNAME);
      settingAttributes.add(SETTING_PORT);
      settingAttributes.add(SETTING_GCODE);
      settingAttributes.add(SETTING_BEDWIDTH);
      settingAttributes.add(SETTING_BEDHEIGHT);
      settingAttributes.add(SETTING_FLIPX);
      settingAttributes.add(SETTING_MMPERSTEP);
    }
    return settingAttributes;
  }

  @Override
  public String getSettingValue(String attribute)
  {
    if (SETTING_HOSTNAME.equals(attribute))
    {
      return this.getHostname();
    }
    else if (SETTING_FLIPX.equals(attribute))
    {
      return this.isFlipXaxis() ? "yes" : "no";
    }
    else if (SETTING_PORT.equals(attribute))
    {
      return "" + this.getPort();
    }
    else if (SETTING_GCODE.equals(attribute))
    {
      return this.isSimpleMode() ? "no" : "yes";
    }
    else if (SETTING_BEDWIDTH.equals(attribute))
    {
      return "" + this.getBedWidth();
    }
    else if (SETTING_BEDHEIGHT.equals(attribute))
    {
      return "" + this.getBedHeight();
    }
    else if (SETTING_MMPERSTEP.equals(attribute))
    {
      return "" + this.getMmPerStep();
    }
    return null;
  }

  @Override
  public void setSettingValue(String attribute, String value)
  {
    if (SETTING_HOSTNAME.equals(attribute))
    {
      this.setHostname(value);
    }
    else if (SETTING_PORT.equals(attribute))
    {
      this.setPort(Integer.parseInt(value));
    }
    else if (SETTING_GCODE.equals(attribute))
    {
      this.setSimpleMode(!"yes".equals(value));
    }
    else if (SETTING_FLIPX.equals(attribute))
    {
      this.setFlipXaxis("yes".equals(value));
    }
    else if (SETTING_BEDWIDTH.equals(attribute))
    {
      this.setBedWidth(Double.parseDouble(value));
    }
    else if (SETTING_BEDHEIGHT.equals(attribute))
    {
      this.setBedHeight(Double.parseDouble(value));
    }
    else if (SETTING_MMPERSTEP.equals(attribute))
    {
      this.setMmPerStep(Double.parseDouble(value));
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
    clone.flipXaxis = flipXaxis;
    clone.mmPerStep = mmPerStep;
    return clone;
  }
}
