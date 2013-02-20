package com.t_oster.visicut.gui.beans;

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
 * @author thommy
 */
public class BetterJTable extends JTable
{

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
