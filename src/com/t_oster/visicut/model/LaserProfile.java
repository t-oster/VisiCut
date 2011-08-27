/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.visicut.model;

import com.t_oster.liblasercut.LaserJob;
import com.t_oster.liblasercut.LaserProperty;
import com.t_oster.visicut.model.graphicelements.GraphicObject;
import java.awt.Color;
import java.awt.Graphics2D;
import java.io.File;
import java.util.List;

/**
 * A cutting Profile represents a specific way of handling Image
 * Parts. This means a CuttingProfile provides methods
 * to generate preview and laser data out of Graphic parts.
 * @author thommy
 */
public abstract class LaserProfile
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
  protected Color color = new Color(0, 0, 0);

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
  
  public abstract void renderPreview(Graphics2D g, List<GraphicObject> objects);
  
  public abstract void addToLaserJob(LaserJob job, List<GraphicObject> objects);
}
