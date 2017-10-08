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

package com.t_oster.uicomponents.parameter;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ResourceBundle;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class ParameterTableModel extends DefaultTableModel
{
  
  private ResourceBundle bundle = ResourceBundle.getBundle("com/t_oster/uicomponents/parameter/resources/ParameterTableModel");
  
  private Map<String, Parameter> map = new LinkedHashMap<String, Parameter>();
  private String[] keys = new String[0];
  
  public void setParameterMap(Map<String, Parameter> map)
  {
    if (map == null)
    {
      this.map = new LinkedHashMap<String, Parameter>();
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
    return 3;
  }
  
  @Override
  public String getColumnName(int column)
  {
    //TODO: Translate
    switch (column)
    {
      case 0: return bundle.getString("KEY");
      case 1: return bundle.getString("VALUE");
      case 2: return bundle.getString("RESET");
    }
    return null;
  }

  @Override
  public boolean isCellEditable(int row, int column)
  {
    return column >= 1;
  }

  @Override
  public Object getValueAt(int row, int column)
  {
    if (column == 0)
    {
      return map.get(keys[row]).label == null ? keys[row] : map.get(keys[row]).label;
    }
    else
    {
      return map.get(keys[row]).value;
    }
  }
  
  public Parameter getParameterAt(int row)
  {
    return map.get(keys[row]);
  }

  @Override
  public void setValueAt(Object aValue, int row, int column)
  {
    map.get(keys[row]).value =  aValue;
    this.fireTableCellUpdated(row, column);
  }
  
}
