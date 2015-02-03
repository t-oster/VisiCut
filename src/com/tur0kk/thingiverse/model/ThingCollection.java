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
