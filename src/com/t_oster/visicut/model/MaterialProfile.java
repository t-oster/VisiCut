/**
 * This file is part of VisiCut.
 * Copyright (C) 2011 Thomas Oster <thomas.oster@rwth-aachen.de>
 * RWTH Aachen University - 52062 Aachen, Germany
 * 
 *     VisiCut is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *    VisiCut is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 * 
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with VisiCut.  If not, see <http://www.gnu.org/licenses/>.
 **/
package com.t_oster.visicut.model;

import com.t_oster.visicut.gui.ImageListable;
import java.awt.Color;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.text.Collator;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class MaterialProfile implements ImageListable, Cloneable, Comparable
{
  
  protected String description = null;

  /**
   * Get the value of description
   *
   * @return the value of description
   */
  public String getDescription()
  {
    return description;
  }

  /**
   * Set the value of description
   *
   * @param description new value of description
   */
  public void setDescription(String description)
  {
    this.description = description;
  }

  protected String thumbnailPath = null;

  /**
   * Get the value of thumbnailPath
   *
   * @return the value of thumbnailPath
   */
  public String getThumbnailPath()
  {
    return thumbnailPath;
  }

  /**
   * Set the value of thumbnailPath
   *
   * @param thumbnailPath new value of thumbnailPath
   */
  public void setThumbnailPath(String thumbnailPath)
  {
    this.thumbnailPath = thumbnailPath;
  }

  protected Color color = null;

  /**
   * Get the value of color
   *
   * @return the value of color
   */
  public Color getColor()
  {
    return color;
  }

  /**
   * Set the value of color
   *
   * @param color new value of color
   */
  public void setColor(Color color)
  {
    this.color = color;
  }
  protected List<LaserProfile> laserProfiles = new LinkedList<LaserProfile>();
  public static final String PROP_LASERPROFILES = "laserProfiles";

  /**
   * Get the value of laserProfiles
   *
   * @return the value of laserProfiles
   */
  public List<LaserProfile> getLaserProfiles()
  {
    return laserProfiles;
  }

  /**
   * Set the value of laserProfiles
   *
   * @param laserProfiles new value of laserProfiles
   */
  public void setLaserProfiles(List<LaserProfile> laserProfiles)
  {
    List<LaserProfile> oldLaserProfiles = this.laserProfiles;
    this.laserProfiles = laserProfiles;
    propertyChangeSupport.firePropertyChange(PROP_LASERPROFILES, oldLaserProfiles, laserProfiles);
  }

  
  /**
   * Returns the LaserProfile with the given Name.
   * If no Profile with this name is available,
   * null is returned.
   */
  public LaserProfile getLaserProfile(String name)
  {
    for (LaserProfile p :this.laserProfiles)
    {
      if (p.getName().equals(name))
      {
        return p;
      }
    }
    return null;
  }

  protected String name = "Unnamed Material";

  /**
   * Get the value of name
   *
   * @return the value of name
   */
  public String getName()
  {
    return name;
  }

  /**
   * Set the value of name
   *
   * @param name new value of name
   */
  public void setName(String name)
  {
    this.name = name;
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

  protected float depth = 4;
  public static final String PROP_DEPTH = "depth";

  /**
   * Get the value of depth
   *
   * @return the value of depth
   */
  public float getDepth()
  {
    return depth;
  }

  /**
   * Set the value of depth
   *
   * @param depth new value of depth
   */
  public void setDepth(float depth)
  {
    float oldDepth = this.depth;
    this.depth = depth;
    propertyChangeSupport.firePropertyChange(PROP_DEPTH, oldDepth, depth);
  }

  @Override
  public String toString()
  {
    return this.getName()+" ("+this.getDepth()+" mm)";
  }

  @Override
  public MaterialProfile clone()
  {
    MaterialProfile cp = new MaterialProfile();
    cp.setName(this.getName());
    cp.color = this.color;
    cp.depth = this.depth;
    cp.description = this.description;
    cp.thumbnailPath = this.thumbnailPath;
    cp.laserProfiles = new LinkedList<LaserProfile>();
    for (LaserProfile lp:this.laserProfiles)
    {
      cp.laserProfiles.add(lp.clone());
    }
    return cp;
  }

  public int compareTo(Object t)
  {
    if (t instanceof MaterialProfile)
    {
      return Collator.getInstance().compare(this.name, ((MaterialProfile) t).getName());
    }
    return 1;
  }
}
