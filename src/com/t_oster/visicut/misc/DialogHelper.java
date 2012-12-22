/**
 * This file is part of VisiCut. Copyright (C) 2012 Thomas Oster
 * <thomas.oster@rwth-aachen.de> RWTH Aachen University - 52062 Aachen, Germany
 *
 * VisiCut is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * VisiCut is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with VisiCut. If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.t_oster.visicut.misc;

import com.t_oster.visicut.gui.beans.AngleTextfield;
import com.t_oster.visicut.gui.beans.LengthTextfield;
import java.awt.Component;
import java.util.List;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.swing.JOptionPane;

/**
 *
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class DialogHelper
{

  private Component parent;
  private String title;

  public DialogHelper(Component parent, String title)
  {
    this.parent = parent;
    this.title = title;
  }

  public boolean showYesNoQuestion(String text)
  {
    return JOptionPane.showConfirmDialog(parent, text, title, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
  }

  public boolean showOkCancelQuestion(String text)
  {
    return JOptionPane.showConfirmDialog(parent, text, title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.OK_OPTION;
  }

  public boolean showYesNoDialog(String text)
  {
    return JOptionPane.showConfirmDialog(parent, text, title, JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.YES_OPTION;
  }

  public boolean showOkCancelDialog(String text)
  {
    return JOptionPane.showConfirmDialog(parent, text, title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION;
  }

  public void showWaringnMessage(List<String> text)
  {
    String txt = "";
    for(String s : text)
    {
      txt += s + "\n";
    }
    this.showWarningMessage(txt);
  }

  public void showWarningMessage(String text)
  {
    JOptionPane.showMessageDialog(parent, text, title, JOptionPane.WARNING_MESSAGE);
  }

  public void showSuccessMessage(String text)
  {
    JOptionPane.showMessageDialog(parent, text, title, JOptionPane.PLAIN_MESSAGE);
  }

  public void showInfoMessage(String text)
  {
    JOptionPane.showMessageDialog(parent, text, title, JOptionPane.INFORMATION_MESSAGE);
  }

  public void showErrorMessage(Exception cause)
  {
    this.showErrorMessage(cause, "");
  }

  public void showErrorMessage(Exception cause, String text)
  {
    cause.printStackTrace();
    JOptionPane.showMessageDialog(parent, text + "\nError (" + cause.getClass().getSimpleName() + "): " + cause.getLocalizedMessage(), title + " Error", JOptionPane.ERROR_MESSAGE);
  }

  public void showErrorMessage(String text)
  {
    JOptionPane.showMessageDialog(parent, text, title + " Error", JOptionPane.ERROR_MESSAGE);
  }

  public Double askLength(String text, double mm)
  {
    LengthTextfield tf = new LengthTextfield();
    tf.setValue(mm);
    if (JOptionPane.showConfirmDialog(parent, tf, text, JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION)
    {
      return tf.getValue();
    }
    return null;
  }

  public Double askAngle(String text, double rad)
  {
    AngleTextfield tf = new AngleTextfield();
    tf.setValue(rad);
    if (JOptionPane.showConfirmDialog(parent, tf, text, JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION)
    {
      return tf.getValue();
    }
    return null;
  }
}
