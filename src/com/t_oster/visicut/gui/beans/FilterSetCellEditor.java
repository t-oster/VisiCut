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
package com.t_oster.visicut.gui.beans;

import com.t_oster.visicut.VisicutModel;
import com.t_oster.visicut.misc.Helper;
import com.t_oster.visicut.model.PlfFile;
import com.t_oster.visicut.model.PlfPart;
import com.t_oster.visicut.model.graphicelements.GraphicSet;
import com.t_oster.visicut.model.mapping.FilterSet;
import com.t_oster.visicut.model.mapping.MappingFilter;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.LinkedList;
import java.util.List;
import java.util.MissingResourceException;
import javax.swing.AbstractCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.table.TableCellEditor;

/**
 *
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class FilterSetCellEditor extends AbstractCellEditor implements TableCellEditor, ActionListener, PropertyChangeListener, PopupMenuListener
{

  private List<FilterMenuItem> menuItems = new LinkedList<FilterMenuItem>();
  private List<JMenu> menus = new LinkedList<JMenu>();

  private boolean containsFilter(MappingFilter filter)
  {
    return this.resultingFilterSet.contains(filter);
  }

  public void propertyChange(PropertyChangeEvent pce)
  {
    if (pce.getSource() == VisicutModel.getInstance() && pce.getPropertyName().equals(VisicutModel.PROP_SELECTEDPART))
    {
      this.fillMenu(VisicutModel.getInstance().getSelectedPart());
    }
  }

  public void popupMenuWillBecomeVisible(PopupMenuEvent pme)
  {
    //throw new UnsupportedOperationException("Not supported yet.");
  }

  public void popupMenuWillBecomeInvisible(PopupMenuEvent pme)
  {
    //throw new UnsupportedOperationException("Not supported yet.");
  }

  public void popupMenuCanceled(PopupMenuEvent pme)
  {
    this.fireEditingCanceled();
  }

  boolean canRepresent(FilterSet filterSet)
  {
    for (MappingFilter f : filterSet)
    {
      if (f.isInverted())
      {
        return false;
      }
    }
    return true;
  }

  public static String translateAttVal(String attribute)
  {
    try
    {
      return java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/beans/resources/FilterSetCellEditor").getString(attribute.toUpperCase());
    }
    catch (MissingResourceException ex)
    {
      return attribute;
    }
  }

  private void enforceMultiSelectState()
  {
    // if multiSelect is not enabled, only one selection may be chosen. This is enforced by removing everything but the last filter
    if (!multiselect.isSelected()) {
      while (FilterSetCellEditor.this.resultingFilterSet.size() > 1) {
        FilterSetCellEditor.this.resultingFilterSet.removeFirst();
      }
    }
  }

  class FilterMenuItem extends JCheckBoxMenuItem implements ActionListener
  {
    MappingFilter filter = null;
    JMenu parent = null;
    public FilterMenuItem(String attribute, Object value, JMenu parent)
    {
      if (value instanceof Color)
      {
        String color = Helper.toHtmlRGB((Color) value);
        this.setText("<html><table><tr><td border=1 bgcolor=" + color + ">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</td><td>"+color+"</tr></table></html>");
      }
      else if (value != null)
      {
        this.setText(translateAttVal(value.toString()));
      }
      filter = new MappingFilter(attribute, value);
      this.parent = parent;
      this.addActionListener(this);
    }

    public void refreshState()
    {
      if (FilterSetCellEditor.this.containsFilter(filter))
      {
        String txt = parent.getText();
        if (!txt.startsWith(">"))
        {
          parent.setText(">"+txt);
        }
        this.setSelected(true);
      }
      else
      {
        this.setSelected(false);
      }
    }

    public void actionPerformed(ActionEvent ae)
    {
      if (this.isSelected())
      {
        FilterSetCellEditor.this.addFilter(filter);
      }
      else
      {
        FilterSetCellEditor.this.removeFilter(filter);
      }
    }
  }

  JButton bt = new JButton(java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/beans/resources/FilterSetCellEditor").getString("SELECT"));
  JPopupMenu menu = new JPopupMenu(java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/beans/resources/FilterSetCellEditor").getString("MENU"));
  FilterSet resultingFilterSet = new FilterSet();
  /// multiselect: if this is enabled, the user can select more than one item, see enforceMultiSelectState()
  JCheckBoxMenuItem multiselect = new JCheckBoxMenuItem(java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/beans/resources/FilterSetCellEditor").getString("MULTISELECT"));

  private void fillMenu(PlfPart p)
  {
    menu.removeAll();
    this.menuItems.clear();
    JMenuItem everything = new JMenuItem(java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/beans/resources/FilterSetCellEditor").getString("EVERYTHING"));
    everything.addActionListener(new ActionListener(){

      public void actionPerformed(ActionEvent ae)
      {
        FilterSetCellEditor.this.clearFilters();
      }
    });

    if (p != null)
    {
      GraphicSet gs = p.getGraphicObjects();
      menu.add(everything);
      for(final String s: gs.getAttributes())
      {
        JMenu m = new JMenu(translateAttVal(s));
        for(final Object o: gs.getAttributeValues(s))
        {
          FilterMenuItem mi = new FilterMenuItem(s, o, m);
          menuItems.add(mi);
          m.add(mi);
        }
        menu.add(m);
        menus.add(m);
      }
      menu.add(multiselect);
    }
    
  }

  private void addFilter(MappingFilter f)
  {
    this.resultingFilterSet.add(f);
    this.enforceMultiSelectState();
    this.fireEditingStopped();
  }

  private void removeFilter(MappingFilter f)
  {
    this.resultingFilterSet.remove(f);
    this.fireEditingStopped();
  }


  /**
   * this should be called on all "external" changes to resultingFilterSet
   * (e.g. when selecting a different PlfPart)
   */
  private void prepareMenu()
  {
    for (JMenu m : this.menus)
    {
      String txt = m.getText();
      if (txt.startsWith(">"))
      {
        m.setText(txt.substring(1));
      }
    }
    for (FilterMenuItem i : this.menuItems)
    {
      i.refreshState();
    }
    multiselect.setSelected(resultingFilterSet.multiselectEnabled);   
  }

  public FilterSetCellEditor()
  {
    this.fillMenu(VisicutModel.getInstance().getSelectedPart());
    bt.addActionListener(this);
    VisicutModel.getInstance().addPropertyChangeListener(this);
    menu.addPopupMenuListener(this);
    multiselect.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e)
      {
        resultingFilterSet.multiselectEnabled=multiselect.isSelected();
        enforceMultiSelectState();
        fireEditingStopped();
      }
    });
  }


  public Object getCellEditorValue()
  {
    return resultingFilterSet;
  }

  public Component getTableCellEditorComponent(JTable jtable, Object o, boolean bln, int i, int i1)
  {
    this.resultingFilterSet = ((FilterSet) o).clone();
    if (resultingFilterSet.size() > 1) {
      // backwards compatibility: if the filter set uses a multiple selection, enable multiselect
      // otherwise all items exept for the last one would be removed
      resultingFilterSet.multiselectEnabled = true;
    } 
    bt.setText(((FilterSet) o).isEmpty() ? java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/beans/resources/FilterSetCellEditor").getString("EVERYTHING") : java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/beans/resources/FilterSetCellEditor").getString("CUSTOM"));
    this.prepareMenu();
    return bt;
  }

  public void actionPerformed(ActionEvent ae)
  {
    menu.show(bt, bt.getX(), bt.getY());
  }

  private void clearFilters()
  {
    this.resultingFilterSet.clear();
    this.fireEditingStopped();
  }

}
