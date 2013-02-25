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

import com.t_oster.visicut.misc.Helper;
import com.t_oster.visicut.model.graphicelements.GraphicSet;
import com.t_oster.visicut.model.mapping.FilterSet;
import com.t_oster.visicut.model.mapping.MappingFilter;
import java.awt.Color;
import java.awt.Component;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

/**
 *
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class SimpleFilterSetCellRenderer extends DefaultTableCellRenderer
{

  static Component updateComponent(Component c, Object value)
  {
    if (c instanceof JLabel)
    {
      if (value == null)
      {
        String text = java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/mapping/resources/CustomMappingPanel").getString("EVERYTHING_ELSE");
        ((JLabel) c).setText(text);
      }
      else if (value instanceof FilterSet)
      {
        String text = "";
        if (((FilterSet) value).isEmpty())
        {
          text = java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/mapping/resources/CustomMappingPanel").getString("EVERYTHING");
        }
        else
        {
          MappingFilter f = ((FilterSet) value).getFirst();
          if (f.getValue() instanceof Color)
          {
            String color = Helper.toHtmlRGB((Color) f.getValue());
            text = "<html><table><tr><td border=1 bgcolor=" + color + ">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</td></tr></table></html>";
          }
          else if (f.getValue() != null)
          {
            text = text + " " + GraphicSet.translateAttVal(f.getValue().toString());
          }
        }
        ((JLabel) c).setText(text);
      }
    }
    return c;
  }
  
  @Override
  public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
  {
    return updateComponent(super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column), value);
  }
}
