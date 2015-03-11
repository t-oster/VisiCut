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
