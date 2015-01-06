/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tur0kk.thingiverse.gui.mapping;

import com.tur0kk.thingiverse.model.ThingFile;
import java.awt.Component;
import java.awt.Font;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;

/**
 *
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
      
      JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
      
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
