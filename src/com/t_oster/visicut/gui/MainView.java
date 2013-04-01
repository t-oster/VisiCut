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
package com.t_oster.visicut.gui;

import com.apple.eawt.AppEvent.AboutEvent;
import com.apple.eawt.AppEvent.OpenFilesEvent;
import com.apple.eawt.AppEvent.QuitEvent;
import com.apple.eawt.QuitResponse;
import com.t_oster.liblasercut.IllegalJobException;
import com.t_oster.liblasercut.LaserCutter;
import com.t_oster.liblasercut.LaserProperty;
import com.t_oster.liblasercut.ProgressListener;
import com.t_oster.liblasercut.platform.Util;
import com.t_oster.uicomponents.PlatformIcon;
import com.t_oster.uicomponents.Ruler;
import com.t_oster.uicomponents.warnings.Message;
import com.t_oster.visicut.VisicutModel;
import com.t_oster.visicut.gui.beans.CreateNewMaterialDialog;
import com.t_oster.visicut.gui.beans.CreateNewThicknessDialog;
import com.t_oster.visicut.managers.LaserDeviceManager;
import com.t_oster.visicut.managers.LaserPropertyManager;
import com.t_oster.visicut.managers.MappingManager;
import com.t_oster.visicut.managers.MaterialManager;
import com.t_oster.visicut.managers.PreferencesManager;
import com.t_oster.visicut.managers.ProfileManager;
import com.t_oster.visicut.misc.DialogHelper;
import com.t_oster.visicut.misc.ExtensionFilter;
import com.t_oster.visicut.misc.Helper;
import com.t_oster.visicut.model.LaserDevice;
import com.t_oster.visicut.model.LaserProfile;
import com.t_oster.visicut.model.MaterialProfile;
import com.t_oster.visicut.model.PlfPart;
import com.t_oster.visicut.model.VectorProfile;
import com.t_oster.visicut.model.mapping.MappingSet;
import java.awt.FileDialog;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.filechooser.FileFilter;
import org.jdesktop.application.Action;

