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
package com.tur0kk.thingiverse.model;

import javax.swing.ImageIcon;

/**
 * Represents a collection of things as done in the thingiverse api.
 * @author Sven, Patrick Schmidt
 */
public class ThingCollection
{
  String id;
  String name;
  String imageUrl;
  ImageIcon image;
  
  public ThingCollection(String id, String name, String imageLocation)
  {
    this.id = id;
    this.name = name;
    this.imageUrl = imageLocation;
  }
  
  public String getId()
  {
    return this.id;
  }
  
  public String getName()
  {
    return this.name;
  }
  
  public String getImageUrl()
  {
    return this.imageUrl;
  }
  
  /**
   * Collection thumbnail as ImaceIcon. This is not available by default! Check if null!
   * You can attach an image icon to a Thing using setImage.
   * @return 
   */
  public ImageIcon getImage()
  {
    return this.image;
  }
  
  public void setImage(ImageIcon image)
  {
    this.image = image;
  }
  
  @Override
  public String toString()
  {
    return "ThingCollection: " + name;
  }
}
