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
package com.t_oster.liblasercut;

/**
 * The LaserProperty holds all the parameters for parts of the LaserJob.
 * The Frequency value is ignored for Engraving operations
 * 
 * @author oster
 */
public class LaserProperty implements Cloneable
{

  private int power = 20;
  private int speed = 100;
  private int frequency = 5000;
  private float focus = 0;

  public LaserProperty()
  {
  }

  public LaserProperty(int power, int speed)
  {
      this(power, speed, 5000, 0);
  }
  
  public LaserProperty(int power, int speed, int frequency)
  {
    this(power, speed, frequency, 0);
  }

  public LaserProperty(int power, int speed, int frequency, float focus)
  {
    this.power = power;
    this.speed = speed;
    this.frequency = frequency;
    this.focus = focus;
  }

  /**
   * Sets the Laserpower. Valid values are from 0 to 100.
   * In 3d-Raster mode, the intensity is scaled to this power setting
   * @param power 
   */
  public void setPower(int power)
  {
    power = power < 0 ? 0 : power;
    power = power > 100 ? 100 : power;
    this.power = power;
  }

  public int getPower()
  {
    return power;
  }

  /**
   * Sets the speed for the Laser. Valid values is from 0 to 100
   * @param speed 
   */
  public void setSpeed(int speed)
  {
    speed = speed < 0 ? 0 : speed;
    speed = speed > 100 ? 100 : speed;
    this.speed = speed;
  }

  public int getSpeed()
  {
    return speed;
  }

  public void setFrequency(int frequency)
  {
    frequency = frequency < 100 ? 100 : frequency;
    frequency = frequency > 5000 ? 5000 : frequency;
    this.frequency = frequency;
  }

  public int getFrequency()
  {
    return frequency;
  }

  /**
   * Sets the Focus aka moves the Z axis. Values are given in mm.
   * Positive values move the Z axis down aka makes the distance between
   * laser and object bigger.
   * The possible range depends on the LaserCutter, so wrong setting
   * may result in IllegalJobExceptions
   * @param focus the relative Distance from object to Laser in mm
   */
  public void setFocus(float focus)
  {
    this.focus = focus;
  }

  /**
   * Returns the relative (to the distance at starting the job) distance
   * between laser and object in mm/10s
   */
  public float getFocus()
  {
    return this.focus;
  }

  @Override
  public LaserProperty clone()
  {
    return new LaserProperty(power, speed, frequency, focus);
  }
}
