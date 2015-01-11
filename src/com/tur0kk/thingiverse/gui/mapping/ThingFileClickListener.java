/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
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
 *
 * @author Sven
 */
public class ThingFileClickListener extends MouseAdapter
{
  // save main view for loading files
  MainView mainview;
  JLabel lblOpeningFile;

  public ThingFileClickListener(MainView mainview, JLabel lblOpeningFile)
  {
    this.mainview = mainview;
    this.lblOpeningFile = lblOpeningFile;
  }
  
  
  @Override
  public void mouseClicked(MouseEvent evt) {
    JList list = (JList) evt.getSource();

    if(evt.getClickCount() == 2){
      // user feedback
      SwingUtilities.invokeLater(new Runnable() {
        public void run()
        {
          lblOpeningFile.setVisible(true);
        }
      });
      
      // load file
      this.lblOpeningFile.setVisible(true);
      int index = list.locationToIndex(evt.getPoint());
      ThingFile aFile = (ThingFile) list.getModel().getElementAt(index);
      ThingiverseManager thingiverse = ThingiverseManager.getInstance();
      File svgfile = thingiverse.downloadThingFile(aFile);
      this.mainview.loadFile(svgfile, false);
      
      // disable user feedback
      SwingUtilities.invokeLater(new Runnable() {
        public void run()
        {
          lblOpeningFile.setVisible(false);
        }
      });
    }
  }
}
