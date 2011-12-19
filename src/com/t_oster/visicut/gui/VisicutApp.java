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
package com.t_oster.visicut.gui;

import com.t_oster.visicut.VisicutModel;
import com.t_oster.visicut.misc.Helper;
import com.t_oster.visicut.model.graphicelements.ImportException;
import java.io.File;
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

  public static Level GLOBAL_LOG_LEVEL = Level.SEVERE;
  
  /**
   * At startup create and show the main frame of the application.
   */
  @Override
  protected void startup()
  {
    this.processProgramArguments(arguments);
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
    if (Helper.isMacOS())
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
    if ("GTK look and feel".equals(UIManager.getLookAndFeel().getName()))
    {//new GTK file chooser if we're on Gnome etc
      //disabled due to a bug in gtkjfilechooser, will be re-enabled as soon
      //as they resolve the bug
      //https://code.google.com/p/gtkjfilechooser/issues/detail?can=2&start=0&num=100&q=&colspec=ID%20Type%20Status%20Priority%20Milestone%20Owner%20Summary&groupby=&sort=&id=82
      //UIManager.put("FileChooserUI", "eu.kostia.gtkjfilechooser.ui.GtkFileChooserUI");
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
  
  public void processProgramArguments(String[] args)
  {
    boolean nogui = false;
    for (int i=0;i<args.length;i++)
    {
      String s = args[i];
      if (s.startsWith("-"))
      {
        if ("--debug".equals(s) || "-d".equals(s))
        {
          GLOBAL_LOG_LEVEL = Level.FINE;
        }
        else if ("--help".equals(s) || "-h".equals(s))
        {
          System.out.println("Usage: visicut [-h]");
          System.out.println("\t visicut [options] [<filename>]");
          System.out.println("\t visicut [options] --execute filename" );
          System.out.println(" --nogui\t disable UI (only valid with --execute)");
          System.out.println(" --resolution");
          System.out.println(" --material");
          System.out.println(" --laserdevice");
          System.out.println(" --mapping");
          System.exit(0);
        }
        else if ("--nogui".equals(s))
        {
          nogui = true;
        }
        else if ("--resolution".equals(s))
        {
          try
          {
            int resolution = Integer.parseInt(args[i+1]);
            VisicutModel.getInstance().setResolution(resolution);
          }
          catch (Exception e)
          {
            System.err.println("Invalid resolution");
            System.exit(1);
          }
        }
        else if ("--material".equals(s))
        {
          //...
        }
        else if ("--laserdevice".equals(s))
        {
          //...
        }
        else if ("--mapping".equals(s))
        {
          //...
        }
        else
        {
          System.err.println("Unknown command line option: "+s);
          System.err.println("Use -h or --help for help");
          System.exit(1);
        }
      }
      else
      {
        File f = new File(s);
        if (f.exists())
        {
          try
          {
            VisicutModel.getInstance().loadGraphicFile(f);
          }
          catch (ImportException ex)
          {
            if (nogui)
            {
              ex.printStackTrace();
              System.err.println("Could not load file "+f.getName());
              System.exit(1);
            }
            else
            {
              JOptionPane.showMessageDialog(null, "Could not load file "+f.getName(), "Error", JOptionPane.ERROR);
              System.exit(1);
            }
          }
        }
      }
    }
  }
  
}
