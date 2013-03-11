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

package com.t_oster.uicomponents;

import com.t_oster.visicut.misc.Helper;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import sun.awt.UNIXToolkit;

/**
 * This class provides a set of icons and tries to load the native
 * platform icon. If not possible, an icon of the default icon set will
 * be returned.
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class PlatformIcon 
{
 
  public static final int NEW = 1;
  public static final int SAVE = 2;
  public static final int NEXT = 3;
  public static final int PREV = 4;
  public static final int UP = 5;
  public static final int DOWN = 6;
  public static final int LOAD = 7;
  public static final int ADD = 8;
  public static final int REMOVE = 9;
  public static final int EDIT = 10;
  public static final int UNDO = 11;
  
  private static final Map<Integer, String> gtkIconNames = new LinkedHashMap<Integer, String>();
  static
  {
    gtkIconNames.put(NEW, "gtk-new");
    gtkIconNames.put(SAVE, "gtk-save");
    gtkIconNames.put(UP, "gtk-go-up");
    gtkIconNames.put(DOWN, "gtk-go-down");
    gtkIconNames.put(NEXT, "gtk-go-forward");
    gtkIconNames.put(PREV, "gtk-go-back");
    gtkIconNames.put(EDIT, "gtk-edit");
    gtkIconNames.put(ADD, "gtk-add");
    gtkIconNames.put(REMOVE, "gtk-delete");
    gtkIconNames.put(LOAD, "gtk-open");
    gtkIconNames.put(UNDO, "gtk-undo");
  }
  
  public static Icon get(int type)
  {
    try
    {
      if (Helper.isLinux())
      {
        return loadGtkIcon(gtkIconNames.get(type));
      }
    }
    catch (Exception e)
    {
      Logger.getLogger(PlatformIcon.class.getName()).log(Level.SEVERE, null, e);
    }
    try
    {
      switch (type)
      {
        case ADD: return new ImageIcon(ImageIO.read(PlatformIcon.class.getResource("resources/add.png")));
        case REMOVE: return new ImageIcon(ImageIO.read(PlatformIcon.class.getResource("resources/remove.png")));
        case UP: return new ImageIcon(ImageIO.read(PlatformIcon.class.getResource("resources/up.png")));
        case DOWN: return new ImageIcon(ImageIO.read(PlatformIcon.class.getResource("resources/down.png")));
        case EDIT: return new ImageIcon(ImageIO.read(PlatformIcon.class.getResource("resources/edit.png")));
        case SAVE: return new ImageIcon(ImageIO.read(PlatformIcon.class.getResource("resources/save.png")));
        case LOAD: return new ImageIcon(ImageIO.read(PlatformIcon.class.getResource("resources/load.png")));
        case UNDO: return new ImageIcon(ImageIO.read(PlatformIcon.class.getResource("resources/undo.png")));
      }
    }
    catch (Exception ex)
    {
      Logger.getLogger(PlatformIcon.class.getName()).log(Level.SEVERE, null, ex);
    }
    try
    {
      return new ImageIcon(ImageIO.read(PlatformIcon.class.getResource("resources/edit.png")));
    }
    catch (Exception e)
    {
      Logger.getLogger(PlatformIcon.class.getName()).log(Level.SEVERE, null, e);
    }
    return null;
  }
  
  private static Icon loadGtkIcon(String id) {
    int widgetType = -1; // Go with the default 
    int dir = 1; // NONE=0 ; LTR=1; RTL=2;
    int size = 1;

    UNIXToolkit utk = (UNIXToolkit)Toolkit.getDefaultToolkit();
    BufferedImage img = utk.getStockIcon(widgetType,id,size,dir,null);                                       
    return new ImageIcon(img); // Will throw if img==null
  }
}
