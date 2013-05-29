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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.EventObject;
import java.util.LinkedList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.event.CellEditorListener;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

/**
 * from http://stackoverflow.com/questions/1475543/how-to-add-button-in-a-row-of-jtable-in-swing-java
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class TableButton extends JButton implements TableCellRenderer, TableCellEditor {
  private int selectedRow;
  private int selectedColumn;
  List<TableButtonListener> listener;

  public TableButton(String text) {
    super(text); 
    listener = new LinkedList<TableButtonListener>();
    addActionListener(new ActionListener() { 
      public void actionPerformed( ActionEvent e ) { 
        for(TableButtonListener l : listener) { 
          l.tableButtonClicked(selectedRow, selectedColumn);
      }
    }});
  }

  public void addTableButtonListener( TableButtonListener l ) {
    listener.add(l);
  }

  public void removeTableButtonListener( TableButtonListener l ) { 
    listener.remove(l);
  }

  @Override 
  public Component getTableCellRendererComponent(JTable table,
    Object value, boolean isSelected, boolean hasFocus, int row, int col) {
    return this;
  }

  @Override
  public Component getTableCellEditorComponent(JTable table,
      Object value, boolean isSelected, int row, int col) {
    selectedRow = row;
    selectedColumn = col;
    return this;
  } 

  @Override
  public void addCellEditorListener(CellEditorListener arg0) {      
  } 

  @Override
  public void cancelCellEditing() {
  } 

  @Override
  public Object getCellEditorValue() {
    return "";
  }

  @Override
  public boolean isCellEditable(EventObject arg0) {
    return true;
  }

  @Override
  public void removeCellEditorListener(CellEditorListener arg0) {
  }

  @Override
  public boolean shouldSelectCell(EventObject arg0) {
    return true;
  }

  @Override
  public boolean stopCellEditing() {
    return true;
  }
}
