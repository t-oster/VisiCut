/**
 * This file is part of VisiCut.
 * Copyright (C) 2011 - 2013 Thomas Oster <thomas.oster@rwth-aachen.de>
 * RWTH Aachen University - 52062 Aachen, Germany
 *
 *     VisiCut is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     VisiCut is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with VisiCut.  If not, see <http://www.gnu.org/licenses/>.
 **/
package com.t_oster.visicut.model.mapping;

import com.t_oster.liblasercut.platform.Tuple;
import com.t_oster.visicut.model.LaserProfile;

/**
 * A Mapping represents a Set of Filters to Match
 * Elements of the InputImage and a Target which is
 * a CuttingProfile, specifying how the matched 
 * Elements should be rendered and cut on the LaserCutter
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class Mapping extends Tuple<FilterSet,LaserProfile> implements Cloneable
{
  public Mapping()
  {
  }
  
  public Mapping(FilterSet filterSet, LaserProfile profile)
  {
    super(filterSet,profile);
  }
  
  public FilterSet getFilterSet()
  {
    return this.getA();
  }
  
  public void setFilterSet(FilterSet f)
  {
    this.setA(f);
  }
  
  public LaserProfile getProfile()
  {
    return this.getB();
  }
  
  public void setProfile(LaserProfile profile)
  {
    this.setB(profile);
  }
  
  @Override
  public String toString()
  {
    String fs = "";
    for (MappingFilter f:this.getA())
    {
      fs+= fs.equals("") ? f.toString() : ","+f.toString();
    }
    return fs+"->"+this.getB();
  }
  
  @Override
  public Mapping clone()
  {
    return new Mapping(this.getFilterSet() != null ? (FilterSet) this.getFilterSet().clone() : null, this.getProfile() != null ? this.getProfile().clone() : null);
  }
}
