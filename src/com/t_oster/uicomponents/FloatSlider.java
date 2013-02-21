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
package com.t_oster.uicomponents;

import javax.swing.JSlider;

/**
 *
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class FloatSlider extends JSlider
{

  private float stepWidth = 1;
  public FloatSlider()
  {
    super.setMinimum(0);
    super.setMaximum(100);
  }
  protected float floatMin = 0;
  public static final String PROP_FLOATMIN = "floatMin";


  /**
   * Get the value of floatMin
   *
   * @return the value of floatMin
   */
  public float getFloatMin()
  {
    return floatMin;
  }

  @Override
  public void setValue(int i)
  {
    super.setValue(i);
    this.setFloatValue(floatMin+stepWidth*i);
  }

  /**
   * Set the value of floatMin
   *
   * @param floatMin new value of floatMin
   */
  public void setFloatMin(float floatMin)
  {
    float oldFloatMin = this.floatMin;
    this.floatMin = floatMin;
    firePropertyChange(PROP_FLOATMIN, oldFloatMin, floatMin);
    this.setSteps(this.getSteps());
  }
  protected float floatMax = 100;
  public static final String PROP_FLOATMAX = "floatMax";

  /**
   * Get the value of floatMax
   *
   * @return the value of floatMax
   */
  public float getFloatMax()
  {
    return floatMax;
  }

  /**
   * Set the value of floatMax
   *
   * @param floatMax new value of floatMax
   */
  public void setFloatMax(float floatMax)
  {
    float oldFloatMax = this.floatMax;
    this.floatMax = floatMax;
    firePropertyChange(PROP_FLOATMAX, oldFloatMax, floatMax);
    this.setSteps(this.getSteps());
  }
  protected float floatValue = 50;
  public static final String PROP_FLOATVALUE = "floatValue";

  /**
   * Get the value of floatValue
   *
   * @return the value of floatValue
   */
  public float getFloatValue()
  {
    return floatValue;
  }

  /**
   * Set the value of floatValue
   *
   * @param floatValue new value of floatValue
   */
  public void setFloatValue(float floatValue)
  {
    super.setValue((int) ((floatValue-floatMin)/stepWidth));
    float oldFloatValue = this.floatValue;
    this.floatValue = floatValue;
    firePropertyChange(PROP_FLOATVALUE, oldFloatValue, floatValue);
  }

  protected int steps = 100;
  public static final String PROP_STEPS = "steps";

  /**
   * Get the value of steps
   *
   * @return the value of steps
   */
  public int getSteps()
  {
    return steps;
  }

  /**
   * Set the value of steps
   *
   * @param steps new value of steps
   */
  public void setSteps(int steps)
  {
    int oldSteps = this.steps;
    this.steps = steps;
    this.stepWidth = (this.floatMax-this.floatMin)/steps;
    this.setMaximum(steps);
    firePropertyChange(PROP_STEPS, oldSteps, steps);
  }

}
