/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.visicut.model.mapping;

import java.util.LinkedList;

/**
 *
 * @author thommy
 */
public class MappingSet extends LinkedList<Mapping>
{

  protected String name = "UnnamedMapping";

  /**
   * Get the value of name
   *
   * @return the value of name
   */
  public String getName()
  {
    return name;
  }

  /**
   * Set the value of name
   *
   * @param name new value of name
   */
  public void setName(String name)
  {
    this.name = name;
  }

  @Override
  public String toString()
  {
    return this.name;
  }
  
}
