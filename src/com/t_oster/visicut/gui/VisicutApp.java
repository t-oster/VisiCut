/*
 * JepilogApp.java
 */
package com.t_oster.visicut.gui;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
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
    System.setProperty("apple.laf.useScreenMenuBar", "true");
    System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Jepilog");
    //System.setProperty("apple.awt.brushMetalLook", "true");
    System.setProperty("apple.awt.antialiasing", "on");
    System.setProperty("apple.awt.textantialiasing", "on");
    System.setProperty("com.apple.mrj.application.growbox.intrudes", "false");
    System.setProperty("com.apple.mrj.application.live-resize", "true");
    System.setProperty("com.apple.macos.smallTabs", "true");
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
    launch(VisicutApp.class, args);
  }
}
