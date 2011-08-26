package com.t_oster.visicut.model;

import com.t_oster.liblasercut.LaserProperty;
import java.awt.Color;
import java.io.File;

/**
 * This class represents a Line Profile,
 * which means a kind of line which can be
 * cut with the lasercutten in a specified
 * Material
 * 
 * @author thommy
 */
public class LineProfile
{

  protected LaserProperty cuttingProperty = new LaserProperty();

  /**
   * Get the value of cuttingProperty
   *
   * @return the value of cuttingProperty
   */
  public LaserProperty getCuttingProperty()
  {
    return cuttingProperty;
  }

  /**
   * Set the value of cuttingProperty
   *
   * @param cuttingProperty new value of cuttingProperty
   */
  public void setCuttingProperty(LaserProperty cuttingProperty)
  {
    this.cuttingProperty = cuttingProperty;
  }
  protected float width = 1;

  /**
   * Get the value of width
   *
   * @return the value of width
   */
  public float getWidth()
  {
    return width;
  }

  /**
   * Set the value of width
   *
   * @param width new value of width
   */
  public void setWidth(float width)
  {
    this.width = width;
  }
  protected Color color = new Color(0,0,0);

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
  protected File previewThumbnail = new File("/home/thommy/NetBeansProjects/jepilog/materials/Fliess/bigcut.png");

  /**
   * Get the value of previewThumbnail
   *
   * @return the value of previewThumbnail
   */
  public File getPreviewThumbnail()
  {
    return previewThumbnail;
  }

  /**
   * Set the value of previewThumbnail
   *
   * @param previewThumbnail new value of previewThumbnail
   */
  public void setPreviewThumbnail(File previewThumbnail)
  {
    this.previewThumbnail = previewThumbnail;
  }
  protected String name = "broad line";

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

}
