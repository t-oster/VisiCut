/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.visicut.model;

import com.t_oster.liblasercut.LaserProperty;
import java.io.File;

/**
 * A cutting Profile represents a specific way of handling Image
 * Parts. This means a CuttingProfile provides methods
 * to generate preview and laser data out of Graphic parts.
 * @author thommy
 */
abstract class CuttingProfile
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
