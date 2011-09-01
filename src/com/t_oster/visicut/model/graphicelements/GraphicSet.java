/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.visicut.model.graphicelements;

import com.t_oster.liblasercut.platform.Util;
import com.t_oster.visicut.Helper;
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
   * Get the Translate Part of the current Transform
   * aka the offset, points are moved before rendering
   * @return 
   */
  public AffineTransform getTranslatePart()
  {
    return transform == null ? AffineTransform.getTranslateInstance(0, 0) : AffineTransform.getTranslateInstance(transform.getTranslateX(), transform.getTranslateY());
  }

  public AffineTransform getScalePart()
  {
    return transform == null ? AffineTransform.getScaleInstance(1, 1) : AffineTransform.getScaleInstance(transform.getScaleX(), transform.getScaleY());
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
   * Returns the BoundingBox of this Set ignoring the Transform
   * @return 
   */
  public Rectangle2D getOriginalBoundingBox()
  {
    Rectangle2D result = null;
    for (GraphicObject o : this)
    {
      Rectangle2D current = o.getBoundingBox();
      if (result == null)
      {
        result = current;
      }
      else
      {
        Rectangle2D.union(result, current, result);
      }
    }
    return result;
  }
  
  /**
   * Returns the BoundingBox of this Set when rendered with the current
   * Transformation.
   * @return 
   */
  public Rectangle2D getBoundingBox()
  {
    Rectangle2D result = null;
    for (GraphicObject o : this)
    {
      Rectangle2D current = o.getBoundingBox();
      if (this.transform != null)
      {
        current = Helper.transform(current, this.transform);
      }
      if (result == null)
      {
        result = current;
      }
      else
      {
        Rectangle2D.union(result, current, result);
      }
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
