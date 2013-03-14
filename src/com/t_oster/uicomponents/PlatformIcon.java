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
 
  public static final String ADD = "add";
  public static final String CAMERA = "camera";
  public static final String DOWN = "down";
  public static final String EDIT = "edit";
  public static final String LOAD = "load";
  //public static final String NEW = "new";
  //public static final String NEXT = "next";
  public static final String NO_IMAGE = "no-image";
  //public static final String PREV = "prev";
  public static final String REMOVE = "remove";
  public static final String SAVE = "save";
  public static final String UNDO = "undo";
  public static final String UP = "up";
  public static final String ZOOM_ACTUAL_EQUAL = "zoom-actual-equal";
  public static final String ZOOM_ACTUAL = "zoom-actual";
  public static final String ZOOM_FIT = "zoom-fit";
  public static final String ZOOM_IN = "zoom-in";
  public static final String ZOOM_OUT = "zoom-out";
  
  private static final Map<String, String> gtkIconNames = new LinkedHashMap<String, String>();
  static
  {
    //gtkIconNames.put(NEW, "gtk-new");
    gtkIconNames.put(SAVE, "gtk-save");
    gtkIconNames.put(UP, "gtk-go-up");
    gtkIconNames.put(DOWN, "gtk-go-down");
    //gtkIconNames.put(NEXT, "gtk-go-forward");
    //gtkIconNames.put(PREV, "gtk-go-back");
    gtkIconNames.put(EDIT, "gtk-edit");
    gtkIconNames.put(ADD, "gtk-add");
    gtkIconNames.put(REMOVE, "gtk-delete");
    gtkIconNames.put(LOAD, "gtk-open");
    gtkIconNames.put(UNDO, "gtk-undo");
  }
  
  public static Icon get(String type)
  {
    if (type == null)
    {
      return null;
    }
    try
    {
      if (Helper.isLinux() && gtkIconNames.containsKey(type))
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
      return new ImageIcon(ImageIO.read(PlatformIcon.class.getResource("platformicons/"+type+".png")));
    }
    catch (Exception ex)
    {
      System.err.println("Error loading "+"platformicons/"+type+".png");
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
