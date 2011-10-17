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
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.liblasercut;

import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class VectorPart
{

  private LaserProperty currentCuttingProperty;
  private int maxX;
  private int maxY;
  private int minX;
  private int minY;
  private List<VectorCommand> commands;

  public VectorPart(LaserProperty initialProperty)
  {
    this.currentCuttingProperty = initialProperty.clone();
    commands = new LinkedList<VectorCommand>();
    commands.add(new VectorCommand(VectorCommand.CmdType.SETPOWER, currentCuttingProperty.getPower()));
    commands.add(new VectorCommand(VectorCommand.CmdType.SETSPEED, currentCuttingProperty.getSpeed()));
    commands.add(new VectorCommand(VectorCommand.CmdType.SETFREQUENCY, currentCuttingProperty.getFrequency()));
  }

  public LaserProperty getCurrentCuttingProperty()
  {
    return currentCuttingProperty;
  }

  public void setCurrentCuttingProperty(LaserProperty cp)
  {
    this.setFrequency(cp.getFrequency());
    this.setPower(cp.getPower());
    this.setSpeed(cp.getSpeed());
    this.setFocus(cp.getFocus());
  }

  public VectorCommand[] getCommandList()
  {
    return commands.toArray(new VectorCommand[0]);
  }

  public void setSpeed(int speed)
  {
    if (speed != this.currentCuttingProperty.getSpeed())
    {
      commands.add(new VectorCommand(VectorCommand.CmdType.SETSPEED, speed));
      this.currentCuttingProperty.setSpeed(speed);
    }
  }

  public void setPower(int power)
  {
    if (power != this.currentCuttingProperty.getPower())
    {
      commands.add(new VectorCommand(VectorCommand.CmdType.SETPOWER, power));
      this.currentCuttingProperty.setPower(power);
    }
  }

  public void setFrequency(int frequency)
  {
    if (frequency != this.currentCuttingProperty.getFrequency())
    {
      commands.add(new VectorCommand(VectorCommand.CmdType.SETFREQUENCY, frequency));
      this.currentCuttingProperty.setFrequency(frequency);
    }
  }

  public void setFocus(float focus)
  {
    if (focus != this.currentCuttingProperty.getFocus())
    {
      commands.add(new VectorCommand(VectorCommand.CmdType.SETFOCUS, focus));
      this.currentCuttingProperty.setFocus(focus);
    }
  }

  private void checkMin(int x, int y)
  {
    if (x < minX)
    {
      minX = x;
    }
    if (y < minY)
    {
      minY = y;
    }
  }

  private void checkMax(int x, int y)
  {
    if (x > maxX)
    {
      maxX = x;
    }
    if (y > maxY)
    {
      maxY = y;
    }
  }

  public void moveto(int x, int y)
  {
    commands.add(new VectorCommand(VectorCommand.CmdType.MOVETO, x, y));
    checkMin(x, y);
    checkMax(x, y);
  }

  public void lineto(int x, int y)
  {
    commands.add(new VectorCommand(VectorCommand.CmdType.LINETO, x, y));
    checkMin(x, y);
    checkMax(x, y);
  }

  /**
   * Returns the Width of the CuttingPart in Pixels
   * @return 
   */
  public int getWidth()
  {
    return maxX - minX;
  }

  /**
   * Returns the height of the CuttingPart in Pixels
   * @return 
   */
  public int getHeight()
  {
    return maxY - minY;
  }
}
