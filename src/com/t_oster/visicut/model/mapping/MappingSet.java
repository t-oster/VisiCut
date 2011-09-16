/**
 * This file is part of VisiCut.
 * 
 *     VisiCut is free software: you can redistribute it and/or modify
 *     it under the terms of the Lesser GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *    VisiCut is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     Lesser GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with VisiCut.  If not, see <http://www.gnu.org/licenses/>.
 **/
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
  
  @Override
  public MappingSet clone()
  {
    MappingSet result = new MappingSet();
    result.name = name;
    for (Mapping m:this)
    {
      result.add(m.clone());
    }
    return result;
  }

  @Override
  public boolean equals(Object obj)
  {
    if (obj == null)
    {
      return false;
    }
    if (getClass() != obj.getClass())
    {
      return false;
    }
    final MappingSet other = (MappingSet) obj;
    if ((this.name == null) ? (other.name != null) : !this.name.equals(other.name))
    {
      return false;
    }
    if (other.size() != this.size())
    {
      return false;
    }
    return super.equals(obj);
  }

  @Override
  public int hashCode()
  {
    int hash = 7;
    hash = 13 * hash + (this.name != null ? this.name.hashCode() : 0);
    return hash;
  }
}