/**
 *
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class MainView extends javax.swing.JFrame
{

  private static MainView instance = null;
  private ResourceBundle bundle = java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/resources/MainView");

  public static MainView getInstance()
  {
    return instance;
  }

  final protected DialogHelper dialog = new DialogHelper(this, this.getTitle())
  {

    @Override
    public void showWarningMessage(String text)
    {
      MainView.this.warningPanel.addMessage(new Message("Warning", text, Message.Type.WARNING, null));
    }

    @Override
    public void showSuccessMessage(String text)
    {
      MainView.this.warningPanel.addMessage(new Message("Success", text, Message.Type.SUCCESS, null));
    }

    @Override
    public void showInfoMessage(String text)
    {
      MainView.this.warningPanel.addMessage(new Message("Info", text, Message.Type.INFO, null));
    }

    @Override
    public void showErrorMessage(Exception cause)
    {
      cause.printStackTrace();
      MainView.this.warningPanel.addMessage(new Message("Error", "Exception: "+cause.getLocalizedMessage(), Message.Type.ERROR, null));
    }

    @Override
    public void showErrorMessage(Exception cause, String text)
    {
      cause.printStackTrace();
      MainView.this.warningPanel.addMessage(new Message("Error", text+": "+cause.getLocalizedMessage(), Message.Type.ERROR, null));
    }

    @Override
    public void showErrorMessage(String text)
    {
      MainView.this.warningPanel.addMessage(new Message("Error", text, Message.Type.ERROR, null));
    }
  };
  private boolean ignoreLaserCutterComboBoxUpdates;

  public MainView(File loadedFile)
  {
    this();
    this.loadFileReal(loadedFile, true);
  }

  /** Creates new form MainView */
  public MainView()
  {
    instance = this;
    initComponents();
    jScrollPane2.setColumnHeaderView(new Ruler(this.previewPanel, Ruler.HORIZONTAL));
    jScrollPane2.setRowHeaderView(new Ruler(this.previewPanel, Ruler.VERTICAL));
    jScrollPane2.setCorner(JScrollPane.UPPER_LEFT_CORNER, new JLabel("cm"));
    //fixes slow scrolling issue on Mac OS X
    JViewport viewPort = jScrollPane2.getViewport();
    viewPort.setScrollMode(JViewport.BLIT_SCROLL_MODE);
    viewPort.setScrollMode(JViewport.BACKINGSTORE_SCROLL_MODE);
    viewPort.setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
    jScrollPane2.addComponentListener(new ComponentListener(){
      public void componentResized(ComponentEvent ce)
      {
        MainView.this.previewPanel.resizeToFitZoomAndArea();
      }
      public void componentMoved(ComponentEvent ce){}
      public void componentShown(ComponentEvent ce){}
      public void componentHidden(ComponentEvent ce){}
    });
    if (Helper.isMacOS())
    {//Mac OS has its own exit menu and different Keybindings
      fileMenu.remove(exitMenuItem);
      openMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.META_MASK));
      reloadMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_R, java.awt.event.InputEvent.META_MASK));
      newMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_N, java.awt.event.InputEvent.META_MASK));
      saveMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.META_MASK));
      executeJobMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_P, java.awt.event.InputEvent.META_MASK));
      zoomInMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ADD, java.awt.event.InputEvent.META_MASK));
      zoomOutMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_SUBTRACT, java.awt.event.InputEvent.META_MASK));
    }
    fillComboBoxes();
    refreshMaterialThicknessesComboBox();

    if (this.visicutModel1.getSelectedLaserDevice() != null && this.visicutModel1.getSelectedLaserDevice().getCameraURL() != null)
    {
      this.captureImage();
    }
    if (Helper.isMacOS())
    {
      com.apple.eawt.Application macApplication = com.apple.eawt.Application.getApplication();
      macApplication.setQuitHandler(new com.apple.eawt.QuitHandler() {

        public void handleQuitRequestWith(QuitEvent qe, QuitResponse qr)
        {
          MainView.this.visicutModel1.updatePreferences();
          System.exit(0);
        }
      });
      macApplication.setAboutHandler(new com.apple.eawt.AboutHandler() {

        public void handleAbout(AboutEvent ae)
        {
          MainView.this.aboutMenuItemActionPerformed(null);
        }
      });
      macApplication.setOpenFileHandler(new com.apple.eawt.OpenFilesHandler() {

        public void openFiles(OpenFilesEvent ofe)
        {
          for (File f : ofe.getFiles())
          {
            MainView.this.loadFile(f, false);
          }
        }
      });
    }
    //Window listener for capturing close and save preferences before exiting
    this.addWindowListener(new WindowListener()
    {

      public void windowOpened(WindowEvent e)
      {
      }

      public void windowClosing(WindowEvent e)
      {
        PreferencesManager.getInstance().getPreferences().setWindowBounds(MainView.this.getBounds());
        MainView.this.visicutModel1.updatePreferences();
      }

      public void windowClosed(WindowEvent e)
      {
      }

      public void windowIconified(WindowEvent e)
      {
      }

      public void windowDeiconified(WindowEvent e)
      {
      }

      public void windowActivated(WindowEvent e)
      {
      }

      public void windowDeactivated(WindowEvent e)
      {
      }
    });
    this.refreshRecentFilesMenu();
    this.jmInstallInkscape.setEnabled(Helper.isInkscapeExtensionInstallable());
    this.jmInstallIllustrator.setEnabled(Helper.isIllustratorScriptInstallable());
    if (!Helper.isInkscapeExtensionInstallable() && !Helper.isIllustratorScriptInstallable())
    {
      this.jmExtras.setVisible(false);
    }
    this.refreshExampleMenu();
    //initialize states coorectly
    this.visicutModel1PropertyChange(new java.beans.PropertyChangeEvent(visicutModel1, VisicutModel.PROP_SELECTEDLASERDEVICE, null, null));
    this.visicutModel1PropertyChange(new java.beans.PropertyChangeEvent(visicutModel1, VisicutModel.PROP_SELECTEDPART, null, null));

    //apply the saved window size and position, if in current screen size
    Rectangle lastBounds = PreferencesManager.getInstance().getPreferences().getWindowBounds();
    if (lastBounds != null && this.getGraphicsConfiguration().getBounds().contains(lastBounds))
    {
      this.setSize(lastBounds.width, lastBounds.height);
      this.validate();
      this.setLocation(lastBounds.x, lastBounds.y);
    }
    PositionPanelController c = new PositionPanelController(positionPanel, visicutModel1);
    this.warningPanel.setVisible(false);
    LaserDeviceManager.getInstance().addPropertyChangeListener(new PropertyChangeListener(){
      public void propertyChange(PropertyChangeEvent pce)
      {
        refreshLaserDeviceComboBox();
      }
    });
  }

  private ActionListener exampleItemClicked = new ActionListener(){
      public void actionPerformed(ActionEvent ae)
      {
        if (!"".equals(ae.getActionCommand()))
        {
          MainView.this.loadFile(PreferencesManager.getInstance().getExampleFile(ae.getActionCommand()), false);
        }
      }
    };

  private JMenuItem openExamples;
  private void refreshExampleMenu()
  {
    jmExamples.removeAll();
    for (String example : PreferencesManager.getInstance().getExampleFilenames())
    {
      JMenuItem item = new JMenuItem(example);
      item.setActionCommand(example);
      item.addActionListener(exampleItemClicked);
      this.jmExamples.add(item);
    }
    if (openExamples == null)
    {
      //TODO: i10n
      openExamples = new JMenuItem(bundle.getString("EDIT"));
      openExamples.addActionListener(new ActionListener(){

        public void actionPerformed(ActionEvent ae)
        {
          dialog.openInFilebrowser(new File(Helper.getBasePath(), "examples"));
        }
      });
    }
    jmExamples.add(openExamples);
  }

  /**
   * Fills the recent files menu from the current
   * list in preferences
   */
  private void refreshRecentFilesMenu()
  {
    this.recentFilesMenu.removeAll();
    for (String p: this.visicutModel1.getPreferences().getRecentFiles())
    {
      final File f = new File(p);
      if (f.isFile())
      {
        JMenuItem i = new JMenuItem(f.getName());
        i.addActionListener(new ActionListener(){
          public void actionPerformed(ActionEvent ae)
          {
            loadFile(f, false);
          }
        });
        this.recentFilesMenu.add(i);
      }
    }
  }

  private void refreshMaterialComboBox()
  {
    this.ignoreMaterialComboBoxChanges = true;
    String sp = this.visicutModel1.getMaterial() != null ? this.visicutModel1.getMaterial().getName() : null;
    this.materialComboBox.removeAllItems();
    for (MaterialProfile mp : MaterialManager.getInstance().getAll())
    {
      this.materialComboBox.addItem(mp);
      if (mp.getName().equals(sp))
      {
        this.materialComboBox.setSelectedItem(mp);
      }
    }
    this.ignoreMaterialComboBoxChanges = false;
    //hide lasercutter combo box if only one lasercutter available
    boolean materialUiVisible = true;
    if (this.materialComboBox.getItemCount() == 1 && MaterialManager.getInstance().getAll().get(0).getMaterialThicknesses().size() == 1)
    {
      this.materialComboBox.setSelectedIndex(0);
      materialUiVisible = false;
    }
    this.materialComboBox.setVisible(materialUiVisible);
    this.btAddMaterial.setVisible(materialUiVisible);
    this.cbMaterialThickness.setVisible(materialUiVisible);
    this.btAddMaterialThickness.setVisible(materialUiVisible);
    this.jLabel1.setVisible(materialUiVisible);
    this.jLabel5.setVisible(materialUiVisible);
  }

  /*
   * Initially fills LaserCutter, Material, Object and Mapping ComboBox with all possible Elements
   */
  private void fillComboBoxes()
  {
    this.refreshLaserDeviceComboBox();
    this.refreshMaterialComboBox();
    this.refreshObjectComboBox();
  }
  
  private void refreshLaserDeviceComboBox()
  {
    String sld = this.visicutModel1.getSelectedLaserDevice() != null ? this.visicutModel1.getSelectedLaserDevice().getName() : null;
    ignoreLaserCutterComboBoxUpdates = true;
    this.laserCutterComboBox.removeAllItems();
    for (LaserDevice ld : LaserDeviceManager.getInstance().getAll())
    {
      this.laserCutterComboBox.addItem(ld);
      if (ld.getName().equals(sld))
      {
        this.laserCutterComboBox.setSelectedItem(ld);
      }
    }
    //hide lasercutter combo box if only one lasercutter available
    if (this.laserCutterComboBox.getItemCount() == 1)
    {
      this.laserCutterComboBox.setSelectedIndex(0);
      this.laserCutterComboBox.setVisible(false);
      this.jLabel9.setVisible(false);
    }
    else
    {
      this.laserCutterComboBox.setVisible(true);
      this.jLabel9.setVisible(true);
    }
    ignoreLaserCutterComboBoxUpdates = false;
  }
   /**
   * update entries of objectComboBox, then update selection
   * @param forceUpdate even update if the list of PlfParts has not changed
   * @throws RuntimeException if forceUpdate==false and PlfParts have not changed
   */
  public void refreshObjectComboBox() {        
    this.ignoreObjectComboBoxEvents = true;
    // fill new list of PlfItems
    this.objectComboBox.removeAllItems();
    if (VisicutModel.getInstance().getSelectedPart() == null) {
      // add default "nothing selected" item if nothing is selected
      this.objectComboBox.addItem(java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/resources/MainView").getString("(nothing selected)"));
    }
    for (PlfPart p: VisicutModel.getInstance().getPlfFile()) {
      if (p == null || p.getSourceFile() == null) { // necessary? 
        continue;
      }
      // add regular item
      this.objectComboBox.addItem(p);
    }
    
    // now set the correct selection
    if (VisicutModel.getInstance().getSelectedPart() != null) {
      // something is selected, also select this in the combobox
      this.objectComboBox.setSelectedItem(VisicutModel.getInstance().getSelectedPart());
    } else {
      // no PlfPart is selected, so select the pseudo-item "nothing selected"
      this.objectComboBox.setSelectedIndex(0);
    }
    //gradually hide complexity. Issue #71
    int files = this.visicutModel1.getPlfFile().size();
    boolean selected = this.visicutModel1.getSelectedPart() != null;
    if (files == 1 && selected)
    {
      this.jLabel2.setText("<html>"+this.bundle.getString("jLabel2.text")+" <b>"+this.visicutModel1.getSelectedPart().getSourceFile().getName()+"</b></html>");
    }
    else
    {
      this.jLabel2.setText(this.bundle.getString("jLabel2.text") + (files == 1 && !selected ? " "+this.bundle.getString("(nothing selected)") : ""));
    }
    
    this.objectComboBox.setVisible(files > 1);
    this.btRemoveObject.setVisible(files > 0 && selected);
    this.ignoreObjectComboBoxEvents = false;
  }
  
  /** This method is called from within the constructor to
   * initialize the form.
   * WARNING: Do NOT modify this code. The content of this method is
   * always regenerated by the Form Editor.
   */
  @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        bindingGroup = new org.jdesktop.beansbinding.BindingGroup();

        visicutModel1 = VisicutModel.getInstance();
        filesDropSupport1 = new com.t_oster.uicomponents.FilesDropSupport();
        saveFileChooser = new javax.swing.JFileChooser();
        buttonGroup1 = new javax.swing.ButtonGroup();
        captureImageButton = new javax.swing.JButton();
        progressBar = new javax.swing.JProgressBar();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        btFitScreen = new javax.swing.JButton();
        bt1to1 = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        previewPanel = new com.t_oster.visicut.gui.beans.PreviewPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jPanel2 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        materialComboBox = new com.t_oster.uicomponents.ImageComboBox();
        jLabel9 = new javax.swing.JLabel();
        laserCutterComboBox = new com.t_oster.uicomponents.ImageComboBox();
        jLabel10 = new javax.swing.JLabel();
        calculateTimeButton = new javax.swing.JButton();
        timeLabel = new javax.swing.JLabel();
        mappingTabbedPane = new javax.swing.JTabbedPane();
        mappingPanel = new com.t_oster.visicut.gui.mapping.MappingPanel();
        positionPanel = new com.t_oster.uicomponents.PositionPanel();
        propertyPanelContainer = new javax.swing.JScrollPane();
        propertiesPanel = new com.t_oster.visicut.gui.propertypanel.PropertiesPanel();
        btAddMaterial = new javax.swing.JButton();
        cbMaterialThickness = new javax.swing.JComboBox();
        btAddMaterialThickness = new javax.swing.JButton();
        jCheckBox1 = new javax.swing.JCheckBox();
        executeJobButton = new javax.swing.JButton();
        objectComboBox = new javax.swing.JComboBox();
        jSeparator1 = new javax.swing.JSeparator();
        btRemoveObject = new javax.swing.JButton();
        btAddObject = new javax.swing.JButton();
        warningPanel = new com.t_oster.uicomponents.warnings.WarningPanel();
        menuBar = new javax.swing.JMenuBar();
        fileMenu = new javax.swing.JMenu();
        newMenuItem = new javax.swing.JMenuItem();
        openMenuItem = new javax.swing.JMenuItem();
        importMenuItem = new javax.swing.JMenuItem();
        recentFilesMenu = new javax.swing.JMenu();
        jmExamples = new javax.swing.JMenu();
        saveMenuItem = new javax.swing.JMenuItem();
        saveAsMenuItem = new javax.swing.JMenuItem();
        exitMenuItem = new javax.swing.JMenuItem();
        editMenu = new javax.swing.JMenu();
        calibrateCameraMenuItem = new javax.swing.JMenuItem();
        reloadMenuItem = new javax.swing.JMenuItem();
        executeJobMenuItem = new javax.swing.JMenuItem();
        jMenu1 = new javax.swing.JMenu();
        jmImportSettings = new javax.swing.JMenuItem();
        jmExportSettings = new javax.swing.JMenuItem();
        editMappingMenuItem = new javax.swing.JMenuItem();
        jmManageLaserprofiles = new javax.swing.JMenuItem();
        jMenuItem2 = new javax.swing.JMenuItem();
        materialMenuItem = new javax.swing.JMenuItem();
        viewMenu = new javax.swing.JMenu();
        showGridMenuItem = new javax.swing.JCheckBoxMenuItem();
        jmShowPhoto = new javax.swing.JCheckBoxMenuItem();
        zoomInMenuItem = new javax.swing.JMenuItem();
        zoomOutMenuItem = new javax.swing.JMenuItem();
        jmExtras = new javax.swing.JMenu();
        jmInstallInkscape = new javax.swing.JMenuItem();
        jmInstallIllustrator = new javax.swing.JMenuItem();
        helpMenu = new javax.swing.JMenu();
        aboutMenuItem = new javax.swing.JMenuItem();

        visicutModel1.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                visicutModel1PropertyChange(evt);
            }
        });

        filesDropSupport1.setComponent(previewPanel);
        filesDropSupport1.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                filesDropSupport1PropertyChange(evt);
            }
        });

        saveFileChooser.setAcceptAllFileFilterUsed(false);
        saveFileChooser.setDialogType(javax.swing.JFileChooser.SAVE_DIALOG);
        saveFileChooser.setFileFilter(new ExtensionFilter(".plf", java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/resources/MainView").getString("VISICUT PORTABLE LASER FILE")));
        saveFileChooser.setName("saveFileChooser"); // NOI18N

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(com.t_oster.visicut.gui.VisicutApp.class).getContext().getResourceMap(MainView.class);
        setTitle(resourceMap.getString("Form.title")); // NOI18N
        setLocationByPlatform(true);
        setName("Form"); // NOI18N

        captureImageButton.setIcon(PlatformIcon.get(PlatformIcon.CAMERA));
        captureImageButton.setText(resourceMap.getString("captureImageButton.text")); // NOI18N
        captureImageButton.setMaximumSize(new java.awt.Dimension(129, 35));
        captureImageButton.setMinimumSize(new java.awt.Dimension(129, 35));
        captureImageButton.setName("captureImageButton"); // NOI18N
        captureImageButton.setPreferredSize(new java.awt.Dimension(129, 35));
        captureImageButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                captureImageButtonActionPerformed(evt);
            }
        });

        progressBar.setName("progressBar"); // NOI18N

        jButton1.setIcon(PlatformIcon.get(PlatformIcon.ZOOM_IN));
        jButton1.setText(resourceMap.getString("jButton1.text")); // NOI18N
        jButton1.setToolTipText(resourceMap.getString("jButton1.toolTipText")); // NOI18N
        jButton1.setMaximumSize(new java.awt.Dimension(30, 30));
        jButton1.setMinimumSize(new java.awt.Dimension(30, 30));
        jButton1.setName("jButton1"); // NOI18N
        jButton1.setPreferredSize(new java.awt.Dimension(35, 35));
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jButton2.setIcon(PlatformIcon.get(PlatformIcon.ZOOM_OUT));
        jButton2.setText(resourceMap.getString("jButton2.text")); // NOI18N
        jButton2.setToolTipText(resourceMap.getString("jButton2.toolTipText")); // NOI18N
        jButton2.setMaximumSize(new java.awt.Dimension(30, 30));
        jButton2.setMinimumSize(new java.awt.Dimension(30, 30));
        jButton2.setName("jButton2"); // NOI18N
        jButton2.setPreferredSize(new java.awt.Dimension(35, 35));
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        btFitScreen.setIcon(PlatformIcon.get(PlatformIcon.ZOOM_LASERBED));
        btFitScreen.setText(resourceMap.getString("btFitScreen.text")); // NOI18N
        btFitScreen.setToolTipText(resourceMap.getString("btFitScreen.toolTipText")); // NOI18N
        btFitScreen.setMaximumSize(new java.awt.Dimension(35, 35));
        btFitScreen.setMinimumSize(new java.awt.Dimension(35, 35));
        btFitScreen.setName("btFitScreen"); // NOI18N
        btFitScreen.setPreferredSize(new java.awt.Dimension(35, 35));
        btFitScreen.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btFitScreenActionPerformed(evt);
            }
        });

        bt1to1.setIcon(PlatformIcon.get(PlatformIcon.ZOOM_ACTUAL));
        bt1to1.setText(resourceMap.getString("bt1to1.text")); // NOI18N
        bt1to1.setToolTipText(resourceMap.getString("bt1to1.toolTipText")); // NOI18N
        bt1to1.setMaximumSize(new java.awt.Dimension(35, 35));
        bt1to1.setMinimumSize(new java.awt.Dimension(35, 35));
        bt1to1.setName("bt1to1"); // NOI18N
        bt1to1.setPreferredSize(new java.awt.Dimension(35, 35));
        bt1to1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bt1to1ActionPerformed(evt);
            }
        });

        jScrollPane2.setBorder(javax.swing.BorderFactory.createTitledBorder(resourceMap.getString("jPanel1.border.title"))); // NOI18N
        jScrollPane2.setName("jScrollPane2"); // NOI18N
        jScrollPane2.setWheelScrollingEnabled(false);

        previewPanel.setName("previewPanel"); // NOI18N
        com.t_oster.visicut.gui.PreviewPanelKeyboardMouseHandler ppMouseHandler = new com.t_oster.visicut.gui.PreviewPanelKeyboardMouseHandler(this.previewPanel);

        javax.swing.GroupLayout previewPanelLayout = new javax.swing.GroupLayout(previewPanel);
        previewPanel.setLayout(previewPanelLayout);
        previewPanelLayout.setHorizontalGroup(
            previewPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 716, Short.MAX_VALUE)
        );
        previewPanelLayout.setVerticalGroup(
            previewPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 671, Short.MAX_VALUE)
        );

        jScrollPane2.setViewportView(previewPanel);

        jScrollPane1.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        jScrollPane1.setName("jScrollPane1"); // NOI18N

        jPanel2.setName("jPanel2"); // NOI18N

        jLabel1.setText(resourceMap.getString("jLabel1.text")); // NOI18N
        jLabel1.setName("jLabel1"); // NOI18N

        jLabel2.setText(resourceMap.getString("jLabel2.text")); // NOI18N
        jLabel2.setName("jLabel2"); // NOI18N

        jLabel5.setText(resourceMap.getString("jLabel5.text")); // NOI18N
        jLabel5.setName("jLabel5"); // NOI18N

        materialComboBox.setName("materialComboBox"); // NOI18N
        materialComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                materialComboBoxActionPerformed(evt);
            }
        });

        jLabel9.setText(resourceMap.getString("jLabel9.text")); // NOI18N
        jLabel9.setName("jLabel9"); // NOI18N

        laserCutterComboBox.setName("laserCutterComboBox"); // NOI18N
        laserCutterComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                laserCutterComboBoxActionPerformed(evt);
            }
        });

        jLabel10.setText(resourceMap.getString("jLabel10.text")); // NOI18N
        jLabel10.setName("jLabel10"); // NOI18N

        calculateTimeButton.setText(resourceMap.getString("calculateTimeButton.text")); // NOI18N
        calculateTimeButton.setEnabled(false);
        calculateTimeButton.setName("calculateTimeButton"); // NOI18N
        calculateTimeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                calculateTimeButtonActionPerformed(evt);
            }
        });

        timeLabel.setText(resourceMap.getString("timeLabel.text")); // NOI18N
        timeLabel.setName("timeLabel"); // NOI18N

        mappingTabbedPane.setName("Custom"); // NOI18N

        mappingPanel.setName("mappingPanel"); // NOI18N
        mappingTabbedPane.addTab(resourceMap.getString("mappingPanel.TabConstraints.tabTitle"), mappingPanel); // NOI18N

        positionPanel.setName("positionPanel"); // NOI18N
        mappingTabbedPane.addTab(resourceMap.getString("positionPanel.TabConstraints.tabTitle"), positionPanel); // NOI18N

        propertyPanelContainer.setName("propertyPanelContainer"); // NOI18N

        propertiesPanel.setName("propertiesPanel"); // NOI18N
        propertiesPanel.setLayout(new javax.swing.BoxLayout(propertiesPanel, javax.swing.BoxLayout.Y_AXIS));
        propertyPanelContainer.setViewportView(propertiesPanel);

        mappingTabbedPane.addTab(resourceMap.getString("propertyPanelContainer.TabConstraints.tabTitle"), propertyPanelContainer); // NOI18N

        btAddMaterial.setIcon(PlatformIcon.get(PlatformIcon.ADD));
        btAddMaterial.setName("btAddMaterial"); // NOI18N
        btAddMaterial.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btAddMaterialActionPerformed(evt);
            }
        });

        cbMaterialThickness.setName("cbMaterialThickness"); // NOI18N
        cbMaterialThickness.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cbMaterialThicknessActionPerformed(evt);
            }
        });

        btAddMaterialThickness.setIcon(PlatformIcon.get(PlatformIcon.ADD));
        btAddMaterialThickness.setName("btAddMaterialThickness"); // NOI18N
        btAddMaterialThickness.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btAddMaterialThicknessActionPerformed(evt);
            }
        });

        jCheckBox1.setText(resourceMap.getString("jCheckBox1.text")); // NOI18N
        jCheckBox1.setToolTipText(resourceMap.getString("jCheckBox1.toolTipText")); // NOI18N
        jCheckBox1.setName("jCheckBox1"); // NOI18N

        org.jdesktop.beansbinding.Binding binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, visicutModel1, org.jdesktop.beansbinding.ELProperty.create("${useThicknessAsFocusOffset}"), jCheckBox1, org.jdesktop.beansbinding.BeanProperty.create("selected"), "cbUseThickness");
        bindingGroup.addBinding(binding);

        executeJobButton.setText(resourceMap.getString("executeJobButton.text")); // NOI18N
        executeJobButton.setName("executeJobButton"); // NOI18N
        executeJobButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                executeJobButtonActionPerformed(evt);
            }
        });

        objectComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        objectComboBox.setName("objectComboBox"); // NOI18N
        objectComboBox.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                objectComboBoxChangeHandler(evt);
            }
        });

        jSeparator1.setName("jSeparator1"); // NOI18N

        btRemoveObject.setIcon(PlatformIcon.get(PlatformIcon.REMOVE_FILE));
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("com/t_oster/uicomponents/resources/EditableTablePanel"); // NOI18N
        btRemoveObject.setToolTipText(bundle.getString("-")); // NOI18N
        btRemoveObject.setMaximumSize(new java.awt.Dimension(35, 35));
        btRemoveObject.setMinimumSize(new java.awt.Dimension(35, 35));
        btRemoveObject.setName("btRemoveObject"); // NOI18N
        btRemoveObject.setPreferredSize(new java.awt.Dimension(35, 35));
        btRemoveObject.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btRemoveObjectActionPerformed(evt);
            }
        });

        btAddObject.setIcon(PlatformIcon.get(PlatformIcon.ADD_FILE));
        btAddObject.setToolTipText(bundle.getString("+")); // NOI18N
        btAddObject.setMaximumSize(new java.awt.Dimension(35, 35));
        btAddObject.setMinimumSize(new java.awt.Dimension(35, 35));
        btAddObject.setName("btAddObject"); // NOI18N
        btAddObject.setPreferredSize(new java.awt.Dimension(35, 35));
        btAddObject.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btAddObjectActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(mappingTabbedPane, 0, 0, Short.MAX_VALUE)
                        .addContainerGap())
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(executeJobButton, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(jLabel10)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(timeLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 166, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 232, Short.MAX_VALUE)
                                .addComponent(calculateTimeButton)))
                        .addContainerGap())
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel2Layout.createSequentialGroup()
                                .addComponent(jLabel2)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(objectComboBox, 0, 492, Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btAddObject, javax.swing.GroupLayout.DEFAULT_SIZE, 36, Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btRemoveObject, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(jSeparator1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 606, Short.MAX_VALUE)
                            .addComponent(jLabel9, javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel1, javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(laserCutterComboBox, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 606, Short.MAX_VALUE)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(materialComboBox, javax.swing.GroupLayout.DEFAULT_SIZE, 569, Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btAddMaterial, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel2Layout.createSequentialGroup()
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addGroup(jPanel2Layout.createSequentialGroup()
                                        .addComponent(cbMaterialThickness, javax.swing.GroupLayout.PREFERRED_SIZE, 122, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(btAddMaterialThickness, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addComponent(jLabel5))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 250, Short.MAX_VALUE)
                                .addComponent(jCheckBox1, javax.swing.GroupLayout.PREFERRED_SIZE, 197, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(20, 20, 20))))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel9)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(laserCutterComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel1)
                .addGap(6, 6, 6)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(btAddMaterial, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(materialComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel5)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(cbMaterialThickness, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btAddMaterialThickness, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addComponent(jCheckBox1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 8, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(objectComboBox)
                        .addComponent(jLabel2))
                    .addComponent(btRemoveObject, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btAddObject, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(mappingTabbedPane, javax.swing.GroupLayout.PREFERRED_SIZE, 405, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(timeLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel10)
                    .addComponent(calculateTimeButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(executeJobButton)
                .addContainerGap())
        );

        java.util.ResourceBundle bundle1 = java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/resources/MainView"); // NOI18N
        mappingTabbedPane.getAccessibleContext().setAccessibleName(bundle1.getString("MAPPING")); // NOI18N

        jScrollPane1.setViewportView(jPanel2);

        warningPanel.setName("warningPanel"); // NOI18N
        warningPanel.setPreferredSize(new java.awt.Dimension(276, 123));

        menuBar.setName("menuBar"); // NOI18N

        fileMenu.setMnemonic('f');
        fileMenu.setText(resourceMap.getString("fileMenu.text")); // NOI18N
        fileMenu.setName("fileMenu"); // NOI18N

        newMenuItem.setText(resourceMap.getString("newMenuItem.text")); // NOI18N
        newMenuItem.setName("newMenuItem"); // NOI18N
        newMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                newMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(newMenuItem);

        openMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.CTRL_MASK));
        openMenuItem.setText(resourceMap.getString("openMenuItem.text")); // NOI18N
        openMenuItem.setName("openMenuItem"); // NOI18N
        openMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(openMenuItem);

        importMenuItem.setText(resourceMap.getString("importMenuItem.text")); // NOI18N
        importMenuItem.setName("importMenuItem"); // NOI18N
        importMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(importMenuItem);

        recentFilesMenu.setText(resourceMap.getString("recentFilesMenu.text")); // NOI18N
        recentFilesMenu.setName("recentFilesMenu"); // NOI18N
        fileMenu.add(recentFilesMenu);

        jmExamples.setText(resourceMap.getString("jmExamples.text")); // NOI18N
        jmExamples.setName("jmExamples"); // NOI18N
        fileMenu.add(jmExamples);

        saveMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.CTRL_MASK));
        saveMenuItem.setMnemonic('s');
        saveMenuItem.setText(resourceMap.getString("saveMenuItem.text")); // NOI18N
        saveMenuItem.setName("saveMenuItem"); // NOI18N
        saveMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(saveMenuItem);

        saveAsMenuItem.setMnemonic('a');
        saveAsMenuItem.setText(resourceMap.getString("saveAsMenuItem.text")); // NOI18N
        saveAsMenuItem.setName("saveAsMenuItem"); // NOI18N
        saveAsMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveAsMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(saveAsMenuItem);

        exitMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F4, java.awt.event.InputEvent.ALT_MASK));
        exitMenuItem.setMnemonic('x');
        exitMenuItem.setText(resourceMap.getString("exitMenuItem.text")); // NOI18N
        exitMenuItem.setName("exitMenuItem"); // NOI18N
        exitMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exitMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(exitMenuItem);

        menuBar.add(fileMenu);

        editMenu.setMnemonic('e');
        editMenu.setText(resourceMap.getString("editMenu.text")); // NOI18N
        editMenu.setName("editMenu"); // NOI18N

        calibrateCameraMenuItem.setText(resourceMap.getString("calibrateCameraMenuItem.text")); // NOI18N
        calibrateCameraMenuItem.setName("calibrateCameraMenuItem"); // NOI18N
        calibrateCameraMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                calibrateCameraMenuItemActionPerformed(evt);
            }
        });
        editMenu.add(calibrateCameraMenuItem);

        reloadMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_R, java.awt.event.InputEvent.CTRL_MASK));
        reloadMenuItem.setText(resourceMap.getString("reloadMenuItem.text")); // NOI18N
        reloadMenuItem.setToolTipText(resourceMap.getString("reloadMenuItem.toolTipText")); // NOI18N
        reloadMenuItem.setEnabled(false);
        reloadMenuItem.setName("reloadMenuItem"); // NOI18N
        reloadMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                reloadMenuItemActionPerformed(evt);
            }
        });
        editMenu.add(reloadMenuItem);

        executeJobMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_P, java.awt.event.InputEvent.CTRL_MASK));
        executeJobMenuItem.setText(resourceMap.getString("executeJobMenuItem.text")); // NOI18N
        executeJobMenuItem.setName("executeJobMenuItem"); // NOI18N
        executeJobMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                executeJobMenuItemActionPerformed(evt);
            }
        });
        editMenu.add(executeJobMenuItem);

        jMenu1.setText(resourceMap.getString("jMenu1.text")); // NOI18N
        jMenu1.setName("jMenu1"); // NOI18N

        jmImportSettings.setText(resourceMap.getString("jmImportSettings.text")); // NOI18N
        jmImportSettings.setName("jmImportSettings"); // NOI18N
        jmImportSettings.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jmImportSettingsActionPerformed(evt);
            }
        });
        jMenu1.add(jmImportSettings);

        jmExportSettings.setText(resourceMap.getString("jmExportSettings.text")); // NOI18N
        jmExportSettings.setName("jmExportSettings"); // NOI18N
        jmExportSettings.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jmExportSettingsActionPerformed(evt);
            }
        });
        jMenu1.add(jmExportSettings);

        editMappingMenuItem.setText(resourceMap.getString("editMappingMenuItem.text")); // NOI18N
        editMappingMenuItem.setName("editMappingMenuItem"); // NOI18N
        editMappingMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                editMappingMenuItemActionPerformed(evt);
            }
        });
        jMenu1.add(editMappingMenuItem);

        jmManageLaserprofiles.setText(resourceMap.getString("jmManageLaserprofiles.text")); // NOI18N
        jmManageLaserprofiles.setName("jmManageLaserprofiles"); // NOI18N
        jmManageLaserprofiles.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jmManageLaserprofilesActionPerformed(evt);
            }
        });
        jMenu1.add(jmManageLaserprofiles);

        jMenuItem2.setText(resourceMap.getString("jMenuItem2.text")); // NOI18N
        jMenuItem2.setName("jMenuItem2"); // NOI18N
        jMenuItem2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem2ActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItem2);

        materialMenuItem.setText(resourceMap.getString("materialMenuItem.text")); // NOI18N
        materialMenuItem.setName("materialMenuItem"); // NOI18N
        materialMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                materialMenuItemActionPerformed(evt);
            }
        });
        jMenu1.add(materialMenuItem);

        editMenu.add(jMenu1);

        menuBar.add(editMenu);

        viewMenu.setText(resourceMap.getString("viewMenu.text")); // NOI18N
        viewMenu.setName("viewMenu"); // NOI18N

        showGridMenuItem.setText(resourceMap.getString("showGridMenuItem.text")); // NOI18N
        showGridMenuItem.setName("showGridMenuItem"); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, previewPanel, org.jdesktop.beansbinding.ELProperty.create("${showGrid}"), showGridMenuItem, org.jdesktop.beansbinding.BeanProperty.create("selected"), "ShowGrid");
        bindingGroup.addBinding(binding);

        viewMenu.add(showGridMenuItem);

        jmShowPhoto.setText(resourceMap.getString("jmShowPhoto.text")); // NOI18N
        jmShowPhoto.setName("jmShowPhoto"); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, previewPanel, org.jdesktop.beansbinding.ELProperty.create("${showBackgroundImage}"), jmShowPhoto, org.jdesktop.beansbinding.BeanProperty.create("selected"), "jmShowBackground");
        bindingGroup.addBinding(binding);

        viewMenu.add(jmShowPhoto);

        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(com.t_oster.visicut.gui.VisicutApp.class).getContext().getActionMap(MainView.class, this);
        zoomInMenuItem.setAction(actionMap.get("zoomIn")); // NOI18N
        zoomInMenuItem.setText(resourceMap.getString("zoomInMenuItem.text")); // NOI18N
        zoomInMenuItem.setName("zoomInMenuItem"); // NOI18N
        viewMenu.add(zoomInMenuItem);

        zoomOutMenuItem.setAction(actionMap.get("zoomOut")); // NOI18N
        zoomOutMenuItem.setText(resourceMap.getString("zoomOutMenuItem.text")); // NOI18N
        zoomOutMenuItem.setName("zoomOutMenuItem"); // NOI18N
        viewMenu.add(zoomOutMenuItem);

        menuBar.add(viewMenu);

        jmExtras.setText(resourceMap.getString("jmExtras.text")); // NOI18N
        jmExtras.setName("jmExtras"); // NOI18N

        jmInstallInkscape.setText(resourceMap.getString("jmInstallInkscape.text")); // NOI18N
        jmInstallInkscape.setName("jmInstallInkscape"); // NOI18N
        jmInstallInkscape.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jmInstallInkscapeActionPerformed(evt);
            }
        });
        jmExtras.add(jmInstallInkscape);

        jmInstallIllustrator.setText(resourceMap.getString("jmInstallIllustrator.text")); // NOI18N
        jmInstallIllustrator.setName("jmInstallIllustrator"); // NOI18N
        jmInstallIllustrator.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jmInstallIllustratorActionPerformed(evt);
            }
        });
        jmExtras.add(jmInstallIllustrator);

        menuBar.add(jmExtras);

        helpMenu.setAction(actionMap.get("showAboutDialog")); // NOI18N
        helpMenu.setText(resourceMap.getString("helpMenu.text")); // NOI18N
        helpMenu.setName("helpMenu"); // NOI18N

        aboutMenuItem.setAction(actionMap.get("showAboutDialog")); // NOI18N
        aboutMenuItem.setText(resourceMap.getString("aboutMenuItem.text")); // NOI18N
        aboutMenuItem.setName("aboutMenuItem"); // NOI18N
        aboutMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                aboutMenuItemActionPerformed(evt);
            }
        });
        helpMenu.add(aboutMenuItem);

        menuBar.add(helpMenu);

        setJMenuBar(menuBar);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(warningPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 0, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jScrollPane2, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 743, Short.MAX_VALUE)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jButton2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btFitScreen, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(bt1to1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(captureImageButton, javax.swing.GroupLayout.PREFERRED_SIZE, 155, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 278, Short.MAX_VALUE)
                                .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(8, 8, 8)))
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 494, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(20, 20, 20))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 704, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jButton1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jButton2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(btFitScreen, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(bt1to1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(progressBar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(captureImageButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 651, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(warningPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );

        bindingGroup.bind();

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void exitMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exitMenuItemActionPerformed
      this.visicutModel1.updatePreferences();
      System.exit(0);
    }//GEN-LAST:event_exitMenuItemActionPerformed

  public void loadFile(File file, final boolean discardCurrent)
  {
    final File fileToLoad = file;
    lastDirectory = file.getParentFile();
    //refresh recent files
    List<String> recent = this.visicutModel1.getPreferences().getRecentFiles();
    recent.remove(file.getAbsolutePath());
    recent.add(0, file.getAbsolutePath());
    if (recent.size() > 5)
    {
      for (int i=recent.size()-1;i>=5;i--)
      {
        recent.remove(i);
      }
    }
    this.refreshRecentFilesMenu();
    try
    {
      PreferencesManager.getInstance().savePreferences();
    }
    catch (Exception ex)
    {
      Logger.getLogger(MainView.class.getName()).log(Level.SEVERE, null, ex);
    }
    new Thread()
    {

      @Override
      public void run()
      {
        MainView.this.loadFileReal(fileToLoad, discardCurrent);
      }
    }.start();
  }

  private void fitObjectsIntoBed()
  {
    final Map<PlfPart, AffineTransform> backup = new LinkedHashMap<PlfPart, AffineTransform>();
    for (PlfPart p : visicutModel1.getPlfFile())
    {
      backup.put(p, new AffineTransform(p.getGraphicObjects().getTransform()));
    }
    String text = "";
    switch (this.visicutModel1.fitObjectsIntoBed())
    {
      case MOVE:
      {
        text = bundle.getString("NEEDED_MOVE");
        break;
      }
      case ROTATE:
      {
        text = bundle.getString("NEEDED_ROTATE");
        break;
      }
      case RESIZE:
      {
        text = bundle.getString("NEEDED_REFIT");
        break;
      }
      case NONE:
      {
        return;
      }
    }
    warningPanel.addMessage(new Message("Info", text, Message.Type.INFO, new com.t_oster.uicomponents.warnings.Action[]
    {
      new com.t_oster.uicomponents.warnings.Action(bundle.getString("UNDO"))
      {
        @Override
        public boolean clicked()
        {
          for (Entry<PlfPart, AffineTransform> e : backup.entrySet())
          {
            e.getKey().getGraphicObjects().setTransform(e.getValue());
            VisicutModel.getInstance().firePartUpdated(e.getKey());
          }
          return true;
        } 
      }
    }
    ));
  }
  
  private void loadFileReal(File file, boolean discardCurrent)
  {
    try
    {
      this.progressBar.setIndeterminate(true);
      LinkedList<String> warnings = new LinkedList<String>();
      this.visicutModel1.loadFile(MappingManager.getInstance(), file, warnings, discardCurrent);
      if (!warnings.isEmpty())
        {
          dialog.showWarningMessage(warnings);
        }
      //if the image is too big, fit it a nd notify the user
      this.fitObjectsIntoBed();
      this.progressBar.setIndeterminate(false);
    }
    catch (Exception e)
    {
      this.progressBar.setIndeterminate(false);
      dialog.showErrorMessage(e, bundle.getString("ERROR WHILE OPENING '") + file.getName() + "'");
    }
  }

  /**
   * Sets all Buttons to their correct state (disabled/enabled)
   */
  private void refreshButtonStates()
  {
    boolean cam = this.visicutModel1.getSelectedLaserDevice() != null && this.visicutModel1.getSelectedLaserDevice().getCameraURL() != null;
    this.calibrateCameraMenuItem.setEnabled(cam);
    this.captureImageButton.setVisible(cam);
    this.jmShowPhoto.setEnabled(cam);
    boolean estimateSupported = this.visicutModel1.getSelectedLaserDevice() != null && this.visicutModel1.getSelectedLaserDevice().getLaserCutter().canEstimateJobDuration();
    this.calculateTimeButton.setVisible(estimateSupported);
    this.timeLabel.setVisible(estimateSupported);
    this.jLabel10.setVisible(estimateSupported);
    //check for focus-property in at least one profile type
    boolean focusSupported = false;
    if (this.visicutModel1.getSelectedLaserDevice() != null)
    {
      LaserCutter lc = this.visicutModel1.getSelectedLaserDevice().getLaserCutter();
      for (LaserProperty p : new LaserProperty[]{
        lc.getLaserPropertyForVectorPart(),
        lc.getLaserPropertyForRasterPart(),
        lc.getLaserPropertyForRaster3dPart()})
      {
        if (p != null && Arrays.asList(p.getPropertyKeys()).contains("focus"))
        {
          focusSupported = true;
          break;
        }
      }
    }
    if (!focusSupported || (MaterialManager.getInstance().getAll().size() == 1 && MaterialManager.getInstance().getAll().get(0).getMaterialThicknesses().size() == 1))
    {
      this.jCheckBox1.setSelected(false);
      this.jCheckBox1.setVisible(false);
      this.jSeparator1.setVisible(this.laserCutterComboBox.isVisible());
    }
    else
    {
      this.jCheckBox1.setVisible(true);
      this.jSeparator1.setVisible(true);
    }
    
    boolean execute = this.visicutModel1.getMaterial() != null
      && this.visicutModel1.getSelectedLaserDevice() != null
      && this.visicutModel1.getPlfFile().size() > 0;
    if (execute)
    {
      boolean jobEmpty = true;
      for(PlfPart p:this.visicutModel1.getPlfFile())
      {
        if (p.getMapping() != null && p.getMapping().size() > 0)
        {
          jobEmpty = false;
          break;
        }
      }
      if (jobEmpty)
      {
        execute = false;
      }
    }
    this.calculateTimeButton.setEnabled(execute);
    this.executeJobButton.setEnabled(execute);
    this.executeJobMenuItem.setEnabled(execute);
  }
  private File lastDirectory = null;

  private void openFileDialog(boolean discardCurrent)
  {
    final FileFilter allFilter = VisicutModel.getInstance().getAllFileFilter();
    //On Mac os, awt.FileDialog looks more native
    if (Helper.isMacOS())
    {
      FileDialog openFileChooser = new FileDialog(this, bundle.getString("PLEASE SELECT A FILE"));
      openFileChooser.setMode(FileDialog.LOAD);
      if (lastDirectory != null)
      {
        openFileChooser.setDirectory(lastDirectory.getAbsolutePath());
      }
      openFileChooser.setFilenameFilter(new FilenameFilter()
      {
        public boolean accept(File dir, String file)
        {
          return allFilter.accept(new File(dir, file));
        }
      });
      openFileChooser.setVisible(true);
      if (openFileChooser.getFile() != null)
      {
        File file = new File(new File(openFileChooser.getDirectory()), openFileChooser.getFile());
        loadFile(file, discardCurrent);
      }
    }
    else
    {
      JFileChooser openFileChooser = new JFileChooser();
      openFileChooser.setAcceptAllFileFilterUsed(false);
      for (FileFilter f : this.visicutModel1.getGraphicFileImporter().getFileFilters())
      {
        openFileChooser.addChoosableFileFilter(f);
      }
      openFileChooser.addChoosableFileFilter(allFilter);
      openFileChooser.setFileFilter(allFilter);
      openFileChooser.setCurrentDirectory(lastDirectory);
      int returnVal = openFileChooser.showOpenDialog(this);
      if (returnVal == JFileChooser.APPROVE_OPTION)
      {
        File file = openFileChooser.getSelectedFile();
        loadFile(file, discardCurrent);
      }
    }
  }

private void openMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openMenuItemActionPerformed
  this.openFileDialog(true);
}//GEN-LAST:event_openMenuItemActionPerformed

  private void editMappings() throws FileNotFoundException, IOException
  {
    List<MappingSet> mappingsets = new LinkedList<MappingSet>();
    for (MappingSet m : MappingManager.getInstance().getAll())
    {
      mappingsets.add(m.clone());
    }
    EditMappingsDialog d = new EditMappingsDialog(this, true);
    d.setMappingSets(mappingsets);
    d.setVisible(true);
    mappingsets = d.getMappingSets();
    if (mappingsets != null)
    {
      MappingManager.getInstance().setAll(mappingsets);
      this.fillComboBoxes();
      this.previewPanel.repaint();
    }
  }



private void aboutMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aboutMenuItemActionPerformed
  VisicutAboutBox box = new VisicutAboutBox(this);
  box.setModal(true);
  box.setVisible(true);
}//GEN-LAST:event_aboutMenuItemActionPerformed
  private int jobnumber = 0;

  private void executeJob()
  {
    try
    {
      final Map<LaserProfile, List<LaserProperty>> cuttingSettings = this.getPropertyMapForCurrentJob();
      if (cuttingSettings == null)
      {
        return;
      }
      if (VisicutModel.getInstance().getStartPoint() != null)
      {
        if (!dialog.showYesNoQuestion(bundle.getString("STARTPOINTWARNING")))
        {
          return;
        }
      }
      new Thread()
      {
        @Override
        public void run()
        {
          ProgressListener pl = new ProgressListener()
          {
            public void progressChanged(Object o, int i)
            {
              MainView.this.progressBar.setValue(i);
              MainView.this.progressBar.repaint();
            }
            public void taskChanged(Object o, String string)
            {
              MainView.this.progressBar.setString(string);
            }
          };
          MainView.this.progressBar.setMinimum(0);
          MainView.this.progressBar.setMaximum(100);
          MainView.this.progressBar.setValue(1);
          MainView.this.progressBar.setStringPainted(true);
          MainView.this.executeJobButton.setEnabled(false);
          MainView.this.executeJobMenuItem.setEnabled(false);
          try
          {
            jobnumber++;
            String prefix = MainView.this.visicutModel1.getSelectedLaserDevice().getJobPrefix();
            List<String> warnings = new LinkedList<String>();
            MainView.this.visicutModel1.sendJob(prefix+jobnumber, pl, cuttingSettings, warnings);
            for (String w : warnings)
            {
              dialog.showWarningMessage(w);
            }
            MainView.this.progressBar.setValue(0);
            MainView.this.progressBar.setString("");
            MainView.this.progressBar.setStringPainted(false);
            String txt = MainView.this.visicutModel1.getSelectedLaserDevice().getJobSentText();
            txt = txt.replace("$jobname", prefix + jobnumber).replace("$name", MainView.this.visicutModel1.getSelectedLaserDevice().getName());
            dialog.showSuccessMessage(txt);
          }
          catch (Exception ex)
          {
            if (ex instanceof IllegalJobException && ex.getMessage().startsWith("Illegal Focus value"))
            {
              dialog.showWarningMessage(bundle.getString("YOU MATERIAL IS TOO HIGH FOR AUTOMATIC FOCUSSING.PLEASE FOCUS MANUALLY AND SET THE TOTAL HEIGHT TO 0."));
            }
            else
            {
              dialog.showErrorMessage(ex);
            }
          }
          MainView.this.progressBar.setString("");
          MainView.this.progressBar.setValue(0);
          MainView.this.executeJobButton.setEnabled(true);
          MainView.this.executeJobMenuItem.setEnabled(true);
        }
      }.start();
    }
    catch (Exception ex)
    {
      dialog.showErrorMessage(ex);
    }
  }

private void executeJobButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_executeJobButtonActionPerformed
    this.executeJob();
}//GEN-LAST:event_executeJobButtonActionPerformed

