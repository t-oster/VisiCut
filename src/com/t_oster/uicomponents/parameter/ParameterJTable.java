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

import com.t_oster.uicomponents.BetterJTable;
import com.t_oster.uicomponents.PlatformIcon;
import com.t_oster.uicomponents.TableButton;
import com.t_oster.uicomponents.TableButtonListener;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

/**
 *
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class ParameterJTable extends BetterJTable
{

  private TableButton resetButton;
  
  public ParameterJTable()
  {
    this.setRowHeight(28);
    resetButton = new TableButton("");
    resetButton.setIcon(PlatformIcon.get(PlatformIcon.UNDO));
    resetButton.addTableButtonListener(new TableButtonListener() {
      @Override
      public void tableButtonClicked(int row, int col) {
        TableModel m = getModel();
        if (m instanceof ParameterTableModel)
        {
          setValueAt(((ParameterTableModel) m).getParameterAt(row).deflt, row, col);
        }
      }     
    }); 
  }
  
  @Override
  public TableCellRenderer getCellRenderer(int row, int column)
  {
    if (this.getModel() instanceof ParameterTableModel && column == 2)
    {
      return resetButton;
    }
    /*
     * if (this.getModel() instanceof ParameterTableModel)
    {
      Parameter p = ((ParameterTableModel) this.getModel()).getParameterAt(row);
      if (p.minValue != null && p.maxValue != null && p.minValue instanceof Integer)
      {
        return new SliderCellRenderer((Integer) p.minValue, (Integer) p.maxValue);
      }
    }
    * */
    return super.getCellRenderer(row, column); //To change body of generated methods, choose Tools | Templates.
  }

  
  @Override
  public TableCellEditor getCellEditor(int row, int column)
  {
    if (this.getModel() instanceof ParameterTableModel)
    {
      if (column == 2)
      {
        return resetButton;
      }
      Parameter p = ((ParameterTableModel) this.getModel()).getParameterAt(row);
      if (p.possibleValues != null)
      {
        JComboBox cb = new JComboBox();
        for (Object e : p.possibleValues)
        {
          cb.addItem(e);
        }
        cb.setEditable(true);
        cb.setSelectedItem(p.value);
        return new DefaultCellEditor(cb);
      }
      else if (p.minValue != null && p.maxValue != null && p.minValue instanceof Integer)
      {
        return new SliderCellEditor((Integer) p.minValue, (Integer) p.maxValue);
      }
    }
    return super.getCellEditor(row, column);
  }
  
}
