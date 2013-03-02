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
import com.t_oster.visicut.model.mapping.MappingSet;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

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

  public String NONE = java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/mapping/resources/PredefinedMappingBox").getString("NONE");
  public String BY_PROPERTY = "<html><b>"+java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/mapping/resources/PredefinedMappingBox").getString("BY_PROPERTY")+"...</b></html>";
  public String CUSTOM = "<html><b>"+java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/mapping/resources/PredefinedMappingBox").getString("CUSTOM")+"...</b></html>";
  private PlfPart lastSelectedPlfPart = null;
  /**
   * Creates new form MappingPanel
   */
  public PredefinedMappingBox()
  {
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

  private void updateComboBoxContent()
  {
    ignoreUiUpdates = true;
    Object selected = this.getSelectedItem();
    this.removeAllItems();
    this.addItem(NONE);
    for (MappingSet m : MappingManager.getInstance().generateDefaultMappings())
    {
      this.addItem(m);
    }
    for (MappingSet m : MappingManager.getInstance().getAll())
    {
      this.addItem(m);
    }
    this.addItem(BY_PROPERTY);
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
      else if (CUSTOM.equals(selected) || BY_PROPERTY.equals(selected))
      {
        //do nothing
      }
      else
      {
        VisicutModel.getInstance().getSelectedPart().setMapping((MappingSet) selected);
        VisicutModel.getInstance().firePartUpdated(VisicutModel.getInstance().getSelectedPart());
      }
    }
  }

  private void propertyChanged(PropertyChangeEvent pce)
  {
    if (pce.getSource().equals(VisicutModel.getInstance()))
    {
      if (VisicutModel.PROP_SELECTEDPART.equals(pce.getPropertyName()))
      {
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
