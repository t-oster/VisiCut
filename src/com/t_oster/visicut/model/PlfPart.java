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

import com.t_oster.liblasercut.platform.Tuple;
import com.t_oster.visicut.model.graphicelements.GraphicSet;
import com.t_oster.visicut.model.mapping.Mapping;
import com.t_oster.visicut.model.mapping.MappingSet;
import java.io.File;
import java.util.LinkedList;

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
  
  /**
   * get a list of mappings and their matching graphic objects
   */
  public LinkedList<Tuple<Mapping, GraphicSet>> getMappedGraphicObjects() {
    LinkedList<Tuple<Mapping, GraphicSet>> mappedSets = new LinkedList<Tuple<Mapping, GraphicSet>>();
    if (this.getMapping()==null) {
      // return empty list
      return mappedSets;
    }
    GraphicSet matchedElements=new GraphicSet();
    
    // stage 1: match all normal elements
    for (Mapping m : this.getMapping())
    {
      GraphicSet set = null;
      if (!m.getFilterSet().matchEverythingElse) {
        set=m.getFilterSet().getMatchingObjects(this.getGraphicObjects());
        matchedElements.addAll(set);
      }
      mappedSets.add(new Tuple<Mapping, GraphicSet>(m,set));
    }
    
    // stage 2: get all unmatched elements, they are for the "everything else" mappings
    GraphicSet unmatchedElements = this.getGraphicObjects().clone();
    unmatchedElements.removeAll(matchedElements);
    // put them into the "everything else" mappings
    for (Tuple<Mapping, GraphicSet> t: mappedSets)
    {
      Mapping m = t.getA();
      if (m.getFilterSet().matchEverythingElse) {
        t.setB(unmatchedElements);
      }
    }
    
    return mappedSets;
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

}
