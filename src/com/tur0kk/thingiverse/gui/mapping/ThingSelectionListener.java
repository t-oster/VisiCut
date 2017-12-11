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

import com.tur0kk.thingiverse.ThingiverseManager;
import com.tur0kk.thingiverse.model.Thing;
import com.tur0kk.thingiverse.model.ThingFile;
import com.frochr123.icons.IconLoader;
import java.util.Iterator;
import java.util.List;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JList;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * This class listens for clicks in a thing list (left panel) to load the corresponding thing files.
 * This listener disables or enables the I made one button dependent if a thing file is selected (I made one can only be published if a thing is selected)
 * @author Sven
 */
public class ThingSelectionListener implements ListSelectionListener
{
  private JList displayResult; // thing file list where the files to display
  private JCheckBox cbExtensions; // check if filter for extensions is enabled to filter files
  private JButton btnMadeOne; // to disable or enable depending on selection
  
  public ThingSelectionListener(JList displayResult, JCheckBox cbExtensions, JButton btnMadeOne){
    this.displayResult = displayResult;
    this.cbExtensions = cbExtensions;
    this.btnMadeOne = btnMadeOne;
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
            this.btnMadeOne.setEnabled(true); // to upload "I made one" a thing must be selected
            
            final Thing selectionValue = (Thing) list.getSelectedValue();

            if(selectionValue == null){ // break, no valid thing selected
              return;
            }

            // display files of selected thing
            new Thread(new Runnable() {

              public void run()
              {        
                final ThingiverseManager thingiverse = ThingiverseManager.getInstance();

                // get things grom thingiverse
                List<ThingFile> things = thingiverse.getFiles(selectionValue, cbExtensions.isSelected());

                // init my things model with loading images
                DefaultListModel fileModel = new DefaultListModel(); // model for JList
                Iterator<ThingFile> i1 = things.iterator(); // iterate over each file and add to model
                int index = 0;
                while (i1.hasNext()) 
                {
                  // get loading icon
                  ImageIcon loadingIcon = IconLoader.loadIcon(IconLoader.ICON_LOADING_CIRCLE_MEDIUM);

                  // set changing observer for loading images to update gif 
                  loadingIcon.setImageObserver(new AnimationImageObserverList(displayResult, index));

                  // add thing to model
                  ThingFile aThing = i1.next();
                  aThing.setThumbnail(loadingIcon);
                  fileModel.addElement(aThing);

                  index +=1;
                }

                // display message if no files were found
                if(things.isEmpty()){
                  fileModel.addElement("No lasercutterfiles found.");
                }

                // display svgModel in search thing list
                final DefaultListModel model = fileModel;
                SwingUtilities.invokeLater(new Runnable() {
                  public void run()
                  {
                    displayResult.setModel(model);            
                  }
                });

                // start a thread for each image to load image asynchronously
                for (final ThingFile entry : things)
                {
                  final String url = entry.getThumbnailUrl();
                  new Thread(new Runnable() 
                  {
                      public void run()
                      {
                        String file = thingiverse.downloadImage(url); // download  image
                        ImageIcon imageIcon = null;
                        if("".equals(file)){ // load default image if image not avaliable
                          imageIcon = IconLoader.loadIcon(IconLoader.ICON_THINGIVERSE_NO_IMG);
                        }
                        else{
                          imageIcon = new ImageIcon(file);
                        }

                        // overwrite image
                        final ImageIcon objectImage = imageIcon;
                        SwingUtilities.invokeLater(new Runnable() {
                          public void run()
                          {
                            // overwrite image
                            entry.setThumbnail(objectImage);
                            displayResult.updateUI();
                          }
                        });
                      }
                    }).start();
                  }

              }
            }).start();
          }
          else{
            // nothing is selected
            this.btnMadeOne.setEnabled(false); // to upload "I made one" a thing must be selected
            this.displayResult.setModel(new DefaultListModel()); // clear list
          }
        }
      }
  
}