private void filesDropSupport1PropertyChange(java.beans.PropertyChangeEvent evt)//GEN-FIRST:event_filesDropSupport1PropertyChange
{//GEN-HEADEREND:event_filesDropSupport1PropertyChange
  if (this.filesDropSupport1.getDroppedFiles() != null && this.filesDropSupport1.getDroppedFiles().size() > 0)
  {
    for (File f : this.filesDropSupport1.getDroppedFiles())
    {
      this.loadFile(f, false);
    }
  }
}//GEN-LAST:event_filesDropSupport1PropertyChange

  private void save()
  {
    File file = null;
    //On Mac os, awt.FileDialog looks more native
    if (Helper.isMacOS())
    {
      FileDialog fdialog = new java.awt.FileDialog(this);
      fdialog.setMode(FileDialog.SAVE);
      if (lastDirectory != null)
      {
        fdialog.setDirectory(lastDirectory.getAbsolutePath());
      }
      fdialog.setVisible(true);
      if (fdialog.getFile() != null)
      {
        file = new File(new File(fdialog.getDirectory()), fdialog.getFile());
      }
    }
    else
    {
      saveFileChooser.setCurrentDirectory(lastDirectory);
      if (saveFileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION)
      {
        file = saveFileChooser.getSelectedFile();
      }
    }
    if (file != null)
    {
      lastDirectory = file.getParentFile();
      if (!file.getName().endsWith("plf"))
      {
        file = new File(file.getAbsolutePath() + ".plf");
      }
      try
      {
        this.visicutModel1.saveToFile(MaterialManager.getInstance(), MappingManager.getInstance(), file);
      }
      catch (Exception ex)
      {
        dialog.showErrorMessage(ex, bundle.getString("ERROR SAVING FILE"));
      }
    }
    else
    {
      //File access cancelled by user.
    }
  }

