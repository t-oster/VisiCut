/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.liblasercut;

/**
 *
 * @author thommy
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
    return r3dPart != null;
  }

  public boolean containsVector()
  {
    return vPart != null;
  }

  public boolean containsRaster()
  {
    return rPart != null;
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
