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
package de.thomas_oster.visicut.gui;

import de.thomas_oster.liblasercut.LaserCutter;
import de.thomas_oster.liblasercut.LaserProperty;
import de.thomas_oster.liblasercut.LibInfo;
import de.thomas_oster.liblasercut.ProgressListener;
import de.thomas_oster.visicut.VisicutModel;
import de.thomas_oster.visicut.managers.LaserDeviceManager;
import de.thomas_oster.visicut.managers.LaserPropertyManager;
import de.thomas_oster.visicut.managers.MappingManager;
import de.thomas_oster.visicut.managers.MaterialManager;
import de.thomas_oster.visicut.managers.PreferencesManager;
import de.thomas_oster.visicut.managers.ProfileManager;
import de.thomas_oster.visicut.misc.ApplicationInstanceListener;
import de.thomas_oster.visicut.misc.ApplicationInstanceManager;
import de.thomas_oster.visicut.misc.DialogHelper;
import de.thomas_oster.visicut.misc.Helper;
import de.thomas_oster.visicut.model.LaserDevice;
import de.thomas_oster.visicut.model.LaserProfile;
import de.thomas_oster.visicut.model.MaterialProfile;
import de.thomas_oster.visicut.model.PlfPart;
import de.thomas_oster.visicut.model.mapping.Mapping;
import java.awt.AWTEvent;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Toolkit;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
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
    try
    {
      this.processProgramArguments(args);
    }
    catch (FileNotFoundException ex)
    {
      Logger.getLogger(VisicutApp.class.getName()).log(Level.SEVERE, null, ex);
    }
    catch (IOException ex)
    {
      Logger.getLogger(VisicutApp.class.getName()).log(Level.SEVERE, null, ex);
    }
    super.initialize(args);
  }

  /**
   * Main method launching the application.
   */
  public static void main(String[] args)
  {
    final String weFuckedUp = "Sorry: An unexpected Error occured\n Please try to reproduce it and fill in a Bugreport at\nhttps://github.com/t-oster/VisiCut/issues\n\n";
    // catch all unhandled exceptions, show an error message so that we can fix them
    // based on http://ruben42.wordpress.com/2009/03/30/catching-all-runtime-exceptions-in-swing/
    class EventQueueProxy extends EventQueue {
      boolean ignoreFutureErrors = false;
      protected void dispatchEvent(AWTEvent newEvent) {
          try {
            super.dispatchEvent(newEvent);
          } catch (Throwable t) {
            if (ignoreFutureErrors) {
              return;
            }
            ignoreFutureErrors = true;
            // TODO localize
            // TODO make the dialog nicer, more like the MATLAB crash dialog https://i.stack.imgur.com/lHGM1.png
            JOptionPane.showMessageDialog(null, DialogHelper.getHumanReadableErrorMessage(t, weFuckedUp, true), "Error", JOptionPane.ERROR_MESSAGE);
            JOptionPane.showMessageDialog(null, "Please restart VisiCut now.\n (If you continue without a restart, \n no more error messages will be shown, \n but the behaviour may be unpredictable.)", "Error", JOptionPane.ERROR_MESSAGE);
          }
      }
    }
    Toolkit.getDefaultToolkit().getSystemEventQueue().push(new EventQueueProxy());

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
    else
    {
      try
      {
        if (Helper.isWindows())
        {
          // Fix font size on Windows 2022 / Win11 in certain rare conditions
          // https://github.com/t-oster/VisiCut/issues/668
          // https://stackoverflow.com/a/72089786
          java.util.Enumeration<Object> keys = UIManager.getDefaults().keys();
          while (keys.hasMoreElements()) {
              Object key = keys.nextElement();
              Object value = UIManager.get(key);
              if (value instanceof javax.swing.plaf.FontUIResource) {
                  var cast_value = (javax.swing.plaf.FontUIResource) value;
                  javax.swing.plaf.FontUIResource f = new javax.swing.plaf.FontUIResource(new Font("Segoe UI", Font.PLAIN, cast_value.getSize()));
                  UIManager.put(key, f);
              }
          }

          UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
        else if (Helper.isLinux())
        {
          //KDE etc. use ugly metal instead of nice GTK as "System", so we
          //have to set it manually
          UIManager.setLookAndFeel("com.sun.java.swing.plaf.gtk.GTKLookAndFeel");
        }
      }
      catch (Exception ex)
      {
        //if native LAF doesn't work try at least nimbus
        try
        {
          UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        }
        catch (Exception e)
        {
          Logger.getLogger(VisicutApp.class.getName()).log(Level.SEVERE, null, ex);
        }
      }
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
      JOptionPane.showMessageDialog(null, weFuckedUp, "Error", JOptionPane.ERROR_MESSAGE);
      e.printStackTrace();
      System.exit(1);
    }
  }

  public void processProgramArguments(String[] args) throws FileNotFoundException, IOException
  {
    String laserdevice = null;
    String material = null;
    Integer resolution = null;
    Integer port = null;
    String mapping = null;
    String file = null;
    String basepath = null;
    Float height = null;
    boolean add = false;
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
          else if ("--convertsettings".equals(s))
          {
            convertSettings();
            System.exit(0);
          }
          else if ("--basepath".equals(s) || "-b".equals(s))
          {
            basepath = args[++i];
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
            org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(de.thomas_oster.visicut.gui.VisicutApp.class).getContext().getResourceMap(VisicutApp.class);
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
            System.out.println(" --add (do not replace old file if VisiCut is already running)");
            System.out.println(" --material <materialname e.g. \"Acrylic Glass 2mm\">");
            System.out.println(" --laserdevice <laserdevice e.g. \"Epilog ZING @ Miltons Office\">");
            System.out.println(" --mapping <mapping e.g. \"Cut\">");
            System.out.println(" --total-height <Height in mm e.g. \"2.5\"> (only valid with --execute)");
            System.out.println(" --singleinstanceport <port> (Set port to check for running instances -- 0 to disable)");
            System.out.println(" --basepath <path> \t Sets VisiCuts settings directory (default is $HOME/.visicut)");
            System.out.println(" --gtkfilechooser (experimental)");
            System.exit(0);
          }
          else if ("--total-height".equals(s))
          {
            height = Float.parseFloat(args[++i]);
          }
          else if ("--laserdevice".equals(s))
          {
            laserdevice = args[++i];
          }
          else if ("--material".equals(s))
          {
            material = args[++i];
          }
          else if ("--execute".equals(s))
          {
            execute = true;
          }
          else if ("--add".equals(s))
          {
            add = true;
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
    if (basepath != null)
    {
      Helper.setBasePath(new File(basepath));
    }
    if (port == null) {
        port = ApplicationInstanceManager.getSingleInstancePort();
    }
    if (port != null)
    {
      System.out.println("using single-instance port: " + port);
      // encode message to send to already running instance (if there is one)
      String singleInstanceMessage = "";
      if (file != null)
      {
          if (add) {
              singleInstanceMessage = "@";
          }
          singleInstanceMessage += file;
      }
      if (port != 0) {
        // single instance mode is active -- try contacting the already runnig instance
        if (!ApplicationInstanceManager.registerInstance(port, singleInstanceMessage))
        {
          System.out.println("found already running instance. Opening file in that instance. Exiting.");
          System.exit(0);
        }
      }
      ApplicationInstanceManager.setApplicationInstanceListener(new ApplicationInstanceListener()
      {

        public void newInstanceCreated(String message)
        {
          if (message != null && !"".equals(message))
          {
            if (message.startsWith("@"))
            {
              message = message.substring(1);
              VisicutApp.this.mainView.loadFile(new File(message), false);
            }
            else
            {
              VisicutApp.this.mainView.loadFile(new File(message), true);
            }
          }
          VisicutApp.this.mainView.requestFocus();
        }
      });
    }
    if (laserdevice != null)
    {
      search:
      {
        for (LaserDevice ld : LaserDeviceManager.getInstance().getAll())
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
        for (MaterialProfile mp : MaterialManager.getInstance().getAll())
        {
          if (material.equals(mp.getName()))
          {
            model.setMaterial(mp);
            break search;
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
      model.setMaterialThickness(height);
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
        LinkedList<String> warnings = new LinkedList<String>();
        model.loadFile(MappingManager.getInstance(), f, warnings, false);
        if (execute && !VisicutModel.PLFFilter.accept(f))
        {
          System.err.println("WARNING: execut parameter is only valid for PLF files. Will be ignored");
        }
        for(String s : warnings)
        {
          System.err.println("WARNING: "+s);
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
    if (execute && file.toLowerCase().endsWith("plf"))
    {
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
      Map<LaserProfile, List<LaserProperty>> propmap = new LinkedHashMap<LaserProfile, List<LaserProperty>>();
      //check if all settings are available
      for (PlfPart part : model.getPlfFile())
      {
        for (Mapping ms : part.getMapping())
        {
          LaserProfile p = ms.getProfile();
          if (p == null)
          {
            continue;
          }
          List<LaserProperty> list = LaserPropertyManager.getInstance().getLaserProperties(model.getSelectedLaserDevice(), model.getMaterial(), ms.getProfile(), model.getMaterialThickness());
          if (list == null)
          {
            System.err.println("Combination of Laserdevice, Material and Mapping is not supported");
            System.exit(1);
          }
          propmap.put(p, list);
        }
      }
      try
      {
        List<String> warnings = new LinkedList<String>();
        VisicutModel.getInstance().sendJob("VisiCut 1", new ProgressListener(){

          public void progressChanged(Object source, int percent)
          {
            System.out.println(percent+"%");
          }

          public void taskChanged(Object source, String taskName)
          {
            System.out.println(taskName);
          }
        }, propmap, warnings);
        for (String w : warnings)
        {
          System.out.println("WARNING: "+w);
        }
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

  private void convertSettings()
  {
    System.out.println("Converting settings...");
    MappingManager.getInstance().getAll();
    for (LaserDevice l : LaserDeviceManager.getInstance().getAll())
    {
      for (MaterialProfile m : MaterialManager.getInstance().getAll())
      {
        for (float h : m.getMaterialThicknesses())
        {
          for (LaserProfile p : ProfileManager.getInstance().getAll())
          {
            try
            {
              LaserPropertyManager.getInstance().getLaserProperties(l, m, p, h);
            }
            catch (FileNotFoundException ex)
            {
              Logger.getLogger(VisicutApp.class.getName()).log(Level.SEVERE, null, ex);
            }
            catch (IOException ex)
            {
              Logger.getLogger(VisicutApp.class.getName()).log(Level.SEVERE, null, ex);
            }
          }
        }
      }
    }
    System.out.println("done.");
  }
}
