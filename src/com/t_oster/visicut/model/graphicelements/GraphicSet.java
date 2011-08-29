/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.visicut.model.graphicelements;

import com.t_oster.liblasercut.platform.Util;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.LinkedList;

/**
 *
 * @author thommy
 */
public class GraphicSet extends LinkedList<GraphicObject>
{

  protected AffineTransform transform = null;
  public static final String PROP_TRANSFORM = "transform";

  /**
   * Get the value of transform
   *
   * @return the value of transform
   */
  public AffineTransform getTransform()
  {
    return transform;
  }

  /**
   * Set the value of transform
   *
   * @param transform new value of transform
   */
  public void setTransform(AffineTransform transform)
  {
    AffineTransform oldTransform = this.transform;
    this.transform = transform;
    propertyChangeSupport.firePropertyChange(PROP_TRANSFORM, oldTransform, transform);
  }
  private PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

  public Rectangle2D getBoundingBox()
  {
    Rectangle2D result = new Rectangle();
    for (GraphicObject o : this)
    {
      Rectangle2D current = o.getBoundingBox();
      if (this.transform != null)
      {
        current = Util.transform(current, this.transform);
      }
      Rectangle2D.union(result, current, result);
    }
    return result;
  }
  
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

}
