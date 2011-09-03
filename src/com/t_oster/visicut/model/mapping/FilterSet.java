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
 * @author thommy
 */
public class FilterSet extends LinkedList<MappingFilter>
  {

    public GraphicSet getMatchingObjects(GraphicSet elements)
    {
      GraphicSet result = new GraphicSet();
      result.setTransform(elements.getTransform());
      for (GraphicObject o : elements)
      {
        boolean passed = true;
        for (MappingFilter filter : this)
        {
          if (!filter.matches(o))
          {
            passed = false;
            break;
          }
        }
        if (passed)
        {
          result.add(o);
        }
      }
      return result;
    }
    
  @Override
    public String toString()
    {
      if (this.size()==0)
      {
        return "Everything";
      }
      else
      {
        return this.get(this.size()-1).toString();
      }
    }
}
