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

/**
 *
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
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
  private float foperand;

  public VectorCommand(CmdType type, float f)
  {
    if (type == CmdType.SETFOCUS)
    {
      this.type = type;
      this.foperand = f;
    }
    else
    {
      throw new IllegalArgumentException("Wrong number of Parameters for " + type.toString());
    }
  }
  
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
    if (type == CmdType.SETSPEED || type == CmdType.SETPOWER || type == CmdType.SETFREQUENCY)
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

  public float getFocus()
  {
    if (type == CmdType.SETFOCUS)
    {
      return foperand;
    }
    throw new UnsupportedOperationException("getFocus is not Applicable for " + type.toString());
  }
}
