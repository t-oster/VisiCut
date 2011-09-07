package com.t_oster.visicut.gui.beans;

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
 * @author thommy
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
