package com.t_oster.visicut.model;

import com.t_oster.liblasercut.platform.Tuple;
import com.t_oster.visicut.model.mapping.FilterSet;
import com.t_oster.visicut.model.mapping.MappingFilter;

/**
 * A Mapping represents a Set of Filters to Match
 * Elements of the InputImage and a Target which is
 * a CuttingProfile, specifying how the matched 
 * Elements should be rendered and cut on the LaserCutter
 * @author thommy
 */
public class Mapping extends Tuple<FilterSet,LaserProfile>
{
  public Mapping(FilterSet fs, LaserProfile pf)
  {
    super(fs,pf);
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
