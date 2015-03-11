package com.tur0kk.thingiverse.gui.mapping;

import com.tur0kk.thingiverse.model.ThingCollection;
import java.util.List;
import javax.swing.ComboBoxModel;
import javax.swing.event.ListDataListener;

/**
 * This list is a model for the collection combobox to map our internal CollectionList to items in a dropdown list.
 * @author Sven
 */
public class ThingCollectionComboboxModel implements ComboBoxModel<ThingCollection>
{
  private List<ThingCollection> model;
  private ThingCollection selected; // currently selected collection
  
  public ThingCollectionComboboxModel(List<ThingCollection> model){
    this.model = model;
    if(this.getSize() != 0){
      this.selected = this.getElementAt(0);
    }
    else{
      selected = null;
    }
  }

  // set currently selected item stored in this model
  public void setSelectedItem(Object anItem)
  {
    this.selected = (ThingCollection) anItem;
  }

  // returns currently selected item stored in this model
  public Object getSelectedItem()
  {
    return this.selected;
  }

  // get number of elements in this model
  public int getSize()
  {
    return this.model.size();
  }

  public void addListDataListener(ListDataListener l)
  {
  }

  public void removeListDataListener(ListDataListener l)
  {
  }

  // return specific element at certain index to be rendered in list
  public ThingCollection getElementAt(int index)
  {
    return this.model.get(index);
  }
  
}
