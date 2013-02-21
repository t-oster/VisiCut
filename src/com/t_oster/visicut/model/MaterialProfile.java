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
package com.t_oster.visicut.model;

import com.t_oster.uicomponents.ImageListable;
import java.awt.Color;
import java.text.Collator;
import java.util.LinkedList;

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

  protected Color engraveColor = new Color(0, 0, 0);

  /**
   * Get the value of color
   *
   * @return the value of color
   */
  public Color getEngraveColor()
  {
    return engraveColor;
  }

  /**
   * Set the value of color
   *
   * @param color new value of color
   */
  public void setEngraveColor(Color color)
  {
    this.engraveColor = color;
  }

  protected Color cutColor = new Color(0, 0, 0);

  /**
   * Get the value of color
   *
   * @return the value of color
   */
  public Color getCutColor()
  {
    return cutColor;
  }

  /**
   * Set the value of color
   *
   * @param color new value of color
   */
  public void setCutColor(Color color)
  {
    this.cutColor = color;
  }

  protected Color color = Color.WHITE;

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

  protected String name = java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/resources/EditMaterialsDialog").getString("UNNAMED MATERIAL");

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

  protected LinkedList<Float> materialThicknesses = new LinkedList<Float>();

  /**
   * Get the value of materialThicknesses
   *
   * @return the value of materialThicknesses
   */
  public LinkedList<Float> getMaterialThicknesses()
  {
    if (materialThicknesses == null)
    {
      materialThicknesses = new LinkedList<Float>();
      materialThicknesses.add(2f);
    }
    return materialThicknesses;
  }

  /**
   * Set the value of materialThicknesses
   *
   * @param materialThicknesses new value of materialThicknesses
   */
  public void setMaterialThicknesses(LinkedList<Float> materialThicknesses)
  {
    this.materialThicknesses = materialThicknesses;
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
    cp.engraveColor = this.engraveColor;
    cp.description = this.description;
    cp.thumbnailPath = this.thumbnailPath;
    for (Float f : this.materialThicknesses)
    {
      cp.materialThicknesses.add(f);
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
