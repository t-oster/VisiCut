/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.uicomponents;

import javax.swing.ImageIcon;

/**
 *
 * @author Sven
 */
public class LoadingIcon{
  public static final String CIRCLEBALL_SMALL = "circleball_small";
  public static final String CIRCLEBALL_MEDIUM = "circleball_medium";
  public static final String CIRCLEBALL = "circleball";
  
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

