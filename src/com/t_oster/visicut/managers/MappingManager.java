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
package com.t_oster.visicut.managers;

import com.t_oster.liblasercut.TimeIntensiveOperation;
import com.t_oster.visicut.model.LaserProfile;
import com.t_oster.visicut.model.Raster3dProfile;
import com.t_oster.visicut.model.RasterProfile;
import com.t_oster.visicut.model.VectorProfile;
import com.t_oster.visicut.model.mapping.FilterSet;
import com.t_oster.visicut.model.mapping.Mapping;
import com.t_oster.visicut.model.mapping.MappingFilter;
import com.t_oster.visicut.model.mapping.MappingSet;
import com.thoughtworks.xstream.XStream;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;

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
    xstream.alias("vectorProfile", VectorProfile.class);
    xstream.alias("rasterProfile", RasterProfile.class);
    xstream.alias("raster3dProfile", Raster3dProfile.class);
    xstream.omitField(TimeIntensiveOperation.class, "listeners");
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

  /**
     * Generates an Everything=> Profile mapping for every
     * Occuring MaterialProfile
     * @return
     */
  public List<MappingSet> generateDefaultMappings()
  {
    ResourceBundle bundle = ResourceBundle.getBundle("com/t_oster/visicut/gui/mapping/resources/PredefinedMappingBox");
    String doEverything = bundle.getString("EVERYTHING_DO");
    List<MappingSet> result = new LinkedList<MappingSet>();
    Set<String> profiles = new LinkedHashSet<String>();
    for (LaserProfile lp:ProfileManager.getInstance().getAll())
    {
      if (!profiles.contains(lp.getName()))
      {
        profiles.add(lp.getName());
        MappingSet set = new MappingSet();
        set.add(new Mapping(new FilterSet(), lp));
        set.setName(doEverything.replace("$profile", lp.getName()));
        set.setDescription("An auto-generated mapping");
        result.add(set);
      }
    }
    return result;
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
  
    /**
   * Find a mapping with the given name
   * @param name
   * @return object where (getName()==string), null if not found
   */
  public MappingSet getItemByName(String name) {
    for (MappingSet obj: this.getAll()) {
      if (obj.getName().equals(name)) {
        return obj;
      }
    }
    for (MappingSet obj: this.generateDefaultMappings()) {
      if (obj.getName().equals(name)) {
        return obj;
      }
    }
    return null;
  }

}
