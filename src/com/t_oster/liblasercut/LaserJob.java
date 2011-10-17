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
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.liblasercut;

/**
 *
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class LaserJob
{

  private String title;
  private String name;
  private String user;
  private int resolution;
  private int startX = 0;
  private int startY = 0;
  private VectorPart vPart;
  private Raster3dPart r3dPart;
  private RasterPart rPart;

  public LaserJob(String title, String name, String user, int resolution, Raster3dPart r3dp, VectorPart vp, RasterPart rp)
  {
    this.title = title;
    this.name = name;
    this.user = user;
    this.resolution = resolution;
    this.vPart = vp;
    this.r3dPart = r3dp;
    this.rPart = rp;
  }

  public void setStartPoint(int x, int y)
  {
    startX = x;
    startY = y;
  }

  public int getStartX()
  {
    return startX;
  }

  public int getStartY()
  {
    return startY;
  }

  public String getTitle()
  {
    return title;
  }

  public String getName()
  {
    return name;
  }

  public String getUser()
  {
    return user;
  }

  public boolean contains3dRaster()
  {
    return r3dPart != null && r3dPart.getRasterCount() > 0;
  }

  public boolean containsVector()
  {
    return vPart != null && vPart.getCommandList().length > 0;
  }

  public boolean containsRaster()
  {
    return rPart != null && rPart.getRasterCount() > 0;
  }

  public int getResolution()
  {
    return resolution;
  }

  public VectorPart getVectorPart()
  {
    return vPart;
  }

  public Raster3dPart getRaster3dPart()
  {
    return r3dPart;
  }

  public RasterPart getRasterPart()
  {
    return rPart;
  }
}
