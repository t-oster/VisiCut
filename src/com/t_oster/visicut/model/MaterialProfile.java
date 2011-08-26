/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.visicut.model;

import java.awt.Color;

/**
 *
 * @author thommy
 */
public class MaterialProfile
{

  protected Color color = new Color(180, 20, 40);

  /**
   * Get the value of color
   *
   * @return the value of color
   */
  public Color getColor()
  {
    return color;
  }

  /**
   * Set the value of color
   *
   * @param color new value of color
   */
  public void setColor(Color color)
  {
    this.color = color;
  }
  protected VectorProfile[] lineProfile = new VectorProfile[]{new VectorProfile(), new VectorProfile()};

  /**
   * Get the value of lineProfile
   *
   * @return the value of lineProfile
   */
  public VectorProfile[] getLineProfiles()
  {
    return lineProfile;
  }

  /**
   * Set the value of lineProfile
   *
   * @param lineProfile new value of lineProfile
   */
  public void setLineProfiles(VectorProfile[] lineProfile)
  {
    this.lineProfile = lineProfile;
  }

  /**
   * Get the value of lineProfile at specified index
   *
   * @param index
   * @return the value of lineProfile at specified index
   */
  public VectorProfile getLineProfile(int index)
  {
    return this.lineProfile[index];
  }

  /**
   * Set the value of lineProfile at specified index.
   *
   * @param index
   * @param newLineProfile new value of lineProfile at specified index
   */
  public void setLineProfile(int index, VectorProfile newLineProfile)
  {
    this.lineProfile[index] = newLineProfile;
  }
  protected String name = "Filz (rot)";

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
  protected float height = 4;

  /**
   * Get the value of height
   *
   * @return the value of height
   */
  public float getHeight()
  {
    return height;
  }

  /**
   * Set the value of height
   *
   * @param height new value of height
   */
  public void setHeight(float height)
  {
    this.height = height;
  }

  @Override
  public String toString()
  {
    return this.getName()+" ("+this.getHeight()+"mm)";
  }
}
