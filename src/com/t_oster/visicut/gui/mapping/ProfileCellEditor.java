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
import com.t_oster.visicut.VisicutModel;
import com.t_oster.visicut.managers.LaserPropertyManager;
import com.t_oster.visicut.managers.ProfileManager;
import com.t_oster.visicut.model.LaserDevice;
import com.t_oster.visicut.model.LaserProfile;
import com.t_oster.visicut.model.MaterialProfile;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;

/**
 *
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
class ProfileCellEditor extends DefaultCellEditor{
  private JComboBox profiles;
  public String IGNORE = java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/mapping/resources/CustomMappingPanel").getString("IGNORE");
  
  public ProfileCellEditor()
  {
    super(new JComboBox());
    profiles = (JComboBox) this.getComponent();
  }

  @Override
  public Object getCellEditorValue()
  {
    return IGNORE.equals(super.getCellEditorValue()) ? null : super.getCellEditorValue();
  }
  
  public void refresh(Iterable<MappingTableEntry> entries)
  {
    profiles.removeAllItems();
    List<LaserProfile> addedProfilesList=new LinkedList<LaserProfile>();
    List<String> profileNamesList=new LinkedList<String>();
    // add IGNORE
    profiles.addItem(IGNORE);
    // add laser profiles saved on disk
    for (LaserProfile lp : ProfileManager.getInstance().getAll())
    {
      profiles.addItem(lp);
      addedProfilesList.add(lp);
      profileNamesList.add(lp.getName());
    }
    // add all temporary copies currently in use
    for (MappingTableEntry e: entries) {
      LaserProfile lp = e.profile;
      if (lp != null && !addedProfilesList.contains(lp)) {
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
  }
}
