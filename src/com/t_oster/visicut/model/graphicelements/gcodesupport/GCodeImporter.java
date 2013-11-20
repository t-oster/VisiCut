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
package com.t_oster.visicut.model.graphicelements.gcodesupport;

import com.t_oster.liblasercut.platform.Util;
import com.t_oster.visicut.model.graphicelements.AbstractImporter;
import com.t_oster.visicut.model.graphicelements.GraphicSet;
import com.t_oster.visicut.model.graphicelements.ImportException;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.GeneralPath;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.filechooser.FileFilter;

/**
 *
 * @author thommy
 */
public class GCodeImporter extends AbstractImporter
{

  private static final FileFilter filter = new FileFilter()
  {

    @Override
    public boolean accept(File file)
    {
      String name = file.getAbsolutePath().toLowerCase();
      return file.isDirectory() || name.endsWith(".nc") || name.endsWith(".gcode");
    }

    @Override
    public String getDescription()
    {
      return "G-Code (.nc, .gcode)";
    }

  };
  
  //current mode 0=move, 1=line, 2=arc cw, 3=arc ccw
  private int g_mode = 0;
  //if using absolute coordinates
  private boolean absolute = true;
  //last position for relative coordinates
  private double last_x = 0;
  private double last_y = 0;
  //positions for current operation
  private double x = 0;
  private double y = 0;
  private double cx = 0;
  private double cy = 0;
  private Double r = null;
  //factor for converting coordinates to mm
  private double unit2mm = 1;
  
  private Pattern command_pattern = Pattern.compile("[A-Z]-?[0-9]+[,.]?[0-9]*");
  
  /**
   * returns the coordinate e.g. from X or Y commands
   * assumes the first character to be the control
   * character
   * @param command
   * @return 
   */
  private double getCoordinate(String cmd)
  {
    cmd = cmd.substring(1).replace(",",".");
    if (cmd.endsWith("."))
    {
      cmd = cmd.substring(0, cmd.length()-1);
    }
    return unit2mm*Double.parseDouble(cmd);
  }
  
  @Override
  public GraphicSet importSetFromFile(File inputFile, List<String> warnings) throws ImportException
  {
    try
    {
      BufferedReader reader = new BufferedReader(new FileReader(inputFile));
      GraphicSet result = new GraphicSet();
      GeneralPath resultingShape = new GeneralPath();
      resultingShape.moveTo(0, 0);
      String line = reader.readLine();
      while (line != null)
      {
        //is set if this line issues a move event
        boolean actionTriggered = false;
        r = null;
        Matcher matcher = command_pattern.matcher(line);
        while (matcher.find()) 
        {
          String cmd = matcher.group();
          try
          {
            if (cmd.startsWith("G0") && cmd.length() == 3)
            {
               g_mode = Integer.parseInt(cmd.substring(2));
            }
            else if ("G20".equals(cmd))
            {
              //switch to inch-mode
              unit2mm = Util.inch2mm(1);
            }
            else if ("G21".equals(cmd))
            {
              //mm-mode
              unit2mm = 1;
            }
            else if (cmd.startsWith("X"))
            {
              x = getCoordinate(cmd);
              actionTriggered = true;
            }
            else if (cmd.startsWith("Y"))
            {
              y = getCoordinate(cmd);
              actionTriggered = true;
            }
            else if (cmd.startsWith("I"))
            {
              cx = getCoordinate(cmd);
            }
            else if (cmd.startsWith("J"))
            {
              cy = getCoordinate(cmd);
            }
            else if (cmd.startsWith("R"))
            {
              r = getCoordinate(cmd);
            }
          }
          catch (NumberFormatException e)
          {
            warnings.add("Illegal G-Code: "+cmd);
          }
        }
        if (actionTriggered)
        {
          
          if (g_mode == 0)
          {
            if (absolute)
            {
              resultingShape.moveTo(x, y);
            }
            else
            {
              resultingShape.moveTo(last_x+x, last_y+y);
            }
          }
          else if (g_mode == 1)
          {
            if (absolute)
            {
              resultingShape.lineTo(x, y);
            }
            else
            {
              resultingShape.lineTo(last_x+x, last_y+y);
            }
          }
          else if (g_mode == 2)
          {
            if (absolute)
            {
              double ax,ay,aw,ah,start,end,extend,radius;
              if (r != null)
              {
                radius = r;
                //TODO: Circle cw from last_x/last_y to x/y with radius r
              }
              else
              {
                double mid_x = cx;
                double mid_y = cy;
                radius = Math.sqrt((last_x-cx)*(last_x-cx)+(last_y-cy)*(last_y-cy));
                if (Math.abs(radius - Math.sqrt((x-cx)*(x-cx)+(y-cy)*(y-cy))) >= 0.1)
                {
                  System.err.println("GCODE-WARNING: No real circle");
                }
                start = Math.atan2(last_y-mid_y, last_x-mid_x);
                //convert to positive angle
                if (start < 0) start += 2*Math.PI;
                end = Math.atan2(y-mid_y, x-mid_x);
                //convert to positive angle
                if (end < 0) end += 2*Math.PI;
                extend = start-end;
                //convert to negative angle
                if (extend > 0) extend-=2*Math.PI;
                //strange stuff with the angles, but it seems to work
                Arc2D arc = new Arc2D.Double();
                arc.setArcByCenter(cx, cy, radius, -Math.toDegrees(start)-360, Math.toDegrees(extend)+360, Arc2D.OPEN);
                resultingShape.append(arc, true);
              }         
            }
            else
            {
              //TODO
            }
          }
          else if (g_mode == 3)
          {
            if (absolute)
            {
              double ax,ay,aw,ah,start,end,extend,radius;
              if (r != null)
              {
                radius = r;
                //TODO: Circle cw from last_x/last_y to x/y with radius r
              }
              else
              {
                //TODO: Circle cw from last_x/last_y to x/y with center last_x+cx/last_y+cy
                double mid_x = cx;
                double mid_y = cy;
                radius = Math.sqrt((last_x-cx)*(last_x-cx)+(last_y-cy)*(last_y-cy));
                if (Math.abs(radius - Math.sqrt((x-cx)*(x-cx)+(y-cy)*(y-cy))) >= 0.1)
                {
                  System.err.println("GCODE-WARNING: No real circle");
                }
                start = Math.atan2(last_y-mid_y, last_x-mid_x);
                //convert to positive angle
                if (start < 0) start += 2*Math.PI;
                end = Math.atan2(y-mid_y, x-mid_x);
                //convert to positive angle
                if (end < 0) end += 2*Math.PI;
                extend = start-end;
                //convert to negative angle
                if (extend > 0) extend-=2*Math.PI;
                //no idea why the start coordinate has to be negative, but seems to work
                Arc2D arc = new Arc2D.Double();
                arc.setArcByCenter(cx, cy, radius, -Math.toDegrees(start), Math.toDegrees(extend), Arc2D.OPEN);
                resultingShape.append(arc, true);
              }         
            }
            else
            {
              //TODO
            }
          }
          if (absolute)
          {
            last_x = x;
            last_y = y;
          }
          else
          {
            last_x += x;
            last_y += y;
          }
        }
        line = reader.readLine();
      }
      result.add(new GCodeShape(resultingShape, inputFile));
      result.setBasicTransform(new AffineTransform());
      result.setTransform(result.getBasicTransform());
      return result;
    }
    catch (FileNotFoundException ex)
    {
      throw new ImportException(ex);
    }
    catch (IOException ex)
    {
      throw new ImportException(ex);
    }
  }

  public FileFilter getFileFilter()
  {
    return filter;
  }
  
}
