package com.tur0kk.thingiverse.gui.mapping;

import com.tur0kk.thingiverse.model.Thing;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.util.LinkedList;
import java.util.Map;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
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

      Thing aThing = (Thing) value;
      JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
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
