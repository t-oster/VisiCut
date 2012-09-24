/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.visicut.gui.beans;

import com.t_oster.visicut.VisicutModel;
import com.t_oster.visicut.misc.Helper;
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
    if (pce.getSource() == VisicutModel.getInstance() && pce.getPropertyName().equals(VisicutModel.PROP_GRAPHICOBJECTS))
    {
      this.fillMenu(VisicutModel.getInstance().getGraphicObjects());
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
  
  class FilterMenuItem extends JCheckBoxMenuItem implements ActionListener
  {
    MappingFilter filter = null;
    JMenu parent = null;
    public FilterMenuItem(String attribute, Object value, JMenu parent)
    {
      super(value == null ? "null" : value.toString());
      if (value instanceof Color)
      {
        this.setText(Helper.toHtmlRGB((Color) value));
        this.setForeground((Color) value);
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
  
  JButton bt = new JButton("Select");
  JPopupMenu menu = new JPopupMenu("Menu");
  FilterSet resultingFilterSet = new FilterSet();
  
  private void fillMenu(GraphicSet gs)
  {
    menu.removeAll();
    this.menuItems.clear();
    JMenuItem e = new JMenuItem("Everything");
    e.addActionListener(new ActionListener(){

      public void actionPerformed(ActionEvent ae)
      {
        FilterSetCellEditor.this.clearFilters();
      }
    });
    if (gs != null)
    {
      menu.add(e);
      for(final String s: gs.getAttributes())
      {
        JMenu m = new JMenu(s);
        for(final Object o: gs.getAttributeValues(s))
        {
          FilterMenuItem mi = new FilterMenuItem(s, o, m);
          menuItems.add(mi);
          m.add(mi);
        }
        menu.add(m);
        menus.add(m);
      }
    }
  }

  private void addFilter(MappingFilter f)
  {
    this.resultingFilterSet.add(f);
    this.fireEditingStopped();
  }
  
  private void removeFilter(MappingFilter f)
  {
    this.resultingFilterSet.remove(f);
    this.fireEditingStopped();
  }
  
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
  }
  
  public FilterSetCellEditor()
  {
    this.fillMenu(VisicutModel.getInstance().getGraphicObjects());
    bt.addActionListener(this);
    VisicutModel.getInstance().addPropertyChangeListener(this);
    menu.addPopupMenuListener(this);
  }
  
  
  public Object getCellEditorValue()
  {
    return resultingFilterSet;
  }

  public Component getTableCellEditorComponent(JTable jtable, Object o, boolean bln, int i, int i1)
  {
    this.resultingFilterSet = (FilterSet) o;
    bt.setText(((FilterSet) o).isEmpty() ? "Everything" : "Custom");
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
