/**
 * This file is part of VisiCut.
 * 
 *     VisiCut is free software: you can redistribute it and/or modify
 *     it under the terms of the Lesser GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *    VisiCut is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     Lesser GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with VisiCut.  If not, see <http://www.gnu.org/licenses/>.
 **/
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.visicut.misc;

import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

/**
 * @author Thomas Oster
 * Shows a Window with Progress Bar and optional Text
 * which can both be controlled by it's setter methods
 *
 */
public class ProgressWindow extends JDialog
{

  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  private JProgressBar progBar;
  private JLabel label;
  private JLabel percent;
  private int min;
  private int max;
  private int state;

  public ProgressWindow()
  {
    this(0, 100);
  }

  public ProgressWindow(int min, int max)
  {
    this("Fortschritt", min, max);
  }

  public ProgressWindow(String title, int min, int max)
  {
    this(title, "-", min, max);
  }

  public ProgressWindow(String title, String text, int min, int max)
  {
    this.setTitle(title);
    this.setLocationByPlatform(true);
    this.setResizable(false);
    this.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
    JPanel mainPanel = new JPanel();
    mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
    progBar = new JProgressBar(min, max);
    mainPanel.add(progBar);
    percent = new JLabel("0%");
    mainPanel.add(percent);
    label = new JLabel(text);
    mainPanel.add(label);
    this.setContentPane(mainPanel);
    this.pack();
    this.setState(min, max, 0);
  }

  public void setText(String text)
  {
    label.setText(text);
    this.refresh();
  }

  public void setState(int min, int max, int state)
  {
    this.min = min;
    this.max = max;
    this.state = state;
    progBar.setMaximum(max);
    progBar.setMinimum(min);
    progBar.setValue(state);
    if ((max - min) > 0)
    {
      percent.setText("" + (100 * (state - min) / (max - min)) + "%");
    }
    this.refresh();
  }

  public void setState(int state)
  {
    this.state = state;
    progBar.setValue(state);
    percent.setText("" + (100 * (state - min) / (max - min)) + "%");
    this.refresh();
  }

  public void incrementState(int inc)
  {
    setState(state + inc);
  }

  public void incrementState()
  {
    incrementState(1);
  }

  public void setMin(int min)
  {
    this.min = min;
    percent.setText("" + (100 * (state - min) / (max - min)) + "%");
    progBar.setMinimum(min);
    this.refresh();
  }

  public void setMax(int max)
  {
    this.max = max;
    percent.setText("" + (100 * (state - min) / (max - min)) + "%");
    progBar.setMaximum(max);
    this.refresh();
  }

  public void close()
  {
    this.dispose();
  }

  private void refresh()
  {
    this.pack();
    this.repaint();
  }
}
