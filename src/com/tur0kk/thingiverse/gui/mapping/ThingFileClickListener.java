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
  JLabel lblOpeningFile;

  public ThingFileClickListener(JLabel lblOpeningFile)
  {
    this.lblOpeningFile = lblOpeningFile;
  }
  
  
  @Override
  public void mouseClicked(MouseEvent evt) {
    JList list = (JList) evt.getSource();

    if(evt.getClickCount() == 2){
      int index = list.locationToIndex(evt.getPoint());
      final ThingFile aFile = (ThingFile) list.getModel().getElementAt(index);
      
      if(index < 0 || aFile == null){ // nothing visible
        return;
      }
      
      // user feedback
      SwingUtilities.invokeLater(new Runnable() {
        public void run()
        {
          lblOpeningFile.setVisible(true);
        }
      });
      
      // load file
      new Thread(new Runnable() {

            public void run()
            {   
              ThingiverseManager thingiverse = ThingiverseManager.getInstance();
              File svgfile = thingiverse.downloadThingFile(aFile);
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