private void saveAsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveAsMenuItemActionPerformed
  this.save();
}//GEN-LAST:event_saveAsMenuItemActionPerformed

private void visicutModel1PropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_visicutModel1PropertyChange
  if (evt.getPropertyName().equals(VisicutModel.PROP_PLF_PART_ADDED)
    ||evt.getPropertyName().equals(VisicutModel.PROP_PLF_PART_REMOVED)
    ||evt.getPropertyName().equals(VisicutModel.PROP_SELECTEDPART)) {
    // regenerate list of parts, update selection in ComboBox
    this.refreshObjectComboBox();
  }
    
  if (evt.getPropertyName().equals(VisicutModel.PROP_PLF_FILE_CHANGED))
  {
    MainView.this.timeLabel.setText("");
    if (this.visicutModel1.getPlfFile().getFile() != null)
    {
      this.setTitle("VisiCut - " + this.visicutModel1.getPlfFile().getFile().getName());
    }
    else
    {
      this.setTitle("VisiCut - Unnamed PLF");
    }
    this.refreshButtonStates();
  }
  else if (evt.getPropertyName().equals(VisicutModel.PROP_SELECTEDLASERDEVICE)
    ||evt.getPropertyName().equals(VisicutModel.PROP_PLF_PART_UPDATED)
    ||evt.getPropertyName().equals(VisicutModel.PROP_PLF_PART_REMOVED))
  {
    MainView.this.timeLabel.setText("");
    this.refreshButtonStates();
  }
  else if (evt.getPropertyName().equals(VisicutModel.PROP_SELECTEDPART))
  {
    PlfPart p = this.visicutModel1.getSelectedPart();
    this.reloadMenuItem.setEnabled(p != null);
    this.mappingTabbedPane.setVisible(p != null);
  }
  else if (evt.getPropertyName().equals(VisicutModel.PROP_MATERIAL))
  {
    MainView.this.timeLabel.setText("");
    this.refreshMaterialThicknessesComboBox();
    this.refreshButtonStates();
  }
}//GEN-LAST:event_visicutModel1PropertyChange

