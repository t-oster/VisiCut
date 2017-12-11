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
