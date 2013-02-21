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
import com.thoughtworks.xstream.XStream;
import java.util.*;

/**
 * This class manages the available Material Profiles
 *
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class ProfileManager extends FilebasedManager<LaserProfile>
{

  @Override
  public XStream createXStream()
  {
    XStream xs = super.createXStream();
    xs.alias("vectorProfile", VectorProfile.class);
    xs.alias("rasterProfile", RasterProfile.class);
    xs.alias("raster3dProfile", Raster3dProfile.class);
    xs.omitField(TimeIntensiveOperation.class, "listeners");
    return xs;
  }

  private static ProfileManager instance;

  public static ProfileManager getInstance()
  {
    if (instance == null)
    {
      instance = new ProfileManager();
    }
    return instance;
  }
  /*
   * Need a public constructior for UI manager
   * Do not use. Use getInstance instead
   */
  public ProfileManager()
  {
    if (instance != null)
    {
      System.err.println("ProfileManager should not be instanciated directly");
    }
  }

  public LaserProfile getProfileByName(String profileName)
  {
    for (LaserProfile l:this.getAll())
    {
      if (profileName.equals(l.getName()))
      {
        return l;
      }
    }
    return null;
  }

  @Override
  protected String getSubfolderName()
  {
    return "profiles";
  }

  @Override
  public String getThumbnail(LaserProfile o)
  {
    return o.getThumbnailPath();
  }

  @Override
  public void setThumbnail(LaserProfile o, String f)
  {
    o.setThumbnailPath(f);
  }

  private Comparator<LaserProfile> comp = new Comparator<LaserProfile>(){

    public int compare(LaserProfile t, LaserProfile t1)
    {
      if (t instanceof VectorProfile && !(t1 instanceof VectorProfile))
      {
        return 1;
      }
      else if (!(t instanceof VectorProfile) && t1 instanceof VectorProfile)
      {
        return -1;
      }
      if (t instanceof RasterProfile && t1 instanceof Raster3dProfile)
      {
        return 1;
      }
      if (t instanceof Raster3dProfile && t1 instanceof RasterProfile)
      {
        return -1;
      }
      return t.getName().compareTo(t1.getName());
    }
  };

  @Override
  protected Comparator<LaserProfile> getComparator()
  {
    return comp;
  }

  public List<VectorProfile> getVectorProfiles()
  {
    List<VectorProfile> result = new LinkedList<VectorProfile>();
    for(LaserProfile p : this.getAll())
    {
      if (p instanceof VectorProfile)
      {
        result.add((VectorProfile) p);
      }
    }
    return result;
  }
}
