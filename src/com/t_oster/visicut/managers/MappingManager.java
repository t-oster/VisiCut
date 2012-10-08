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
package com.t_oster.visicut.managers;

import com.t_oster.visicut.model.mapping.FilterSet;
import com.t_oster.visicut.model.mapping.Mapping;
import com.t_oster.visicut.model.mapping.MappingFilter;
import com.t_oster.visicut.model.mapping.MappingSet;
import com.thoughtworks.xstream.XStream;
import java.util.Comparator;

/**
 *
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class MappingManager extends FilebasedManager<MappingSet>
{

  @Override
  protected XStream createXStream()
  {
    XStream xstream = super.createXStream();
    xstream.alias("filter", MappingFilter.class);
    xstream.alias("filters", FilterSet.class);
    xstream.alias("mapping", Mapping.class);
    return xstream;
  }
  
  private static MappingManager instance;
  
  public static MappingManager getInstance()
  {
    if (instance == null)
    {
      instance = new MappingManager();
    }
    return instance;
  }
  
  /**
   * Need public constructor for UI Editor.
   * Do not use. Use getInstance instead
   */
  public MappingManager()
  {
    if (instance != null)
    {
      System.err.println("Should not directly instanctiate MappingManager");
    }
  }

  @Override
  protected String getSubfolderName()
  {
    return "mappings";
  }

  @Override
  public String getThumbnail(MappingSet o)
  {
    return "";
  }

  @Override
  public void setThumbnail(MappingSet o, String f)
  {
  }

  private Comparator<MappingSet> comp = new Comparator<MappingSet>()
  {

    public int compare(MappingSet t, MappingSet t1)
    {
      return t.getName().compareTo(t1.getName());
    }
    
  };
  
  @Override
  protected Comparator<MappingSet> getComparator()
  {
    return comp;
  }

}
