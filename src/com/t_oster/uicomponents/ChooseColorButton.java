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

import com.t_oster.visicut.misc.Helper;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JColorChooser;

/**
 * This class implements a Button which displays the
 * current Color. If clicked, it will provide a color
 * chooser to change the color.
 *
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class ChooseColorButton extends JButton implements ActionListener
{

  public ChooseColorButton(Color c)
  {
    this.setSelectedColor(c);
    this.addActionListener(this);
  }

  public ChooseColorButton()
  {
    this(null);
  }
  protected Color selectedColor = null;
  public static final String PROP_SELECTEDCOLOR = "selectedColor";

  /**
   * Get the value of selectedColor
   *
   * @return the value of selectedColor
   */
  public Color getSelectedColor()
  {
    return selectedColor;
  }

  /**
   * Set the value of selectedColor
   *
   * @param selectedColor new value of selectedColor
   */
  public final void setSelectedColor(Color selectedColor)
  {
    Color oldSelectedColor = this.selectedColor;
    this.selectedColor = selectedColor;
    firePropertyChange(PROP_SELECTEDCOLOR, oldSelectedColor, selectedColor);
    if (selectedColor == null)
    {
      this.setText("no color");
    }
    else
    {
      this.setText("<html><table bgcolor=" + Helper.toHtmlRGB(selectedColor) + "><tr><td>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</td></tr></table>");
    }
  }

  public void actionPerformed(ActionEvent ae)
  {
    Color selected = JColorChooser.showDialog(this, "Please select a Color", this.getSelectedColor());
    if (selected != null)
    {
      this.setSelectedColor(selected);
    }
  }
}
