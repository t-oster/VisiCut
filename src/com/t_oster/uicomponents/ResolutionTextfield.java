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
public class ResolutionTextfield extends UnitTextfield
{

  private static final String[] units = new String[]{"dpi","dpmm"};

  @Override
  protected String[] getUnits()
  {
    return units;
  }

  protected double removeUnit(double value, String unit)
  {
    if ("dpi".equals(unit))
    {
      return value;
    }
    else if ("dpmm".equals(unit))
    {
      return Util.dpmm2dpi(value);
    }
    return value;
  }

  protected double addUnit(double value, String unit)
  {
    if ("dpi".equals(unit))
    {
      return value;
    }
    else if ("dpmm".equals(unit))
    {
      return Util.dpi2dpmm(value);
    }
    return value;
  }

}
