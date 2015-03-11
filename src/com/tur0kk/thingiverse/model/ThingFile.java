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
