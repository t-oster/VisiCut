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

import com.t_oster.visicut.managers.ProfileManager;
import com.t_oster.visicut.model.LaserProfile;
import java.util.LinkedList;
import java.util.List;
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
    // add IGNORE
    profiles.addItem(IGNORE);
    // add laser profiles saved on disk
    for (LaserProfile lp : ProfileManager.getInstance().getAll())
    {
      profiles.addItem(lp);
      addedProfilesList.add(lp);
    }
    // add all laser-profiles contained in the list (e.g. from loaded file
    // so they are not necessarily added from disk yet
    for (MappingTableEntry e: entries) 
    {
      LaserProfile lp = e.profile;
      if (lp != null && !addedProfilesList.contains(lp)) {
        addedProfilesList.add(lp);
        profiles.addItem(lp);
      }
    }
  }
}
