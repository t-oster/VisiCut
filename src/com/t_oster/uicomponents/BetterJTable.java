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

import java.awt.Component;
import javax.swing.DefaultCellEditor;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

/**
 * This table gives out the default Editors and Renderers
 * for Booleans, Integers, Doubles and Strings without
 * restricting the columns to be from one class
 * 
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class BetterJTable extends JTable
{

  public BetterJTable()
  {
    if (this.getRowHeight() < 23)
    {
      this.setRowHeight(23);
    }
  }
  
  private int[] relativeWidths = null;
  
  public void setColumnRelations(int[] relativeWidths)
  {
    this.relativeWidths = relativeWidths;
  }

  
  
  @Override
  public void doLayout()
  {
    int width = this.getWidth();
    if (this.relativeWidths != null)
    {
      double sum = 0;
      for (int w : this.relativeWidths)
      {
        sum += w;
      }
      double factor = width/sum;
      //this.setAutoResizeMode(AUTO_RESIZE_OFF);
      for (int c = 0; c < this.relativeWidths.length; c++)
      {
        this.getColumnModel().getColumn(c).setWidth((int) (factor*this.relativeWidths[c]));
        this.getColumnModel().getColumn(c).setMinWidth((int) (factor*this.relativeWidths[c]));
        this.getColumnModel().getColumn(c).setMaxWidth((int) (factor*this.relativeWidths[c]));
        this.getColumnModel().getColumn(c).setPreferredWidth((int) (factor*this.relativeWidths[c]));
      }
    }
    super.doLayout();
  }
  
  
  
  @Override
  public TableCellEditor getCellEditor(int row, int column)
  {
    Object o = this.getValueAt(row, column);
    TableCellEditor result;
    if (o instanceof Boolean)
    {
      result = this.getDefaultEditor(Boolean.class);
    }
    else if (o instanceof String || o instanceof Integer || o instanceof Float || o instanceof Double)
    {
      //return textfield-editor which selects the whole text by default
      result = new DefaultCellEditor(new JTextField()){
          @Override
          public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column)
          {
            Component c = super.getTableCellEditorComponent(table, value, isSelected, row, column);
            if (c instanceof JTextField)
            {
              ((JTextField) c).setBorder(null);
              ((JTextField) c).setSelectionStart(0);
              ((JTextField) c).setSelectionEnd(((JTextField) c).getText().length());
            }
            return c;
          }
      };
    }
    else
    {
      result = super.getCellEditor(row, column);
    }
    return result;
  }

  @Override
  public void setValueAt(Object aValue, int row, int column)
  {
    Object o = this.getValueAt(row, column);
    try
    {
      if (o instanceof Integer)
      {
        super.setValueAt(Integer.parseInt(aValue.toString()), row, column);
      }
      else if (o instanceof Double)
      {
        super.setValueAt(Double.parseDouble(aValue.toString().replace(",", ".")), row, column);
      }
      else if (o instanceof Float)
      {
        super.setValueAt(Float.parseFloat(aValue.toString().replace(",", ".")), row, column);
      }
      else
      {
        super.setValueAt(aValue, row, column);
      }
    }
    catch (NumberFormatException e)
    {
    }
  }
  
  //directly enable editing on focus change
  @Override
  public void changeSelection(final int row, final int column, boolean toggle, boolean extend)
  {
      super.changeSelection(row, column, toggle, extend);
      this.editCellAt(row, column);
      this.transferFocus();
  }
  

  @Override
  public TableCellRenderer getCellRenderer(int row, int column)
  {
    Object o = this.getValueAt(row, column);
    if (o instanceof Boolean)
    {
      return this.getDefaultRenderer(Boolean.class);
    }
    return super.getCellRenderer(row, column);
  }
  
}