private void saveMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveMenuItemActionPerformed
  if (this.visicutModel1.getPlfFile().getFile() == null || !this.visicutModel1.getPlfFile().getFile().exists())
  {//File is not PLF or no file loaded yet
    this.saveAsMenuItemActionPerformed(evt);
  }
  else
  {
    try
    {
      this.visicutModel1.saveToFile(MaterialManager.getInstance(), MappingManager.getInstance(), this.visicutModel1.getPlfFile().getFile());
    }
    catch (Exception ex)
    {
      dialog.showErrorMessage(ex, bundle.getString("ERROR SAVING FILE"));
    }
  }
}//GEN-LAST:event_saveMenuItemActionPerformed

private void newMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_newMenuItemActionPerformed
  this.previewPanel.setZoom(100d);
  this.visicutModel1.newPlfFile();
}//GEN-LAST:event_newMenuItemActionPerformed

private void calibrateCameraMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_calibrateCameraMenuItemActionPerformed
  if (this.visicutModel1.getBackgroundImage() == null)
  {
    dialog.showErrorMessage(bundle.getString("THE CAMERA DOESN'T SEEM TO BE WORKING. PLEASE CHECK THE URL IN THE LASERCUTTER SETTINGS"));
    return;
  }
  List<VectorProfile> profiles = ProfileManager.getInstance().getVectorProfiles();
  if (profiles.isEmpty())
  {
    dialog.showErrorMessage(bundle.getString("NEED_VECTOR_PROFILE"));
    return;
  }
  VectorProfile p = dialog.askElement(profiles, bundle.getString("SELECT_VECTOR_PROFILE"));
  if (p == null)
  {
    return;
  }
  //TODO ask user for VectorProfile and make sure the properties for current
  //material and cutter are available
  CamCalibrationDialog ccd = new CamCalibrationDialog(this, true);
  ccd.setVectorProfile(p);
  ccd.setBackgroundImage(this.visicutModel1.getBackgroundImage());
  ccd.setImageURL(this.visicutModel1.getSelectedLaserDevice().getCameraURL());
  ccd.setResultingTransformation(this.visicutModel1.getSelectedLaserDevice().getCameraCalibration());
  ccd.setVisible(true);
  this.visicutModel1.getSelectedLaserDevice().setCameraCalibration(ccd.getResultingTransformation());
  try
  {
    LaserDeviceManager.getInstance().save(this.visicutModel1.getSelectedLaserDevice());
  }
  catch (Exception ex)
  {
    dialog.showErrorMessage(ex, bundle.getString("ERROR WHILE SAVING SETTINGS"));
  }
  this.previewPanel.repaint();
}//GEN-LAST:event_calibrateCameraMenuItemActionPerformed

