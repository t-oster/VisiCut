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
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

/**
 *
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class SliderCellRenderer extends DefaultTableCellRenderer
{
  private JSlider slider;
  
  public SliderCellRenderer()
  {
    slider = new JSlider();
  }
  
  public SliderCellRenderer(int min, int max)
  {
    this();
    slider.setMaximum(max);
    slider.setMinimum(min);
  }

  @Override
  public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
  {
    if (value instanceof Integer)
    {
      slider.setValue((Integer) value);
      return slider;
    }
    return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
  }
  
}
