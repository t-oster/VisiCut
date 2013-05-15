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

package com.t_oster.visicut.model.graphicelements.psvgsupport;

import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class ParameterTableModel extends DefaultTableModel
{
  
  private Map<String, Object> map = new LinkedHashMap<String, Object>();
  private String[] keys = new String[0];
  
  public void setParameterMap(Map<String, Object> map)
  {
    if (map == null)
    {
      this.map = new LinkedHashMap<String, Object>();
      this.keys = new String[0];
    }
    else
    {
      this.map = map;
      this.keys = map.keySet().toArray(new String[0]);
    }
    this.fireTableDataChanged();
  }

  @Override
  public int getRowCount()
  {
    return map != null ? map.size() : 0;
  }

  @Override
  public int getColumnCount()
  {
    return 2;
  }
  
  @Override
  public String getColumnName(int column)
  {
    return column == 0 ? "Key" : "Value";
  }

  @Override
  public boolean isCellEditable(int row, int column)
  {
    return column == 1;
  }

  @Override
  public Object getValueAt(int row, int column)
  {
    return column == 0 ? keys[row] : map.get(keys[row]);
  }

  @Override
  public void setValueAt(Object aValue, int row, int column)
  {
    map.put(keys[row], aValue);
    this.fireTableCellUpdated(row, column);
  }
  
}
