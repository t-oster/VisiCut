package com.tur0kk.thingiverse.gui.mapping;

import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.ImageObserver;
import javax.swing.JList;

/**
 * This class can be set to an image to listen for image changes such as it is when using gifs. It redraws the list element when the image changes.
 * @param list model structure in which the image changes
 * @param element index in the list
 * @author Sven
 */
public class AnimationImageObserverList implements ImageObserver
{
  JList list; // list to observe
  int index; // index of the item to observe in the list

  public AnimationImageObserverList(JList componentToUpdate, int index)
  {
    this.list = componentToUpdate;
    this.index = index;
  }
  
  // this function redraws the UI if the image changed
  public boolean imageUpdate(Image img, int infoflags, int x, int y, int width, int height)
  {
    if ((infoflags & (FRAMEBITS|ALLBITS)) != 0) {
      if(this.list.isShowing()){
        Rectangle rect = this.list.getCellBounds(index, index); // item bounds in list
        if(rect == null){
          return false;
        }
        this.list.repaint(rect); // repaint UI
      }
    }
    return (infoflags & (ALLBITS|ABORT)) == 0;
  }
  
}
