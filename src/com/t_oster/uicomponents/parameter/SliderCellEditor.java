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

import java.awt.Component;
import javax.swing.AbstractCellEditor;
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.TableCellEditor;

/**
 *
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class SliderCellEditor extends AbstractCellEditor implements TableCellEditor
{

  private JSlider slider;
  
  public SliderCellEditor()
  {
    slider = new JSlider();
    slider.addChangeListener(new ChangeListener()
    {
      public void stateChanged(ChangeEvent ce)
      {
        if (!slider.getValueIsAdjusting())
        {
          fireEditingStopped();
        }
      }
    });
  }
  
  public SliderCellEditor(int min, int max)
  {
    this();
    slider.setMaximum(max);
    slider.setMinimum(min);
  }
  
  public Object getCellEditorValue()
  {
    return slider.getValue();
  }

  public Component getTableCellEditorComponent(JTable jtable, Object o, boolean bln, int i, int i1)
  {
    if (o instanceof Integer)
    {
      slider.setValue((Integer) o);
      
    }
    return slider;
  }
  
}
