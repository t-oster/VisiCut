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

import com.t_oster.visicut.model.LaserDevice;
import com.thoughtworks.xstream.XStream;
import java.util.*;

/**
 * This class manages the available Material Profiles
 * 
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class LaserDeviceManager extends FilebasedManager<LaserDevice>
{

  @Override
  protected XStream createXStream()
  {
    XStream xs = super.createXStream();
    xs.alias("laserDevice", LaserDevice.class);
    return xs;
  }
  
  private static LaserDeviceManager instance;
  
  public static LaserDeviceManager getInstance()
  {
    if (instance == null)
    {
      instance = new LaserDeviceManager();
    }
    return instance;
  }
  
  /*
   * Need a public constructior for UI manager
   * Do not use. Use getInstance instead
   */
  public LaserDeviceManager()
  {
    if (instance != null)
    {
      System.err.println("ProfileManager should not be instanciated directly");
    }
  }

  @Override
  protected String getSubfolderName()
  {
    return "devices";
  }

  @Override
  public String getThumbnail(LaserDevice o)
  {
    return o.getThumbnailPath();
  }

  @Override
  public void setThumbnail(LaserDevice o, String f)
  {
    o.setThumbnailPath(f);
  }

  private Comparator<LaserDevice> comparator = new Comparator<LaserDevice>()
  {
    public int compare(LaserDevice t, LaserDevice t1)
    {
      return t.getName().compareTo(t1.getName());
    }
  };
  
  @Override
  protected Comparator getComparator()
  {
    return comparator;
  }

}
