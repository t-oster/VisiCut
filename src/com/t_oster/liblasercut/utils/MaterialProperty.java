/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.liblasercut.utils;

import com.t_oster.liblasercut.LaserProperty;
import com.t_oster.liblasercut.LaserProperty;

/**
 *
 * @author oster
 */
public class MaterialProperty
{

  private String name = "Default Material";
  private double height = 1;
  private LaserProperty cuttingProperty = new LaserProperty();
  private LaserProperty LaserProperty = new LaserProperty();

  public MaterialProperty()
  {
  }

  public MaterialProperty(String name, double height, int engravingPower, int engravingSpeed, int cuttingPower, int cuttingSpeed, int cuttingFrequency)
  {
    this.name = name;
    this.height = height;
    this.LaserProperty = new LaserProperty(engravingPower, engravingSpeed);
    this.cuttingProperty = new LaserProperty(cuttingPower, cuttingSpeed, cuttingFrequency);
  }

  public String getName()
  {
    return this.name;
  }

  public double getHeight()
  {
    return this.height;
  }

  public LaserProperty getLaserProperty()
  {
    return this.LaserProperty;
  }

  /**
   * Returns the cuttingProperty to cut completely through the material
   * @return 
   */
  public LaserProperty getCuttingProperty()
  {
    return this.cuttingProperty;
  }

  /**
   * Tries to interpolate the CuttingProperty to cut exactly depth mm
   * into the material
   * @param depth
   * @return 
   */
  public LaserProperty getCuttingProperty(double depth)
  {
    if (depth > height)
    {
      throw new IllegalArgumentException(("Can't cut deeper than the Material is"));
    }
    else
    {
      return new LaserProperty(
        (int) (this.cuttingProperty.getPower() * depth / height),
        this.cuttingProperty.getSpeed(),
        this.cuttingProperty.getFrequency());
    }
  }

  /**
   * Tries to guess how deep the cutting will be with the given cutting property
   * This should give at least the same value when the property is generated
   * via getCuttingProperty(depth). If it is unknown it can either guess
   * or return -1
   * @param cp
   * @return estimated cutting depth or -1
   */
  public double getCuttingPropertyDepth(LaserProperty cp)
  {
    if (cp.getFrequency() == this.cuttingProperty.getFrequency() && cp.getSpeed() == this.cuttingProperty.getSpeed())
    {
      return cp.getPower() * this.height / this.cuttingProperty.getPower();
    }
    else
    {
      return -1;
    }
  }

  @Override
  public String toString()
  {
    return this.getName() + " (" + this.getHeight() + " mm)";
  }
}
