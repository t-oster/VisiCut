/**
 * This file is part of VisiCut.
 * Copyright (C) 2011 Thomas Oster <thomas.oster@rwth-aachen.de>
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
package com.t_oster.visicut.model;

import com.t_oster.liblasercut.LaserProperty;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;


/**
 * A wrapper to the LaserProperty to add
 * Property Change Support
 * 
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class LaserPropertyBean
{
  private LaserProperty decoratee;
  public LaserPropertyBean()
  {
    decoratee = new LaserProperty();
    init();
  }
  public LaserPropertyBean(LaserProperty lp)
  {
    decoratee = lp;
    init();
  }
  private void init()
  {
    power=decoratee.getPower();
    speed=decoratee.getSpeed();
    frequency=decoratee.getFrequency();
    focus = decoratee.getFocus();
  }
  public LaserProperty getLaserProperty()
  {
    return decoratee;
  }
  public void setLaserProperty(LaserProperty lp)
  {
    this.decoratee = lp;
    setPower(decoratee.getPower());
    setSpeed(decoratee.getSpeed());
    setFrequency(decoratee.getFrequency());
    setFocus(decoratee.getFocus());
  }
  protected int power = 0;
  public static final String PROP_POWER = "power";
  protected int speed = 0;
  public static final String PROP_SPEED = "speed";
  protected int frequency = 500;
  public static final String PROP_FREQUENCY = "frequency";
  protected float focus = 0;
  public static final String PROP_FOCUS = "focus";

  /**
   * Get the value of focus
   *
   * @return the value of focus
   */
  public float getFocus()
  {
    return focus;
  }

  /**
   * Set the value of focus
   *
   * @param focus new value of focus
   */
  public void setFocus(float focus)
  {
    decoratee.setFocus(focus);
    float oldFocus = this.focus;
    this.focus = focus;
    propertyChangeSupport.firePropertyChange(PROP_FOCUS, oldFocus, focus);
  }

  /**
   * Get the value of frequency
   *
   * @return the value of frequency
   */
  public int getFrequency()
  {
    return frequency;
  }

  /**
   * Set the value of frequency
   *
   * @param frequency new value of frequency
   */
  public void setFrequency(int frequency)
  {
    decoratee.setFrequency(frequency);
    int oldFrequency = this.frequency;
    this.frequency = frequency;
    propertyChangeSupport.firePropertyChange(PROP_FREQUENCY, oldFrequency, frequency);
  }

  /**
   * Get the value of speed
   *
   * @return the value of speed
   */
  public int getSpeed()
  {
    return speed;
  }

  /**
   * Set the value of speed
   *
   * @param speed new value of speed
   */
  public void setSpeed(int speed)
  {
    decoratee.setSpeed(speed);
    int oldSpeed = this.speed;
    this.speed = speed;
    propertyChangeSupport.firePropertyChange(PROP_SPEED, oldSpeed, speed);
  }

  /**
   * Get the value of power
   *
   * @return the value of power
   */
  public int getPower()
  {
    return power;
  }

  /**
   * Set the value of power
   *
   * @param power new value of power
   */
  public void setPower(int power)
  {
    decoratee.setPower(power);
    int oldPower = this.power;
    this.power = power;
    propertyChangeSupport.firePropertyChange(PROP_POWER, oldPower, power);
  }
  private PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

  /**
   * Add PropertyChangeListener.
   *
   * @param listener
   */
  public void addPropertyChangeListener(PropertyChangeListener listener)
  {
    propertyChangeSupport.addPropertyChangeListener(listener);
  }

  /**
   * Remove PropertyChangeListener.
   *
   * @param listener
   */
  public void removePropertyChangeListener(PropertyChangeListener listener)
  {
    propertyChangeSupport.removePropertyChangeListener(listener);
  }

}
