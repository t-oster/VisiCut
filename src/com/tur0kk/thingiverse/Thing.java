/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tur0kk.thingiverse;

import javax.swing.ImageIcon;

/**
 *
 * @author Sven
 */
public class Thing
{
  String name;
  String imageLocation;
  ImageIcon image;
  
  public Thing(String name, String imageLocation){
    this.name = name;
    this.imageLocation = imageLocation;
  }
  
  public String getName(){
    return this.name;
  }
  
  public String getImageLocation(){
    return this.imageLocation;
  }
  
  public ImageIcon getImage(){
    return this.image;
  }
  
  public void setImage(ImageIcon image){
    this.image = image;
  }
}
