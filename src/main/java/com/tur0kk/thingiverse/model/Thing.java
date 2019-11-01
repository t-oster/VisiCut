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
 * Represents a "Thing" (also refered to as item or project) from the thingiverse
 * website.
 * @author Sven, Patrick Schmidt
 */
public class Thing
{
  String id;
  String name;
  String imageUrl;
  ImageIcon image = null;
  
  public Thing(String id, String name, String imageLocation)
  {
    this.id = id;
    this.name = name;
    this.imageUrl = imageLocation;
  }
  
  /**
   * Thing id within the thingiverse api.
   * @return Id as string
   */
  public String getId()
  {
    return this.id;
  }
  
  /**
   * Thing display name.
   * @return Name as string
   */
  public String getName()
  {
    return this.name;
  }
  
  /**
   * Absolute url to the image. (Most probably hosted at amazon s3)
   * @return Url as string
   */
  public String getImageUrl()
  {
    return this.imageUrl;
  }
  
  /**
   * Thing thumbnail as ImaceIcon. This is not available by default! Check if null!
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
    return "Thing: " + name;
  }
}