private void executeJobMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_executeJobMenuItemActionPerformed
  this.executeJob();
}//GEN-LAST:event_executeJobMenuItemActionPerformed
  private boolean capturing = false;

  private void captureImage()
  {
    if (!capturing)
    {
      capturing = true;
      new Thread()
      {

        @Override
        public void run()
        {
          MainView.this.captureImageButton.setEnabled(false);
          MainView.this.progressBar.setStringPainted(true);
          MainView.this.progressBar.setString(bundle.getString("CAPTURING PHOTO..."));
          MainView.this.progressBar.setIndeterminate(true);
          MainView.this.progressBar.repaint();
          try
          {
            URL src = new URL(MainView.this.visicutModel1.getSelectedLaserDevice().getCameraURL());
            BufferedImage back = ImageIO.read(src);
            if (back != null && MainView.this.visicutModel1.getBackgroundImage() == null)
            {//First Time Image is Captured => resize View
              MainView.this.previewPanel.setZoom(100d);
            }
            MainView.this.visicutModel1.setBackgroundImage(back);
            MainView.this.jmShowPhoto.setSelected(true);
            MainView.this.progressBar.setString("");
            MainView.this.progressBar.setStringPainted(false);
            MainView.this.progressBar.setIndeterminate(false);
            MainView.this.progressBar.repaint();
          }
          catch (Exception ex)
          {
            MainView.this.progressBar.setString("");
            MainView.this.dialog.showErrorMessage(ex, bundle.getString("ERROR CAPTURING PHOTO"));
            MainView.this.progressBar.setIndeterminate(false);
            MainView.this.progressBar.repaint();
          }
          MainView.this.captureImageButton.setEnabled(true);
          MainView.this.capturing = false;
        }
      }.start();
    }
  }

  @Action
  public void zoomIn()
  {
    previewPanel.setZoom(previewPanel.getZoom() - (-2 * previewPanel.getZoom() / 32));
  }

  @Action
  public void zoomOut()
  {
    previewPanel.setZoom(previewPanel.getZoom() - (2 * previewPanel.getZoom() / 32));
  }

private void captureImageButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_captureImageButtonActionPerformed
  captureImage();
}//GEN-LAST:event_captureImageButtonActionPerformed

private void editMappingMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_editMappingMenuItemActionPerformed
    try
    {
      this.editMappings();
    }
    catch (Exception ex)
    {
      dialog.showErrorMessage(ex);
    }
}//GEN-LAST:event_editMappingMenuItemActionPerformed

