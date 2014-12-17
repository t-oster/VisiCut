/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.visicut.gui.mapping;

import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.ImageObserver;
import javax.swing.JList;

/**
 *
 * @author Sven
 */
public class AnimationImageObserverList implements ImageObserver
{
  JList list;
  int index;

  public AnimationImageObserverList(JList componentToUpdate, int index)
  {
    this.list = componentToUpdate;
    this.index = index;
  }
  
  public boolean imageUpdate(Image img, int infoflags, int x, int y, int width, int height)
  {
    if ((infoflags & (FRAMEBITS|ALLBITS)) != 0) {
      if(this.list.isShowing()){
        Rectangle rect = this.list.getCellBounds(index, index);
        this.list.repaint(rect);
      }
    }
    return (infoflags & (ALLBITS|ABORT)) == 0;
  }
  
}
