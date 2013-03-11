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
package com.t_oster.visicut.gui.propertypanel;

import com.t_oster.liblasercut.LaserProperty;
import com.t_oster.liblasercut.platform.Util;
import java.util.List;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class LaserPropertiesTableModel extends DefaultTableModel
{

  public void setLaserProperties(List<LaserProperty> lp)
  {
    this.lp = lp;
    if (lp.isEmpty())
    {
      columnNames = new String[]{"no property given ;("};
    }
    else
    {
      columnNames = lp.get(0).getPropertyKeys();
    }
    this.fireTableStructureChanged();
  }
  private List<LaserProperty> lp = null;
  private String[] columnNames = new String[]{"No property given ;("};

  @Override
  public Class getColumnClass(int x)
  {
    if (lp.isEmpty())
    {
      return String.class;
    }
    else
    {
      return lp.get(0).getProperty(this.columnNames[x]).getClass();
    }
  }
  
  @Override
  public int getColumnCount()
  {
    return columnNames.length;
  }

  @Override
  public String getColumnName(int i)
  {
    return columnNames[i];
  }

  @Override
  public Object getValueAt(int y, int x)
  {
    return lp.get(y).getProperty(this.columnNames[x]);
  }

  @Override
  public boolean isCellEditable(int i, int i1)
  {
    return true;
  }

  @Override
  public void setValueAt(Object o, int y, int x)
  {
    Object old = lp.get(y).getProperty(this.columnNames[x]);
    if (Util.differ(old, o))
    {
      lp.get(y).setProperty(this.columnNames[x], o);
      this.fireTableDataChanged();
    }
  }

  @Override
  public int getRowCount()
  {
    return lp == null ? 0 : lp.size();
  }
  
}
