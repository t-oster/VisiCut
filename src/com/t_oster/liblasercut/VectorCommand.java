/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.liblasercut;

/**
 *
 * @author thommy
 */
public class VectorCommand
{

  public static enum CmdType
  {

    SETSPEED,
    SETPOWER,
    SETFREQUENCY,
    SETFOCUS,
    MOVETO,
    LINETO
  }
  private CmdType type;
  private int[] operands;

  public VectorCommand(CmdType type, int x, int y)
  {
    if (type == CmdType.MOVETO || type == CmdType.LINETO)
    {
      this.type = type;
      this.operands = new int[]
      {
        x, y
      };
    }
    else
    {
      throw new IllegalArgumentException("Wrong number of Parameters for " + type.toString());
    }
  }

  public CmdType getType()
  {
    return type;
  }

  public int getX()
  {
    if (this.type == CmdType.MOVETO || this.type == CmdType.LINETO)
    {
      return operands[0];
    }
    throw new UnsupportedOperationException("getX not supported for " + type.toString());
  }

  public int getY()
  {
    if (this.type == CmdType.MOVETO || this.type == CmdType.LINETO)
    {
      return operands[1];
    }
    throw new UnsupportedOperationException("getX not supported for " + type.toString());
  }

  public VectorCommand(CmdType type, int operand1)
  {
    if (type == CmdType.SETSPEED || type == CmdType.SETPOWER || type == CmdType.SETFREQUENCY || type == CmdType.SETFOCUS)
    {
      this.type = type;
      operands = new int[]
      {
        operand1
      };
    }
    else
    {
      throw new IllegalArgumentException("Wrong number of Parameters for " + type.toString());
    }
  }

  public int getPower()
  {
    if (type == CmdType.SETPOWER)
    {
      return operands[0];
    }
    throw new UnsupportedOperationException("getPower is not Applicable for " + type.toString());
  }

  public int getSpeed()
  {
    if (type == CmdType.SETSPEED)
    {
      return operands[0];
    }
    throw new UnsupportedOperationException("getSpeed is not Applicable for " + type.toString());
  }

  public int getFrequency()
  {
    if (type == CmdType.SETFREQUENCY)
    {
      return operands[0];
    }
    throw new UnsupportedOperationException("getFrequency is not Applicable for " + type.toString());
  }

  public int getFocus()
  {
    if (type == CmdType.SETFOCUS)
    {
      return operands[0];
    }
    throw new UnsupportedOperationException("getFocus is not Applicable for " + type.toString());
  }
}
