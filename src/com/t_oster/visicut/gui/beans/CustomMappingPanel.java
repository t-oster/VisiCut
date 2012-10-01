package com.t_oster.visicut.gui.beans;

import com.t_oster.visicut.VisicutModel;
import com.t_oster.visicut.managers.ProfileManager;
import com.t_oster.visicut.model.LaserProfile;
import com.t_oster.visicut.model.VectorProfile;
import com.t_oster.visicut.model.mapping.FilterSet;
import com.t_oster.visicut.model.mapping.Mapping;
import com.t_oster.visicut.model.mapping.MappingSet;
import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class CustomMappingPanel extends EditableTablePanel implements EditableTableProvider, PropertyChangeListener
{
  private DefaultTableModel model = new DefaultTableModel()
  {
    
    private String[] columns = new String[]{java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/beans/resources/CustomMappingPanel").getString("ENABLED"), java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/beans/resources/CustomMappingPanel").getString("SELECTION"), java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/beans/resources/CustomMappingPanel").getString("PROFILE")};
    private Class[] classes = new Class[]{Boolean.class, FilterSet.class, LaserProfile.class};

    @Override
    public int getColumnCount()
    {
      return columns.length;
    }

    @Override
    public String getColumnName(int column)
    {
      return columns[column];
    }

    @Override
    public int getRowCount()
    {
      return entries == null ? 0 : entries.size();
    }

    @Override
    public Object getValueAt(int row, int column)
    {
      Entry e = entries.get(row);
      switch (column)
      {
        case 0: return (Boolean) e.enabled;
        case 1: return e.filterSet;
        case 2: return e.profile;
        default: return null;
      }
    }

    @Override
    public boolean isCellEditable(int row, int column)
    {
      return true;
    }

    @Override
    public void setValueAt(Object aValue, int row, int column)
    {
      Entry e = entries.get(row);
      boolean enabledOrChange = e.enabled;
      switch (column)
      {
        case 0: 
        {
          e.enabled = (Boolean) aValue;
          enabledOrChange = true;
          break;
        }
        case 1:
        {
          e.filterSet = (FilterSet) aValue;
          break;
        }
        case 2:
        {
          e.profile = (LaserProfile) aValue;
          break;
        }
      }
      if (enabledOrChange)
      {
        VisicutModel.getInstance().setMappings(CustomMappingPanel.this.getResultingMappingSet());
      }
    }

    @Override
    public Class<?> getColumnClass(int columnIndex)
    {
      return classes[columnIndex];
    }
    
    @Override
    public void removeRow(int row)
    {
      
    }
  };

  public Object getNewInstance()
  {
    Entry e = new Entry();
    e.enabled = true;
    e.filterSet = new FilterSet();
    e.profile = ProfileManager.getInstance().getAll().isEmpty() ? null : ProfileManager.getInstance().getAll().get(0);
    return e;
  }

  public Object editObject(Object o)
  {
    return o;
  }

  /*
   * Tries to represent a MappingSet and returns true, 
   * if it is completely representable.
   * Otherwise the content of the Panel is not modified
   */
  public boolean representMapping(MappingSet ms)
  {
    List<Entry> result = new LinkedList<Entry>();
    for (Mapping m : ms)
    {
      Entry e = new Entry();
      e.enabled = true;
      e.filterSet = m.getFilterSet();
      if (!this.filterSetEditor.canRepresent(e.filterSet))
      {
        return false;
      }
      e.profile = ProfileManager.getInstance().getProfileByName(m.getProfileName());
      result.add(e);
    }
    this.entries.clear();
    this.entries.addAll(result);
    this.model.fireTableDataChanged();
    return true;
  }
  
  public void propertyChange(PropertyChangeEvent pce)
  {
    if (pce.getSource() == ProfileManager.getInstance())
    {
      this.checkProfiles();
      this.refreshProfilesEditor();
    }
  }

  /**
   * checks for every row if the contained laser-profile
   * is still existing
   */
  private void checkProfiles()
  {
    for (int row = this.entries.size()-1; row >= 0 ; row--)
    {
      LaserProfile lp = this.entries.get(row).profile;
      if (ProfileManager.getInstance().getProfileByName(lp.getName()) == null)
      {
        this.entries.remove(row);
        this.model.fireTableRowsDeleted(row, row);
      }
    }
  }
  
  class Entry
  {
    boolean enabled = true;
    FilterSet filterSet = new FilterSet();
    LaserProfile profile = null;
  }
  
  private final List<Entry> entries = new LinkedList<Entry>();
  
  public CustomMappingPanel()
  {
    this.generateDefaultEntries();
    this.setTableModel(model);
    this.setProvider(this);
    this.setObjects((List) entries);
    this.setEditButtonVisible(false);
    this.getTable().setDefaultEditor(FilterSet.class, filterSetEditor);
    this.refreshProfilesEditor();
    this.getTable().setDefaultRenderer(FilterSet.class, filterSetRenderer);
    this.setMoveButtonsVisible(true);
    ProfileManager.getInstance().addPropertyChangeListener(this);
  }
  
  private void refreshProfilesEditor()
  {
    JComboBox profiles = new JComboBox();
    for (LaserProfile lp : ProfileManager.getInstance().getAll())
    {
      profiles.addItem(lp);
    }
    this.getTable().setDefaultEditor(LaserProfile.class, new DefaultCellEditor(profiles));
  }
  
  private FilterSetCellEditor filterSetEditor = new FilterSetCellEditor();
  
  private DefaultTableCellRenderer filterSetRenderer = new DefaultTableCellRenderer()
  {

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
    {
      Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
      if (c instanceof JLabel && value instanceof FilterSet)
      {
        ((JLabel) c).setText(((FilterSet) value).isEmpty() ? java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/beans/resources/CustomMappingPanel").getString("EVERYTHING") : java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/beans/resources/CustomMappingPanel").getString("CUSTOM"));
      }
      return c;
    }
    
  };
  
  private void generateDefaultEntries()
  {
    entries.clear();
    for (LaserProfile lp : ProfileManager.getInstance().getAll())
    {
      Entry e = new Entry();
      e.enabled = false;
      e.filterSet = new FilterSet();
      e.profile = lp;
      entries.add(e);
    }
    Collections.sort(entries, new Comparator<Entry>(){

      private boolean isCut(Entry e)
      {
        return e.profile instanceof VectorProfile && ((VectorProfile) e.profile).isIsCut();
      }
      
      public int compare(Entry t, Entry t1)
      {
        if (isCut(t) || !isCut(t1))
        {
          return 1;
        }
        else if (isCut(t1) || !isCut(t))
        {
          return -1;
        }
        else
        {
          return t.profile.getName().compareToIgnoreCase(t1.profile.getName());
        }
      }
    });
  }
  
  public MappingSet getResultingMappingSet()
  {
    MappingSet result = new MappingSet();
    for(Entry e:entries)
    {
      if (e.enabled)
      {
        Mapping m = new Mapping();
        m.setFilterSet(e.filterSet);
        m.setProfileName(e.profile.getName());
        result.add(m);
      }
    }
    return result;
  }
}
