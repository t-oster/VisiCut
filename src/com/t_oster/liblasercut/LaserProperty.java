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
  private int focus = 0;

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

  public LaserProperty(int power, int speed, int frequency, int focus)
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
    this.speed = speed;
  }

  public int getSpeed()
  {
    return speed;
  }

  public void setFrequency(int frequency)
  {
    this.frequency = frequency;
  }

  public int getFrequency()
  {
    return frequency;
  }

  public void setFocus(int focus)
  {
    this.focus = focus;
  }

  public int getFocus()
  {
    return this.focus;
  }

  @Override
  public LaserProperty clone()
  {
    return new LaserProperty(power, speed, frequency, focus);
  }
}
