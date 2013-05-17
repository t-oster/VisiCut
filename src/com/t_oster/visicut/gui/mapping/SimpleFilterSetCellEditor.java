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

import com.t_oster.visicut.model.mapping.FilterSet;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;

/**
 *
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
class SimpleFilterSetCellEditor extends DefaultCellEditor
{

  private JComboBox filterSets;
  public String EVERYTHING_ELSE = java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/mapping/resources/CustomMappingPanel").getString("EVERYTHING_ELSE");

  public SimpleFilterSetCellEditor()
  {
    super(new JComboBox());
    filterSets = (JComboBox) this.getComponent();
    filterSets.setRenderer(new SimpleFilterSetListCellRenderer());
  }

  @Override
  public Object getCellEditorValue()
  {
    return EVERYTHING_ELSE.equals(super.getCellEditorValue()) ? null : super.getCellEditorValue();
  }

  public void refresh(Iterable<FilterSet> entries)
  {
    filterSets.removeAllItems();
    filterSets.addItem(new FilterSet());
    // add EVERYTHING_ELSE
    filterSets.addItem(EVERYTHING_ELSE);
    for (FilterSet e : entries)
    {
      filterSets.addItem(e);
    }
  }
}
