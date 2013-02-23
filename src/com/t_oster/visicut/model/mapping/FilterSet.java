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

import com.t_oster.visicut.model.graphicelements.GraphicObject;
import com.t_oster.visicut.model.graphicelements.GraphicSet;
import java.util.LinkedList;

/**
 *
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class FilterSet extends LinkedList<MappingFilter>
{
  
  /**
   * is "multiple selection" enabled in the GUI for this filter?
   * only affects the GUI, not the behaviour of the FilterSet
   */ 
  public boolean multiselectEnabled;
  
  /**
   * false: normal behaviour
   * true: pseudo-FilerSet that matches everything unmatched by any other FilterSet of the same PlfPart
   */
  public boolean matchEverythingElse;
  
  /**
   * filter a GraphicSet, return only matching elements
   * this may not be called if matchEverythingElse==true, use PlfPart.getMappedGraphicObjects instead in this case!
   * @param elements elements to be matched
   * @return matching subset of the given elements
   */
  public GraphicSet getMatchingObjects(GraphicSet elements)
  {
    if (this.matchEverythingElse) {
      throw new RuntimeException("getMatchingObjects must not be called when matchEverythingElse==true. Use PlfPart.getMappedGraphicObjects instead!");
      // we cannot work around this because we would introduce the possibility of infinite recursion loops otherwise! PlfPart.getMappedGraphicsObjects calls this function.
    }
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
      
      // equals() does NOT compare multiselectEnabled, because the behaviour is still equal.
      // TODO: are there possible side effects of comparing multiselectEnabled or not doing so?
      
      if (f.matchEverythingElse != matchEverythingElse) {
        return false;
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
    result.multiselectEnabled=multiselectEnabled;
    result.matchEverythingElse=matchEverythingElse;
    return result;
  }
}
