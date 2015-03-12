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

import com.t_oster.visicut.gui.MainView;
import com.tur0kk.thingiverse.ThingiverseManager;
import com.tur0kk.thingiverse.model.ThingFile;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.SwingUtilities;

/**
 * This class listens for double clicks on a thing file (right panel) to load it into thingiverse
 * @author Sven
 */
public class ThingFileClickListener extends MouseAdapter
{
  // save main view for loading files
  JLabel lblOpeningFile;

  public ThingFileClickListener(JLabel lblOpeningFile)
  {
    this.lblOpeningFile = lblOpeningFile;
  }
  
  
  @Override
  public void mouseClicked(MouseEvent evt) {
    JList list = (JList) evt.getSource(); // clieckt list

    if(evt.getClickCount() == 2){ // double click
      
      int index = list.locationToIndex(evt.getPoint());
      if(index < 0){ // nothing visible
        return;
      }
      
      final ThingFile aFile = (ThingFile) list.getModel().getElementAt(index);
      if(aFile == null){ // nothing visible
        return;
      }
      
      // enable user feedback
      SwingUtilities.invokeLater(new Runnable() {
        public void run()
        {
          lblOpeningFile.setVisible(true);
        }
      });
      
      // load file into thingiverse
      new Thread(new Runnable() {

            public void run()
            {   
              // request file from thingivserse
              ThingiverseManager thingiverse = ThingiverseManager.getInstance();
              File svgfile = thingiverse.downloadThingFile(aFile);
              
              // display file
              MainView.getInstance().loadFile(svgfile, false);
              
              // disable user feedback
              SwingUtilities.invokeLater(new Runnable() {
                public void run()
                {
                  lblOpeningFile.setVisible(false);
                }
              });
            }
      }).start();

    }
  }
}