private boolean ignoreMaterialComboBoxChanges = false;
private void materialComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_materialComboBoxActionPerformed
  if (ignoreMaterialComboBoxChanges)
  {
    return;
  }
  MaterialProfile newMaterial = this.materialComboBox.getSelectedItem() instanceof MaterialProfile ? (MaterialProfile) this.materialComboBox.getSelectedItem() : null;
  if (!Util.differ(newMaterial, visicutModel1.getMaterial()))
  {
    return;
  }
  this.visicutModel1.setMaterial(newMaterial);
}//GEN-LAST:event_materialComboBoxActionPerformed

  private void materialMenuItemActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_materialMenuItemActionPerformed
  {//GEN-HEADEREND:event_materialMenuItemActionPerformed
    EditMaterialsDialog d = new EditMaterialsDialog(this, true);
    d.setMaterials(MaterialManager.getInstance().getAll());
    d.setVisible(true);
    List<MaterialProfile> result = d.getMaterials();
    if (result != null)
    {
      try
      {
        MaterialManager.getInstance().setAll(result);
        this.refreshMaterialComboBox();
        this.visicutModel1.setMaterial(this.materialComboBox.getSelectedItem() instanceof MaterialProfile ? (MaterialProfile) this.materialComboBox.getSelectedItem() : null);
      }
      catch (Exception ex)
      {
        dialog.showErrorMessage(ex);
      }
    }
  }//GEN-LAST:event_materialMenuItemActionPerformed

  private void laserCutterComboBoxActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_laserCutterComboBoxActionPerformed
  {//GEN-HEADEREND:event_laserCutterComboBoxActionPerformed
    if (!ignoreLaserCutterComboBoxUpdates)
    {
      LaserDevice newDev = laserCutterComboBox.getSelectedItem() instanceof LaserDevice ? (LaserDevice) laserCutterComboBox.getSelectedItem() : null;
      if (!Util.differ(newDev, visicutModel1.getSelectedLaserDevice()))
      {
        return;
      }
      this.visicutModel1.setSelectedLaserDevice(newDev);
      if (this.visicutModel1.getSelectedLaserDevice() == null || this.visicutModel1.getSelectedLaserDevice().getCameraURL() == null || "".equals(this.visicutModel1.getSelectedLaserDevice().getCameraURL()))
      {
        this.visicutModel1.setBackgroundImage(null);
      }
      else
      {
        this.captureImage();
      }
      this.refreshButtonStates();
      //if the image is too big, fit it and notify the user
      this.fitObjectsIntoBed();
    }
  }//GEN-LAST:event_laserCutterComboBoxActionPerformed

  private void jMenuItem2ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jMenuItem2ActionPerformed
  {//GEN-HEADEREND:event_jMenuItem2ActionPerformed
    ManageLasercuttersDialog d = new ManageLasercuttersDialog(this, true);
    d.setLaserCutters(LaserDeviceManager.getInstance().getAll());
    d.setVisible(true);
    List<LaserDevice> result = d.getLaserCutters();
    if (result != null)
    {
      try
      {
        LaserDevice old = VisicutModel.getInstance().getSelectedLaserDevice();
        LaserDeviceManager.getInstance().setAll(result);
        if (old != null)
        {
          boolean found = false;
          VisicutModel.getInstance().setSelectedLaserDevice(null);
          for (LaserDevice ld : result)
          {
            if (ld.getName().equals(old.getName()))
            {
              found = true;
              VisicutModel.getInstance().setSelectedLaserDevice(ld);
              break;
            }
          }
          if (!found && result.size() > 0)
          {//if the current selected lasercutter was deleted, select the first
            VisicutModel.getInstance().setSelectedLaserDevice(result.get(0));
          }
        }
      }
      catch (Exception ex)
      {
        dialog.showErrorMessage(ex, bundle.getString("ERROR SAVING PREFERENCES"));
      }
      this.fillComboBoxes();
      this.refreshButtonStates();
    }
  }//GEN-LAST:event_jMenuItem2ActionPerformed

  private void calculateTimeButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_calculateTimeButtonActionPerformed
  {//GEN-HEADEREND:event_calculateTimeButtonActionPerformed
    new Thread()
    {

      @Override
      public void run()
      {
        try
        {
          MainView.this.calculateTimeButton.setEnabled(false);
          MainView.this.timeLabel.setText("...");
          MainView.this.timeLabel.setText(Helper.toHHMMSS(MainView.this.visicutModel1.estimateTime(MainView.this.getPropertyMapForCurrentJob())));
          MainView.this.calculateTimeButton.setEnabled(true);
        }
        catch (Exception ex)
        {
          dialog.showErrorMessage(ex);
          MainView.this.timeLabel.setText("error");
          MainView.this.calculateTimeButton.setEnabled(true);
        }
      }
    }.start();
  }//GEN-LAST:event_calculateTimeButtonActionPerformed

  private void jButton1ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButton1ActionPerformed
  {//GEN-HEADEREND:event_jButton1ActionPerformed
    previewPanel.setZoom(previewPanel.getZoom() + (previewPanel.getZoom() / 32));
  }//GEN-LAST:event_jButton1ActionPerformed

  private void jButton2ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButton2ActionPerformed
  {//GEN-HEADEREND:event_jButton2ActionPerformed
    previewPanel.setZoom(previewPanel.getZoom() - (previewPanel.getZoom() / 32));
  }//GEN-LAST:event_jButton2ActionPerformed

private void reloadMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_reloadMenuItemActionPerformed
  if (this.visicutModel1.getSelectedPart() != null)
  {
    try
    {
      LinkedList<String> warnings = new LinkedList<String>();
      this.visicutModel1.reloadSelectedPart(warnings);
      for(String s : warnings)
      {
        dialog.showWarningMessage(s);
      }
    }
    catch (Exception ex)
    {
      dialog.showErrorMessage(ex, bundle.getString("ERROR RELOADING FILE"));
    }
  }
}//GEN-LAST:event_reloadMenuItemActionPerformed

  private void jmExportSettingsActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jmExportSettingsActionPerformed
  {//GEN-HEADEREND:event_jmExportSettingsActionPerformed
    File file = null;
    //On Mac os, awt.FileDialog looks more native
    if (Helper.isMacOS())
    {
      FileDialog fileDialog = new java.awt.FileDialog(this);
      fileDialog.setMode(FileDialog.SAVE);
      if (lastDirectory != null)
      {
        fileDialog.setDirectory(lastDirectory.getAbsolutePath());
      }
      fileDialog.setVisible(true);
      if (fileDialog.getFile() != null)
      {
        file = new File(new File(fileDialog.getDirectory()), fileDialog.getFile());
      }
    }
    else
    {
      JFileChooser chooser = new JFileChooser();
      chooser.setDialogType(JFileChooser.SAVE_DIALOG);
      chooser.setCurrentDirectory(lastDirectory);
      if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION)
      {
        file = chooser.getSelectedFile();
      }
    }
    if (file != null)
    {
      if (!file.getName().toLowerCase().endsWith("zip"))
      {
        file = new File(file.getParentFile(), file.getName()+".zip");
      }
      try
      {
        PreferencesManager.getInstance().exportSettings(file);
        dialog.showSuccessMessage(bundle.getString("SETTINGS SUCCESSFULLY EXPORTED"));
      }
      catch (Exception ex)
      {
        dialog.showErrorMessage(ex);
      }
    }
  }//GEN-LAST:event_jmExportSettingsActionPerformed

  private void jmImportSettingsActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jmImportSettingsActionPerformed
  {//GEN-HEADEREND:event_jmImportSettingsActionPerformed
    switch (JOptionPane.showConfirmDialog(this, bundle.getString("THIS WILL OVERWRITE ALL YOUR SETTINGS INCLUDING LASERCUTTERS AND MATERIALS DO YOU WANT TO BACKUP YOUR SETTINGS BEFORE?"), bundle.getString("WARNING"), JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE))
    {
      case JOptionPane.YES_OPTION:
      {
        this.jmExportSettingsActionPerformed(null);
        //no break statement, because we want to import after export
      }
      case JOptionPane.NO_OPTION:
      {
        try
        {
          final FileFilter zipFilter = new ExtensionFilter("zip", bundle.getString("ZIPPED SETTINGS (*.ZIP)"));
          File file = null;
          //On Mac os, awt.FileDialog looks more native
          if (Helper.isMacOS())
          {
            FileDialog openFileChooser = new FileDialog(this, bundle.getString("PLEASE SELECT A FILE"));
            openFileChooser.setMode(FileDialog.LOAD);
            if (lastDirectory != null)
            {
              openFileChooser.setDirectory(lastDirectory.getAbsolutePath());
            }
            openFileChooser.setFilenameFilter(new FilenameFilter()
            {

              public boolean accept(File dir, String file)
              {
                return zipFilter.accept(new File(dir, file));
              }
            });
            openFileChooser.setVisible(true);
            if (openFileChooser.getFile() != null)
            {
              file = new File(new File(openFileChooser.getDirectory()), openFileChooser.getFile());
            }
          }
          else
          {
            JFileChooser openFileChooser = new JFileChooser();
            openFileChooser.setAcceptAllFileFilterUsed(false);
            openFileChooser.addChoosableFileFilter(zipFilter);
            openFileChooser.setFileFilter(zipFilter);
            openFileChooser.setCurrentDirectory(lastDirectory);
            int returnVal = openFileChooser.showOpenDialog(this);
            if (returnVal == JFileChooser.APPROVE_OPTION)
            {
              file = openFileChooser.getSelectedFile();
            }
          }
          if (file != null)
          {
            PreferencesManager.getInstance().importSettings(file);
            this.visicutModel1.setPreferences(PreferencesManager.getInstance().getPreferences());
            this.fillComboBoxes();
            this.refreshExampleMenu();
            dialog.showSuccessMessage(bundle.getString("SETTINGS SUCCESSFULLY IMPORTED"));
          }
        }
        catch (Exception e)
        {
          dialog.showErrorMessage(e);
        }
      }
    }
  }//GEN-LAST:event_jmImportSettingsActionPerformed

  private void jmManageLaserprofilesActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jmManageLaserprofilesActionPerformed
  {//GEN-HEADEREND:event_jmManageLaserprofilesActionPerformed
    EditProfilesDialog d = new EditProfilesDialog(this, true);
    List<LaserProfile> profiles = new LinkedList<LaserProfile>();
    profiles.addAll(ProfileManager.getInstance().getAll());
    d.setProfiles(profiles);
    d.setVisible(true);
    List<LaserProfile> result = d.getProfiles();
    if (result != null)
    {
      try
      {
        ProfileManager.getInstance().setAll(result);
      }
      catch (Exception ex)
      {
        dialog.showErrorMessage(ex);
      }
      this.fillComboBoxes();
    }
  }//GEN-LAST:event_jmManageLaserprofilesActionPerformed

  private void btAddMaterialActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btAddMaterialActionPerformed
  {//GEN-HEADEREND:event_btAddMaterialActionPerformed
    CreateNewMaterialDialog cd = new CreateNewMaterialDialog(this, true);
    cd.setVisible(true);
    if (!cd.isOkClicked())
    {//user pressed cancel
      return;
    }
    MaterialProfile m;
    MaterialProfile om = cd.getSelectedProfile();
    if (om != null)
    {//we're using an existing material profile
      m = om.clone();
      //create only the selected thicknesses
      m.setMaterialThicknesses(cd.getSelectedThicknesses());
      m.setName(m.getName()+" 2");
    }
    else
    {//create from scratch
      m = new MaterialProfile();
    }
    EditMaterialDialog d = new EditMaterialDialog(this, true);
    d.setMaterial(m);
    d.setVisible(true);
    m = d.getMaterial();
    if (m != null)
    {
      try
      {
        MaterialManager.getInstance().add(m);
        if (om != null)
        {//we were cloning, so copy all lasercutter settings for the selected thicknesses
          for (Float f : cd.getSelectedThicknesses())
          {
            if (m.getMaterialThicknesses().contains(f))
            {
              for (LaserDevice ld : LaserDeviceManager.getInstance().getAll())
              {
                for (LaserProfile lp : ProfileManager.getInstance().getAll())
                {
                  List<LaserProperty> props = LaserPropertyManager.getInstance().getLaserProperties(ld, om, lp, f);
                  if (props != null)
                  {
                    List<LaserProperty> clones = new LinkedList<LaserProperty>();
                    for (LaserProperty p : props)
                    {
                      clones.add(p.clone());
                    }
                    LaserPropertyManager.getInstance().saveLaserProperties(ld, m, lp, f, clones);
                  }
                }
              }
            }
          }
        }
        this.refreshMaterialComboBox();
        this.materialComboBox.setSelectedItem(m);
      }
      catch (Exception ex)
      {
        dialog.showErrorMessage(ex);
      }
    }
  }//GEN-LAST:event_btAddMaterialActionPerformed

  private void jmInstallInkscapeActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jmInstallInkscapeActionPerformed
  {//GEN-HEADEREND:event_jmInstallInkscapeActionPerformed
    try
    {
      Helper.installInkscapeExtension();
      dialog.showSuccessMessage(bundle.getString("INSTALLED EXTENSION SUCCESSFULLY"));
    }
    catch (Exception e)
    {
      dialog.showErrorMessage(e, bundle.getString("THERE WAS AN ERROR DURING THE INSTALLATION"));
    }
  }//GEN-LAST:event_jmInstallInkscapeActionPerformed

  private void jmInstallIllustratorActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jmInstallIllustratorActionPerformed
  {//GEN-HEADEREND:event_jmInstallIllustratorActionPerformed
    try
    {
      Helper.installIllustratorScript();
      dialog.showSuccessMessage(bundle.getString("INSTALLED EXTENSION SUCCESSFULLY"));
    }
    catch (Exception e)
    {
      dialog.showErrorMessage(e, bundle.getString("THERE WAS AN ERROR DURING THE INSTALLATION"));
    }
  }//GEN-LAST:event_jmInstallIllustratorActionPerformed
