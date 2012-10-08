/**
 * This file is part of VisiCut.
 * Copyright (C) 2012 Thomas Oster <thomas.oster@rwth-aachen.de>
 * RWTH Aachen University - 52062 Aachen, Germany
 * 
 *     VisiCut is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *    VisiCut is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 * 
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with VisiCut.  If not, see <http://www.gnu.org/licenses/>.
 **/
package com.t_oster.visicut.gui.mappingdialog;

import javax.swing.table.TableCellEditor;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JTable;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JTextField;

public class FilterValueEditor extends DefaultCellEditor
  implements TableCellEditor,
  ActionListener
{

  Object currentObject;
  JButton button;
  JColorChooser colorChooser;
  JDialog dialog;
  DefaultCellEditor selectTypeEditor;
  protected static final String EDIT = "edit";

  public FilterValueEditor()
  {
    super(new JTextField());
    button = new JButton();
    button.setActionCommand(EDIT);
    button.addActionListener(this);
    button.setBorderPainted(false);

    //Set up the dialog that the button brings up.
    colorChooser = new JColorChooser();
    dialog = JColorChooser.createDialog(button,
      "Pick a Color",
      true, //modal
      colorChooser,
      this, //OK button handler
      null); //no CANCEL button handler
    JComboBox types = new JComboBox();
    types.addItem("Select Type");
    types.addItem("Color");
    types.addItem("Number");
    types.addItem("Text");
    this.selectTypeEditor = new DefaultCellEditor(types);
  }

  /**
   * Handles events from the editor button and from
   * the dialog's OK button.
   */
  public void actionPerformed(ActionEvent e)
  {
    if (currentObject instanceof Color)
    {
      if (EDIT.equals(e.getActionCommand()))
      {

        button.setBackground((Color) currentObject);
        colorChooser.setColor((Color) currentObject);
        dialog.setVisible(true);
        //Make the renderer reappear.
        fireEditingStopped();
      }
      else
      { //User pressed dialog's "OK" button.
        currentObject = colorChooser.getColor();
      }
    }
  }

  //Implement the one CellEditor method that AbstractCellEditor doesn't.
  @Override
  public Object getCellEditorValue()
  {
    if (currentObject == null)
    {
      String type = (String) this.selectTypeEditor.getCellEditorValue();
      if ("Select Type".equals(type))
      {
        return null;
      }
      if ("Color".equals(type))
      {
        return Color.black;
      }
      if ("Number".equals(type))
      {
        return "1.0";
      }
      return "example";
    }
    if (currentObject instanceof Color)
    {
      return currentObject;
    }
    return super.getCellEditorValue();
  }

  //Implement the one method defined by TableCellEditor.
  @Override
  public Component getTableCellEditorComponent(JTable table,
    Object value,
    boolean isSelected,
    int row,
    int column)
  {
    currentObject = value;
    if (currentObject == null)
    {
      return this.selectTypeEditor.getTableCellEditorComponent(table, value, isSelected, row, row);
    }
    if (currentObject instanceof Color)
    {
      return button;
    }
    return super.getTableCellEditorComponent(table, value, isSelected, row, column);
  }
}
