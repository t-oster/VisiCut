/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.visicut.model.graphicelements;

import java.awt.Graphics2D;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author thommy
 */
public interface GraphicObject
{
  
  /**
   * Returns a list of attribute values for the given
   * Attribute.
   * @param name
   * @return 
   */
  public List<Object> getAttributeValues(String name);
  /**
   * Returns a List of Attributes where the Object
   * has values set
   * @return 
   */
  public List<String> getAttributes();
  
  /**
   * Renders the Object on the given Graphcis2D
   * @param g 
   */
  public void render(Graphics2D g);
}
