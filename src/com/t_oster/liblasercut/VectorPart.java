/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.liblasercut;

import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author thommy
 */
public class VectorPart
{

  private CuttingProperty currentCuttingProperty;
  private int maxX;
  private int maxY;
  private int minX;
  private int minY;
  private List<VectorCommand> commands;

  public VectorPart(CuttingProperty initialProperty)
  {
    this.currentCuttingProperty = initialProperty.clone();
    commands = new LinkedList<VectorCommand>();
    commands.add(new VectorCommand(VectorCommand.CmdType.SETPOWER, currentCuttingProperty.getPower()));
    commands.add(new VectorCommand(VectorCommand.CmdType.SETSPEED, currentCuttingProperty.getSpeed()));
    commands.add(new VectorCommand(VectorCommand.CmdType.SETFREQUENCY, currentCuttingProperty.getFrequency()));
  }

  public CuttingProperty getCurrentCuttingProperty()
  {
    return currentCuttingProperty;
  }

  public void setCurrentCuttingProperty(CuttingProperty cp)
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

  public void setFocus(int focus)
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
