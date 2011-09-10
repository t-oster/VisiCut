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
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 *
 * @author thommy
 */
public class MappingFilter
{

  protected boolean inverted = false;
  public static final String PROP_INVERTED = "inverted";

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
    boolean oldInverted = this.inverted;
    this.inverted = inverted;
    propertyChangeSupport.firePropertyChange(PROP_INVERTED, oldInverted, inverted);
  }
  private PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

  /**
   * Add PropertyChangeListener.
   *
   * @param listener
   */
  public void addPropertyChangeListener(PropertyChangeListener listener)
  {
    propertyChangeSupport.addPropertyChangeListener(listener);
  }

  /**
   * Remove PropertyChangeListener.
   *
   * @param listener
   */
  public void removePropertyChangeListener(PropertyChangeListener listener)
  {
    propertyChangeSupport.removePropertyChangeListener(listener);
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
    if (inverted)
    {
      return !(attribute == null || e.getAttributeValues(attribute).contains(value));
    }
    else
    {
      return attribute == null || e.getAttributeValues(attribute).contains(value);
    }
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
    for (GraphicObject e : elements)
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
    String result;
    if (value == null)
    {
      result = "null";
    }
    else if (value instanceof Color)
    {
      result = Helper.toHtmlRGB((Color) value);
    }
    else
    {
      result = value.toString();
    }
    return (inverted ? "IS NOT " : "IS ")+result;
  }

  @Override
  public int hashCode()
  {
    int hash = 7;
    hash = 29 * hash + (this.attribute != null ? this.attribute.hashCode() : 0);
    hash = 29 * hash + (this.value != null ? this.value.hashCode() : 0);
    hash = 29 * hash + (this.inverted ? 1 : 0);
    return hash;
  }

  @Override
  public boolean equals(Object o)
  {
    if (o instanceof MappingFilter)
    {
      MappingFilter f = (MappingFilter) o;
      return f.inverted == inverted && !Util.differ(f.attribute, attribute) && !Util.differ(f.value, value); 
    }
    return super.equals(o);
  }

  @Override
  public MappingFilter clone()
  {
    MappingFilter result = new MappingFilter(attribute, value);
    result.inverted = inverted;
    return result;
  }
}
