package com.t_oster.visicut.model;

import com.t_oster.visicut.gui.ImageListable;
import java.awt.Color;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 *
 * @author thommy
 */
public class MaterialProfile implements ImageListable, Cloneable
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

  
  protected Color cutColor = Color.GRAY;

  /**
   * Get the value of cutColor
   *
   * @return the value of cutColor
   */
  public Color getCutColor()
  {
    return cutColor;
  }

  /**
   * Set the value of cutColor
   *
   * @param cutColor new value of cutColor
   */
  public void setCutColor(Color cutColor)
  {
    this.cutColor = cutColor;
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
  protected LaserProfile[] laserProfile = new LaserProfile[]{new VectorProfile(), new RasterProfile()};

  /**
   * Get the value of lineProfile
   *
   * @return the value of lineProfile
   */
  public LaserProfile[] getLaserProfiles()
  {
    return laserProfile;
  }

  /**
   * Set the value of lineProfile
   *
   * @param lineProfile new value of lineProfile
   */
  public void setLaserProfiles(LaserProfile[] lineProfile)
  {
    this.laserProfile = lineProfile;
  }

  /**
   * Get the value of lineProfile at specified index
   *
   * @param index
   * @return the value of lineProfile at specified index
   */
  public LaserProfile getLaserProfile(int index)
  {
    return this.laserProfile[index];
  }
  
  /**
   * Returns the LaserProfile with the given Name.
   * If no Profile with this name is available,
   * null is returned.
   */
  public LaserProfile getLaserProfile(String name)
  {
    for (LaserProfile p :this.laserProfile)
    {
      if (p.getName().equals(name))
      {
        return p;
      }
    }
    return null;
  }

  /**
   * Set the value of lineProfile at specified index.
   *
   * @param index
   * @param newLineProfile new value of lineProfile at specified index
   */
  public void setLaserProfile(int index, LaserProfile newLineProfile)
  {
    this.laserProfile[index] = newLineProfile;
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
  protected int width = 600;
  public static final String PROP_WIDTH = "width";

  /**
   * Get the value of width
   *
   * @return the value of width
   */
  public int getWidth()
  {
    return width;
  }

  /**
   * Set the value of width
   *
   * @param width new value of width
   */
  public void setWidth(int width)
  {
    int oldWidth = this.width;
    this.width = width;
    propertyChangeSupport.firePropertyChange(PROP_WIDTH, oldWidth, width);
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

  protected int height = 300;
  public static final String PROP_HEIGHT = "height";

  /**
   * Get the value of height
   *
   * @return the value of height
   */
  public int getHeight()
  {
    return height;
  }

  /**
   * Set the value of height
   *
   * @param height new value of height
   */
  public void setHeight(int height)
  {
    int oldHeight = this.height;
    this.height = height;
    propertyChangeSupport.firePropertyChange(PROP_HEIGHT, oldHeight, height);
  }


  @Override
  public String toString()
  {
    return this.getName();
  }

  @Override
  public MaterialProfile clone()
  {
    MaterialProfile cp = new MaterialProfile();
    cp.setName(this.getName());
    cp.color = this.color;
    cp.cutColor = this.cutColor;
    cp.depth = this.depth;
    cp.description = this.description;
    cp.height = this.height;
    cp.thumbnailPath = this.thumbnailPath;
    cp.width = this.width;
    cp.laserProfile = new LaserProfile[this.laserProfile.length];
    for (int i=0;i<cp.laserProfile.length;i++)
    {
      cp.laserProfile[i] = this.laserProfile[i].clone();
    }
    return cp;
  }
}