private void btAddMaterialThicknessActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btAddMaterialThicknessActionPerformed
  MaterialProfile m = VisicutModel.getInstance().getMaterial();
  if (m != null)
  {
    CreateNewThicknessDialog d = new CreateNewThicknessDialog(this, true);
    d.setAvailableThicknesses(m.getMaterialThicknesses());
    d.setVisible(true);
    if (!d.isOkClicked())
    {
      return;
    }
    Float f = d.getSelectedNewThickness();
    List<Float> th = m.getMaterialThicknesses();
    if (th.contains(f))
    {
      return;
    }
    th.add(f);
    Collections.sort(th);
    try
    {
      MaterialManager.getInstance().save(m);
    }
    catch (Exception ex)
    {
      dialog.showErrorMessage(ex, "Could not save material thickness");
    }
    Float copyThickness = d.getSelectedCopyThickness();
    if (copyThickness != null)
    {
      for (LaserDevice ld : LaserDeviceManager.getInstance().getAll())
      {
        for (LaserProfile lp : ProfileManager.getInstance().getAll())
        {
          try
          {
            List<LaserProperty> props = LaserPropertyManager.getInstance().getLaserProperties(ld, m, lp, f);
            if (props != null)
            {
              List<LaserProperty> clones = new LinkedList<LaserProperty>();
              for (LaserProperty p : props)
              {
                clones.add(p.clone());
              }
              LaserPropertyManager.getInstance().saveLaserProperties(ld, m, lp, f, clones);
            }
          }
          catch (Exception ex)
          {
            dialog.showErrorMessage(ex, "Could not copy material thickness");
          }
        }
      }
    }
    this.refreshMaterialThicknessesComboBox();
    this.cbMaterialThickness.setSelectedItem(f);
  }
}//GEN-LAST:event_btAddMaterialThicknessActionPerformed

private void cbMaterialThicknessActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cbMaterialThicknessActionPerformed
  Float f = (Float) this.cbMaterialThickness.getSelectedItem();
  if (f != null)
  {
    this.visicutModel1.setMaterialThickness(f);
  }
}//GEN-LAST:event_cbMaterialThicknessActionPerformed

  private void btFitScreenActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btFitScreenActionPerformed
  {//GEN-HEADEREND:event_btFitScreenActionPerformed
    this.previewPanel.setZoom(100d);
  }//GEN-LAST:event_btFitScreenActionPerformed

  private void bt1to1ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_bt1to1ActionPerformed
  {//GEN-HEADEREND:event_bt1to1ActionPerformed
    this.previewPanel.setOneToOneZoom();
  }//GEN-LAST:event_bt1to1ActionPerformed

private void importMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importMenuItemActionPerformed
  this.openFileDialog(false);
}//GEN-LAST:event_importMenuItemActionPerformed

private void btRemoveObjectActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btRemoveObjectActionPerformed
  VisicutModel.getInstance().removeSelectedPart();
}//GEN-LAST:event_btRemoveObjectActionPerformed

private void btAddObjectActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btAddObjectActionPerformed
  importMenuItemActionPerformed(evt);
}//GEN-LAST:event_btAddObjectActionPerformed

private boolean ignoreObjectComboBoxEvents = false;
private void objectComboBoxChangeHandler(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_objectComboBoxChangeHandler
  if (ignoreObjectComboBoxEvents || !(this.objectComboBox.getSelectedItem() instanceof PlfPart)) {
    // the user selected the "please select something" item - ignore this
    return;
  }
  PlfPart selected = (PlfPart) this.objectComboBox.getSelectedItem();
  if (!VisicutModel.getInstance().getPlfFile().contains(selected)) {
    // not available - can this happen? maybe if a strange timing occurs while loading a file and changing the combobox
    return;
  }
  if (evt.getStateChange()!=java.awt.event.ItemEvent.SELECTED) {
    return;
  }
  VisicutModel.getInstance().setSelectedPart(selected);
}//GEN-LAST:event_objectComboBoxChangeHandler

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem aboutMenuItem;
    private javax.swing.JButton bt1to1;
    private javax.swing.JButton btAddMaterial;
    private javax.swing.JButton btAddMaterialThickness;
    private javax.swing.JButton btAddObject;
    private javax.swing.JButton btFitScreen;
    private javax.swing.JButton btRemoveObject;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JButton calculateTimeButton;
    private javax.swing.JMenuItem calibrateCameraMenuItem;
    private javax.swing.JButton captureImageButton;
    private javax.swing.JComboBox cbMaterialThickness;
    private javax.swing.JMenuItem editMappingMenuItem;
    private javax.swing.JMenu editMenu;
    private javax.swing.JButton executeJobButton;
    private javax.swing.JMenuItem executeJobMenuItem;
    private javax.swing.JMenuItem exitMenuItem;
    private javax.swing.JMenu fileMenu;
    private com.t_oster.uicomponents.FilesDropSupport filesDropSupport1;
    private javax.swing.JMenu helpMenu;
    private javax.swing.JMenuItem importMenuItem;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JCheckBox jCheckBox1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenuItem jMenuItem2;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JMenu jmExamples;
    private javax.swing.JMenuItem jmExportSettings;
    private javax.swing.JMenu jmExtras;
    private javax.swing.JMenuItem jmImportSettings;
    private javax.swing.JMenuItem jmInstallIllustrator;
    private javax.swing.JMenuItem jmInstallInkscape;
    private javax.swing.JMenuItem jmManageLaserprofiles;
    private javax.swing.JCheckBoxMenuItem jmShowPhoto;
    private com.t_oster.uicomponents.ImageComboBox laserCutterComboBox;
    private com.t_oster.visicut.gui.mapping.MappingPanel mappingPanel;
    private javax.swing.JTabbedPane mappingTabbedPane;
    private com.t_oster.uicomponents.ImageComboBox materialComboBox;
    private javax.swing.JMenuItem materialMenuItem;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JMenuItem newMenuItem;
    private javax.swing.JComboBox objectComboBox;
    private javax.swing.JMenuItem openMenuItem;
    private com.t_oster.uicomponents.PositionPanel positionPanel;
    private com.t_oster.visicut.gui.beans.PreviewPanel previewPanel;
    private javax.swing.JProgressBar progressBar;
    private com.t_oster.visicut.gui.propertypanel.PropertiesPanel propertiesPanel;
    private javax.swing.JScrollPane propertyPanelContainer;
    private javax.swing.JMenu recentFilesMenu;
    private javax.swing.JMenuItem reloadMenuItem;
    private javax.swing.JMenuItem saveAsMenuItem;
    private javax.swing.JFileChooser saveFileChooser;
    private javax.swing.JMenuItem saveMenuItem;
    private javax.swing.JCheckBoxMenuItem showGridMenuItem;
    private javax.swing.JLabel timeLabel;
    private javax.swing.JMenu viewMenu;
    private com.t_oster.visicut.VisicutModel visicutModel1;
    private com.t_oster.uicomponents.warnings.WarningPanel warningPanel;
    private javax.swing.JMenuItem zoomInMenuItem;
    private javax.swing.JMenuItem zoomOutMenuItem;
    private org.jdesktop.beansbinding.BindingGroup bindingGroup;
    // End of variables declaration//GEN-END:variables

  private void refreshMaterialThicknessesComboBox()
  {
    if (VisicutModel.getInstance().getMaterial() != null)
    {
      Float current = (Float) this.cbMaterialThickness.getSelectedItem();
      this.cbMaterialThickness.removeAllItems();
      for (float f : this.visicutModel1.getMaterial().getMaterialThicknesses())
      {
        this.cbMaterialThickness.addItem((Float) f);
        if (((Float) f).equals(current))
        {
          this.cbMaterialThickness.setSelectedItem((Float) f);
        }
      }
    }
  }

  /**
   * get a list of used LaserProfiles and their corresponding LaserProperty
   *
   * @param reallyExecuting true if the laserjob is about to be sent, false if we are only calculating the time
   * @param mayShowEditDialog true if this function may open a LaserProperty edit dialog for unknown profiles, false if not.
   * @return
   */
  private Map<LaserProfile, List<LaserProperty>> getPropertyMapForCurrentJob()
  {
    Map<LaserProfile, List<LaserProperty>> result = this.propertiesPanel.getPropertyMap();
    for (LaserProfile lp : result.keySet())
    {
      if (lp == null)//ignore-profile
      {
        continue;
      }
      if (!this.visicutModel1.getSelectedLaserDevice().getLaserCutter().getResolutions().contains((Double) lp.getDPI()))
      {
        double dist = -1;
        double res = 0;
        double soll = lp.getDPI();
        for(double r : this.visicutModel1.getSelectedLaserDevice().getLaserCutter().getResolutions())
        {
          if (dist == -1 || dist > Math.abs(soll-r))
          {
            dist = Math.abs(soll-r);
            res = r;
          }
        }
        if (!dialog.showYesNoQuestion(bundle.getString("THE LASERCUTTER YOU SELECTED, DOES NOT SUPPORT ")+soll+bundle.getString("DPI DO YOU WANT TO USE ")+res+bundle.getString("DPI INSTEAD?")))
        {
          return null;
        }
        //changing the DPI changes the hash-code, so we have to
        //remove and re-assign the profile to the map
        List<LaserProperty> val = result.get(lp);
        result.remove(lp);
        lp.setDPI(res);
        result.put(lp, val);
      }
    }
    return result;
  }

}
