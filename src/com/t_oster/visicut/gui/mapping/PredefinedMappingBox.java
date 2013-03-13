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

import com.t_oster.visicut.VisicutModel;
import com.t_oster.visicut.managers.MappingManager;
import com.t_oster.visicut.managers.ProfileManager;
import com.t_oster.visicut.model.PlfPart;
import com.t_oster.visicut.model.graphicelements.GraphicSet;
import com.t_oster.visicut.model.mapping.MappingSet;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

/**
 * This implements a combo box, listing all predefined mappings.
 * If the current selected part has one of those mappings, the 
 * combo box is updated. It shows "none" if the mapping is null
 * and "Custom" if the mapping is none of the predefined ones.
 * If the selection is changed by the user, the mapping will
 * of the selected part will be set
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class PredefinedMappingBox extends javax.swing.JComboBox
{

  /**
   * This class is only for having entries like "Map by color"
   */
  public class MapByPropertyEntry
  {
    public MapByPropertyEntry(String property)
    {
      this.property = property;
    }
    public String property;
  }
  
  private ResourceBundle bundle = ResourceBundle.getBundle("com/t_oster/visicut/gui/mapping/resources/PredefinedMappingBox");
  
  public String NONE = "<html><b>"+bundle.getString("NONE")+"</b></html>";
  public String ONE_PROFILE_FOR_EVERYTHING = "<html><b>"+bundle.getString("ONE_PROFILE_FOR_EVERYTHING")+"</b></html>";
  public String PREDEFINED_MAPPINGS = "<html><b>"+bundle.getString("PREDEFINED_MAPPINGS")+"</b></html>";
  public String BY_PROPERTY = "<html><b>"+bundle.getString("BY_PROPERTY")+"...</b></html>";
  public String CUSTOM = "<html><b>"+bundle.getString("CUSTOM")+"...</b></html>";
  private PlfPart lastSelectedPlfPart = null;
  /**
   * Creates new form MappingPanel
   */
  public PredefinedMappingBox()
  {
    this.setRenderer(cbRenderer);
    PropertyChangeListener pl = new PropertyChangeListener(){
      public void propertyChange(PropertyChangeEvent pce)
      {
        propertyChanged(pce);
      }
    };
    MappingManager.getInstance().addPropertyChangeListener(pl);
    ProfileManager.getInstance().addPropertyChangeListener(pl);
    VisicutModel.getInstance().addPropertyChangeListener(pl);
    this.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent ae)
      {
        comboBoxActionPerformed(ae);
      }
    });
    updateComboBoxContent();
    updateUi();
  }
  private boolean ignoreUiUpdates = false;

  //this renderer adds spaces before each mapping name
  private ListCellRenderer cbRenderer = new DefaultListCellRenderer()
  {
    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
    {
      String mapBy = bundle.getString("MAP_BY");
      Component result = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
      if (result instanceof JLabel)
      {
        if (value instanceof MappingSet)
        {
          ((JLabel) result).setText((isPopupVisible() ? "   " : "" ) + ((MappingSet) value).toString());
        }
        else if (value instanceof MapByPropertyEntry)
        {
          ((JLabel) result).setText("   "+mapBy.replace("$property", GraphicSet.translateAttVal(((MapByPropertyEntry) value).property)));
        }
      }
      return result;
    }
  };
  
  private void updateComboBoxContent()
  {
    ignoreUiUpdates = true;
    Object selected = this.getSelectedItem();
    this.removeAllItems();
    this.addItem(NONE);
    this.addItem(ONE_PROFILE_FOR_EVERYTHING);
    for (MappingSet m : MappingManager.getInstance().generateDefaultMappings())
    {
      this.addItem(m);
    }
    List<MappingSet> all = MappingManager.getInstance().getAll();
    if (all.size() > 0)
    {
      this.addItem(PREDEFINED_MAPPINGS);
      for (MappingSet m : all)
      {
        this.addItem(m);
      }
    }
    
    if (VisicutModel.getInstance().getSelectedPart() != null)
    {   
      Iterable<String> props = VisicutModel.getInstance().getSelectedPart().getGraphicObjects().getInterestingAttributes();
      if (props.iterator().hasNext())
      {
        this.addItem(BY_PROPERTY);
        int count = 0;
        for(String att: props)
        {
          if (++count > 4)
          {
            break;
          }
          this.addItem(new MapByPropertyEntry(att));
        }
      }
    }
    this.addItem(CUSTOM);
    this.setSelectedItem(selected);
    ignoreUiUpdates = false;
  }

  /**
   * Updates the UI to represent the current this.mapping value
   * WITHOUT generating propery change events.
   */
  private void updateUi()
  {
    
    if (VisicutModel.getInstance().getSelectedPart() != null)
    {
      ignoreUiUpdates = true;
      MappingSet ms = VisicutModel.getInstance().getSelectedPart().getMapping();
      // guess the selected entry from the MappingSet
      // we have no information about the name, so we need to see which entry matches
      
      if ((lastSelectedPlfPart == VisicutModel.getInstance().getSelectedPart()) && (this.getSelectedItem() == CUSTOM || this.getSelectedItem() == BY_PROPERTY)) {
        // special case:
        // the selected PlfPart ("object") is still the same.
        // CUSTOM/BY_PROPERTY was selected and the mapping was edited
        // even if the mapping is now equal to a saved one, don't switch back to the entry of the saved mapping!
        // otherwise the CustomMappingPanel would be hidden

        // change nothing, selectedItem is still CUSTOM
      } else {
        // default case:
        // show NONE if empty mapping
        // show saved mapping if one is equal to the current mapping
        // show "CUSTOM" otherwise

        if (ms == null || ms.isEmpty())
        {
          Object selected = this.getSelectedItem();
          //NONE, By_Property and Custom can represent a null mapping, so leave
          //them alone if they are selected. Otherwise select NONE by default
          if (!NONE.equals(selected) && !BY_PROPERTY.equals(selected) && !CUSTOM.equals(selected))
          {
            this.setSelectedItem(NONE);
          }
        } 
        else 
        {
          this.setSelectedItem(PropertyMappingPanel.getPropertyMappingProperty(ms) != null ? BY_PROPERTY : CUSTOM);
          this.setSelectedItem(ms); // only changes the selection if the mapping exists in the comboBox
        }
      }
      ignoreUiUpdates = false;
    }
    lastSelectedPlfPart = VisicutModel.getInstance().getSelectedPart();
  }

  private void comboBoxActionPerformed(java.awt.event.ActionEvent evt)
  {
    if (!ignoreUiUpdates && VisicutModel.getInstance().getSelectedPart() != null)
    {
      Object selected = this.getSelectedItem();
      if (selected == null || NONE.equals(selected))
      {
        VisicutModel.getInstance().getSelectedPart().setMapping(null);
        VisicutModel.getInstance().firePartUpdated(VisicutModel.getInstance().getSelectedPart());
      }
      else if (selected instanceof MappingSet)
      {
        VisicutModel.getInstance().getSelectedPart().setMapping((MappingSet) selected);
        VisicutModel.getInstance().firePartUpdated(VisicutModel.getInstance().getSelectedPart());
      }
      else if (selected instanceof MapByPropertyEntry)
      {
        //do nothing. MappingPanel will handle this
      }
      else //some string selected, which should not be selectable => revert state
      {
        updateUi();
      }
    }
  }

  private void propertyChanged(PropertyChangeEvent pce)
  {
    if (pce.getSource().equals(VisicutModel.getInstance()))
    {
      if (VisicutModel.PROP_SELECTEDPART.equals(pce.getPropertyName()))
      {
        updateComboBoxContent();
        updateUi();
      }
      else if (VisicutModel.PROP_PLF_PART_UPDATED.equals(pce.getPropertyName()) && pce.getNewValue().equals(VisicutModel.getInstance().getSelectedPart()))
      {
        updateUi();
      }
    }
    else if (pce.getSource().equals(MappingManager.getInstance()) || pce.getSource().equals(ProfileManager.getInstance()))
    {
      updateComboBoxContent();
    }
  }
}
