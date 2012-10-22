/**
 * This file is part of VisiCut.
 * Copyright (C) 2012 Thomas Oster <thomas.oster@rwth-aachen.de>
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
 * MainView.java
 *
 * Created on 18.08.2011, 01:42:04
 */
package com.t_oster.visicut.gui;

import com.apple.eawt.AppEvent.AboutEvent;
import com.apple.eawt.AppEvent.OpenFilesEvent;
import com.apple.eawt.AppEvent.QuitEvent;
import com.apple.eawt.QuitResponse;
import com.t_oster.liblasercut.IllegalJobException;
import com.t_oster.liblasercut.LaserProperty;
import com.t_oster.liblasercut.ProgressListener;
import com.t_oster.liblasercut.platform.Util;
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
import com.t_oster.visicut.misc.MultiFilter;
import com.t_oster.visicut.model.LaserDevice;
import com.t_oster.visicut.model.LaserProfile;
import com.t_oster.visicut.model.MaterialProfile;
import com.t_oster.visicut.model.Raster3dProfile;
import com.t_oster.visicut.model.RasterProfile;
import com.t_oster.visicut.model.VectorProfile;
import com.t_oster.visicut.model.graphicelements.GraphicSet;
import com.t_oster.visicut.model.mapping.Mapping;
import com.t_oster.visicut.model.mapping.MappingSet;
import java.awt.FileDialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import org.jdesktop.application.Action;

