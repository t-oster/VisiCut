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
package com.tur0kk.thingiverse.gui.mapping;

import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * This file listen on ThingFileLists to enable the I made one button iff a thing file is selected
 * @author Sven
 */
public class ThingFileSelectionListener implements ListSelectionListener
{
  JButton btnOpenFile;;
  
  public ThingFileSelectionListener(JButton btnOpenFile){
    this.btnOpenFile = btnOpenFile;
  }
  public void valueChanged(ListSelectionEvent e)
  {
    boolean adjust = e.getValueIsAdjusting();
    if (!adjust) // wait until list finisehd adjusting
    {
      // get selected list element
      JList list = (JList) e.getSource();
      int selection = list.getSelectedIndex();
          
      if(selection != -1){ // something is selected
        this.btnOpenFile.setEnabled(true);
      }
      else{
        // nothing is selected
        this.btnOpenFile.setEnabled(false); // to upload "I made one" a thing must be selected
      }
    }
  }
  
}
