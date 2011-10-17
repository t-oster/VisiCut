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
/*
 * JepilogApp.java
 */
package com.t_oster.visicut.gui;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import org.jdesktop.application.Application;
import org.jdesktop.application.SingleFrameApplication;

/**
 * The main class of the application.
 * This class implements the Controller which bridges the model and the view
 */
public class VisicutApp extends SingleFrameApplication
{

  /**
   * At startup create and show the main frame of the application.
   */
  @Override
  protected void startup()
  {
    show(new MainView());
  }

  /**
   * This method is to initialize the specified window by injecting resources.
   * Windows shown in our application come fully initialized from the GUI
   * builder, so this additional configuration is not needed.
   */
  @Override
  protected void configureWindow(java.awt.Window root)
  {
  }

  /**
   * A convenient static getter for the application instance.
   * @return the instance of JepilogApp
   */
  public static VisicutApp getApplication()
  {
    return Application.getInstance(VisicutApp.class);
  }
  private String[] arguments;

  public String[] getProgramArguments()
  {
    return arguments;
  }

  @Override
  protected void initialize(String[] args)
  {
    super.initialize(args);
    this.arguments = args;
  }

  /**
   * Main method launching the application.
   */
  public static void main(String[] args)
  {
    //Mac Specific
    if (System.getProperty("os.name").toLowerCase().contains("mac"))
    {
      System.setProperty("apple.laf.useScreenMenuBar", "true");
      System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Visicut");
      //System.setProperty("apple.awt.brushMetalLook", "true");
      System.setProperty("apple.awt.antialiasing", "on");
      System.setProperty("apple.awt.textantialiasing", "on");
      System.setProperty("com.apple.mrj.application.growbox.intrudes", "false");
      System.setProperty("com.apple.mrj.application.live-resize", "true");
      System.setProperty("com.apple.macos.smallTabs", "true");
    }
    try
    {
      UIManager.setLookAndFeel(
        UIManager.getSystemLookAndFeelClassName());
    }
    catch (ClassNotFoundException ex)
    {
      Logger.getLogger(VisicutApp.class.getName()).log(Level.SEVERE, null, ex);
    }
    catch (InstantiationException ex)
    {
      Logger.getLogger(VisicutApp.class.getName()).log(Level.SEVERE, null, ex);
    }
    catch (IllegalAccessException ex)
    {
      Logger.getLogger(VisicutApp.class.getName()).log(Level.SEVERE, null, ex);
    }
    catch (UnsupportedLookAndFeelException ex)
    {
      Logger.getLogger(VisicutApp.class.getName()).log(Level.SEVERE, null, ex);
    }
    try
    {
      launch(VisicutApp.class, args);
    }
    catch (Exception e)
    {
      JOptionPane.showMessageDialog(null, "Sorry: An unexpected Error occured\n Please try to reproduce it and fill in a Bugreport at\nhttps://github.com/t-oster/VisiCut/issues", "Error", JOptionPane.ERROR_MESSAGE);
      e.printStackTrace();
      System.exit(1);
    }
  }
}
