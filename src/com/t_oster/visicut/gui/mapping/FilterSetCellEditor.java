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

import com.t_oster.visicut.VisicutModel;
import com.t_oster.visicut.model.mapping.FilterSet;
import com.t_oster.visicut.model.mapping.Mapping;
import java.awt.Component;
import java.util.LinkedList;
import java.util.ResourceBundle;
import javax.swing.AbstractCellEditor;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;

/**
 *
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class FilterSetCellEditor extends AbstractCellEditor implements TableCellEditor
{

  private ResourceBundle bundle = ResourceBundle.getBundle("com/t_oster/visicut/gui/mapping/resources/FilterSetCellEditor");
  private FilterSet value;
  private FilterSetTreePanel panel;
  
  public FilterSetCellEditor()
  {
    panel = new FilterSetTreePanel();
  }
  
  private boolean prepareTree(FilterSet fs)
  {
    if (VisicutModel.getInstance().getSelectedPart() == null || VisicutModel.getInstance().getSelectedPart().getGraphicObjects() == null)
    {
      return false;
    }
    MappingJTree tree = panel.getMappingTree();
    tree.setGraphicObjects(VisicutModel.getInstance().getSelectedPart().getGraphicObjects());
    tree.setMappings(new LinkedList<Mapping>());
    tree.representFilterSet(fs);
    return true;
  }
  
  public Object getCellEditorValue()
  {
    return value;
  }

  private boolean running = false;
  
  public Component getTableCellEditorComponent(JTable jtable, Object o, boolean bln, int i, int i1)
  { 
    value = (FilterSet) o;
    if (!running && prepareTree(value))
    {
      running = true;
      new Thread(){
        @Override
        public void run()
        {
          if (JOptionPane.showConfirmDialog(null, panel, bundle.getString("SELECT_A_FILTER"), JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION)
          {
            value = panel.getMappingTree().getSelectedFilterSet();
            FilterSetCellEditor.this.fireEditingStopped();
          }
          else
          {
            FilterSetCellEditor.this.fireEditingCanceled();
          }
          FilterSetCellEditor.this.running = false;
        }
      }.start();
    }
    return new JLabel(bundle.getString("EDITING"));
  }

}
