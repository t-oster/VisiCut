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
package com.t_oster.visicut.gui.mapping;

import com.t_oster.liblasercut.LaserProperty;
import com.t_oster.uicomponents.EditableTablePanel;
import com.t_oster.uicomponents.EditableTableProvider;
import com.t_oster.visicut.VisicutModel;
import com.t_oster.visicut.gui.EditRaster3dProfileDialog;
import com.t_oster.visicut.gui.EditRasterProfileDialog;
import com.t_oster.visicut.gui.EditVectorProfileDialog;
import com.t_oster.visicut.managers.LaserPropertyManager;
import com.t_oster.visicut.managers.ProfileManager;
import com.t_oster.visicut.misc.Helper;
import com.t_oster.visicut.model.LaserDevice;
import com.t_oster.visicut.model.LaserProfile;
import com.t_oster.visicut.model.MaterialProfile;
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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
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
    private String[] columns = new String[]{java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/mapping/resources/CustomMappingPanel").getString("ENABLED"), java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/mapping/resources/CustomMappingPanel").getString("SELECTION"), java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/mapping/resources/CustomMappingPanel").getString("PROFILE")};
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
  private boolean suppressMappingUpdate = false;
  private String lastMappingName = null;
  public boolean representMapping(MappingSet ms)
  {
    lastMappingName = ms != null ? ms.getName() : null;
    if (ms == null || ms.isEmpty())
    {
      this.generateDefaultEntries();
    }
    else
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
      //TODO: Check if endless loop
      suppressMappingUpdate = true;
      this.model.fireTableDataChanged();
      suppressMappingUpdate = false;
    }
    return true;
  }

  private boolean ignorePartUpdate = false;

  public void propertyChange(PropertyChangeEvent pce)
  {
    if (pce.getSource().equals(ProfileManager.getInstance()))
    {
      this.refreshProfilesEditor();
    }
    if (pce.getSource().equals(VisicutModel.getInstance()))
    {
      if (VisicutModel.PROP_SELECTEDPART.equals(pce.getPropertyName()))
      {
        if (VisicutModel.getInstance().getSelectedPart() != null)
        {
          this.representMapping(VisicutModel.getInstance().getSelectedPart().getMapping());
        }
      }
      else if (VisicutModel.PROP_PLF_PART_UPDATED.equals(pce.getPropertyName()) && pce.getNewValue().equals(VisicutModel.getInstance().getSelectedPart()))
      {
        if (!ignorePartUpdate)
        {
          this.representMapping(VisicutModel.getInstance().getSelectedPart().getMapping());
        }
      }
    }
  }

  public void tableChanged(TableModelEvent tme)
  {
    if (!suppressMappingUpdate)
    {
      this.refreshProfilesEditor(); // generate necessary new temporary copies
      ignorePartUpdate = true;
      VisicutModel.getInstance().getSelectedPart().setMapping(this.getResultingMappingSet());
      VisicutModel.getInstance().firePartUpdated(VisicutModel.getInstance().getSelectedPart());
      ignorePartUpdate = false;
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
    this.setEditButtonVisible(true);
    this.getTable().setDefaultEditor(FilterSet.class, filterSetEditor);
    this.refreshProfilesEditor();
    this.getTable().setDefaultRenderer(FilterSet.class, filterSetRenderer);
    this.setMoveButtonsVisible(true);
    this.setSaveButtonVisible(true);
    this.setLoadButtonVisible(true);
    ProfileManager.getInstance().addPropertyChangeListener(this);
    VisicutModel.getInstance().addPropertyChangeListener(this);
    model.addTableModelListener(this);
  }

  private void refreshProfilesEditor()
  {
    JComboBox profiles = new JComboBox();
    List<LaserProfile> addedProfilesList=new LinkedList<LaserProfile>();
    List<String> profileNamesList=new LinkedList<String>();
    // add laser profiles saved on disk
    for (LaserProfile lp : ProfileManager.getInstance().getAll())
    {
      profiles.addItem(lp);
      addedProfilesList.add(lp);
      profileNamesList.add(lp.getName());
    }
    // add all temporary copies currently in use
    for (Entry e: entries) {
      LaserProfile lp = e.profile;
      if (!addedProfilesList.contains(lp)) {
        addedProfilesList.add(lp);
        profiles.addItem(lp);
        profileNamesList.add(lp.getName());
      }
    }

    // add one unused temporary copy of each stored profile to the list of available profiles
    for (LaserProfile lp: ProfileManager.getInstance().getAll()) {
      if (lp.isTemporaryCopy()) {
        // we don't want copies of copies
        throw new RuntimeException("a temporary profile was stored on disk! WTF?");
      }
      // okay, we have a original
      LaserProfile temporaryCopy=lp.clone();
      // TODO make sure that isTemporaryCopy is considered in .equals() (and hashCode())
      temporaryCopy.setTemporaryCopy(true);

      // find the next free temp123_profilename name
      String newName="";
      int numberOfTempCopies=0;
      do {
        numberOfTempCopies++;
        newName="temp"+numberOfTempCopies+"_"+lp.getName();
      } while (profileNamesList.contains(newName));

      temporaryCopy.setName(newName);
      profiles.addItem(temporaryCopy);
      // remove previously stored laser-settings for the temporary copy from disk,
      // reset them to the ones of the original
      // (the LaserProfile does not get stored,
      // but the corresponding LaserProperty must be stored on disk, because
      // LaserPropertyManager.getLaserProperties() loads them directly from disk
      VisicutModel v = VisicutModel.getInstance();
      float thickness = v.getMaterialThickness();
      MaterialProfile material = v.getMaterial();
      LaserDevice laser = v.getSelectedLaserDevice();
      
      if (laser != null && material != null && temporaryCopy != null && lp != null)
      {
        try
        {
          // get original properties
          List<LaserProperty> originalProperties = LaserPropertyManager.getInstance().getLaserProperties(laser,material,lp,thickness);
          // set tempCopy's properties to original's
          LaserPropertyManager.getInstance().saveLaserProperties(laser, material, temporaryCopy, thickness, originalProperties);
        }
        catch (FileNotFoundException ex)
        {
        }
        catch (IOException ex)
        {
          Logger.getLogger(CustomMappingPanel.class.getName()).log(Level.SEVERE, null, ex);
        }
      }
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
        String text = java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/mapping/resources/CustomMappingPanel").getString("EVERYTHING");
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
    List<LaserProfile> profiles = ProfileManager.getInstance().getAll();
    for (LaserProfile lp : profiles)
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
    if (lastMappingName != null)
    {
      result.setName(lastMappingName);
    }
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
