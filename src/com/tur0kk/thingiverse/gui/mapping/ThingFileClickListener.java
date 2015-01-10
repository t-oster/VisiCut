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
import javax.swing.JList;

/**
 *
 * @author Sven
 */
public class ThingFileClickListener extends MouseAdapter
{
  // save main view for loading files
  MainView mainview;

  public ThingFileClickListener(MainView mainview)
  {
    this.mainview = mainview;
  }
  
  
  @Override
  public void mouseClicked(MouseEvent evt) {
    JList list = (JList) evt.getSource();

    if(evt.getClickCount() == 2){
      int index = list.locationToIndex(evt.getPoint());
      ThingFile aFile = (ThingFile) list.getModel().getElementAt(index);
      ThingiverseManager thingiverse = ThingiverseManager.getInstance();
      File svgfile = thingiverse.downloadThingFile(aFile);
      mainview.loadFile(svgfile, false);
    }
  }
}
