package com.t_oster.visicut.gui.mapping;

import java.awt.Component;
import java.awt.Font;
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
public class ImageListRenderer extends DefaultListCellRenderer {

  Font font = new Font("helvitica", Font.BOLD, 24);
  Map<String, ImageIcon> model = null;

  public ImageListRenderer(Map<String, ImageIcon> mapModel){
    this.model = mapModel;
  }

  @Override
  public Component getListCellRendererComponent(
          JList list, Object value, int index,
          boolean isSelected, boolean cellHasFocus) {

      setIcon(model.get((String) value));
      setHorizontalTextPosition(JLabel.RIGHT);
      setFont(font);
      return this;
  }

  @Override
  public void setFont(Font font){
    this.font = font;
  }
}
