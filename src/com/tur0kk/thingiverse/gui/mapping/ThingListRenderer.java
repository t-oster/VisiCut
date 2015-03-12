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
package com.tur0kk.thingiverse.gui.mapping;

import com.tur0kk.thingiverse.model.Thing;
import java.awt.Component;
import java.awt.Font;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;

/**
 * This class is for rendering images at the left side of each list element.
 * @param Map<String, ImageIcon> is a mapping of String label of list elements to images to display.
 * Suggestion would be to use the same map for this Renderer and the List and create the list with List(map.keySet().toArray()) 
 * or even better use the corresponding MapListModel.
 * @author Sven
 */
public class ThingListRenderer extends DefaultListCellRenderer {

  Font font = new Font("helvitica", Font.BOLD, 24);

  public ThingListRenderer(){
    
  }

  @Override
  public Component getListCellRendererComponent(
          JList list, Object value, int index,
          boolean isSelected, boolean cellHasFocus) {

      Thing aThing = (Thing) value; // list of listmodel to render in this list item
      JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus); // label of list item
      
      // design list item
      label.setText(aThing.getName());
      label.setIcon(aThing.getImage());
      label.setHorizontalAlignment(JLabel.LEFT);
      label.setFont(font);
      return label;
  }

  @Override
  public void setFont(Font font){
    this.font = font;
  }
}
