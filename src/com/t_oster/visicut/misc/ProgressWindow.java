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
package com.t_oster.visicut.misc;

import java.awt.Dialog;
import java.awt.Frame;
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
  private int min = 0;
  private int max = 100;
  private int state = 0;
  private String title = "Fortschritt";
  private String text = "...";

  public ProgressWindow(Frame parent, boolean modal)
  {
    super(parent, modal);
    this.initializeComponents();
  }
  
  public ProgressWindow(Dialog parent, boolean modal)
  {
    super(parent, modal);
    this.initializeComponents();
  }
  
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
    this.title = title;
    this.text = text;
    this.min = min;
    this.max = max;
    this.initializeComponents();
  }
  
  private void initializeComponents()
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
    this.setState(min, max, state);
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