/**
 *
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class MainView extends javax.swing.JFrame
{

  private boolean initComplete = false;
  final protected DialogHelper dialog = new DialogHelper(this, this.getTitle());
  
  public MainView(File loadedFile)
  {
    this();
    this.loadFileReal(loadedFile);
  }
  
  /** Creates new form MainView */
  public MainView()
  {
    initComponents();
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

    this.customMappingPanel2.getSaveButton().addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent ae)
      {
        String name = JOptionPane.showInputDialog(MainView.this, java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/resources/MainView").getString("NAME_FOR_MAPPING"));
        if (name != null)
        {
          MappingSet ms = MainView.this.customMappingPanel2.getResultingMappingSet().clone();
          ms.setName(name);
          try
          {
            MappingManager.getInstance().add(ms);
            MainView.this.refreshPredefinedMappingList();
          }
          catch (Exception ex)
          {
            MainView.this.dialog.showErrorMessage(ex);
          }
        }
      }
    });
    
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
          PreferencesManager.getInstance().getPreferences().setEditSettingsBeforeExecuting(MainView.this.cbEditBeforeExecute.isSelected());
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
          MainView.this.loadFile(ofe.getFiles().get(0));
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
        PreferencesManager.getInstance().getPreferences().setEditSettingsBeforeExecuting(MainView.this.cbEditBeforeExecute.isSelected());
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
    this.cbEditBeforeExecute.setSelected(PreferencesManager.getInstance().getPreferences().isEditSettingsBeforeExecuting());
    initComplete = true;
    //initialize states coorectly
    this.visicutModel1PropertyChange(new java.beans.PropertyChangeEvent(visicutModel1, VisicutModel.PROP_LOADEDFILE, null, null));
    this.visicutModel1PropertyChange(new java.beans.PropertyChangeEvent(visicutModel1, VisicutModel.PROP_SELECTEDLASERDEVICE, null, null));
    this.visicutModel1PropertyChange(new java.beans.PropertyChangeEvent(visicutModel1, VisicutModel.PROP_SOURCEFILE, null, null));
      }

  private ActionListener exampleItemClicked = new ActionListener(){
      public void actionPerformed(ActionEvent ae)
      {
        if (!"".equals(ae.getActionCommand()))
        {
          MainView.this.loadFile(PreferencesManager.getInstance().getExampleFile(ae.getActionCommand()));
        }
      }
    };
  
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
            loadFile(f);
          }
        });
        this.recentFilesMenu.add(i);
      }
    }
  }
  
  private void refreshMaterialComboBox()
  {
    String sp = this.visicutModel1.getMaterial() != null ? this.visicutModel1.getMaterial().getName() : null;
    this.materialComboBox.removeAllItems();
    this.materialComboBox.addItem(null);
    this.materialComboBox.setSelectedIndex(0);
    for (MaterialProfile mp : MaterialManager.getInstance().getAll())
    {
      this.materialComboBox.addItem(mp);
      if (mp.getName().equals(sp))
      {
        this.materialComboBox.setSelectedItem(mp);
      }
    }
  }
  
  /*
   * Initially fills LaserCutter,Material and Mapping ComboBox with all possible Elements
   */
  private void fillComboBoxes()
  {
    String sld = this.visicutModel1.getSelectedLaserDevice() != null ? this.visicutModel1.getSelectedLaserDevice().getName() : null;
    this.laserCutterComboBox.removeAllItems();
    this.laserCutterComboBox.addItem(null);
    this.laserCutterComboBox.setSelectedIndex(0);
    for (LaserDevice ld : LaserDeviceManager.getInstance().getAll())
    {
      this.laserCutterComboBox.addItem(ld);
      if (ld.getName().equals(sld))
      {
        this.laserCutterComboBox.setSelectedItem(ld);
      }
    }
    //hide lasercutter combo box if only one lasercutter available
    if (this.laserCutterComboBox.getItemCount() == 2)
    {
      this.laserCutterComboBox.setSelectedIndex(1);
      this.laserCutterComboBox.setVisible(false);
      this.jLabel9.setVisible(false);
    }
    else
    {
      this.laserCutterComboBox.setVisible(true);
      this.jLabel9.setVisible(true);
    }
    this.refreshMaterialComboBox();
    this.refreshPredefinedMappingList();
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
        profileManager1 = com.t_oster.visicut.managers.MaterialManager.getInstance();
        filesDropSupport1 = new com.t_oster.visicut.gui.beans.FilesDropSupport();
        mappingManager1 = MappingManager.getInstance();
        saveFileChooser = new javax.swing.JFileChooser();
        jPanel2 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        materialComboBox = new com.t_oster.visicut.gui.beans.ImageComboBox();
        jLabel9 = new javax.swing.JLabel();
        laserCutterComboBox = new com.t_oster.visicut.gui.beans.ImageComboBox();
        jLabel10 = new javax.swing.JLabel();
        calculateTimeButton = new javax.swing.JButton();
        timeLabel = new javax.swing.JLabel();
        mappingTabbedPane = new javax.swing.JTabbedPane();
        predefinedMappingPanel = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        predefinedMappingList = new com.t_oster.visicut.gui.beans.ImageListableList();
        customMappingPanel1 = new javax.swing.JPanel();
        customMappingPanel2 = new com.t_oster.visicut.gui.beans.CustomMappingPanel();
        btAddMaterial = new javax.swing.JButton();
        cbMaterialThickness = new javax.swing.JComboBox();
        btAddMaterialThickness = new javax.swing.JButton();
        jCheckBox1 = new javax.swing.JCheckBox();
        cbEditBeforeExecute = new javax.swing.JCheckBox();
        executeJobButton = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        previewPanel = new com.t_oster.visicut.gui.beans.PreviewPanel();
        captureImageButton = new javax.swing.JButton();
        progressBar = new javax.swing.JProgressBar();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        menuBar = new javax.swing.JMenuBar();
        fileMenu = new javax.swing.JMenu();
        newMenuItem = new javax.swing.JMenuItem();
        openMenuItem = new javax.swing.JMenuItem();
        recentFilesMenu = new javax.swing.JMenu();
        jmExamples = new javax.swing.JMenu();
        reloadMenuItem = new javax.swing.JMenuItem();
        saveMenuItem = new javax.swing.JMenuItem();
        saveAsMenuItem = new javax.swing.JMenuItem();
        exitMenuItem = new javax.swing.JMenuItem();
        editMenu = new javax.swing.JMenu();
        calibrateCameraMenuItem = new javax.swing.JMenuItem();
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
        setLocationByPlatform(true);
        setName("Form"); // NOI18N

        org.jdesktop.beansbinding.Binding binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ, visicutModel1, org.jdesktop.beansbinding.ELProperty.create("${loadedFile} - VisiCut"), this, org.jdesktop.beansbinding.BeanProperty.create("title"), "Filename to Title");
        binding.setSourceNullValue("VisiCut");
        bindingGroup.addBinding(binding);

        jPanel2.setName("jPanel2"); // NOI18N

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(com.t_oster.visicut.gui.VisicutApp.class).getContext().getResourceMap(MainView.class);
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
        mappingTabbedPane.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                mappingTabbedPaneStateChanged(evt);
            }
        });

        predefinedMappingPanel.setName("predefinedMappingPanel"); // NOI18N

        jScrollPane1.setName("jScrollPane1"); // NOI18N

        predefinedMappingList.setName("predefinedMappingList"); // NOI18N
        predefinedMappingList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                predefinedMappingListValueChanged(evt);
            }
        });
        jScrollPane1.setViewportView(predefinedMappingList);

        javax.swing.GroupLayout predefinedMappingPanelLayout = new javax.swing.GroupLayout(predefinedMappingPanel);
        predefinedMappingPanel.setLayout(predefinedMappingPanelLayout);
        predefinedMappingPanelLayout.setHorizontalGroup(
            predefinedMappingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 350, Short.MAX_VALUE)
        );
        predefinedMappingPanelLayout.setVerticalGroup(
            predefinedMappingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 261, Short.MAX_VALUE)
        );

        mappingTabbedPane.addTab(resourceMap.getString("predefinedMappingPanel.TabConstraints.tabTitle"), predefinedMappingPanel); // NOI18N

        customMappingPanel1.setName("customMappingPanelContainer"); // NOI18N

        customMappingPanel2.setName("customMappingPanel2"); // NOI18N

        javax.swing.GroupLayout customMappingPanel1Layout = new javax.swing.GroupLayout(customMappingPanel1);
        customMappingPanel1.setLayout(customMappingPanel1Layout);
        customMappingPanel1Layout.setHorizontalGroup(
            customMappingPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(customMappingPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, 350, Short.MAX_VALUE)
        );
        customMappingPanel1Layout.setVerticalGroup(
            customMappingPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(customMappingPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, 261, Short.MAX_VALUE)
        );

        mappingTabbedPane.addTab(resourceMap.getString("customMappingPanelContainer.TabConstraints.tabTitle"), customMappingPanel1); // NOI18N

        btAddMaterial.setText(resourceMap.getString("btAddMaterial.text")); // NOI18N
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

        btAddMaterialThickness.setText(resourceMap.getString("btAddMaterialThickness.text")); // NOI18N
        btAddMaterialThickness.setName("btAddMaterialThickness"); // NOI18N
        btAddMaterialThickness.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btAddMaterialThicknessActionPerformed(evt);
            }
        });

        jCheckBox1.setText(resourceMap.getString("jCheckBox1.text")); // NOI18N
        jCheckBox1.setToolTipText(resourceMap.getString("jCheckBox1.toolTipText")); // NOI18N
        jCheckBox1.setName("jCheckBox1"); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, visicutModel1, org.jdesktop.beansbinding.ELProperty.create("${useThicknessAsFocusOffset}"), jCheckBox1, org.jdesktop.beansbinding.BeanProperty.create("selected"), "cbUseThickness");
        bindingGroup.addBinding(binding);

        cbEditBeforeExecute.setText(resourceMap.getString("cbEditBeforeExecute.text")); // NOI18N
        cbEditBeforeExecute.setName("cbEditBeforeExecute"); // NOI18N

        executeJobButton.setText(resourceMap.getString("executeJobButton.text")); // NOI18N
        executeJobButton.setName("executeJobButton"); // NOI18N
        executeJobButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                executeJobButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(mappingTabbedPane, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 362, Short.MAX_VALUE)
                    .addComponent(laserCutterComboBox, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 362, Short.MAX_VALUE)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(materialComboBox, javax.swing.GroupLayout.DEFAULT_SIZE, 334, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btAddMaterial))
                    .addComponent(jCheckBox1, javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel9, javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel1, javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel2Layout.createSequentialGroup()
                        .addComponent(cbMaterialThickness, javax.swing.GroupLayout.PREFERRED_SIZE, 134, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btAddMaterialThickness))
                    .addComponent(jLabel5, javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel2, javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(cbEditBeforeExecute)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 78, Short.MAX_VALUE)
                        .addComponent(executeJobButton))
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel10)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(timeLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 166, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(calculateTimeButton)))
                .addContainerGap())
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
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(materialComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btAddMaterial))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel5)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cbMaterialThickness, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btAddMaterialThickness))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCheckBox1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(mappingTabbedPane, javax.swing.GroupLayout.DEFAULT_SIZE, 308, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(calculateTimeButton)
                    .addComponent(timeLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel10))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(executeJobButton)
                    .addComponent(cbEditBeforeExecute))
                .addContainerGap())
        );

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(resourceMap.getString("jPanel1.border.title"))); // NOI18N
        jPanel1.setName("jPanel1"); // NOI18N

        previewPanel.setName("previewPanel"); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ, visicutModel1, org.jdesktop.beansbinding.ELProperty.create("${backgroundImage}"), previewPanel, org.jdesktop.beansbinding.BeanProperty.create("backgroundImage"), "BackImageFromModel");
        bindingGroup.addBinding(binding);
        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ, visicutModel1, org.jdesktop.beansbinding.ELProperty.create("${graphicObjects}"), previewPanel, org.jdesktop.beansbinding.BeanProperty.create("graphicObjects"), "ModelToPreviewObjects");
        bindingGroup.addBinding(binding);
        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ, visicutModel1, org.jdesktop.beansbinding.ELProperty.create("${mappings}"), previewPanel, org.jdesktop.beansbinding.BeanProperty.create("mappings"), "MappingsFromModelToPreviewPanel");
        bindingGroup.addBinding(binding);
        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, visicutModel1, org.jdesktop.beansbinding.ELProperty.create("${material}"), previewPanel, org.jdesktop.beansbinding.BeanProperty.create("material"));
        bindingGroup.addBinding(binding);

        com.t_oster.visicut.gui.PreviewPanelKeyboardMouseHandler ppMouseHandler = new com.t_oster.visicut.gui.PreviewPanelKeyboardMouseHandler(this.previewPanel);

        javax.swing.GroupLayout previewPanelLayout = new javax.swing.GroupLayout(previewPanel);
        previewPanel.setLayout(previewPanelLayout);
        previewPanelLayout.setHorizontalGroup(
            previewPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 525, Short.MAX_VALUE)
        );
        previewPanelLayout.setVerticalGroup(
            previewPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 585, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(previewPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(previewPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        captureImageButton.setIcon(resourceMap.getIcon("captureImageButton.icon")); // NOI18N
        captureImageButton.setText(resourceMap.getString("captureImageButton.text")); // NOI18N
        captureImageButton.setName("captureImageButton"); // NOI18N
        captureImageButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                captureImageButtonActionPerformed(evt);
            }
        });

        progressBar.setName("progressBar"); // NOI18N

        jButton1.setIcon(resourceMap.getIcon("jButton1.icon")); // NOI18N
        jButton1.setText(resourceMap.getString("jButton1.text")); // NOI18N
        jButton1.setName("jButton1"); // NOI18N
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jButton2.setIcon(resourceMap.getIcon("jButton2.icon")); // NOI18N
        jButton2.setText(resourceMap.getString("jButton2.text")); // NOI18N
        jButton2.setName("jButton2"); // NOI18N
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

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

        recentFilesMenu.setText(resourceMap.getString("recentFilesMenu.text")); // NOI18N
        recentFilesMenu.setName("recentFilesMenu"); // NOI18N
        fileMenu.add(recentFilesMenu);

        jmExamples.setText(resourceMap.getString("jmExamples.text")); // NOI18N
        jmExamples.setName("jmExamples"); // NOI18N
        fileMenu.add(jmExamples);

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
        fileMenu.add(reloadMenuItem);

        saveMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.CTRL_MASK));
        saveMenuItem.setMnemonic('s');
        saveMenuItem.setText(resourceMap.getString("saveMenuItem.text")); // NOI18N
        saveMenuItem.setEnabled(false);
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
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 57, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton2, javax.swing.GroupLayout.PREFERRED_SIZE, 54, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(captureImageButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 144, Short.MAX_VALUE)
                        .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                .addComponent(jButton1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jButton2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(captureImageButton)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );

        bindingGroup.bind();

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void exitMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exitMenuItemActionPerformed
      PreferencesManager.getInstance().getPreferences().setEditSettingsBeforeExecuting(MainView.this.cbEditBeforeExecute.isSelected());
      this.visicutModel1.updatePreferences();
      System.exit(0);
    }//GEN-LAST:event_exitMenuItemActionPerformed

  public void loadFile(File file)
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
        MainView.this.loadFileReal(fileToLoad);
      }
    }.start();
  }

  public void loadFileReal(File file)
  {
    try
    {
      this.progressBar.setIndeterminate(true);
      if (VisicutModel.PLFFilter.accept(file))
      {
        //Set Panel to Predefined Mappings because there the loaded mapping will appear    
        this.mappingTabbedPane.setSelectedComponent(this.predefinedMappingPanel);
        this.custom = null;
        this.visicutModel1.loadFromFile(this.mappingManager1, file);
        if (this.visicutModel1.getMappings() != null)
        {
          if (this.custom == null)
          {
            custom = this.visicutModel1.getMappings();
            custom.setName(java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/resources/MainView").getString("LOADED MAPPING"));
            this.predefinedMappingList.addItem(custom);
          }
          else
          {
            custom.clear();
            custom.addAll(this.visicutModel1.getMappings());
          }
          this.predefinedMappingList.setSelectedValue(custom, true);
          this.customMappingPanel2.representMapping(this.visicutModel1.getMappings());
        }
      }
      else
      {
        this.visicutModel1.loadGraphicFile(file);
      }
      this.previewPanel.setZoom(100);
      this.previewPanel.setEditRectangle(null);
      this.progressBar.setIndeterminate(false);
      this.refreshButtonStates();
    }
    catch (Exception e)
    {
      this.progressBar.setIndeterminate(false);
      dialog.showErrorMessage(e, java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/resources/MainView").getString("ERROR WHILE OPENING '") + file.getName() + "'");
    }
  }

  /**
   * Sets all Buttons to their correct state (disabled/enabled)
   */
  private void refreshButtonStates()
  {
    this.customMappingPanel1.setEnabled(
      this.visicutModel1.getMaterial() != null
      && this.visicutModel1.getGraphicObjects() != null
      && this.visicutModel1.getGraphicObjects().size() > 0);
    this.calculateTimeButton.setEnabled(this.visicutModel1.getMaterial() != null
      && this.visicutModel1.getSelectedLaserDevice() != null
      && this.visicutModel1.getMappings() != null
      && this.visicutModel1.getMappings().size() > 0);
    if (this.visicutModel1.getSelectedLaserDevice() == null || this.visicutModel1.getMaterial() == null
      || this.visicutModel1.getMappings() == null || this.cbMaterialThickness.getSelectedItem() == null)
    {
      this.executeJobButton.setEnabled(false);
      this.executeJobMenuItem.setEnabled(false);
    }
    else
    {
      this.executeJobButton.setEnabled(true);
      this.executeJobMenuItem.setEnabled(true);
    }
  }
  private File lastDirectory = null;
private void openMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openMenuItemActionPerformed
  final FileFilter allFilter =
    new MultiFilter(
    new FileFilter[]
    {
      this.visicutModel1.getGraphicFileImporter().getFileFilter(),
      VisicutModel.PLFFilter
    }, java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/resources/MainView").getString("ALL SUPPORTED FILES"));

  //On Mac os, awt.FileDialog looks more native
  if (Helper.isMacOS())
  {
    FileDialog openFileChooser = new FileDialog(this, java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/resources/MainView").getString("PLEASE SELECT A FILE"));
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
      loadFile(file);
    }
  }
  else
  {
    JFileChooser openFileChooser = new JFileChooser();
    openFileChooser.setAcceptAllFileFilterUsed(false);
    openFileChooser.addChoosableFileFilter(VisicutModel.PLFFilter);
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
      loadFile(file);
    }
  }
}//GEN-LAST:event_openMenuItemActionPerformed

  private void editMappings() throws FileNotFoundException, IOException
  {
    List<MappingSet> mappingsets = new LinkedList<MappingSet>();
    for (MappingSet m : this.mappingManager1.getAll())
    {
      mappingsets.add(m.clone());
    }
    EditMappingsDialog d = new EditMappingsDialog(this, true);
    d.setGraphicElements(this.visicutModel1.getGraphicObjects());
    d.setMappingSets(mappingsets);
    d.setMaterial(this.visicutModel1.getMaterial());
    d.setVisible(true);
    mappingsets = d.getMappingSets();
    if (mappingsets != null)
    {
      this.mappingManager1.setAll(mappingsets);
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
    for (Mapping m : this.visicutModel1.getMappings())
    {
      LaserProfile lp = m.getProfile();
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
        if (!dialog.showYesNoQuestion(java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/resources/MainView").getString("THE LASERCUTTER YOU SELECTED, DOES NOT SUPPORT ")+soll+java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/resources/MainView").getString("DPI DO YOU WANT TO USE ")+res+java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/resources/MainView").getString("DPI INSTEAD?")))
        {
          return;
        }
        lp.setDPI(res);
      }
    }
    try
    {
      LaserDevice device = this.visicutModel1.getSelectedLaserDevice();
      MaterialProfile material = this.visicutModel1.getMaterial();
      //get all profiles used in the job
      //and check if they're supported yet
      boolean unknownProfilesUsed = false;
      Map<LaserProfile, List<LaserProperty>> usedSettings = new LinkedHashMap<LaserProfile, List<LaserProperty>>();
      for (Mapping m:this.visicutModel1.getMappings())
      {
        LaserProfile profile = m.getProfile();
        List<LaserProperty> props = LaserPropertyManager.getInstance().getLaserProperties(device, material, profile, this.visicutModel1.getMaterialThickness());
        if (props == null)
        {
          unknownProfilesUsed = true;
          props = new LinkedList<LaserProperty>();
        }
        if (props.isEmpty())
        {//we have to add at least one sample for the dialog to know the kind of LaserPropery
          if (profile instanceof RasterProfile)
          {
            props.add(device.getLaserCutter().getLaserPropertyForRasterPart());
          }
          else if (profile instanceof VectorProfile)
          {
            props.add(device.getLaserCutter().getLaserPropertyForVectorPart());
          }
          else if (profile instanceof Raster3dProfile)
          {
            props.add(device.getLaserCutter().getLaserPropertyForRaster3dPart());
          }
        }
        usedSettings.put(profile, props);
      }

      if (this.cbEditBeforeExecute.isSelected() || unknownProfilesUsed)
      {
        if (unknownProfilesUsed)
        {
          dialog.showInfoMessage(java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/resources/MainView").getString("FOR SOME PROFILE YOU SELECTED, THERE ARE NO LASERCUTTER SETTINGS YET YOU WILL HAVE TO ENTER THEM IN THE FOLLOWING DIALOG."));
        }
        String heading = java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/resources/MainView").getString("SETTINGS FOR ")+device.getName()+java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/resources/MainView").getString(" WITH MATERIAL ")+material.toString()+" ("+this.visicutModel1.getMaterialThickness()+" mm)";
        //Adapt Settings before execute
        AdaptSettingsDialog asd = new AdaptSettingsDialog(this, true, heading);
        asd.setLaserProperties(usedSettings, this.visicutModel1.getSelectedLaserDevice().getLaserCutter());
        asd.setVisible(true);
        Map<LaserProfile, List<LaserProperty>> result = asd.getLaserProperties();
        if (result == null)
        {//user cancelled
          return;
        }
        //save changes
        for (Entry<LaserProfile, List<LaserProperty>> e:result.entrySet())
        {
          LaserPropertyManager.getInstance().saveLaserProperties(device, material, e.getKey(), this.visicutModel1.getMaterialThickness(), e.getValue());
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
            MainView.this.visicutModel1.sendJob(prefix+jobnumber, pl);
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
              dialog.showWarningMessage(java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/resources/MainView").getString("YOU MATERIAL IS TOO HIGH FOR AUTOMATIC FOCUSSING.PLEASE FOCUS MANUALLY AND SET THE TOTAL HEIGHT TO 0."));
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
      this.loadFile(f);
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
        this.visicutModel1.saveToFile(this.profileManager1, this.mappingManager1, file);
      }
      catch (Exception ex)
      {
        dialog.showErrorMessage(ex, java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/resources/MainView").getString("ERROR SAVING FILE"));
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
  if (evt.getPropertyName().equals(VisicutModel.PROP_LOADEDFILE))
  {
    this.saveMenuItem.setEnabled(this.visicutModel1.getLoadedFile() != null);
    if (this.visicutModel1.getLoadedFile() != null)
    {
      this.setTitle("VisiCut - " + this.visicutModel1.getLoadedFile().getName());
    }
  }
  else if (evt.getPropertyName().equals(VisicutModel.PROP_SELECTEDLASERDEVICE))
  {
    boolean cam = this.visicutModel1.getSelectedLaserDevice() != null && this.visicutModel1.getSelectedLaserDevice().getCameraURL() != null;
    this.calibrateCameraMenuItem.setEnabled(cam);
    this.captureImageButton.setEnabled(cam);
    this.jmShowPhoto.setEnabled(cam);
    if (cam)
    {
      this.captureImage();
    }
    boolean estimate = this.visicutModel1.getSelectedLaserDevice() != null && this.visicutModel1.getSelectedLaserDevice().getLaserCutter().canEstimateJobDuration();
    this.calculateTimeButton.setVisible(estimate);
    this.timeLabel.setVisible(estimate);
    this.jLabel10.setVisible(estimate);
  }
  else if (evt.getPropertyName().equals(VisicutModel.PROP_SOURCEFILE))
  {
    this.reloadMenuItem.setEnabled(this.visicutModel1.getSourceFile() != null);
    if (this.visicutModel1.getLoadedFile() == null)
    {
      this.setTitle(this.visicutModel1.getSourceFile() == null
        ? "VisiCut"
        : "VisiCut - " + this.visicutModel1.getSourceFile().getName());
    }
  }
  else if (evt.getPropertyName().equals(VisicutModel.PROP_MATERIAL))
  {
    this.refreshMaterialThicknessesComboBox();
  }
}//GEN-LAST:event_visicutModel1PropertyChange

private void saveMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveMenuItemActionPerformed
  if (this.visicutModel1.getLoadedFile() == null || !VisicutModel.PLFFilter.accept(this.visicutModel1.getLoadedFile()))
  {//File is not PLF or no file loaded yet
    this.saveAsMenuItemActionPerformed(evt);
    return;
  }
  try
  {
    this.visicutModel1.saveToFile(this.profileManager1, this.mappingManager1, this.visicutModel1.getLoadedFile());
  }
  catch (Exception ex)
  {
    dialog.showErrorMessage(ex, java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/resources/MainView").getString("ERROR SAVING FILE"));
  }
}//GEN-LAST:event_saveMenuItemActionPerformed

private void newMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_newMenuItemActionPerformed
  this.previewPanel.setEditRectangle(null);
  this.previewPanel.setZoom(100);
  this.visicutModel1.setGraphicObjects(new GraphicSet());
}//GEN-LAST:event_newMenuItemActionPerformed

private void calibrateCameraMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_calibrateCameraMenuItemActionPerformed
  if (this.visicutModel1.getBackgroundImage() == null)
  {
    dialog.showErrorMessage(java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/resources/MainView").getString("THE CAMERA DOESN'T SEEM TO BE WORKING. PLEASE CHECK THE URL IN THE LASERCUTTER SETTINGS"));
    return;
  }
  if (true)
  {
    dialog.showErrorMessage("Currently disabled");
    return;
  }
  //TODO ask user for VectorProfile and make sure the properties for current
  //material and cutter are available
  CamCalibrationDialog ccd = new CamCalibrationDialog();
  ccd.setBackgroundImage(this.visicutModel1.getBackgroundImage());
  ccd.setImageURL(this.visicutModel1.getSelectedLaserDevice().getCameraURL());
  //ccd.setVectorProfile(vp);
  ccd.setResultingTransformation(this.visicutModel1.getSelectedLaserDevice().getCameraCalibration());
  ccd.setVisible(true);
  this.visicutModel1.getSelectedLaserDevice().setCameraCalibration(ccd.getResultingTransformation());
  try
  {
    PreferencesManager.getInstance().savePreferences();
  }
  catch (Exception ex)
  {
    dialog.showErrorMessage(ex, java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/resources/MainView").getString("ERROR WHILE SAVING SETTINGS"));
  }
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
          MainView.this.progressBar.setString(java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/resources/MainView").getString("CAPTURING PHOTO..."));
          MainView.this.progressBar.setIndeterminate(true);
          MainView.this.progressBar.repaint();
          try
          {
            URL src = new URL(MainView.this.visicutModel1.getSelectedLaserDevice().getCameraURL());
            if (src != null)
            {
              BufferedImage back = ImageIO.read(src);
              if (back != null && MainView.this.visicutModel1.getBackgroundImage() == null)
              {//First Time Image is Captured => resize View
                MainView.this.previewPanel.setZoom(100);
              }
              MainView.this.visicutModel1.setBackgroundImage(back);
            }
            MainView.this.progressBar.setString("");
            MainView.this.progressBar.setStringPainted(false);
            MainView.this.progressBar.setIndeterminate(false);
            MainView.this.progressBar.repaint();
          }
          catch (Exception ex)
          {
            MainView.this.progressBar.setString(java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/resources/MainView").getString("ERROR CAPTURING PHOTO"));
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

private void materialComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_materialComboBoxActionPerformed
  //Check if Material supports all Mappings
  MaterialProfile newMaterial = this.materialComboBox.getSelectedItem() instanceof MaterialProfile ? (MaterialProfile) this.materialComboBox.getSelectedItem() : null;
  if (!Util.differ(newMaterial, visicutModel1.getMaterial()))
  {
    return;
  }
  if (this.materialComboBox.isDisabled(newMaterial))
  {
    this.materialComboBox.setSelectedItem(visicutModel1.getMaterial());
    return;
  }
  this.visicutModel1.setMaterial(newMaterial);
  this.refreshButtonStates();
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
      }
      catch (Exception ex)
      {
        dialog.showErrorMessage(ex);
      }
    }
  }//GEN-LAST:event_materialMenuItemActionPerformed

  private void laserCutterComboBoxActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_laserCutterComboBoxActionPerformed
  {//GEN-HEADEREND:event_laserCutterComboBoxActionPerformed
    LaserDevice newDev = laserCutterComboBox.getSelectedItem() instanceof LaserDevice ? (LaserDevice) laserCutterComboBox.getSelectedItem() : null;
    if (!Util.differ(newDev, visicutModel1.getSelectedLaserDevice()))
    {
      return;
    }
    if (this.laserCutterComboBox.isDisabled(newDev))
    {
      this.laserCutterComboBox.setSelectedItem(visicutModel1.getSelectedLaserDevice());
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
        LaserDeviceManager.getInstance().setAll(result);
      }
      catch (Exception ex)
      {
        dialog.showErrorMessage(ex, java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/resources/MainView").getString("ERROR SAVING PREFERENCES"));
      }
      this.fillComboBoxes();
    }
  }//GEN-LAST:event_jMenuItem2ActionPerformed
  private MappingSet custom = null;

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
          MainView.this.progressBar.setIndeterminate(true);
          MainView.this.timeLabel.setText(Helper.toHHMMSS(MainView.this.visicutModel1.estimateTime()));
          MainView.this.progressBar.setIndeterminate(false);
          MainView.this.calculateTimeButton.setEnabled(true);
        }
        catch (Exception ex)
        {
          dialog.showErrorMessage(ex);
        }
      }
    }.start();
  }//GEN-LAST:event_calculateTimeButtonActionPerformed

  private void predefinedMappingListValueChanged(javax.swing.event.ListSelectionEvent evt)//GEN-FIRST:event_predefinedMappingListValueChanged
  {//GEN-HEADEREND:event_predefinedMappingListValueChanged
    try
    {
      Object selected = this.predefinedMappingList.getSelectedValue();
      MappingSet ms = selected instanceof MappingSet ? (MappingSet) selected : null;
      this.setMappings(ms);
      this.refreshButtonStates();
    }
    catch (Exception ex)
    {
      dialog.showErrorMessage(ex);
    }
  }//GEN-LAST:event_predefinedMappingListValueChanged

  private void setMappings(MappingSet mappings) throws FileNotFoundException, IOException
  {
    this.visicutModel1.setMappings(mappings);
  }

  private void mappingTabbedPaneStateChanged(javax.swing.event.ChangeEvent evt)//GEN-FIRST:event_mappingTabbedPaneStateChanged
  {//GEN-HEADEREND:event_mappingTabbedPaneStateChanged
    if (!initComplete)
    {//ignore all Events during initialization
      return;
    }
    MappingSet mapping = null;
    if (this.mappingTabbedPane.getSelectedComponent().equals(this.predefinedMappingPanel))
    {
      Object sel = this.predefinedMappingList.getSelectedValue();
      if (sel instanceof MappingSet)
      {
        mapping = (MappingSet) sel;
      }
    }
    else
    {
      mapping = this.customMappingPanel2.getResultingMappingSet();
    }
    try
    {
      this.setMappings(mapping);
    }
    catch (Exception ex)
    {
      dialog.showErrorMessage(ex);
    }
    this.refreshButtonStates();
  }//GEN-LAST:event_mappingTabbedPaneStateChanged

  private void jButton1ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButton1ActionPerformed
  {//GEN-HEADEREND:event_jButton1ActionPerformed
    previewPanel.setZoom(previewPanel.getZoom() + (previewPanel.getZoom() / 32));
  }//GEN-LAST:event_jButton1ActionPerformed

  private void jButton2ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButton2ActionPerformed
  {//GEN-HEADEREND:event_jButton2ActionPerformed
    previewPanel.setZoom(previewPanel.getZoom() - (previewPanel.getZoom() / 32));
  }//GEN-LAST:event_jButton2ActionPerformed

private void reloadMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_reloadMenuItemActionPerformed
  if (this.visicutModel1.getLoadedFile() != null && this.visicutModel1.getLoadedFile().isFile())
  {
    try
    {
      this.visicutModel1.loadFromFile(this.mappingManager1, this.visicutModel1.getLoadedFile());
    }
    catch (Exception ex)
    {
      dialog.showErrorMessage(ex, java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/resources/MainView").getString("ERROR RELOADING FILE"));
    }
  }
  else if (this.visicutModel1.getSourceFile() != null && this.visicutModel1.getSourceFile().isFile())
  {
    try
    {
      this.visicutModel1.loadGraphicFile(this.visicutModel1.getSourceFile(), true);
    }
    catch (Exception e)
    {
      this.progressBar.setIndeterminate(false);
      dialog.showErrorMessage(e, java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/resources/MainView").getString("ERROR WHILE OPENING '") + this.visicutModel1.getSourceFile().getName() + "'");
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
        dialog.showSuccessMessage(java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/resources/MainView").getString("SETTINGS SUCCESSFULLY EXPORTED"));
      }
      catch (Exception ex)
      {
        dialog.showErrorMessage(ex);
      }
    }
  }//GEN-LAST:event_jmExportSettingsActionPerformed

  private void jmImportSettingsActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jmImportSettingsActionPerformed
  {//GEN-HEADEREND:event_jmImportSettingsActionPerformed
    switch (JOptionPane.showConfirmDialog(this, java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/resources/MainView").getString("THIS WILL OVERWRITE ALL YOUR SETTINGS INCLUDING LASERCUTTERS AND MATERIALS DO YOU WANT TO BACKUP YOUR SETTINGS BEFORE?"), java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/resources/MainView").getString("WARNING"), JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE))
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
          final FileFilter zipFilter = new ExtensionFilter("zip", java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/resources/MainView").getString("ZIPPED SETTINGS (*.ZIP)"));
          //On Mac os, awt.FileDialog looks more native
          if (Helper.isMacOS())
          {
            FileDialog openFileChooser = new FileDialog(this, java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/resources/MainView").getString("PLEASE SELECT A FILE"));
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
              File file = new File(new File(openFileChooser.getDirectory()), openFileChooser.getFile());
              PreferencesManager.getInstance().importSettings(file);
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
              File file = openFileChooser.getSelectedFile();
              PreferencesManager.getInstance().importSettings(file);
              this.visicutModel1.setPreferences(PreferencesManager.getInstance().getPreferences());
              this.cbEditBeforeExecute.setSelected(PreferencesManager.getInstance().getPreferences().isEditSettingsBeforeExecuting());
              this.fillComboBoxes();
              this.refreshExampleMenu();
              dialog.showSuccessMessage(java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/resources/MainView").getString("SETTINGS SUCCESSFULLY IMPORTED"));
            }
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
      dialog.showSuccessMessage(java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/resources/MainView").getString("INSTALLED EXTENSION SUCCESSFULLY"));
    }
    catch (Exception e)
    {
      dialog.showErrorMessage(e, java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/resources/MainView").getString("THERE WAS AN ERROR DURING THE INSTALLATION"));
    }
  }//GEN-LAST:event_jmInstallInkscapeActionPerformed

  private void jmInstallIllustratorActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jmInstallIllustratorActionPerformed
  {//GEN-HEADEREND:event_jmInstallIllustratorActionPerformed
    try
    {
      Helper.installIllustratorScript();
      dialog.showSuccessMessage(java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/resources/MainView").getString("INSTALLED EXTENSION SUCCESSFULLY"));
    }
    catch (Exception e)
    {
      dialog.showErrorMessage(e, java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/resources/MainView").getString("THERE WAS AN ERROR DURING THE INSTALLATION"));
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

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem aboutMenuItem;
    private javax.swing.JButton btAddMaterial;
    private javax.swing.JButton btAddMaterialThickness;
    private javax.swing.JButton calculateTimeButton;
    private javax.swing.JMenuItem calibrateCameraMenuItem;
    private javax.swing.JButton captureImageButton;
    private javax.swing.JCheckBox cbEditBeforeExecute;
    private javax.swing.JComboBox cbMaterialThickness;
    private javax.swing.JPanel customMappingPanel1;
    private com.t_oster.visicut.gui.beans.CustomMappingPanel customMappingPanel2;
    private javax.swing.JMenuItem editMappingMenuItem;
    private javax.swing.JMenu editMenu;
    private javax.swing.JButton executeJobButton;
    private javax.swing.JMenuItem executeJobMenuItem;
    private javax.swing.JMenuItem exitMenuItem;
    private javax.swing.JMenu fileMenu;
    private com.t_oster.visicut.gui.beans.FilesDropSupport filesDropSupport1;
    private javax.swing.JMenu helpMenu;
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
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JMenu jmExamples;
    private javax.swing.JMenuItem jmExportSettings;
    private javax.swing.JMenu jmExtras;
    private javax.swing.JMenuItem jmImportSettings;
    private javax.swing.JMenuItem jmInstallIllustrator;
    private javax.swing.JMenuItem jmInstallInkscape;
    private javax.swing.JMenuItem jmManageLaserprofiles;
    private javax.swing.JCheckBoxMenuItem jmShowPhoto;
    private com.t_oster.visicut.gui.beans.ImageComboBox laserCutterComboBox;
    private com.t_oster.visicut.managers.MappingManager mappingManager1;
    private javax.swing.JTabbedPane mappingTabbedPane;
    private com.t_oster.visicut.gui.beans.ImageComboBox materialComboBox;
    private javax.swing.JMenuItem materialMenuItem;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JMenuItem newMenuItem;
    private javax.swing.JMenuItem openMenuItem;
    private com.t_oster.visicut.gui.beans.ImageListableList predefinedMappingList;
    private javax.swing.JPanel predefinedMappingPanel;
    private com.t_oster.visicut.gui.beans.PreviewPanel previewPanel;
    private com.t_oster.visicut.managers.MaterialManager profileManager1;
    private javax.swing.JProgressBar progressBar;
    private javax.swing.JMenu recentFilesMenu;
    private javax.swing.JMenuItem reloadMenuItem;
    private javax.swing.JMenuItem saveAsMenuItem;
    private javax.swing.JFileChooser saveFileChooser;
    private javax.swing.JMenuItem saveMenuItem;
    private javax.swing.JCheckBoxMenuItem showGridMenuItem;
    private javax.swing.JLabel timeLabel;
    private javax.swing.JMenu viewMenu;
    private com.t_oster.visicut.VisicutModel visicutModel1;
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

  private void refreshPredefinedMappingList()
  {
    MappingSet ms = this.visicutModel1.getMappings();
    this.predefinedMappingList.clearList();
    this.predefinedMappingList.addItem(java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/resources/MainView").getString("NO MAPPING"));
    this.predefinedMappingList.setSelectedIndex(0);
    for (MappingSet m : this.visicutModel1.generateDefaultMappings())
    {
      this.predefinedMappingList.addItem(m);
      if (m.equals(ms))
      {
        this.predefinedMappingList.setSelectedValue(m, true);
      }
    }
    for (MappingSet m : this.mappingManager1.getAll())
    {
      this.predefinedMappingList.addItem(m);
      if (m.equals(ms))
      {
        this.predefinedMappingList.setSelectedValue(m, true);
      }
    }
  }
}
