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
