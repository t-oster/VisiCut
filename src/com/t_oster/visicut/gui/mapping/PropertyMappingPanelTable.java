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

import com.t_oster.liblasercut.platform.Util;
import com.t_oster.uicomponents.BetterJTable;
import com.t_oster.uicomponents.EditableTablePanel;
import com.t_oster.uicomponents.EditableTableProvider;
import com.t_oster.visicut.VisicutModel;
import com.t_oster.visicut.gui.EditRaster3dProfileDialog;
import com.t_oster.visicut.gui.EditRasterProfileDialog;
import com.t_oster.visicut.gui.EditVectorProfileDialog;
import com.t_oster.visicut.managers.ProfileManager;
import com.t_oster.visicut.model.LaserProfile;
import com.t_oster.visicut.model.Raster3dProfile;
import com.t_oster.visicut.model.RasterProfile;
import com.t_oster.visicut.model.VectorProfile;
import com.t_oster.visicut.model.graphicelements.GraphicSet;
import com.t_oster.visicut.model.mapping.FilterSet;
import com.t_oster.visicut.model.mapping.Mapping;
import com.t_oster.visicut.model.mapping.MappingFilter;
import com.t_oster.visicut.model.mapping.MappingSet;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.LinkedList;
import java.util.List;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

/**
 *
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class PropertyMappingPanelTable extends EditableTablePanel implements EditableTableProvider, TableModelListener, PropertyChangeListener
{
  private VisicutModel vm = VisicutModel.getInstance();
  
  private MappingTableModel model;

  private final ProfileCellEditor profileEditor = new ProfileCellEditor();

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

  private String attribute = null;

  public String getAttribute()
  {
    return attribute;
  }

  public void setAttribute(String attribute)
  {
    if (Util.differ(attribute, this.attribute))
    {
      boolean oldSuppressMappingUpdate = this.suppressMappingUpdate;
      this.suppressMappingUpdate = true;
      this.attribute = attribute;
      this.model.setColumnTitle(1, GraphicSet.translateAttVal(attribute));
      this.refreshPropertiesEditor();
      if (vm.getSelectedPart() != null && vm.getSelectedPart().getMapping() == null)
      {
        this.generateDefaultEntries(attribute);
      }
      this.suppressMappingUpdate = oldSuppressMappingUpdate;
    }
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
      this.generateDefaultEntries(this.attribute);
    }
    else
    {
      String attr = PropertyMappingPanel.getPropertyMappingProperty(ms);
      if (attr == null)
      {
        return false;
      }
      else
      {
        this.setAttribute(attr);
      }
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
      this.addDefaultEntries(attribute);
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
          this.refreshPropertiesEditor();
          this.representMapping(VisicutModel.getInstance().getSelectedPart().getMapping());
        }
      }
      else if (VisicutModel.PROP_PLF_PART_UPDATED.equals(pce.getPropertyName()) && pce.getNewValue().equals(VisicutModel.getInstance().getSelectedPart()))
      {
        if (!ignorePartUpdate)
        {
          this.refreshPropertiesEditor();
          this.representMapping(VisicutModel.getInstance().getSelectedPart().getMapping());
        }
      }
    }
  }

  public void tableChanged(TableModelEvent tme)
  {
    if (!suppressMappingUpdate)
    {
      ignorePartUpdate = true;
      if (VisicutModel.getInstance().getSelectedPart() != null)
      {
        VisicutModel.getInstance().getSelectedPart().setMapping(this.getResultingMappingSet());
        VisicutModel.getInstance().firePartUpdated(VisicutModel.getInstance().getSelectedPart());
      }
      ignorePartUpdate = false;
    }
  }

  private SimpleFilterSetCellEditor filterSetEditor = new SimpleFilterSetCellEditor();
  
  private final List<MappingTableEntry> entries = new LinkedList<MappingTableEntry>();

  public PropertyMappingPanelTable()
  {
    model = new MappingTableModel(entries);
    model.addTableModelListener(this);
    this.generateDefaultEntries(this.attribute);
    this.setTableModel(model);
    this.setProvider(this);
    this.setObjects((List) entries);
    this.setEditButtonVisible(true);
    this.refreshProfilesEditor();
    this.getTable().setDefaultRenderer(FilterSet.class, new SimpleFilterSetCellRenderer());
    this.getTable().setDefaultEditor(FilterSet.class, filterSetEditor);
    this.getTable().setDefaultEditor(LaserProfile.class, profileEditor);
    this.getTable().setDefaultRenderer(LaserProfile.class, new ProfileCellRenderer());
    this.setMoveButtonsVisible(true);
    this.setSaveButtonVisible(true);
    this.setLoadButtonVisible(true);
    ProfileManager.getInstance().addPropertyChangeListener(this);
    VisicutModel.getInstance().addPropertyChangeListener(this);
    ((BetterJTable) this.getTable()).setColumnRelations(new int[]{6,16,9});
  }
  
  private void refreshPropertiesEditor()
  {
    List<FilterSet> fss = new LinkedList<FilterSet>();
    if (attribute != null && vm.getSelectedPart() != null && vm.getSelectedPart().getGraphicObjects() != null)
    {
      for (Object value : vm.getSelectedPart().getGraphicObjects().getAttributeValues(attribute))
      {
        FilterSet fs = new FilterSet();
        fs.add(new MappingFilter(attribute, value));
        fss.add(fs);
      }
    }
    filterSetEditor.refresh(fss);
  }
  
  private void refreshProfilesEditor()
  {
    profileEditor.refresh(entries);
  }

  /// fill the table with default entries
  private void generateDefaultEntries(String attribute) {
    entries.clear();
    addDefaultEntries(attribute);
  }
  
  /// add a disabled default entry for all values of the given attribute that have no entry yet
  private void addDefaultEntries(String attribute)
  {
    LaserProfile defaultProfile = ProfileManager.getInstance().getAll().isEmpty() ? null : ProfileManager.getInstance().getAll().get(0);
    if (attribute != null && vm.getSelectedPart() != null && vm.getSelectedPart().getGraphicObjects() != null)
    {
      for (Object value : VisicutModel.getInstance().getSelectedPart().getGraphicObjects().getAttributeValues(attribute))
      {
        MappingTableEntry e = new MappingTableEntry();
        e.enabled = false;
        e.filterSet = new FilterSet();
        e.filterSet.add(new MappingFilter(attribute, value));
        e.profile = defaultProfile;
        
        // does the table already have an entry of this attribute-value combination?
        boolean alreadyExists=false;
        for (MappingTableEntry existingEntry: entries) {
          if (e.filterSet.equals(existingEntry.filterSet)) {
            alreadyExists=true;
            break;
          }
        }
        
        // add entry if not already existing
        if (!alreadyExists) {
          entries.add(e);
        }
      }
    }
    //add everything else
    MappingTableEntry e = new MappingTableEntry();
    e.enabled = false;
    e.filterSet = null;
    e.profile = defaultProfile;
    entries.add(e);
    suppressMappingUpdate = true;
    model.fireTableDataChanged();
    suppressMappingUpdate = false;
  }

  private MappingSet getResultingMappingSet()
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
