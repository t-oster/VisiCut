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
package com.t_oster.visicut.gui.beans;

import com.t_oster.liblasercut.Customizable;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class CustomizableTableModel extends DefaultTableModel
{
  final Customizable c;
  public CustomizableTableModel(Customizable c)
  {
    this.c = c;
  }

  private String[] cols = new String[]
  {
    java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/resources/EditLaserDeviceDialog").getString("ATTRIBUTE"), java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/resources/EditLaserDeviceDialog").getString("VALUE")
  };

  @Override
  public int getColumnCount()
  {
    return cols.length;
  }

  @Override
  public String getColumnName(int i)
  {
    return cols[i];
  }

  @Override
  public int getRowCount()
  {
    return c!= null ? c.getPropertyKeys().length : 0;
  }

  @Override
  public Object getValueAt(int y, int x)
  {
    if (c == null)
    {
      return null;
    }
    String attribute = c.getPropertyKeys()[y];
    if (x == 0)
    {
      return attribute;
    }
    else if (x == 1)
    {
      return c.getProperty(attribute);
    }
    return null;
  }

  @Override
  public boolean isCellEditable(int y, int x)
  {
    return x == 1;
  }

  @Override
  public void setValueAt(Object o, int y, int x)
  {
    if (c != null && x == 1)
    {
      String attribute = c.getPropertyKeys()[y];
      c.setProperty(attribute, o);
    }
  }

}
