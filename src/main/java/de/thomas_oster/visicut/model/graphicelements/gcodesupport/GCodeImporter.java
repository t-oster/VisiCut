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
package de.thomas_oster.visicut.model.graphicelements.gcodesupport;

import de.thomas_oster.liblasercut.platform.Util;
import de.thomas_oster.visicut.model.graphicelements.AbstractImporter;
import de.thomas_oster.visicut.model.graphicelements.GraphicSet;
import de.thomas_oster.visicut.model.graphicelements.ImportException;
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
  //if using absolute coordinates (G90) or relative ones (G91)
  private boolean absolute = true;
  //if arc centers I/J are absolute (G90.1) or relative to the arc start (G91.1, default)
  private boolean absoluteArcCenter = false;
  //current position
  private double last_x = 0;
  private double last_y = 0;
  //factor for converting coordinates to mm
  private double unit2mm = 1;
  
  private Pattern command_pattern = Pattern.compile("[A-Z]-?[0-9]+[,.]?[0-9]*");
  
  /**
   * returns the number after the first character
   * e.g. from G or X commands
   */
  private double getNumber(String cmd)
  {
    cmd = cmd.substring(1).replace(",",".");
    if (cmd.endsWith("."))
    {
      cmd = cmd.substring(0, cmd.length()-1);
    }
    return Double.parseDouble(cmd);
  }
  
  /**
   * returns the coordinate e.g. from X or Y commands
   * assumes the first character to be the control
   * character
   */
  private double getCoordinate(String cmd)
  {
    return unit2mm*getNumber(cmd);
  }
  
  /**
   * Appends an arc from last_x/last_y to x/y around cx/cy.
   * Angles are measured as in the G-Code coordinates, i.e. growing from +X
   * towards +Y. Start and end at the same point give a full circle.
   */
  private void appendArc(GeneralPath path, double x, double y, double cx, double cy, boolean clockwise)
  {
    double radius = Math.hypot(last_x-cx, last_y-cy);
    if (Math.abs(radius - Math.hypot(x-cx, y-cy)) >= 0.1)
    {
      System.err.println("GCODE-WARNING: No real circle");
    }
    double start = Math.atan2(last_y-cy, last_x-cx);
    double end = Math.atan2(y-cy, x-cx);
    double sweep = clockwise ? start-end : end-start;
    //normalize to [0, 2*PI)
    sweep -= 2*Math.PI*Math.floor(sweep/(2*Math.PI));
    if (Math.hypot(x-last_x, y-last_y) < 1e-9)
    {
      sweep = 2*Math.PI;
    }
    //Arc2D angles grow in the opposite direction
    Arc2D arc = new Arc2D.Double();
    arc.setArcByCenter(cx, cy, radius, -Math.toDegrees(start), clockwise ? Math.toDegrees(sweep) : -Math.toDegrees(sweep), Arc2D.OPEN);
    path.append(arc, true);
  }
  
  /**
   * Center of an arc from last_x/last_y to x/y with radius r.
   * A positive radius gives the shorter arc, a negative one the longer arc.
   * @return center, or null if it is not defined (full circle)
   */
  private double[] getArcCenter(double x, double y, double r, boolean clockwise, List<String> warnings)
  {
    double dx = x-last_x;
    double dy = y-last_y;
    double chord = Math.hypot(dx, dy);
    if (chord < 1e-9)
    {
      warnings.add("G-Code: full circle with R is ambiguous and ignored");
      return null;
    }
    double h = 4*r*r - chord*chord;
    if (h < 0)
    {
      if (2*Math.abs(r) - chord < -0.1)
      {
        warnings.add("G-Code: arc radius R is smaller than half the distance of its end points");
      }
      h = 0;
    }
    double factor = Math.sqrt(h)/chord;
    if (clockwise)
    {
      factor = -factor;
    }
    if (r < 0)
    {
      factor = -factor;
    }
    return new double[]{last_x + 0.5*(dx - dy*factor), last_y + 0.5*(dy + dx*factor)};
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
        //values given in this line, null if missing
        Double x = null;
        Double y = null;
        Double i = null;
        Double j = null;
        Double r = null;
        Matcher matcher = command_pattern.matcher(line);
        while (matcher.find()) 
        {
          String cmd = matcher.group();
          try
          {
            if (cmd.startsWith("G"))
            {
              switch ((int) Math.round(getNumber(cmd)*10))
              {
                case 0: g_mode = 0; break;
                case 10: g_mode = 1; break;
                case 20: g_mode = 2; break;
                case 30: g_mode = 3; break;
                //switch to inch-mode
                case 200: unit2mm = Util.inch2mm(1); break;
                //mm-mode
                case 210: unit2mm = 1; break;
                case 900: absolute = true; break;
                case 910: absolute = false; break;
                case 901: absoluteArcCenter = true; break;
                case 911: absoluteArcCenter = false; break;
                default: break;
              }
            }
            else if (cmd.startsWith("X"))
            {
              x = getCoordinate(cmd);
            }
            else if (cmd.startsWith("Y"))
            {
              y = getCoordinate(cmd);
            }
            else if (cmd.startsWith("I"))
            {
              i = getCoordinate(cmd);
            }
            else if (cmd.startsWith("J"))
            {
              j = getCoordinate(cmd);
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
        boolean arc = g_mode == 2 || g_mode == 3;
        //is set if this line issues a move event
        boolean actionTriggered = x != null || y != null || (arc && (i != null || j != null));
        if (actionTriggered)
        {
          double target_x;
          double target_y;
          if (absolute)
          {
            target_x = x != null ? x : last_x;
            target_y = y != null ? y : last_y;
          }
          else
          {
            target_x = last_x + (x != null ? x : 0);
            target_y = last_y + (y != null ? y : 0);
          }
          if (g_mode == 0)
          {
            resultingShape.moveTo(target_x, target_y);
          }
          else if (g_mode == 1)
          {
            resultingShape.lineTo(target_x, target_y);
          }
          else if (arc)
          {
            boolean clockwise = g_mode == 2;
            double[] center;
            if (r != null)
            {
              center = getArcCenter(target_x, target_y, r, clockwise, warnings);
            }
            else if (absoluteArcCenter)
            {
              center = new double[]{i != null ? i : last_x, j != null ? j : last_y};
            }
            else
            {
              center = new double[]{last_x + (i != null ? i : 0), last_y + (j != null ? j : 0)};
            }
            if (center != null)
            {
              appendArc(resultingShape, target_x, target_y, center[0], center[1], clockwise);
            }
          }
          last_x = target_x;
          last_y = target_y;
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
