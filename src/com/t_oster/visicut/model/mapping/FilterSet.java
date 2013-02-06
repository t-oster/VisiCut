/**
 * This file is part of VisiCut.
 ** Copyright (C) 2013 Thomas Oster <thomas.oster@rwth-aachen.de>
 * RWTH Aachen University - 52062 Aachen, Germany
 * 
 *     VisiCut is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *    VisiCut is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 * 
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with VisiCut.  If not, see <http://www.gnu.org/licenses/>.
 **/
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.visicut.model.mapping;

import com.t_oster.visicut.model.graphicelements.GraphicObject;
import com.t_oster.visicut.model.graphicelements.GraphicSet;
import java.util.LinkedList;

/**
 *
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class FilterSet extends LinkedList<MappingFilter>
{

  public GraphicSet getMatchingObjects(GraphicSet elements)
  {
    if (this.isEmpty())
    {
      return elements;
    }
    GraphicSet result = new GraphicSet();
    if (elements==null)
    {
      return result;
    }
    result.setTransform(elements.getTransform());
    for (GraphicObject o : elements)
    {
      for (MappingFilter filter : this)
      {
        if (filter.matches(o))
        {
          result.add(o);
          break;
        }
      }
    }
    return result;
  }
  
  @Override
  public String toString()
  {
    if (this.size() == 0)
    {
      return "Everything";
    }
    else
    {
      return this.get(this.size() - 1).toString();
    }
  }
  
  @Override
  public boolean equals(Object o)
  {
    if (o instanceof FilterSet)
    {
      FilterSet f = (FilterSet) o;
      if (f.size() != this.size())
      {
        return false;
      }
      for (int i=0; i< f.size();i++)
      {
        if (!f.get(i).equals(this.get(i)))
        {
          return false;
        }
      }
      return true;
    }
    return false;
  }
  
  @Override
  public FilterSet clone()
  {
    FilterSet result = new FilterSet();
    for (MappingFilter f:this)
    {
      result.add(f.clone());
    }
    return result;
  }
}
