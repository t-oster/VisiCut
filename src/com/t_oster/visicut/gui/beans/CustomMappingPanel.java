package com.t_oster.visicut.gui.beans;

import com.t_oster.visicut.VisicutModel;
import com.t_oster.visicut.gui.EditRaster3dProfileDialog;
import com.t_oster.visicut.gui.EditRasterProfileDialog;
import com.t_oster.visicut.gui.EditVectorProfileDialog;
import com.t_oster.visicut.managers.ProfileManager;
import com.t_oster.visicut.misc.Helper;
import com.t_oster.visicut.model.LaserProfile;
import com.t_oster.visicut.model.Raster3dProfile;
import com.t_oster.visicut.model.RasterProfile;
import com.t_oster.visicut.model.VectorProfile;
import com.t_oster.visicut.model.mapping.FilterSet;
import com.t_oster.visicut.model.mapping.Mapping;
import com.t_oster.visicut.model.mapping.MappingFilter;
import com.t_oster.visicut.model.mapping.MappingSet;
import java.awt.Color;
import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.MissingResourceException;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class CustomMappingPanel extends EditableTablePanel implements EditableTableProvider, TableModelListener, PropertyChangeListener
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
      switch (column)
      {
        case 0:
        {
          if (e.enabled == (Boolean) aValue)
          {
            return;
          }
          e.enabled = (Boolean) aValue;
          break;
        }
        case 1:
        {
          e.filterSet = (FilterSet) aValue;
          e.enabled = true;
          break;
        }
        case 2:
        {
          if (e.profile.getName().equals(((LaserProfile) aValue).getName()))
          {
            return;
          }
          e.profile = ((LaserProfile) aValue).clone();
          e.enabled = true;
          break;
        }
      }
      this.fireTableRowsUpdated(row, row);
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
    if (o instanceof Entry)
    {
      Entry e = (Entry) o;
      //edit laserprofile
      if (e.profile instanceof VectorProfile)
      {
        EditVectorProfileDialog d = new EditVectorProfileDialog(null, true);
        d.setVectorProfile((VectorProfile) ((VectorProfile) e.profile).clone());
        d.setOnlyEditParameters(true);
        d.setVisible(true);
        if (d.isOkPressed())
        {
          e.profile = d.getVectorProfile();
        }
      }
      else if (e.profile instanceof RasterProfile)
      {
        EditRasterProfileDialog d = new EditRasterProfileDialog(null, true);
        d.setRasterProfile((RasterProfile) e.profile);
        d.setOnlyEditParameters(true);
        d.setVisible(true);
        if (d.getRasterProfile() != null)
        {
          e.profile = d.getRasterProfile();
        }
      }
      else if (e.profile instanceof Raster3dProfile)
      {
        EditRaster3dProfileDialog d = new EditRaster3dProfileDialog(null, true);
        d.setRasterProfile((Raster3dProfile) e.profile);
        d.setOnlyEditParameters(true);
        d.setVisible(true);
        if (d.getRasterProfile() != null)
        {
          e.profile = d.getRasterProfile();
        }
      }
    }
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
      e.profile = m.getProfile();
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
      this.refreshProfilesEditor();
    }
  }

  public void tableChanged(TableModelEvent tme)
  {
    VisicutModel.getInstance().setMappings(this.getResultingMappingSet());
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
    this.setEditButtonVisible(true);
    this.getTable().setDefaultEditor(FilterSet.class, filterSetEditor);
    this.refreshProfilesEditor();
    this.getTable().setDefaultRenderer(FilterSet.class, filterSetRenderer);
    this.setMoveButtonsVisible(true);
    this.setSaveButtonVisible(true);
    ProfileManager.getInstance().addPropertyChangeListener(this);
    model.addTableModelListener(this);
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
        String text = java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/beans/resources/CustomMappingPanel").getString("EVERYTHING");
        if (!((FilterSet) value).isEmpty())
        {
          MappingFilter f = ((FilterSet) value).getFirst();
          text = FilterSetCellEditor.translateAttVal(f.getAttribute());
          String dots = ((FilterSet) value).size() > 1 ? "..." : "";
          if (f.getValue() instanceof Color)
          {
            String color = Helper.toHtmlRGB((Color) f.getValue());
            text = "<html><table><tr><td>"+text+" "+color+"<td border=1 bgcolor=" + color + ">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</td><td>"+dots+"</td></tr></table></html>";
          }
          else if (f.getValue() != null)
          {
            text = text + " "+FilterSetCellEditor.translateAttVal(f.getValue().toString())+dots;
          }
        }
        ((JLabel) c).setText(text);
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
        m.setProfile(e.profile);
        result.add(m);
      }
    }
    return result;
  }
}
