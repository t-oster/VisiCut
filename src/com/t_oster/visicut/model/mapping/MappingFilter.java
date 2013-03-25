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

import com.t_oster.liblasercut.platform.Util;
import com.t_oster.visicut.misc.Helper;
import com.t_oster.visicut.model.graphicelements.GraphicObject;
import com.t_oster.visicut.model.graphicelements.GraphicSet;
import java.awt.Color;

/**
 *
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class MappingFilter
{

  /**
   * If compare is false, the filter will
   * use "=" or "!=" (when inverted),
   * if compare is true, the filter will
   * use "<=" or ">=" (when inverted)
   */
  protected boolean compare = false;

  public boolean isCompare()
  {
    return compare;
  }

  public void setCompare(boolean compare)
  {
    this.compare = compare;
  }
  
  protected boolean inverted = false;

  /**
   * Get the value of inverted
   *
   * @return the value of inverted
   */
  public boolean isInverted()
  {
    return inverted;
  }

  /**
   * Set the value of inverted
   *
   * @param inverted new value of inverted
   */
  public void setInverted(boolean inverted)
  {
    this.inverted = inverted;
  }

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
    boolean result;
    if (value instanceof Number)
    {
      if (e.getAttributeValues(attribute).isEmpty()) {
        // object does not have the relevant attribute -> does not match (except if inverted)
        return inverted;
      } 
      double number = ((Number) value).doubleValue();
      double other = ((Number) e.getAttributeValues(attribute).get(0)).doubleValue();
      result = attribute == null || (compare ? other <= number : other == number);
    }
    else
    {
      result = attribute == null || e.getAttributeValues(attribute).contains(value);
    }
    return inverted ? !result : result;
  }

  public MappingFilter()
  {
  }

  public MappingFilter(String attribute, Object value)
  {
    this.attribute = attribute;
    this.value = value;
  }

  private GraphicSet getMatchingElements(GraphicSet elements, boolean invert)
  {
    GraphicSet result = new GraphicSet();
    result.setBasicTransform(elements.getBasicTransform());
    result.setTransform(elements.getTransform());
    for (GraphicObject e : elements)
    {
      if ((!invert && this.matches(e)) || (invert && !this.matches(e)))
      {
        result.add(e);
      }
    }
    return result;
  }
  
  public GraphicSet getNotMatchingElements(GraphicSet elements)
  {
    return this.getMatchingElements(elements, true);
  }
  
  public GraphicSet getMatchingElements(GraphicSet elements)
  {
    return this.getMatchingElements(elements, false);
  }

  @Override
  public String toString()
  {
    return GraphicSet.translateAttVal(attribute) + " " +getValueString();
  }

  public String getValueString()
  {
    String result = " ";
    if (value == null)
    {
      result += "null";
    }
    else if (value instanceof Color)
    {
      result += Helper.toHtmlRGB((Color) value);
    }
    else
    {
      result += GraphicSet.translateAttVal(value);
    }
    if (compare)
    {
      return (inverted ? GraphicSet.translateAttVal(">=") : GraphicSet.translateAttVal("<="))+result;
    }
    else
    {
      return (inverted ? GraphicSet.translateAttVal("IS NOT") : GraphicSet.translateAttVal("IS"))+result;
    }
    
  }
  
  @Override
  public int hashCode()
  {
    int hash = 7;
    hash = 29 * hash + (this.attribute != null ? this.attribute.hashCode() : 0);
    hash = 29 * hash + (this.value != null ? this.value.hashCode() : 0);
    hash = 29 * hash + (this.inverted ? 1 : 0);
    hash = 29 * hash + (this.compare ? 1 : 0);
    return hash;
  }

  @Override
  public boolean equals(Object o)
  {
    if (o instanceof MappingFilter)
    {
      MappingFilter f = (MappingFilter) o;
      return f.compare == compare && f.inverted == inverted && !Util.differ(f.attribute, attribute) && !Util.differ(f.value, value); 
    }
    return super.equals(o);
  }

  @Override
  public MappingFilter clone()
  {
    MappingFilter result = new MappingFilter(attribute, value);
    result.inverted = inverted;
    result.compare = compare;
    return result;
  }
}
