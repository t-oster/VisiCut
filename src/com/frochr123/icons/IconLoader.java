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
package com.frochr123.icons;

import javax.swing.ImageIcon;

/**
 * IconLoader.java: This class is responsible for loading icons from image files
 * Image files can be found in: com.frochr123.icons.resources
 * Load images as class ImageIcon, which implements the interface Icon
 * @author Christian
 */
public class IconLoader 
{
  // Available icons
  // Loading animations
  public static final String ICON_LOADING_CIRCLE_SMALL = "icon_loading_circle_small.gif";
  public static final String ICON_LOADING_CIRCLE_MEDIUM = "icon_loading_circle_medium.gif";

  // Thingiverse
  public static final String ICON_THINGIVERSE = "icon-thingiverse.png";
  public static final String ICON_THINGIVERSE_NO_AVATAR = "icon-thingiverse-no-avatar.jpg";
  public static final String ICON_THINGIVERSE_NO_IMG = "icon-thingiverse-no-img.png";

  // Facebook
  public static final String ICON_FACEBOOK = "icon-facebook.png";
  
  // QR codes
  public static final String ICON_QRCODE = "icon-qrcode.png";


  // Function to load image as Icon
  public static ImageIcon loadIcon(String filename)
  {
    // Try to avoid invalid input
    if (filename == null || filename.isEmpty())
    {
      return null;
    }
    
    // Try catch for loading image, log exceptions
    try
    {
      return new ImageIcon(IconLoader.class.getResource("resources/" + filename));
    }
    catch (Exception e)
    {
      System.err.println("Error loading icon:" + filename);
      e.printStackTrace();
    }

    return null;
  }
}
