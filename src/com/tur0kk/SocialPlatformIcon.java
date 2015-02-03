package com.tur0kk;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;

/**
 * This class provides a set of icons to hide loading resources globally. 
 * In this special case, the loading of ImageIcons with GUI symbols. 
 * Find all available resources in com.tur0kk.resources and in the static variables of this class.
 * @author Sven
 */
public class SocialPlatformIcon 
{

  // available loading gif resources which can be requested
  public static final String FACEBOOK_LOGO = "facebook-logo";
  
  // global function to load an Imageicon with an loading symbol. Use available global resource variables of this class as parameter.
  public static Icon get(String type)
  {
    if (type == null)
    {
      return null;
    }
    
    try
    {
      return new ImageIcon(ImageIO.read(com.tur0kk.SocialPlatformIcon.class.getResource("resources/"+type+".png")));
    }
    catch (Exception ex)
    {
      System.err.println("Error loading "+"platformicons/"+type+".png");
    }

    return null;
  }
  
}
