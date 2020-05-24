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
package de.thomas_oster.visicut.managers;

import de.thomas_oster.liblasercut.TimeIntensiveOperation;
import de.thomas_oster.visicut.model.LaserProfile;
import de.thomas_oster.visicut.model.Raster3dProfile;
import de.thomas_oster.visicut.model.RasterProfile;
import de.thomas_oster.visicut.model.VectorProfile;
import de.thomas_oster.visicut.model.mapping.FilterSet;
import de.thomas_oster.visicut.model.mapping.Mapping;
import de.thomas_oster.visicut.model.mapping.MappingFilter;
import de.thomas_oster.visicut.model.mapping.MappingSet;
import com.thoughtworks.xstream.XStream;
import java.util.Comparator;
import java.util.Collections;
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
  protected XStream createXStream(boolean forReading)
  {
    XStream xstream = super.createXStream(forReading);
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
     */
  public List<MappingSet> generateDefaultMappings()
  {
    ResourceBundle bundle = ResourceBundle.getBundle("de.thomas_oster/visicut/gui/mapping/resources/PredefinedMappingBox");
    String doEverything = bundle.getString("EVERYTHING_DO");
    List<MappingSet> result = new LinkedList<MappingSet>();
    Set<String> profiles = new LinkedHashSet<String>();
    List<LaserProfile> lp_all = ProfileManager.getInstance().getAll();

    /*
     * Sort the list of laser profiles to
     *  - have vector profiles before non-vector-profiles (bend, mark, cut before all kinds of engrave)
     *  - have vector profiles named "cut..." before others (bend, mark). A cutter cuts.
     * Caution: similar code in
     *  - mapping/CustomMappingPanel.java:generateDefaultEntries()
     *  - our private Comparator<MappingSet> comp
     *
     * TODO:
     *  - check if they all should do the same?
     *  - check if bend and mark should really have isIsCut() return true?
     *    They don't really cut through the material. If the had iscut=false, we could use
     *    that instead of the hackish name prefix comparison "cut".
     */
    Collections.sort(lp_all, new Comparator<LaserProfile>()
      {
	public int compare(LaserProfile p1, LaserProfile p2)
	  {
	    if (p1.getName().toLowerCase().startsWith("cut") !=  p2.getName().toLowerCase().startsWith("cut"))
	      {
	        if (p1.getName().toLowerCase().startsWith("cut")) { return -1; } else { return 1; }
	      }
	    if ((p1 instanceof VectorProfile) != (p2 instanceof VectorProfile))
	      {
	        if (p1 instanceof VectorProfile) { return -1; } else { return 1; }
	      }
	    return p1.getName().compareToIgnoreCase(p2.getName());
	  }
      });

    for (LaserProfile lp : lp_all)
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
