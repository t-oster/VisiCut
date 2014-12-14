/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.visicut.gui.mapping;

import java.util.Map;
import javax.swing.AbstractListModel;
import javax.swing.ImageIcon;

/**
 * This class displays the elements of a list by a given map
 * @param Map from which the key values should be displayed.
 * @author Sven
 */
public class MapListModel<T> extends AbstractListModel{
  Map<String, T> model = null;

  public MapListModel(Map<String, T> mapModel){
    this.model = mapModel;
  }

  public int getSize()
  {
    return this.model.keySet().size();
  }

  public Object getElementAt(int index)
  {
    return this.model.keySet().toArray()[index].toString();
  }

} 
