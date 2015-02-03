package com.tur0kk.thingiverse.gui.mapping;

import com.tur0kk.thingiverse.model.ThingCollection;
import java.awt.Component;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;

/**
 * This class defines how the collection combobox design looks like.
 * It is used to render the objects of the ComoboboxModel, which are ThingCollections
 * @author Sven
 */
public class ThingCollectionComboboxRenderer extends DefaultListCellRenderer
{
  @Override
  public Component getListCellRendererComponent(
          JList list, Object value, int index,
          boolean isSelected, boolean cellHasFocus) {
      
      // label in list
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
