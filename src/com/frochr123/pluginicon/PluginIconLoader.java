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
package com.frochr123.pluginicon;

import javax.swing.ImageIcon;

/**
 * PluginIconLoader.java: This class is responsible for loading plugin icons from image files
 * Image files can be found in: com.frochr123.pluginicon.resources
 * Load images as class ImageIcon, which implements the interface Icon
 * @author Christian
 */
public class PluginIconLoader 
{
  // Available plugin icons
  // Loading animations
  public static final String PLUGIN_LOADING_CIRCLE_SMALL = "plugin_loading_circle_small.gif";
  public static final String PLUGIN_LOADING_CIRCLE_MEDIUM = "plugin_loading_circle_medium.gif";

  // Thingiverse plugin
  public static final String PLUGIN_THINGIVERSE = "plugin-thingiverse.png";
  public static final String PLUGIN_THINGIVERSE_NO_AVATAR = "plugin-thingiverse-no-avatar.jpg";
  public static final String PLUGIN_THINGIVERSE_NO_IMG = "plugin-thingiverse-no-img.png";

  // Facebook plugin
  public static final String PLUGIN_FACEBOOK = "plugin-facebook.png";


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
      return new ImageIcon(PluginIconLoader.class.getResource("resources/" + filename));
    }
    catch (Exception e)
    {
      System.err.println("Error loading plugin icon:" + filename);
      e.printStackTrace();
    }

    return null;
  }
}
