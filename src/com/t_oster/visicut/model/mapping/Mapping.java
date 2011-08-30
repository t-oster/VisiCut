package com.t_oster.visicut.model.mapping;

import com.t_oster.liblasercut.platform.Tuple;

/**
 * A Mapping represents a Set of Filters to Match
 * Elements of the InputImage and a Target which is
 * a CuttingProfile, specifying how the matched 
 * Elements should be rendered and cut on the LaserCutter
 * @author thommy
 */
public class Mapping extends Tuple<FilterSet,String>
{
  public Mapping(FilterSet filterSet, String profileName)
  {
    super(filterSet,profileName);
  }
  
  public FilterSet getFilterSet()
  {
    return this.getA();
  }
  
  public String getProfileName()
  {
    return this.getB();
  }
  
  @Override
  public String toString()
  {
    String fs = "";
    for (MappingFilter f:this.getA())
    {
      fs+="/"+f.toString();
    }
    return fs+"=>"+this.getB();
  }
}
