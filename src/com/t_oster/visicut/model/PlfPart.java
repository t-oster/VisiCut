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

import com.t_oster.liblasercut.platform.Util;
import com.t_oster.visicut.model.graphicelements.GraphicSet;
import com.t_oster.visicut.model.mapping.Mapping;
import com.t_oster.visicut.model.mapping.MappingSet;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;

/**
 *
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class PlfPart {
  private File sourceFile;
  private GraphicSet graphicObjects;
  private MappingSet mapping;

  public File getSourceFile()
  {
    return sourceFile;
  }

  public void setSourceFile(File sourceFile)
  {
    this.sourceFile = sourceFile;
  }

  public GraphicSet getGraphicObjects()
  {
    return graphicObjects;
  }

  private PropertyChangeListener boundingBoxListener = new PropertyChangeListener(){
    public void propertyChange(PropertyChangeEvent pce)
    {
      boundingBoxCache = null;
    }
  };
  
  public void setGraphicObjects(GraphicSet graphicObjects)
  {
    if (Util.differ(graphicObjects, this.graphicObjects))
    {
      if (this.graphicObjects != null)
      {
        this.graphicObjects.removePropertyChangeListener(boundingBoxListener);
      }
      this.graphicObjects = graphicObjects;
      boundingBoxCache = null;
      if (this.graphicObjects != null)
      {
        this.graphicObjects.addPropertyChangeListener(boundingBoxListener);
      }
    }
  }

  public MappingSet getMapping()
  {
    return mapping;
  }

  public void setMapping(MappingSet mapping)
  {
    if (Util.differ(mapping, this.mapping))
    {
      boundingBoxCache = null;
    }
    this.mapping = mapping;
  }
    
  private Rectangle2D boundingBoxCache = null;
  /*
   * Returns the bounding box respecting the
   * current mapping
   */
  public Rectangle2D getBoundingBox()
  {
    if (boundingBoxCache == null)
    {
      GraphicSet objects = this.graphicObjects;
      if (this.mapping != null && !this.mapping.isEmpty())
      {
        objects = new GraphicSet();
        objects.setBasicTransform(this.graphicObjects.getBasicTransform());
        objects.setTransform(this.graphicObjects.getTransform());
        for (Mapping m : this.mapping)
        {
          if (m.getProfile() != null)
          {
            if (m.getFilterSet() == null)
            {
              objects.addAll(this.getUnmatchedObjects());
            }
            else
            {
              objects.addAll(m.getFilterSet().getMatchingObjects(this.graphicObjects));
            }
          }
        }
      }
      boundingBoxCache = objects.getBoundingBox();
    }
    return boundingBoxCache;
  }
  
  @Override
  public String toString() {
    // this is needed for the strings of the items in MainView.objectComboBox
    // (JComboBox displays the toString() value of each object)
    if (sourceFile == null || sourceFile.getName() == null) {
      return super.toString();
    }
    return sourceFile.getName();
  }

  /**
   * Returns all GraphicObjects, which are not matched by any existing filter
   * @return 
   */
  public GraphicSet getUnmatchedObjects()
  {
    GraphicSet all = this.getGraphicObjects().clone();
    if (this.mapping != null)
    {
      for (Mapping m : this.mapping)
      {
        if (m.getFilterSet() == null)//ignore "everything else mapping"
        {
          continue;
        }
        else if (m.getFilterSet().isEmpty())//if everything mapping present, no rest
        {
          return new GraphicSet();
        }
        else
        {
          all.removeAll(m.getFilterSet().getMatchingObjects(all));
        }
      }
    }
    return all;
  }

}
