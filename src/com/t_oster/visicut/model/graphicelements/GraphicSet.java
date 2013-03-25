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
package com.t_oster.visicut.model.graphicelements;

import com.t_oster.visicut.misc.Helper;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;

/**
 *
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class GraphicSet extends LinkedList<GraphicObject>
{

  public static String translateAttVal(Object att)
  {
    if (att == null)
    {
      return null;
    }
    if (att instanceof Number)
    {
      return "" + ((int) (((Number) att).doubleValue()*100))/100d;
    }
    String attribute = att.toString();
    try
    {
      return ResourceBundle.getBundle("com/t_oster/visicut/model/graphicelements/resources/AttributeTranslations").getString(attribute.toUpperCase());
    }
    catch (MissingResourceException ex)
    {
      return attribute;
    }
  }
  
  protected AffineTransform basicTransform = new AffineTransform();

  /**
   * Get the value of basicTransform
   *
   * @return the value of basicTransform
   */
  public AffineTransform getBasicTransform()
  {
    return basicTransform;
  }

  /**
   * Set the value of basicTransform
   * This value shall be used only for the import and for resetting
   * the transform of this object. So it should be written only
   * once after creation of the set
   *
   * @param basicTransform new value of basicTransform
   */
  public void setBasicTransform(AffineTransform basicTransform)
  {
    this.basicTransform = basicTransform;
    this.setTransform(new AffineTransform(basicTransform));
  }

  public AffineTransform transform = null;
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
    this.boundingBoxCache = null;
    firePropertyChange(PROP_TRANSFORM, oldTransform, transform);
  }
  private List<PropertyChangeListener> pcls = new LinkedList<PropertyChangeListener>();
  private Rectangle2D originalBoundingBoxCache = null;

  /**
   * Returns the BoundingBox of this Set ignoring the Transform,
   * also ignoring the basicTransform
   * @return 
   */
  public Rectangle2D getOriginalBoundingBox()
  {
    if (originalBoundingBoxCache == null)
    {
      for (GraphicObject o : this)
      {
        Rectangle2D current = o.getBoundingBox();
        if (originalBoundingBoxCache == null)
        {
          originalBoundingBoxCache = current;
        }
        else
        {
          Rectangle2D.union(originalBoundingBoxCache, current, originalBoundingBoxCache);
        }
      }
    }
    return originalBoundingBoxCache;
  }
  private Rectangle2D boundingBoxCache = null;

  /**
   * Returns the BoundingBox of this Set when rendered with the current
   * Transformation.
   * @return 
   */
  public Rectangle2D getBoundingBox()
  {
    if (boundingBoxCache == null)
    {
      for (GraphicObject o : this)
      {
        Rectangle2D current = o.getBoundingBox();
        if (current != null)
        {
          if (this.transform != null)
          {
            current = Helper.transform(current, this.transform);
          }
          if (boundingBoxCache == null)
          {
            boundingBoxCache = current;
          }
          else
          {
            Rectangle2D.union(boundingBoxCache, current, boundingBoxCache);
          }
        }
      }
      if (boundingBoxCache == null)
      {
        return new Rectangle2D.Double();
      }
    }
    return boundingBoxCache;
  }

  /**
   * Add PropertyChangeListener.
   *
   * @param listener
   */
  public void addPropertyChangeListener(PropertyChangeListener listener)
  {
    pcls.add(listener);
  }

  /**
   * Remove PropertyChangeListener.
   *
   * @param listener
   */
  public void removePropertyChangeListener(PropertyChangeListener listener)
  {
    pcls.remove(listener);
  }

  @Override
  public boolean add(GraphicObject o)
  {
    this.boundingBoxCache = null;
    if (this.attributesCache != null)
    {
      this.attributesCache.addAll(o.getAttributes());
    }
    this.interestingAttributesCache = null;
    return super.add(o);
  }
  
  public boolean remove(GraphicObject o)
  {
    this.boundingBoxCache = null;
    if (this.attributesCache != null)
    {
      this.attributesCache.removeAll(o.getAttributes());
    }
    this.interestingAttributesCache = null;
    return super.remove(o);
  }
  
  @Override
  public GraphicSet clone()
  {
    GraphicSet result = new GraphicSet();
    result.addAll(this);
    result.setTransform(this.getTransform());
    result.boundingBoxCache = boundingBoxCache;
    result.originalBoundingBoxCache = originalBoundingBoxCache;
    result.basicTransform = basicTransform;
    return result;
  }

  private Map<String, Set<Object>> attributeValueCache = null;
  public Set<Object> getAttributeValues(String attribute)
  {
    if (attributeValueCache == null)
    {
      attributeValueCache = new LinkedHashMap<String, Set<Object>>();
    }
    if (!attributeValueCache.containsKey(attribute))
    {
      Set<Object> values = new LinkedHashSet<Object>();
      for(GraphicObject o : this)
      {
        values.addAll(o.getAttributeValues(attribute));
      }
      attributeValueCache.put(attribute, values);
    }
    return attributeValueCache.get(attribute);
  }
  
  private Set<String> attributesCache = null;
  
  public Iterable<String> getAttributes()
  {
    if (attributesCache == null)
    {
      attributesCache = new LinkedHashSet();
      for(GraphicObject o:this)
      {
        attributesCache.addAll(o.getAttributes());
      }
    }
    return attributesCache;
  }
  
  private Set<String> interestingAttributesCache = null;
  /**
   * Returns only those attributes, where at least two different
   * values are present
   * @return 
   */
  public Iterable<String> getInterestingAttributes()
  {
    if (interestingAttributesCache == null)
    {
      interestingAttributesCache = new LinkedHashSet();
      for (String attribute : this.getAttributes())
      {
        //only makes sense if at least two properties are present
        if (this.getAttributeValues(attribute).size() > 1)
        {
          interestingAttributesCache.add(attribute);
        }
      }
    }
    return interestingAttributesCache;
  }

  public void rotateRelative(double angle)
  {
    Rectangle2D bb = this.getBoundingBox();
    //move back
    AffineTransform tr = AffineTransform.getTranslateInstance(bb.getCenterX(), bb.getCenterY());
    //rotate
    tr.concatenate(AffineTransform.getRotateInstance(angle));
    //center
    tr.concatenate(AffineTransform.getTranslateInstance(-bb.getCenterX(), -bb.getCenterY()));
    //apply current
    tr.concatenate(transform);
    this.setTransform(tr);
  }
  
  public void rotateAbsolute(double angle)
  {
    double old = transform != null ? Helper.getRotationAngle(transform) : 0;
    this.rotateRelative(angle-old);
  }

  private void firePropertyChange(String prop, Object oldValue, Object value)
  {
    PropertyChangeEvent e = new PropertyChangeEvent(this, prop, oldValue, value);
    for (PropertyChangeListener l : pcls)
    {
      l.propertyChange(e);
    }
  }
}
