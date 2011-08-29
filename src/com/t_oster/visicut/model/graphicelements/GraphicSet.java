/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.visicut.model.graphicelements;

import java.awt.geom.AffineTransform;
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
