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
 * Represents information about a file which belongs to a particular thing.
 * This class does not download the file on its own.
 * @author Patrick Schmidt
 */
public class ThingFile
{
  String id;
  String name;
  String url;
  String thumbnailUrl;
  ImageIcon thumbnail;
  Thing thing;
  
  public ThingFile(String id, String name, String url, String thumbnailUrl, Thing thing)
  {
    this.id = id;
    this.name = name;
    this.url = url;
    this.thumbnailUrl = thumbnailUrl;
    this.thing = thing;
  }

  public String getId()
  {
    return id;
  }

  public String getName()
  {
    return name;
  }

  public String getUrl()
  {
    return url;
  }

  public String getThumbnailUrl()
  {
    return thumbnailUrl;
  }

  /**
   * File thumbnail as ImaceIcon. This is not available by default! Check if null!
   * You can attach an image icon to a Thing using setImage.
   * @return 
   */
  public ImageIcon getThumbnail()
  {
    return thumbnail;
  }

  public void setThumbnail(ImageIcon thumbnail)
  {
    this.thumbnail = thumbnail;
  }
  
  public Thing getThing()
  {
    return this.thing;
  }
  
  @Override
  public String toString()
  {
    return "Thing File: " + name;
  }
}
