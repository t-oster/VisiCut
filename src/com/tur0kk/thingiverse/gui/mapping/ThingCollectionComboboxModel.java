/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tur0kk.thingiverse.gui.mapping;

import com.tur0kk.thingiverse.model.ThingCollection;
import java.util.List;
import javax.swing.ComboBoxModel;
import javax.swing.event.ListDataListener;

/**
 *
 * @author Sven
 */
public class ThingCollectionComboboxModel implements ComboBoxModel<ThingCollection>
{
  private List<ThingCollection> model;
  private ThingCollection selected;
  
  public ThingCollectionComboboxModel(List<ThingCollection> model){
    this.model = model;
    if(this.getSize() != 0){
      this.selected = this.getElementAt(0);
    }
    else{
      selected = null;
    }
  }

  public void setSelectedItem(Object anItem)
  {
    this.selected = (ThingCollection) anItem;
  }

  public Object getSelectedItem()
  {
    return this.selected;
  }

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

  public ThingCollection getElementAt(int index)
  {
    return this.model.get(index);
  }
  
}
