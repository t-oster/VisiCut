/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.visicut.model.mapping;

import com.t_oster.liblasercut.platform.Util;
import com.t_oster.visicut.model.graphicelements.GraphicObject;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author thommy
 */
public class MappingFilter
{
  private String attribute;
  private Object value;

  public final boolean matches(GraphicObject e)
  {
    return e.getAttributeValues(attribute).contains(value);
  }

  public MappingFilter(String attribute, Object value)
  {
    this.attribute = attribute;
    this.value = value;
  }

  public List<GraphicObject> getMatchingElements(List<GraphicObject> elements)
  {
    List<GraphicObject> result = new LinkedList<GraphicObject>();
    for (GraphicObject e:elements)
    {
      if (this.matches(e))
      {
        result.add(e);
      }
    }
    return result;
  }

  @Override
  public String toString()
  {
    return value == null ? "null" : value.toString();
  }

  @Override
  public int hashCode()
  {
    int hash = 7;
    hash = 29 * hash + (this.attribute != null ? this.attribute.hashCode() : 0);
    hash = 29 * hash + (this.value != null ? this.value.hashCode() : 0);
    return hash;
  }
  
  @Override
  public boolean equals(Object o)
  {
    if (o instanceof MappingFilter)
    {
      MappingFilter f = (MappingFilter) o;
      return f.attribute.equals(attribute) && !Util.differ(f.value, value);
    }
    return super.equals(o);
  }
}
