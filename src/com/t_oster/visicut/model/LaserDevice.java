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

import com.t_oster.liblasercut.LaserCutter;
import com.t_oster.liblasercut.drivers.EpilogZing;
import com.t_oster.uicomponents.ImageListable;
import java.awt.geom.AffineTransform;

/**
 * A wrapper for the LaserCutter class which adds some Attributes
 *
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class LaserDevice implements ImageListable
{

  protected String jobSentText = "Job was sent as '$jobname'\nPlease:\n-Close the lid\n-Turn on the Ventilation\n-And press 'start' on the Lasercutter $name";

  /**
   * Get the value of jobSentText
   *
   * @return the value of jobSentText
   */
  public String getJobSentText()
  {
    return jobSentText;
  }

  /**
   * Set the value of jobSentText
   *
   * @param jobSentText new value of jobSentText
   */
  public void setJobSentText(String jobSentText)
  {
    this.jobSentText = jobSentText;
  }
  protected String jobPrefix = "visicut ";

  /**
   * Get the value of jobPrefix
   *
   * @return the value of jobPrefix
   */
  public String getJobPrefix()
  {
    return jobPrefix;
  }

  /**
   * Set the value of jobPrefix
   *
   * @param jobPrefix new value of jobPrefix
   */
  public void setJobPrefix(String jobPrefix)
  {
    this.jobPrefix = jobPrefix;
  }
  protected LaserCutter laserCutter = new EpilogZing();

  /**
   * Get the value of laserCutter
   *
   * @return the value of laserCutter
   */
  public LaserCutter getLaserCutter()
  {
    return laserCutter;
  }

  /**
   * Set the value of laserCutter
   *
   * @param laserCutter new value of laserCutter
   */
  public void setLaserCutter(LaserCutter laserCutter)
  {
    this.laserCutter = laserCutter;
  }
  protected AffineTransform cameraCalibration = null;

  /**
   * Get the value of cameraCalibration
   *
   * @return the value of cameraCalibration
   */
  public AffineTransform getCameraCalibration()
  {
    return cameraCalibration;
  }

  /**
   * Set the value of cameraCalibration
   *
   * @param cameraCalibration new value of cameraCalibration
   */
  public void setCameraCalibration(AffineTransform cameraCalibration)
  {
    this.cameraCalibration = cameraCalibration;
  }
  protected String cameraURL = null;

  /**
   * Get the value of cameraURL
   *
   * @return the value of cameraURL
   */
  public String getCameraURL()
  {
    return cameraURL;
  }

  /**
   * Set the value of cameraURL
   *
   * @param cameraURL new value of cameraURL
   */
  public void setCameraURL(String cameraURL)
  {
    this.cameraURL = cameraURL;
  }
  protected String thumbnailPath = null;

  /**
   * Get the value of thumbnailPath
   *
   * @return the value of thumbnailPath
   */
  public String getThumbnailPath()
  {
    return thumbnailPath;
  }

  /**
   * Set the value of thumbnailPath
   *
   * @param thumbnailPath new value of thumbnailPath
   */
  public void setThumbnailPath(String thumbnailPath)
  {
    this.thumbnailPath = thumbnailPath;
  }
  protected String description = null;

  /**
   * Get the value of description
   *
   * @return the value of description
   */
  public String getDescription()
  {
    return description;
  }

  /**
   * Set the value of description
   *
   * @param description new value of description
   */
  public void setDescription(String description)
  {
    this.description = description;
  }
  protected String name = null;

  /**
   * Get the value of name
   *
   * @return the value of name
   */
  public String getName()
  {
    return name;
  }

  /**
   * Set the value of name
   *
   * @param name new value of name
   */
  public void setName(String name)
  {
    this.name = name;
  }

  @Override
  public LaserDevice clone()
  {
    LaserDevice result = new LaserDevice();
    result.cameraCalibration = cameraCalibration;
    result.cameraURL = cameraURL;
    result.description = description;
    result.name = name;
    result.laserCutter = laserCutter.clone();
    result.thumbnailPath = thumbnailPath;
    result.jobPrefix = jobPrefix;
    result.jobSentText = jobSentText;
    return result;
  }

  @Override
  public String toString()
  {
    return getName();
  }
}
