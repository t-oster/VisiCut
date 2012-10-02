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

import com.t_oster.liblasercut.LaserJob;
import com.t_oster.liblasercut.LaserProperty;
import com.t_oster.visicut.gui.ImageListable;
import com.t_oster.visicut.misc.Helper;
import com.t_oster.visicut.model.graphicelements.GraphicObject;
import com.t_oster.visicut.model.graphicelements.GraphicSet;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.LinkedList;
import java.util.List;

/**
 * A cutting Profile represents a specific way of handling Image
 * Parts. This means a CuttingProfile provides methods
 * to generate preview and laser data out of Graphic parts.
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public abstract class LaserProfile implements ImageListable, Cloneable
{

  protected String description = "A new Laserprofile";

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

  protected String name = "Unnamed Profile";

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

  public abstract void renderPreview(Graphics2D g, GraphicSet objects, MaterialProfile material);

  public abstract void addToLaserJob(LaserJob job, GraphicSet objects, List<LaserProperty> laserProperties);

  @Override
  public String toString()
  {
    return (this.getName() != null ? this.getName() : super.toString());
  }

  @Override
  public abstract LaserProfile clone();
  
  /**
   * Decomposes a GraphicSet into disjoint paths which
   * have a distance bigger than the sum of their lengths
   * @param set
   * @return 
   */
  public LinkedList<GraphicSet> decompose(GraphicSet set)
  {
    LinkedList<GraphicSet> result = new LinkedList<GraphicSet>();
    //TODO: Develope a good algorithm. For now we just
    //return a set containing a single set
    if (true)
    {
      result.add(set);
      return result;
    }
    for (GraphicObject o:set)
    {
      //We assign every Object to a result bin
      //first we get the dimension of the object
      Rectangle2D bb = o.getBoundingBox();
      if (set.getTransform() != null)
      {//and transform it accordingly
        bb = Helper.transform(bb, set.getTransform());
      }
      findBin:
      {//now we see if we have a bin which is near enough
        for (GraphicSet s:result)
        {
          Rectangle2D sbb = s.getBoundingBox();
          if (Helper.distance(bb, sbb) < Math.min(bb.getWidth()+sbb.getWidth(), bb.getHeight()+sbb.getHeight()))
          {
            s.add(o);
            break findBin;
          }
        }
        //no bin found => create a new one
        GraphicSet s = new GraphicSet();
        s.setBasicTransform(set.getBasicTransform());
        s.setTransform(set.getTransform());
        s.add(o);
      }
    }
    //Now merge the bins until no one is near enough
    //TODO: Efficient merging?
    return result;
  }
  
}
