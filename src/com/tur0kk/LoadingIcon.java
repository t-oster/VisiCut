package com.tur0kk;

import javax.swing.ImageIcon;

/**
 * This class hides the loading of resources. In this special case, the loading of ImageIcons with loading symbols. 
 * Find all available resources in com.tur0kk.resources and in the static variables of this class.
 * @author Sven
 */
public class LoadingIcon{
  
  // available loading gif resources which can be requested
  public static final String CIRCLEBALL_SMALL = "circleball_small";
  public static final String CIRCLEBALL_MEDIUM = "circleball_medium";
  
  
  // global function to load an Imageicon with an loading symbol. Use available global resource variables of this class as parameter.
  public static ImageIcon get(String type){
    try
    {
      return new ImageIcon(LoadingIcon.class.getResource("resources/"+type+".gif"));
    }
    catch (Exception ex)
    {
      System.err.println("Error loading "+"resources/"+type+".gif");
      ex.printStackTrace();
    }
    return null;
  }
  
}

