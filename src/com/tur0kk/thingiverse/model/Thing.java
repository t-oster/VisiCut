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
