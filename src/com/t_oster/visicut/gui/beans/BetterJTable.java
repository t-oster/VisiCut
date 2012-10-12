package com.t_oster.visicut.gui.beans;

import javax.swing.JTable;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

/**
 * This table gives out the default Editors and Renderers
 * for Booleans, Integers, Doubles and Strings without
 * restricting the columns to be from one class
 * 
 * @author thommy
 */
public class BetterJTable extends JTable
{

  @Override
  public TableCellEditor getCellEditor(int row, int column)
  {
    Object o = this.getValueAt(row, column);
    if (o instanceof Boolean)
    {
      return this.getDefaultEditor(Boolean.class);
    }
    return super.getCellEditor(row, column);
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
        super.setValueAt(Double.parseDouble(aValue.toString()), row, column);
      }
      else if (o instanceof Float)
      {
        super.setValueAt(Float.parseFloat(aValue.toString()), row, column);
      }
      else
      {
        super.setValueAt(aValue, row, column);
      }
    }
    catch (NumberFormatException e)
    {
      return;
    }
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
