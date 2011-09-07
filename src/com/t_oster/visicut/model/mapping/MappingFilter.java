/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.visicut.model.mapping;

import com.t_oster.liblasercut.platform.Util;
import com.t_oster.visicut.misc.Helper;
import com.t_oster.visicut.model.graphicelements.GraphicObject;
import com.t_oster.visicut.model.graphicelements.GraphicSet;
import java.awt.Color;

/**
 *
 * @author thommy
 */
public class MappingFilter
{
  private String attribute;
  private Object value;

  public String getAttribute()
  {
    return attribute;
  }
  
  public Object getValue()
  {
    return value;
  }
  
  public void setAttribute(String attribute)
  {
    this.attribute = attribute;
  }
  
  public void setValue(Object value)
  {
    this.value = value;
  }
  
  public final boolean matches(GraphicObject e)
  {
    return attribute == null || e.getAttributeValues(attribute).contains(value);
  }

  public MappingFilter()
  {
    
  }
  
  public MappingFilter(String attribute, Object value)
  {
    this.attribute = attribute;
    this.value = value;
  }

  public GraphicSet getMatchingElements(GraphicSet elements)
  {
    GraphicSet result = new GraphicSet();
    result.setTransform(elements.getTransform());
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
    if (value == null)
    {
      return "null";
    }
    else if (value instanceof Color)
    {
      return Helper.toHtmlRGB((Color) value);
    }
    else
    {
      return value.toString();
    }
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
      return !Util.differ(f.attribute, attribute) && !Util.differ(f.value, value);
    }
    return super.equals(o);
  }
}
