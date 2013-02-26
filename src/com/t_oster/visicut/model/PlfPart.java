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

import com.t_oster.visicut.model.graphicelements.GraphicObject;
import com.t_oster.visicut.model.graphicelements.GraphicSet;
import com.t_oster.visicut.model.mapping.Mapping;
import com.t_oster.visicut.model.mapping.MappingSet;
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

  public void setGraphicObjects(GraphicSet graphicObjects)
  {
    this.graphicObjects = graphicObjects;
  }

  public MappingSet getMapping()
  {
    return mapping;
  }

  public void setMapping(MappingSet mapping)
  {
    this.mapping = mapping;
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
