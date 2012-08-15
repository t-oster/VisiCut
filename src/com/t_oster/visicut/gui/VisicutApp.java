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

import com.t_oster.liblasercut.LaserCutter;
import com.t_oster.liblasercut.LibInfo;
import com.t_oster.visicut.VisicutModel;
import com.t_oster.visicut.managers.MappingManager;
import com.t_oster.visicut.managers.PreferencesManager;
import com.t_oster.visicut.managers.ProfileManager;
import com.t_oster.visicut.misc.ApplicationInstanceListener;
import com.t_oster.visicut.misc.ApplicationInstanceManager;
import com.t_oster.visicut.misc.Helper;
import com.t_oster.visicut.model.LaserDevice;
import com.t_oster.visicut.model.MaterialProfile;
import com.t_oster.visicut.model.mapping.MappingSet;
import java.io.File;
import java.util.LinkedList;
import java.util.List;
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
  
  private MainView mainView;
  private File loadedFile;

  /**
   * At startup create and show the main frame of the application.
   */
  @Override
  protected void startup()
  {
    mainView = loadedFile == null ? new MainView() : new MainView(loadedFile);
    show(mainView);
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
   * @return the instance of VisicutApp
   */
  public static VisicutApp getApplication()
  {
    return Application.getInstance(VisicutApp.class);
  }

  @Override
  protected void initialize(String[] args)
  {
    VisicutModel.getInstance().setPreferences(PreferencesManager.getInstance().getPreferences());
    this.processProgramArguments(args);
    super.initialize(args);
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
    String laserdevice = null;
    String material = null;
    Integer resolution = null;
    Integer port = null;
    String mapping = null;
    String file = null;
    Float height = null;
    VisicutModel model = VisicutModel.getInstance();
    boolean execute = false;
    try
    {
      for (int i = 0; i < args.length; i++)
      {
        String s = args[i];
        if (s.startsWith("-"))
        {
          if ("--debug".equals(s) || "-d".equals(s))
          {
            GLOBAL_LOG_LEVEL = Level.FINE;
          }
          else if ("--singleinstanceport".equals(s))
          {
            port = Integer.parseInt(args[++i]);
          }
          else if ("--gtkfilechooser".equals(s))
          {
            if ("GTK look and feel".equals(UIManager.getLookAndFeel().getName()))
            {
              UIManager.put("FileChooserUI", "eu.kostia.gtkjfilechooser.ui.GtkFileChooserUI");
            }
            else
            {
              System.err.println("GTK look and feel not enabled, cannot apply GtkFileChooser");
            }
          }
          else if ("--version".equals(s) || "-v".equals(s))
          {
            org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(com.t_oster.visicut.gui.VisicutApp.class).getContext().getResourceMap(VisicutApp.class);
            System.out.println(resourceMap.getString("Application.title")+"\t\t Version: "+resourceMap.getString("Application.version"));
            System.out.println(""
              + "LibLaserCut\t Version: "+LibInfo.getVersion());
            System.out.println("\n\tSupported Drivers:");
            for (Class c:LibInfo.getSupportedDrivers())
            {
              System.out.println("\tModel: "+((LaserCutter) c.newInstance()).getModelName()+"\t Driver:"+c.getCanonicalName());
            }
            System.out.println("\n(c) 2011 by T.Oster, Media Computing Group, RWTH Aachen University");
            System.out.println("This Software is licensed under the GNU Lesser General Public License (LGPL)");
            System.exit(0);
          }
          else if ("--help".equals(s) || "-h".equals(s))
          {
            System.out.println("Usage: visicut [-h|--help|-v|--version]");
            System.out.println("\t visicut [options] [<filename>]");
            System.out.println("\t visicut [options] --execute filename");
            System.out.println("Options are:");
            System.out.println(" --resolution <resolution in DPI e.g. 500>");
            System.out.println(" --material <materialname e.g. \"Acrylic Glass 2mm\">");
            System.out.println(" --laserdevice <laserdevice e.g. \"Epilog ZING @ Miltons Office\">");
            System.out.println(" --mapping <mapping e.g. \"Cut\">");
            System.out.println(" --total-height <Height in mm e.g. \"2.5\"> (only valid with --execute)");
            System.out.println(" --singleinstanceport <port> (Tries to open the given port, to check for running instances)");
            System.out.println(" --gtkfilechooser (experimental)");
            System.exit(0);
          }
          else if ("--total-height".equals(s))
          {
            height = Float.parseFloat(args[++i]);
          }
          else if ("--resolution".equals(s))
          {
            resolution = Integer.parseInt(args[++i]);
          }
          else if ("--laserdevice".equals(s))
          {
            laserdevice = args[++i];
          }
          else if ("--material".equals(s))
          {
            material = args[++i];
          }
          else if ("--mapping".equals(s))
          {
            mapping = args[++i];
          }
          else if ("--execute".equals(s))
          {
            execute = true;
          }
          else
          {
            System.err.println("Unknown command line option: " + s);
            System.err.println("Use -h or --help for help");
            System.exit(1);
          }
        }
        else
        {
          if (file == null)
          {
            file = s;
          }
          else
          {
            System.err.println("More than one file is not supported yet");
          }
        }
      }
    }
    catch (Exception e)
    {
      System.err.println("Bad command line argumantes.");
      System.err.println("Use -h or --help for help");
      System.exit(1);
    }
    if (port != null)
    {
      if (!ApplicationInstanceManager.registerInstance(port, (file != null ? file : "")))
      {
        System.exit(0);
      }
      ApplicationInstanceManager.setApplicationInstanceListener(new ApplicationInstanceListener()
      {

        public void newInstanceCreated(String message)
        {
          if (message != null && !"".equals(message))
          {
            VisicutApp.this.mainView.loadFile(new File(message));
          }
          VisicutApp.this.mainView.requestFocus();
        }
      });
    }
    if (laserdevice != null)
    {
      search:
      {
        for (LaserDevice ld : PreferencesManager.getInstance().getPreferences().getLaserDevices())
        {
          if (ld.getName().equals(laserdevice))
          {
            VisicutModel.getInstance().setSelectedLaserDevice(ld);
            break search;
          }
        }
        System.err.println("No such Laserdevice: " + laserdevice);
      }
    }
    if (material != null)
    {
      LaserDevice cld = model.getSelectedLaserDevice();
      search:
      {
        List<LaserDevice> devs = null;
        if (cld == null)
        {
          devs = PreferencesManager.getInstance().getPreferences().getLaserDevices();
        }
        else
        {
          devs = new LinkedList<LaserDevice>();
          devs.add(cld);
        }
        for (LaserDevice ld : devs)
        {

          for (MaterialProfile mp : ProfileManager.getInstance().getMaterials(ld))
          {
            if (material.equals(mp.getName()))
            {
              model.setMaterial(mp);
              break search;
            }
          }
        }
        System.err.println("Material " + material + " not available");
      }
    }
    if (height != null)
    {
      if (!execute)
      {
        System.err.append("Total-height parameter takes only effect with --execute");
      }
      if (model.getMaterial() != null)
      {
        model.getMaterial().setDepth(height);
      }
    }
    if (resolution != null)
    {
      try
      {
        model.setResolution(resolution);
      }
      catch (Exception e)
      {
        System.err.println("Invalid resolution");
        System.exit(1);
      }
    }
    if (mapping != null)
    {
      search:
      {
        for (MappingSet ms : model.generateDefaultMappings())
        {
          if (mapping.equals(ms.getName()))
          {
            model.setMappings(ms);
            break search;
          }
        }
        for (MappingSet ms : MappingManager.getInstance().getMappingSets())
        {
          if (mapping.equals(ms.getName()))
          {
            model.setMappings(ms);
            break search;
          }
        }
        System.err.println("No such Mapping: " + mapping);
      }
    }
    if (file != null)
    {
      File f = new File(file);
      if (!f.isFile() || !f.exists())
      {
        System.err.println("Can not find file: " + file);
        System.exit(1);
      }
      try
      {
        if (file.toLowerCase().endsWith("plf"))
        {
          model.loadFromFile(MappingManager.getInstance(), f);
        }
        else
        {
          model.loadGraphicFile(f);
        }
        this.loadedFile = f;
      }
      catch (Exception ex)
      {
        Logger.getLogger(VisicutApp.class.getName()).log(Level.SEVERE, null, ex);
        System.err.println("Error loading file "+f+" :"+ex.getMessage());
        System.exit(1);
      }
    }
    if (execute)
    {
      if (model.getGraphicObjects() == null || model.getGraphicObjects().size() == 0)
      {
        System.err.println("Nothing to cut/engrave");
        System.exit(1);
      }
      if (model.getSelectedLaserDevice() == null)
      {
        System.err.println("No Laserdevice selected");
        System.exit(1);
      }
      if (model.getMaterial() == null)
      {
        System.err.println("No Material selected");
        System.exit(1);
      }
      if (model.getMappings() == null)
      {
        System.err.println("No Mapping selected");
        System.exit(1);
      }
      if (!model.supported(model.getSelectedLaserDevice(), model.getMaterial(), model.getMappings()))
      {
        System.err.println("Combination of Laserdevice, Material and Mapping is not supported");
        System.exit(1);
      }
      if (!model.getSelectedLaserDevice().getLaserCutter().getResolutions().contains(model.getValidResolution()))
      {
        System.err.println("Resolution " + model.getValidResolution() + " is not supported by " + model.getSelectedLaserDevice().getName());
        System.exit(1);
      }
      try
      {
        VisicutModel.getInstance().sendJob("VisiCut 1");
      }
      catch (Exception ex)
      {
        Logger.getLogger(VisicutApp.class.getName()).log(Level.SEVERE, null, ex);
        System.err.println("Job could not be executed: "+ex.getMessage());
        System.exit(1);
      }
      System.out.println("Job was sucessfully sent.");
      System.out.println("Please press START on the Lasercutter");
      System.exit(0);
    }
  }
}
