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

import com.t_oster.liblasercut.platform.Util;
import com.t_oster.visicut.misc.Helper;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.text.NumberFormat;
import javax.swing.JTextField;

/**
 *
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public abstract class UnitTextfield extends JTextField implements FocusListener, ActionListener
{

  public UnitTextfield()
  {
    this.addFocusListener(this);
    this.addActionListener(this);
    this.setDisplayUnit(this.getUnits()[0]);
  }

  private String displayUnit = null;
  public static final String PROP_DISPLAYUNIT = "displayUnit";

  /**
   * Get the value of displayUnit
   *
   * @return the value of displayUnit
   */
  public String getDisplayUnit()
  {
    return displayUnit;
  }

  /**
   * Set the value of displayUnit
   *
   * @param displayUnit new value of displayUnit
   */
  public void setDisplayUnit(String displayUnit)
  {
    String oldDisplayUnit = this.displayUnit;
    if (Util.differ(oldDisplayUnit, displayUnit))
    {
      this.displayUnit = displayUnit;
      this.updateText();
      firePropertyChange(PROP_DISPLAYUNIT, oldDisplayUnit, displayUnit);
    }
  }

  private void updateValue()
  {
    String text = this.getText().replace(',', '.');
    String expression = text;
    String unit = this.displayUnit;
    for (String u : this.getUnits())
    {
      if (text.endsWith(u))
      {
        expression = text.substring(0, text.length()-u.length());
        unit = u;
        this.displayUnit = u;
        break;
      }
    }
    try
    {
      double oldValue = this.value;
      this.value = removeUnit(Helper.evaluateExpression(expression), unit);
      this.updateText();
      if (oldValue != this.value)
      {
        firePropertyChange(PROP_VALUE, oldValue, value);
      }
    }
    catch (NumberFormatException e)
    {
      this.updateText();
    }
  }

  protected abstract String[] getUnits();
  protected abstract double removeUnit(double value, String unit);
  protected abstract double addUnit(double value, String unit);

  private boolean ignoreTextfieldChanges = false;
  private void updateText()
  {
    ignoreTextfieldChanges = true;
    String txt = this.getText();
    String ntxt = NumberFormat.getNumberInstance().format(addUnit(value, displayUnit))+" "+displayUnit;
    if (Util.differ(txt, ntxt))
    {
      this.setText(ntxt);
    }
    ignoreTextfieldChanges = false;
  }

  private double value = 0;
  public static final String PROP_VALUE = "value";

  public double getValue()
  {
    return value;
  }

  public void setValue(double value)
  {
    double oldValue = this.value;
    if (value != oldValue)
    {
      this.value = value;
      this.updateText();
      firePropertyChange(PROP_VALUE, oldValue, value);
    }
  }

  public void focusGained(FocusEvent fe)
  {
  }

  public void focusLost(FocusEvent fe)
  {
    if (!ignoreTextfieldChanges)
    {
      this.updateValue();
    }
  }

  public void actionPerformed(ActionEvent ae)
  {
    if (!ignoreTextfieldChanges)
    {
      this.updateValue();
    }
  }

}
