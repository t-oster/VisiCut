/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tur0kk.thingiverse.gui.mapping;

import com.tur0kk.thingiverse.model.ThingCollection;
import java.awt.Component;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

/**
 *
 * @author Sven
 */
public class ThingCollectionComboboxRenderer extends DefaultListCellRenderer
{
  @Override
  public Component getListCellRendererComponent(
          JList list, Object value, int index,
          boolean isSelected, boolean cellHasFocus) {
      
      JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

      // default behavior: show name of collection
      ThingCollection aCollection = (ThingCollection) value;
      if(aCollection == null){ // no collections available in model of combobox
        label.setText("");
      }
      else{
        label.setText(aCollection.getName());
      }
      return label;
  }
}
