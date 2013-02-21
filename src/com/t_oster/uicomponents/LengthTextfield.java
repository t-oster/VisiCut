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
package com.t_oster.uicomponents;

import com.t_oster.liblasercut.platform.Util;

/**
 *
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class LengthTextfield extends UnitTextfield
{

  private static final String[] units = new String[]{"mm","cm","m","in"};

  @Override
  protected String[] getUnits()
  {
    return units;
  }

  protected double removeUnit(double value, String unit)
  {
    if ("mm".equals(unit))
    {
      return value;
    }
    else if ("cm".equals(unit))
    {
      return value*10;
    }
    else if ("m".equals(unit))
    {
      return value*1000;
    }
    else if ("in".equals(unit))
    {
      return Util.inch2mm(value);
    }
    return value;
  }

  protected double addUnit(double value, String unit)
  {
    if ("mm".equals(unit))
    {
      return value;
    }
    else if ("cm".equals(unit))
    {
      return value/10;
    }
    else if ("m".equals(unit))
    {
      return value/1000;
    }
    else if ("in".equals(unit))
    {
      return Util.mm2inch(value);
    }
    return value;
  }

}
