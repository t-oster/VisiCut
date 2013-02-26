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
package com.t_oster.visicut.gui.mapping;

import com.t_oster.visicut.model.LaserProfile;
import com.t_oster.visicut.model.mapping.FilterSet;
import java.util.List;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
class MappingTableModel extends DefaultTableModel
{

  private List<MappingTableEntry> entries;

  public MappingTableModel(List<MappingTableEntry> entries)
  {
    this.entries = entries;
  }
  private String[] columns = new String[]
  {
    java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/mapping/resources/CustomMappingPanel").getString("ENABLED"), java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/mapping/resources/CustomMappingPanel").getString("SELECTION"), java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/mapping/resources/CustomMappingPanel").getString("PROFILE")
  };
  private Class[] classes = new Class[]
  {
    Boolean.class, FilterSet.class, LaserProfile.class
  };

  public void setColumnTitle(int column, String title)
  {
    columns[column] = title;
    //TODO check if this is the right event to fire
    this.fireTableStructureChanged();
  }

  @Override
  public int getColumnCount()
  {
    return columns.length;
  }

  @Override
  public String getColumnName(int column)
  {
    return columns[column];
  }

  @Override
  public int getRowCount()
  {
    return entries == null ? 0 : entries.size();
  }

  @Override
  public Object getValueAt(int row, int column)
  {
    MappingTableEntry e = entries.get(row);
    switch (column)
    {
      case 0:
        return (Boolean) e.enabled;
      case 1:
        return e.filterSet;
      case 2:
        return e.profile;
      default:
        return null;
    }
  }

  @Override
  public boolean isCellEditable(int row, int column)
  {
    return true;
  }

  @Override
  public void setValueAt(Object aValue, int row, int column)
  {
    MappingTableEntry e = entries.get(row);
    switch (column)
    {
      case 0:
      {
        if (e.enabled == (Boolean) aValue)
        {
          return;
        }
        e.enabled = (Boolean) aValue;
        break;
      }
      case 1:
      {
        e.filterSet = (FilterSet) aValue;
        e.enabled = true;
        break;
      }
      case 2:
      {
        if (aValue != null && e.profile != null && e.profile.getName().equals(((LaserProfile) aValue).getName()))
        {
          return;
        }
        e.profile = aValue != null ? ((LaserProfile) aValue).clone() : null;
        e.enabled = true;
        break;
      }
    }
    this.fireTableRowsUpdated(row, row);
  }

  @Override
  public Class getColumnClass(int columnIndex)
  {
    return classes[columnIndex];
  }

  @Override
  public void removeRow(int row)
  {
  }
}
