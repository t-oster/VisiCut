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

import com.tur0kk.thingiverse.model.ThingFile;
import java.awt.Component;
import java.awt.Font;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;

/**
 * This file defines how the ThingFileLists look like (right panel), image + thing file name
 * @author Sven
 */
public class ThingFileListRenderer extends DefaultListCellRenderer
{
  Font font = new Font("helvitica", Font.BOLD, 24);
  
  public ThingFileListRenderer(){
    
  }

  @Override
  public Component getListCellRendererComponent(
          JList list, Object value, int index,
          boolean isSelected, boolean cellHasFocus) {
      
      JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus); // get the label of the item in list
      
      // in case of error message (no SVGs found)
      if(value instanceof String){
            label.setText((String) value);
            super.getListCellRendererComponent(list, value, index, false, false); // disable selectable
            return label;
      }
      
      // default behavior: show image and name of thing
      ThingFile aFile = (ThingFile) value;
      label.setText(aFile.getName());
      label.setIcon(aFile.getThumbnail());
      label.setHorizontalAlignment(JLabel.LEFT);
      label.setFont(font);
      return label;
  }

  @Override
  public void setFont(Font font){
    this.font = font;
  }
}
