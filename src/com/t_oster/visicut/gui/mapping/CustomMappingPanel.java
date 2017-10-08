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

import com.t_oster.uicomponents.BetterJTable;
import com.t_oster.uicomponents.EditableTablePanel;
import com.t_oster.uicomponents.EditableTableProvider;
import com.t_oster.visicut.VisicutModel;
import com.t_oster.visicut.gui.MainView;
import com.t_oster.visicut.managers.ProfileManager;
import com.t_oster.visicut.model.LaserProfile;
import com.t_oster.visicut.model.VectorProfile;
import com.t_oster.visicut.model.mapping.FilterSet;
import com.t_oster.visicut.model.mapping.Mapping;
import com.t_oster.visicut.model.mapping.MappingSet;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

/**
 *
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class CustomMappingPanel extends EditableTablePanel implements EditableTableProvider, TableModelListener, PropertyChangeListener
{
  

  public Object getNewInstance()
  {
    MappingTableEntry e = new MappingTableEntry();
    e.enabled = false;
    e.filterSet = new FilterSet();
    e.profile = ProfileManager.getInstance().getAll().isEmpty() ? null : ProfileManager.getInstance().getAll().get(0);
    return e;
  }

  public Object editObject(Object o)
  {
    if (o instanceof MappingTableEntry)
    {
      MappingTableEntry e = (MappingTableEntry) o;
      if (e.profile == null)
      {
        return o;
      }
      LaserProfile p = MainView.getInstance().editLaserProfile(e.profile);
      if (p != null)
      {
        e.profile = p;
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
      List<MappingTableEntry> result = new LinkedList<MappingTableEntry>();
      for (Mapping m : ms)
      {
        MappingTableEntry e = new MappingTableEntry();
        e.enabled = true;
        e.filterSet = m.getFilterSet();
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
      this.refreshProfilesEditor();
      ignorePartUpdate = true;
      VisicutModel.getInstance().getSelectedPart().setMapping(this.getResultingMappingSet());
      VisicutModel.getInstance().firePartUpdated(VisicutModel.getInstance().getSelectedPart());
      ignorePartUpdate = false;
    }
  }

  private final List<MappingTableEntry> entries = new LinkedList<MappingTableEntry>();

  private MappingTableModel model;
  
  public CustomMappingPanel()
  {
    model = new MappingTableModel(entries);
    this.generateDefaultEntries();
    this.setTableModel(model);
    this.setProvider(this);
    this.setObjects((List) entries);
    this.setEditButtonVisible(true);
    this.getTable().setDefaultEditor(FilterSet.class, filterSetEditor);
    this.refreshProfilesEditor();
    this.getTable().setDefaultRenderer(FilterSet.class, new FilterSetCellRenderer());
    this.getTable().setDefaultRenderer(LaserProfile.class, new ProfileCellRenderer());
    this.getTable().setDefaultEditor(LaserProfile.class, profileEditor);
    this.setMoveButtonsVisible(true);
    this.setSaveButtonVisible(true);
    this.setLoadButtonVisible(true);
    ProfileManager.getInstance().addPropertyChangeListener(this);
    VisicutModel.getInstance().addPropertyChangeListener(this);
    model.addTableModelListener(this);
    ((BetterJTable) this.getTable()).setColumnRelations(new int[]{6,16,9});
  }

  private void refreshProfilesEditor()
  {
    profileEditor.refresh(entries);
  }

  private FilterSetCellEditor filterSetEditor = new FilterSetCellEditor();
  private ProfileCellEditor profileEditor = new ProfileCellEditor();

  private void generateDefaultEntries()
  {
    entries.clear();
    List<LaserProfile> profiles = ProfileManager.getInstance().getAll();
    for (LaserProfile lp : profiles)
    {
      MappingTableEntry e = new MappingTableEntry();
      e.enabled = false;
      e.filterSet = new FilterSet();
      e.profile = lp;
      entries.add(e);
    }
    Collections.sort(entries, new Comparator<MappingTableEntry>(){

      private boolean isCut(MappingTableEntry e)
      {
        return e.profile instanceof VectorProfile && ((VectorProfile) e.profile).isIsCut();
      }

      public int compare(MappingTableEntry t, MappingTableEntry t1)
      {
        if (t.profile == null)
        {
          return -1;
        }
        else if (t1.profile == null)
        {
          return 1;
        }
        else if (isCut(t) || !isCut(t1))
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
    for(MappingTableEntry e:entries)
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
