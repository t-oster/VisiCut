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
 * MainView.java
 *
 * Created on 18.08.2011, 01:42:04
 */
package com.t_oster.visicut.gui;

import com.t_oster.liblasercut.IllegalJobException;
import com.t_oster.liblasercut.platform.Util;
import com.t_oster.visicut.misc.ExtensionFilter;
import com.t_oster.visicut.misc.Helper;
import com.t_oster.visicut.managers.PreferencesManager;
import com.t_oster.visicut.VisicutModel;
import com.t_oster.visicut.gui.beans.ImageComboBox;
import com.t_oster.visicut.misc.MultiFilter;
import com.t_oster.visicut.model.LaserDevice;
import com.t_oster.visicut.model.LaserProfile;
import com.t_oster.visicut.model.mapping.Mapping;
import com.t_oster.visicut.model.MaterialProfile;
import com.t_oster.visicut.model.graphicelements.GraphicObject;
import com.t_oster.visicut.model.graphicelements.GraphicSet;
import com.t_oster.visicut.model.mapping.MappingSet;
import java.awt.FileDialog;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.net.URL;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.filechooser.FileFilter;
import org.jdesktop.application.Action;

/**
 *
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class MainView extends javax.swing.JFrame
{

  /** Creates new form MainView */
  public MainView()
  {
    initComponents();
    if (Helper.isMacOS())
    {//Mac OS has its own exit menu
      fileMenu.remove(exitMenuItem);
    }
    this.customMappingTable.getSelectionModel().addListSelectionListener(new ListSelectionListener()
    {

      public void valueChanged(ListSelectionEvent lse)
      {
        if (customMappingTable.getSelectedRowCount() > 0)
        {
          MainView.this.materialComboBox.setEnabled(false);
          MainView.this.laserCutterComboBox.setEnabled(false);
          MainView.this.mappingTabbedPane.setEnabled(false);
          MainView.this.previewPanel.setMappings(MainView.this.customMappingTable.getSelectionMappingSet());
        }
        else
        {
          MainView.this.materialComboBox.setEnabled(true);
          MainView.this.laserCutterComboBox.setEnabled(true);
          MainView.this.mappingTabbedPane.setEnabled(true);
          MappingSet set = MainView.this.customMappingTable.getResultingMappingSet();
          MainView.this.setMappings(set);
          if (!MainView.this.previewPanel.getMappings().equals(set))
          {
            MainView.this.previewPanel.setMappings(set);
          }
          MainView.this.refreshButtonStates();
          MainView.this.refreshComboBoxes();
        }
      }
    });
    this.customMappingTable.getModel().addTableModelListener(new TableModelListener()
    {

      public void tableChanged(TableModelEvent tme)
      {

        if (customMappingTable.getSelectedRowCount() > 0)
        {
          MappingSet selMapSet = MainView.this.customMappingTable.getSelectionMappingSet();
          MainView.this.previewPanel.setMappings(selMapSet);
        }
      }
    });
    this.visicutModel1.setPreferences(PreferencesManager.getInstance().getPreferences());
    fillComboBoxes();
    refreshComboBoxes();

    if (this.visicutModel1.getSelectedLaserDevice() != null && this.visicutModel1.getSelectedLaserDevice().getCameraURL() != null)
    {
      this.captureImage();
    }


    String[] args = VisicutApp.getApplication().getProgramArguments();
    for (String s : args)
    {
      File f = new File(s);
      if (f.exists())
      {
        this.loadFile(f);
      }
    }
    //Window listener for capturing close and save preferences before exiting
    this.addWindowListener(new WindowListener()
    {

      public void windowOpened(WindowEvent e)
      {
      }

      public void windowClosing(WindowEvent e)
      {
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
  }

  /*
   * Initially fills LaserCutter,Material and Mapping ComboBox with all possible Elements
   */
  private void fillComboBoxes()
  {
    HashSet<Integer> resolutions = new LinkedHashSet<Integer>();

    LaserDevice sld = this.visicutModel1.getSelectedLaserDevice();
    this.laserCutterComboBox.removeAllItems();
    this.laserCutterComboBox.addItem(null);
    this.laserCutterComboBox.setSelectedIndex(0);
    for (LaserDevice ld : PreferencesManager.getInstance().getPreferences().getLaserDevices())
    {
      this.laserCutterComboBox.addItem(ld);
      resolutions.addAll(ld.getLaserCutter().getResolutions());
      if (ld.equals(sld))
      {
        this.laserCutterComboBox.setSelectedItem(ld);
      }
    }
    Integer res = this.visicutModel1.getResolution();
    this.resolutionComboBox.removeAllItems();
    this.resolutionComboBox.addItem(null);
    this.resolutionComboBox.setSelectedIndex(0);
    for (Integer i : resolutions)
    {
      this.resolutionComboBox.addItem(i);
      if (i.equals(res))
      {
        this.resolutionComboBox.setSelectedItem(i);
      }
    }
    MaterialProfile sp = this.visicutModel1.getMaterial();
    this.materialComboBox.removeAllItems();
    this.materialComboBox.addItem(null);
    this.materialComboBox.setSelectedIndex(0);
    for (MaterialProfile mp : getAllMaterials())
    {
      this.materialComboBox.addItem(mp);
      if (sp != null && sp.getName().equals(mp.getName()) && sp.getDepth() == mp.getDepth())
      {
        this.materialComboBox.setSelectedItem(mp);
      }
    }
    MappingSet ss = this.visicutModel1.getMappings();
    this.predefinedMappingList.clearList();
    this.predefinedMappingList.addItem("no mapping");
    this.predefinedMappingList.setSelectedIndex(0);
    for (MappingSet m : this.mappingManager1.getMappingSets())
    {
      this.predefinedMappingList.addItem(m);
      if (m.equals(ss))
      {
        this.predefinedMappingList.setSelectedValue(m, true);
      }
    }
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

        visicutModel1 = new com.t_oster.visicut.VisicutModel();
        profileManager1 = new com.t_oster.visicut.managers.ProfileManager();
        filesDropSupport1 = new com.t_oster.visicut.gui.beans.FilesDropSupport();
        mappingManager1 = new com.t_oster.visicut.managers.MappingManager();
        saveFileChooser = new javax.swing.JFileChooser();
        jPanel2 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        dimensionWidthTextField = new javax.swing.JTextField();
        dimesnionsHeightTextfield = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        materialHeightTextField = new javax.swing.JTextField();
        materialComboBox = new com.t_oster.visicut.gui.beans.ImageComboBox();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        laserCutterComboBox = new com.t_oster.visicut.gui.beans.ImageComboBox();
        jLabel10 = new javax.swing.JLabel();
        calculateTimeButton = new javax.swing.JButton();
        timeLabel = new javax.swing.JLabel();
        mappingTabbedPane = new javax.swing.JTabbedPane();
        predefinedMappingPanel = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        predefinedMappingList = new com.t_oster.visicut.gui.beans.ImageListableList();
        customMappingPanel = new javax.swing.JPanel();
        jLabel11 = new javax.swing.JLabel();
        customMappingComboBox = new javax.swing.JComboBox();
        jScrollPane2 = new javax.swing.JScrollPane();
        customMappingTable = new com.t_oster.visicut.gui.beans.CustomMappingTable();
        jLabel8 = new javax.swing.JLabel();
        resolutionComboBox = new com.t_oster.visicut.gui.beans.ImageComboBox();
        jLabel12 = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        previewPanel = new com.t_oster.visicut.gui.beans.PreviewPanel();
        executeJobButton = new javax.swing.JButton();
        captureImageButton = new javax.swing.JButton();
        progressBar = new javax.swing.JProgressBar();
        jPanel3 = new javax.swing.JPanel();
        showEngravingCb = new javax.swing.JCheckBox();
        showCuttingCb = new javax.swing.JCheckBox();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        menuBar = new javax.swing.JMenuBar();
        fileMenu = new javax.swing.JMenu();
        newMenuItem = new javax.swing.JMenuItem();
        openMenuItem = new javax.swing.JMenuItem();
        reloadMenuItem = new javax.swing.JMenuItem();
        saveMenuItem = new javax.swing.JMenuItem();
        saveAsMenuItem = new javax.swing.JMenuItem();
        exitMenuItem = new javax.swing.JMenuItem();
        editMenu = new javax.swing.JMenu();
        calibrateCameraMenuItem = new javax.swing.JMenuItem();
        executeJobMenuItem = new javax.swing.JMenuItem();
        jMenu1 = new javax.swing.JMenu();
        editMappingMenuItem = new javax.swing.JMenuItem();
        jMenuItem2 = new javax.swing.JMenuItem();
        materialMenuItem = new javax.swing.JMenuItem();
        viewMenu = new javax.swing.JMenu();
        showGridMenuItem = new javax.swing.JCheckBoxMenuItem();
        jMenuItem1 = new javax.swing.JMenuItem();
        jMenuItem3 = new javax.swing.JMenuItem();
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
        saveFileChooser.setFileFilter(new ExtensionFilter(".plf", "VisiCut Portable Laser File"));
        saveFileChooser.setName("saveFileChooser"); // NOI18N

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
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

        jLabel3.setText(resourceMap.getString("jLabel3.text")); // NOI18N
        jLabel3.setName("jLabel3"); // NOI18N

        dimensionWidthTextField.setName("dimensionWidthTextField"); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, visicutModel1, org.jdesktop.beansbinding.ELProperty.create("${material.width}"), dimensionWidthTextField, org.jdesktop.beansbinding.BeanProperty.create("text_ON_ACTION_OR_FOCUS_LOST"), "tfw"); // NOI18N
        bindingGroup.addBinding(binding);

        dimesnionsHeightTextfield.setName("dimesnionsHeightTextfield"); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, visicutModel1, org.jdesktop.beansbinding.ELProperty.create("${material.height}"), dimesnionsHeightTextfield, org.jdesktop.beansbinding.BeanProperty.create("text_ON_ACTION_OR_FOCUS_LOST"), "tfh");
        bindingGroup.addBinding(binding);

        jLabel4.setText(resourceMap.getString("jLabel4.text")); // NOI18N
        jLabel4.setName("jLabel4"); // NOI18N

        jLabel5.setText(resourceMap.getString("jLabel5.text")); // NOI18N
        jLabel5.setName("jLabel5"); // NOI18N

        materialHeightTextField.setToolTipText(resourceMap.getString("materialHeightTextField.toolTipText")); // NOI18N
        materialHeightTextField.setName("materialHeightTextField"); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, visicutModel1, org.jdesktop.beansbinding.ELProperty.create("${material.depth}"), materialHeightTextField, org.jdesktop.beansbinding.BeanProperty.create("text_ON_ACTION_OR_FOCUS_LOST"), "tfd");
        bindingGroup.addBinding(binding);

        materialComboBox.setName("materialComboBox"); // NOI18N
        materialComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                materialComboBoxActionPerformed(evt);
            }
        });

        jLabel6.setText(resourceMap.getString("jLabel6.text")); // NOI18N
        jLabel6.setName("jLabel6"); // NOI18N

        jLabel7.setText(resourceMap.getString("jLabel7.text")); // NOI18N
        jLabel7.setName("jLabel7"); // NOI18N

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

        mappingTabbedPane.setName("mappingTabbedPane"); // NOI18N
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
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 353, Short.MAX_VALUE)
        );
        predefinedMappingPanelLayout.setVerticalGroup(
            predefinedMappingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 336, Short.MAX_VALUE)
        );

        mappingTabbedPane.addTab(resourceMap.getString("predefinedMappingPanel.TabConstraints.tabTitle"), predefinedMappingPanel); // NOI18N

        customMappingPanel.setMaximumSize(new java.awt.Dimension(277, 382));
        customMappingPanel.setName("customMappingPanel"); // NOI18N

        jLabel11.setText(resourceMap.getString("jLabel11.text")); // NOI18N
        jLabel11.setName("jLabel11"); // NOI18N

        customMappingComboBox.setName("customMappingComboBox"); // NOI18N
        customMappingComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                customMappingComboBoxActionPerformed(evt);
            }
        });

        jScrollPane2.setName("jScrollPane2"); // NOI18N

        customMappingTable.setName("customMappingTable"); // NOI18N
        jScrollPane2.setViewportView(customMappingTable);

        javax.swing.GroupLayout customMappingPanelLayout = new javax.swing.GroupLayout(customMappingPanel);
        customMappingPanel.setLayout(customMappingPanelLayout);
        customMappingPanelLayout.setHorizontalGroup(
            customMappingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, customMappingPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel11)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(customMappingComboBox, 0, 290, Short.MAX_VALUE)
                .addContainerGap())
            .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 353, Short.MAX_VALUE)
        );
        customMappingPanelLayout.setVerticalGroup(
            customMappingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(customMappingPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(customMappingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel11)
                    .addComponent(customMappingComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 286, Short.MAX_VALUE)
                .addContainerGap())
        );

        mappingTabbedPane.addTab(resourceMap.getString("customMappingPanel.TabConstraints.tabTitle"), customMappingPanel); // NOI18N

        jLabel8.setText(resourceMap.getString("jLabel8.text")); // NOI18N
        jLabel8.setName("jLabel8"); // NOI18N

        resolutionComboBox.setName("resolutionComboBox"); // NOI18N
        resolutionComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                resolutionComboBoxActionPerformed(evt);
            }
        });

        jLabel12.setText(resourceMap.getString("jLabel12.text")); // NOI18N
        jLabel12.setName("jLabel12"); // NOI18N

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(mappingTabbedPane)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel9)
                            .addComponent(jLabel1)
                            .addComponent(jLabel2)
                            .addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, 358, Short.MAX_VALUE)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(dimensionWidthTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 132, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel4)
                                .addGap(18, 18, 18)
                                .addComponent(dimesnionsHeightTextfield, javax.swing.GroupLayout.PREFERRED_SIZE, 152, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jLabel6))
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(jPanel2Layout.createSequentialGroup()
                                        .addComponent(materialHeightTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 134, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jLabel7))
                                    .addComponent(jLabel5))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel8)
                                    .addGroup(jPanel2Layout.createSequentialGroup()
                                        .addComponent(resolutionComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 151, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addComponent(jLabel12)))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 20, Short.MAX_VALUE)))
                        .addContainerGap())
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel10)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(timeLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 193, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(calculateTimeButton))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(materialComboBox, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 348, Short.MAX_VALUE)
                            .addComponent(laserCutterComboBox, javax.swing.GroupLayout.DEFAULT_SIZE, 348, Short.MAX_VALUE))
                        .addContainerGap())))
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
                .addComponent(materialComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(jLabel8))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(materialHeightTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel7)
                    .addComponent(resolutionComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel12))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(dimensionWidthTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel4)
                    .addComponent(dimesnionsHeightTextfield, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel6))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(mappingTabbedPane, javax.swing.GroupLayout.DEFAULT_SIZE, 361, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(calculateTimeButton)
                    .addComponent(jLabel10)
                    .addComponent(timeLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(resourceMap.getString("jPanel1.border.title"))); // NOI18N
        jPanel1.setName("jPanel1"); // NOI18N

        previewPanel.setAutoCenter(true);
        previewPanel.setName("previewPanel"); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ, visicutModel1, org.jdesktop.beansbinding.ELProperty.create("${backgroundImage}"), previewPanel, org.jdesktop.beansbinding.BeanProperty.create("backgroundImage"), "BackImageFromModel");
        bindingGroup.addBinding(binding);
        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ, visicutModel1, org.jdesktop.beansbinding.ELProperty.create("${graphicObjects}"), previewPanel, org.jdesktop.beansbinding.BeanProperty.create("graphicObjects"), "ModelToPreviewObjects");
        bindingGroup.addBinding(binding);
        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ, visicutModel1, org.jdesktop.beansbinding.ELProperty.create("${mappings}"), previewPanel, org.jdesktop.beansbinding.BeanProperty.create("mappings"), "MappingsFromModelToPreviewPanel");
        bindingGroup.addBinding(binding);
        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, visicutModel1, org.jdesktop.beansbinding.ELProperty.create("${material}"), previewPanel, org.jdesktop.beansbinding.BeanProperty.create("material"));
        bindingGroup.addBinding(binding);
        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ, visicutModel1, org.jdesktop.beansbinding.ELProperty.create("${selectedLaserDevice.cameraCalibration}"), previewPanel, org.jdesktop.beansbinding.BeanProperty.create("previewTransformation"), "TransformFromModel");
        bindingGroup.addBinding(binding);
        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ, visicutModel1, org.jdesktop.beansbinding.ELProperty.create("${resolution}"), previewPanel, org.jdesktop.beansbinding.BeanProperty.create("resolution"), "ResolutionToPanel");
        bindingGroup.addBinding(binding);

        PreviewPanelMouseHandler ppMouseHandler = new PreviewPanelMouseHandler(this.previewPanel);
        previewPanel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                previewPanelMouseClicked(evt);
            }
        });

        javax.swing.GroupLayout previewPanelLayout = new javax.swing.GroupLayout(previewPanel);
        previewPanel.setLayout(previewPanelLayout);
        previewPanelLayout.setHorizontalGroup(
            previewPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 660, Short.MAX_VALUE)
        );
        previewPanelLayout.setVerticalGroup(
            previewPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 530, Short.MAX_VALUE)
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

        executeJobButton.setText(resourceMap.getString("executeJobButton.text")); // NOI18N
        executeJobButton.setName("executeJobButton"); // NOI18N
        executeJobButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                executeJobButtonActionPerformed(evt);
            }
        });

        captureImageButton.setIcon(resourceMap.getIcon("captureImageButton.icon")); // NOI18N
        captureImageButton.setText(resourceMap.getString("captureImageButton.text")); // NOI18N
        captureImageButton.setName("captureImageButton"); // NOI18N
        captureImageButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                captureImageButtonActionPerformed(evt);
            }
        });

        progressBar.setName("progressBar"); // NOI18N

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder(resourceMap.getString("jPanel3.border.title"))); // NOI18N
        jPanel3.setName("jPanel3"); // NOI18N

        showEngravingCb.setText(resourceMap.getString("showEngravingCb.text")); // NOI18N
        showEngravingCb.setName("showEngravingCb"); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, previewPanel, org.jdesktop.beansbinding.ELProperty.create("${drawPreview}"), showEngravingCb, org.jdesktop.beansbinding.BeanProperty.create("selected"), "engr");
        bindingGroup.addBinding(binding);

        showEngravingCb.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showEngravingCbActionPerformed(evt);
            }
        });

        showCuttingCb.setText(resourceMap.getString("showCuttingCb.text")); // NOI18N
        showCuttingCb.setName("showCuttingCb"); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, previewPanel, org.jdesktop.beansbinding.ELProperty.create("${highlightCutLines}"), showCuttingCb, org.jdesktop.beansbinding.BeanProperty.create("selected"), "cutl");
        bindingGroup.addBinding(binding);

        showCuttingCb.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showCuttingCbActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(showEngravingCb)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(showCuttingCb)
                .addContainerGap(45, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(showEngravingCb)
                    .addComponent(showCuttingCb))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

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

        editMappingMenuItem.setText(resourceMap.getString("editMappingMenuItem.text")); // NOI18N
        editMappingMenuItem.setName("editMappingMenuItem"); // NOI18N
        editMappingMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                editMappingMenuItemActionPerformed(evt);
            }
        });
        jMenu1.add(editMappingMenuItem);

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

        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(com.t_oster.visicut.gui.VisicutApp.class).getContext().getActionMap(MainView.class, this);
        jMenuItem1.setAction(actionMap.get("zoomIn")); // NOI18N
        jMenuItem1.setText(resourceMap.getString("jMenuItem1.text")); // NOI18N
        jMenuItem1.setName("jMenuItem1"); // NOI18N
        viewMenu.add(jMenuItem1);

        jMenuItem3.setAction(actionMap.get("zoomOut")); // NOI18N
        jMenuItem3.setText(resourceMap.getString("jMenuItem3.text")); // NOI18N
        jMenuItem3.setName("jMenuItem3"); // NOI18N
        viewMenu.add(jMenuItem3);

        menuBar.add(viewMenu);

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
                    .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 57, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton2, javax.swing.GroupLayout.PREFERRED_SIZE, 54, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(captureImageButton))
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(executeJobButton)
                    .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(executeJobButton))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jButton1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jButton2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(captureImageButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );

        bindingGroup.bind();

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void exitMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exitMenuItemActionPerformed
      this.visicutModel1.updatePreferences();
      System.exit(0);
    }//GEN-LAST:event_exitMenuItemActionPerformed

  public void loadFile(File file)
  {
    final File fileToLoad = file;
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
        this.visicutModel1.loadFromFile(this.mappingManager1, file);
        if (this.custom == null)
        {
          custom = this.visicutModel1.getMappings();
          custom.setName("Loaded Mapping");
          this.predefinedMappingList.addItem(custom);
        }
        else
        {
          custom.clear();
          custom.addAll(this.visicutModel1.getMappings());
        }
        this.predefinedMappingList.setSelectedValue(custom, true);
      }
      else
      {
        this.visicutModel1.loadGraphicFile(file);
      }
      this.customMappingTable.setObjects(this.visicutModel1.getGraphicObjects() == null ? new GraphicSet() : this.visicutModel1.getGraphicObjects());
      this.customMappingComboBox.removeAllItems();
      if (this.visicutModel1.getGraphicObjects() != null)
      {
        List<String> attributes = new LinkedList<String>();
        for (GraphicObject g : this.visicutModel1.getGraphicObjects())
        {
          for (String attribute : g.getAttributes())
          {
            if (!attributes.contains(attribute))
            {
              attributes.add(attribute);
              this.customMappingComboBox.addItem(attribute);
            }
          }
        }
      }
      this.previewPanel.setEditRectangle(null);
      this.progressBar.setIndeterminate(false);
      this.refreshButtonStates();
    }
    catch (Exception e)
    {
      this.progressBar.setIndeterminate(false);
      e.printStackTrace();
      JOptionPane.showMessageDialog(this, "Error while opening '" + file.getName() + "':\n" + e.getLocalizedMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
  }

  /**
   * Sets all Buttons to their correct state (disabled/enabled)
   */
  private void refreshButtonStates()
  {
    this.customMappingPanel.setEnabled(
      this.visicutModel1.getMaterial() != null
      && this.visicutModel1.getGraphicObjects() != null
      && this.visicutModel1.getGraphicObjects().size() > 0);
    this.calculateTimeButton.setEnabled(this.visicutModel1.getMaterial() != null
      && this.visicutModel1.getSelectedLaserDevice() != null
      && this.visicutModel1.getMappings() != null
      && this.visicutModel1.getMappings().size() > 0);
    boolean previewModes = this.visicutModel1.getMappings() != null && this.visicutModel1.getMaterial() != null && this.visicutModel1.getMappings().size() > 0;
    this.showCuttingCb.setEnabled(previewModes);
    this.showEngravingCb.setEnabled(previewModes);
    if (this.visicutModel1.getSelectedLaserDevice() == null || this.visicutModel1.getMaterial() == null
      || this.visicutModel1.getMappings() == null || this.visicutModel1.getResolution() == null)
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
    }, "All supported files");

  //On Mac os, awt.FileDialog looks more native
  if (Helper.isMacOS())
  {
    FileDialog openFileChooser = new FileDialog(this, "Please select a file");
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
      File file = new File(openFileChooser.getFile());
      lastDirectory = file.getParentFile();
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
      lastDirectory = file.getParentFile();
      loadFile(file);
    }
  }
}//GEN-LAST:event_openMenuItemActionPerformed

  private void editMappings()
  {
    List<MappingSet> mappingsets = new LinkedList<MappingSet>();
    for (MappingSet m : this.mappingManager1.getMappingSets())
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
      this.mappingManager1.setMappingSets(mappingsets);
      this.mappingManager1.saveAllMappings();
      this.fillComboBoxes();
      this.refreshComboBoxes();
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
    new Thread()
    {

      @Override
      public void run()
      {
        MainView.this.progressBar.setIndeterminate(true);
        MainView.this.executeJobButton.setEnabled(false);
        MainView.this.executeJobMenuItem.setEnabled(false);
        try
        {
          jobnumber++;
          MainView.this.visicutModel1.sendJob("VisiCut " + jobnumber);
          MainView.this.progressBar.setIndeterminate(false);
          JOptionPane.showMessageDialog(MainView.this, "Job was sent as 'VisiCut " + jobnumber + "'\n\n Please:\n- Close the lid\n- Switch the Ventilation on\n- and press START on the Lasercutter:\n     " + MainView.this.visicutModel1.getSelectedLaserDevice().getName(), "Job sent", JOptionPane.INFORMATION_MESSAGE);
        }
        catch (Exception ex)
        {
          if (ex instanceof IllegalJobException && ex.getMessage().startsWith("Illegal Focus value"))
          {
            JOptionPane.showMessageDialog(MainView.this, "You Material is too high for automatic Focussing.\nPlease focus manually and set the total height to 0.", "Error", JOptionPane.ERROR_MESSAGE);
          }
          else
          {
            JOptionPane.showMessageDialog(MainView.this, "Error: " + ex.getLocalizedMessage(), "Error", JOptionPane.ERROR_MESSAGE);
          }
        }
        MainView.this.executeJobButton.setEnabled(true);
        MainView.this.executeJobMenuItem.setEnabled(true);
      }
    }.start();
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

  /**
   * Returns a list of MaterialProfiles
   * for all available lasercutters. Aggregated by name and Depth
   * @return 
   */
  private List<MaterialProfile> getAllMaterials()
  {
    List<MaterialProfile> result = new LinkedList<MaterialProfile>();
    for (LaserDevice ld : PreferencesManager.getInstance().getPreferences().getLaserDevices())
    {
      for (MaterialProfile mp : this.profileManager1.getMaterials(ld))
      {
        boolean found = false;
        for (MaterialProfile pp : result)
        {
          if (pp.getName().equals(mp.getName()) && pp.getDepth() == mp.getDepth())
          {
            found = true;
            break;
          }
        }
        if (!found)
        {
          result.add(mp);
        }
      }
    }
    Collections.sort(result);
    return result;
  }

  /**
   * returns true iff the combination is supported
   * @param ld
   * @param mp
   * @param ms
   * @return 
   */
  private boolean supported(LaserDevice ld, MaterialProfile mp, MappingSet ms)
  {
    if (ld == null && mp == null)
    {
      return true;
    }
    for (MaterialProfile m : ld != null ? this.profileManager1.getMaterials(ld) : getAllMaterials())
    {
      if (mp != null && m.getName().equals(mp.getName()) && m.getDepth() == mp.getDepth())
      {
        if (ms == null)
        {
          return true;
        }
        else
        {
          boolean mappingOK = true;
          for (Mapping map : ms)
          {
            if (m.getLaserProfile(map.getProfileName()) == null)
            {
              mappingOK = false;
              break;
            }
          }
          if (mappingOK)
          {
            return true;
          }
        }
      }
      else
      {
        if (mp == null)
        {
          boolean mappingOK = true;
          if (ms != null)
          {
            for (Mapping map : ms)
            {
              if (m.getLaserProfile(map.getProfileName()) == null)
              {
                mappingOK = false;
                break;
              }
            }
          }
          if (mappingOK)
          {
            return true;
          }
        }
      }
    }
    return false;
  }

  private void save()
  {
    File file = null;
    //On Mac os, awt.FileDialog looks more native
    if (Helper.isMacOS())
    {
      FileDialog dialog = new java.awt.FileDialog(this);
      dialog.setMode(FileDialog.SAVE);
      if (lastDirectory != null)
      {
        dialog.setDirectory(lastDirectory.getAbsolutePath());
      }
      dialog.setVisible(true);
      if (dialog.getFile() != null)
      {
        file = new File(dialog.getFile());
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
        logger.log(Level.SEVERE, null, ex);
        JOptionPane.showMessageDialog(this, "Error saving File: " + ex.getLocalizedMessage());
      }
    }
    else
    {
      System.out.println("File access cancelled by user.");
    }
  }
  private Logger logger = Logger.getLogger(MainView.class.getName());

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
    if (cam)
    {
      this.captureImage();
    }
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
    Logger.getLogger(MainView.class.getName()).log(Level.SEVERE, null, ex);
    JOptionPane.showMessageDialog(this, "Error saving File:\n" + ex.getLocalizedMessage(), "Error", JOptionPane.ERROR_MESSAGE);
  }
}//GEN-LAST:event_saveMenuItemActionPerformed

private void newMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_newMenuItemActionPerformed
  this.previewPanel.setEditRectangle(null);
  this.visicutModel1.setGraphicObjects(new GraphicSet());
}//GEN-LAST:event_newMenuItemActionPerformed

private void calibrateCameraMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_calibrateCameraMenuItemActionPerformed
  if (this.visicutModel1.getBackgroundImage() == null)
  {
    JOptionPane.showMessageDialog(this, "The Camera doesn't seem to be working. Please check the URL in the Lasercutter Settings");
    return;
  }
  CamCalibrationDialog ccd = new CamCalibrationDialog();
  ccd.setBackgroundImage(this.visicutModel1.getBackgroundImage());
  ccd.setImageURL(this.visicutModel1.getSelectedLaserDevice().getCameraURL());
  ccd.setLaserCutter(this.visicutModel1.getSelectedLaserDevice().getLaserCutter());
  ccd.setResultingTransformation(this.visicutModel1.getSelectedLaserDevice().getCameraCalibration());
  ccd.setVisible(true);
  this.visicutModel1.getSelectedLaserDevice().setCameraCalibration(ccd.getResultingTransformation());
  try
  {
    PreferencesManager.getInstance().savePreferences();
  }
  catch (FileNotFoundException ex)
  {
    Logger.getLogger(MainView.class.getName()).log(Level.SEVERE, null, ex);
    JOptionPane.showMessageDialog(this, "Error while saving Settings: " + ex.getLocalizedMessage());
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
          }
          catch (Exception ex)
          {
            JOptionPane.showMessageDialog(MainView.this, "Error loading Image:" + ex.getLocalizedMessage(), "Error", JOptionPane.ERROR_MESSAGE);
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
  this.editMappings();
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
  this.customMappingTable.setLaserProfiles(newMaterial == null ? new LinkedList<LaserProfile>() : newMaterial.getLaserProfiles());
  this.refreshComboBoxes();
  this.refreshButtonStates();
}//GEN-LAST:event_materialComboBoxActionPerformed

  private void materialMenuItemActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_materialMenuItemActionPerformed
  {//GEN-HEADEREND:event_materialMenuItemActionPerformed
    ImageComboBox laserDevs = new ImageComboBox();
    for (LaserDevice ld : this.visicutModel1.getPreferences().getLaserDevices())
    {
      laserDevs.addItem(ld);
      if (ld.equals(this.visicutModel1.getSelectedLaserDevice()))
      {
        laserDevs.setSelectedItem(ld);
      }
    }
    if (laserDevs.getItemCount() == 0)
    {
      JOptionPane.showMessageDialog(this, "You have to add at least one Lasercutter first.", "Error", JOptionPane.ERROR_MESSAGE);
      return;
    }
    if (JOptionPane.showConfirmDialog(this, laserDevs, "Please choose a Lasercutter", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION)
    {
      LaserDevice ld = (LaserDevice) laserDevs.getSelectedItem();
      EditMaterialsDialog d = new EditMaterialsDialog(this, true);
      d.setMaterials(this.profileManager1.getMaterials(ld));
      d.setDefaultDirecoty(new File(ld.getMaterialsPath()));
      d.setVisible(true);
      List<MaterialProfile> result = d.getMaterials();
      if (result != null)
      {
        try
        {
          for (MaterialProfile mp : this.profileManager1.getMaterials(ld))
          {
            this.profileManager1.deleteProfile(mp, ld);
          }
          for (MaterialProfile mp : result)
          {
            this.profileManager1.saveProfile(mp, ld);
          }
          if (ld.equals(this.visicutModel1.getSelectedLaserDevice()))
          {
            this.profileManager1.loadMaterials(ld);
          }
          this.fillComboBoxes();
          this.refreshComboBoxes();
        }
        catch (FileNotFoundException ex)
        {
          JOptionPane.showMessageDialog(this, "Error saving Profile: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
      }
    }
  }//GEN-LAST:event_materialMenuItemActionPerformed

  /**
   * Disables all impossible combinations
   */
  private void refreshComboBoxes()
  {
    LaserDevice ld = this.visicutModel1.getSelectedLaserDevice();
    MaterialProfile mp = this.visicutModel1.getMaterial();
    MappingSet mappings = this.visicutModel1.getMappings();
    Integer res = this.visicutModel1.getResolution();
    for (int i = 1; i < this.laserCutterComboBox.getItemCount(); i++)
    {
      LaserDevice cld = (LaserDevice) this.laserCutterComboBox.getItemAt(i);
      if (res != null && !cld.getLaserCutter().getResolutions().contains(res))
      {
        this.laserCutterComboBox.setDisabled(cld, true, "Resolution not supported");
      }
      else if (supported(cld, mp, mappings))
      {
        this.laserCutterComboBox.setDisabled(cld, false);
      }
      else
      {
        this.laserCutterComboBox.setDisabled(cld, true, mappings == null ? "Material not supported" : "Mapping not supported");
      }
    }
    for (int i = 1; i < this.materialComboBox.getItemCount(); i++)
    {
      MaterialProfile m = (MaterialProfile) this.materialComboBox.getItemAt(i);
      if (supported(ld, m, mappings))
      {
        this.materialComboBox.setDisabled(m, false);
      }
      else
      {
        this.materialComboBox.setDisabled(m, true, mappings == null ? "Lasercutter not supported" : "Mapping not supported");
      }
    }
    for (int i = 1; i < this.resolutionComboBox.getItemCount(); i++)
    {
      Integer r = (Integer) this.resolutionComboBox.getItemAt(i);
      if (ld == null || ld.getLaserCutter().getResolutions().contains(r))
      {
        this.resolutionComboBox.setDisabled(r, false);
      }
      else
      {
        this.resolutionComboBox.setDisabled(r, true, "Resolution not supported by Lasercutter");
      }
    }
    for (int i = 1; i < this.predefinedMappingList.getItemCount(); i++)
    {
      MappingSet m = (MappingSet) this.predefinedMappingList.getItemAt(i);
      if (supported(ld, mp, m))
      {
        this.predefinedMappingList.setDisabled(m, false);
      }
      else
      {
        this.predefinedMappingList.setDisabled(m, true, mp == null ? "Lasercutter not supported" : "Material not supported");
      }
    }
    this.predefinedMappingList.repaint();
  }

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
    if (newDev != null)
    {
      this.profileManager1.loadMaterials(newDev);
      if (this.visicutModel1.getMaterial() != null)
      {
        for (MaterialProfile mp : this.profileManager1.getMaterials())
        {
          if (mp.getName().equals(this.visicutModel1.getMaterial().getName()) && mp.getDepth() == this.visicutModel1.getMaterial().getDepth())
          {
            this.visicutModel1.setMaterial(mp);
            break;
          }
        }
      }
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
    refreshComboBoxes();
    this.refreshButtonStates();
  }//GEN-LAST:event_laserCutterComboBoxActionPerformed

  private void jMenuItem2ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jMenuItem2ActionPerformed
  {//GEN-HEADEREND:event_jMenuItem2ActionPerformed
    ManageLasercuttersDialog d = new ManageLasercuttersDialog(this, true);
    d.setLaserCutters(this.visicutModel1.getPreferences().getLaserDevices());
    d.setDefaultLaserCutter(this.visicutModel1.getPreferences().getLastLaserDevice());
    d.setVisible(true);
    List<LaserDevice> result = d.getLaserCutters();
    if (result != null)
    {
      this.visicutModel1.getPreferences().setLaserDevices(result);
      try
      {
        PreferencesManager.getInstance().savePreferences();
      }
      catch (FileNotFoundException ex)
      {
        JOptionPane.showMessageDialog(this, "Error saving preferences: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
      }
      this.fillComboBoxes();
      this.refreshComboBoxes();
    }
  }//GEN-LAST:event_jMenuItem2ActionPerformed
  private MappingSet custom = null;

  private void calculateTimeButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_calculateTimeButtonActionPerformed
  {//GEN-HEADEREND:event_calculateTimeButtonActionPerformed
    new Thread()
    {

      public void run()
      {
        MainView.this.calculateTimeButton.setEnabled(false);
        MainView.this.progressBar.setIndeterminate(true);
        MainView.this.timeLabel.setText(Helper.toHHMMSS(MainView.this.visicutModel1.estimateTime()));
        MainView.this.progressBar.setIndeterminate(false);
        MainView.this.calculateTimeButton.setEnabled(true);
      }
    }.start();
  }//GEN-LAST:event_calculateTimeButtonActionPerformed

  private void showCuttingCbActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_showCuttingCbActionPerformed
  {//GEN-HEADEREND:event_showCuttingCbActionPerformed
    if (!this.showEngravingCb.isSelected() && !this.showCuttingCb.isSelected())
    {
      this.showCuttingCb.setSelected(true);
    }
    this.previewPanel.repaint();
  }//GEN-LAST:event_showCuttingCbActionPerformed

  private void showEngravingCbActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_showEngravingCbActionPerformed
  {//GEN-HEADEREND:event_showEngravingCbActionPerformed
    if (!this.showEngravingCb.isSelected() && !this.showCuttingCb.isSelected())
    {
      this.showEngravingCb.setSelected(true);
    }
    this.previewPanel.repaint();
  }//GEN-LAST:event_showEngravingCbActionPerformed

  private void predefinedMappingListValueChanged(javax.swing.event.ListSelectionEvent evt)//GEN-FIRST:event_predefinedMappingListValueChanged
  {//GEN-HEADEREND:event_predefinedMappingListValueChanged
    Object selected = this.predefinedMappingList.getSelectedValue();
    MappingSet ms = selected instanceof MappingSet ? (MappingSet) selected : null;
    this.setMappings(ms);
    this.refreshComboBoxes();
    this.refreshButtonStates();
  }//GEN-LAST:event_predefinedMappingListValueChanged

  private void customMappingComboBoxActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_customMappingComboBoxActionPerformed
  {//GEN-HEADEREND:event_customMappingComboBoxActionPerformed
    String attribute = (String) this.customMappingComboBox.getSelectedItem();
    if (attribute != null)
    {
      this.customMappingTable.setAttribute(attribute);
    }
  }//GEN-LAST:event_customMappingComboBoxActionPerformed

  private void setMappings(MappingSet mappings)
  {
    this.visicutModel1.setMappings(mappings);
  }

  private void mappingTabbedPaneStateChanged(javax.swing.event.ChangeEvent evt)//GEN-FIRST:event_mappingTabbedPaneStateChanged
  {//GEN-HEADEREND:event_mappingTabbedPaneStateChanged
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
      mapping = customMappingTable.getResultingMappingSet();
    }
    this.setMappings(mapping);
    this.refreshComboBoxes();
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

  private void previewPanelMouseClicked(java.awt.event.MouseEvent evt)//GEN-FIRST:event_previewPanelMouseClicked
  {//GEN-HEADEREND:event_previewPanelMouseClicked
    if (evt.getButton() == MouseEvent.BUTTON1 && this.mappingTabbedPane.getSelectedComponent().equals(this.customMappingPanel)
      && this.customMappingTable.getSelectedRow() >= 0)
    {
      this.customMappingTable.clearSelection();
    }
  }//GEN-LAST:event_previewPanelMouseClicked

private void reloadMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_reloadMenuItemActionPerformed
  if (this.visicutModel1.getLoadedFile() != null && this.visicutModel1.getLoadedFile().isFile())
  {
    try
    {
      this.visicutModel1.loadFromFile(this.mappingManager1, this.visicutModel1.getLoadedFile());
    }
    catch (Exception ex)
    {
      JOptionPane.showMessageDialog(this, "Error reloading File:\n" + ex.getMessage(), "Error", JOptionPane.ERROR);
      Logger.getLogger(MainView.class.getName()).log(Level.SEVERE, null, ex);
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
      e.printStackTrace();
      JOptionPane.showMessageDialog(this, "Error while opening '" + this.visicutModel1.getSourceFile().getName() + "':\n" + e.getLocalizedMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
  }
}//GEN-LAST:event_reloadMenuItemActionPerformed

private void resolutionComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_resolutionComboBoxActionPerformed
  //Check if Lasercutter supports Resolution
  Integer newResolution = this.resolutionComboBox.getSelectedItem() instanceof Integer ? (Integer) this.resolutionComboBox.getSelectedItem() : null;
  if (!Util.differ(newResolution, (Integer) visicutModel1.getResolution()))
  {
    return;
  }
  if (this.resolutionComboBox.isDisabled(newResolution))
  {
    this.resolutionComboBox.setSelectedItem((Integer) visicutModel1.getResolution());
    return;
  }
  this.visicutModel1.setResolution(newResolution);
  this.refreshComboBoxes();
  this.refreshButtonStates();
}//GEN-LAST:event_resolutionComboBoxActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem aboutMenuItem;
    private javax.swing.JButton calculateTimeButton;
    private javax.swing.JMenuItem calibrateCameraMenuItem;
    private javax.swing.JButton captureImageButton;
    private javax.swing.JComboBox customMappingComboBox;
    private javax.swing.JPanel customMappingPanel;
    private com.t_oster.visicut.gui.beans.CustomMappingTable customMappingTable;
    private javax.swing.JTextField dimensionWidthTextField;
    private javax.swing.JTextField dimesnionsHeightTextfield;
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
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JMenuItem jMenuItem2;
    private javax.swing.JMenuItem jMenuItem3;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private com.t_oster.visicut.gui.beans.ImageComboBox laserCutterComboBox;
    private com.t_oster.visicut.managers.MappingManager mappingManager1;
    private javax.swing.JTabbedPane mappingTabbedPane;
    private com.t_oster.visicut.gui.beans.ImageComboBox materialComboBox;
    private javax.swing.JTextField materialHeightTextField;
    private javax.swing.JMenuItem materialMenuItem;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JMenuItem newMenuItem;
    private javax.swing.JMenuItem openMenuItem;
    private com.t_oster.visicut.gui.beans.ImageListableList predefinedMappingList;
    private javax.swing.JPanel predefinedMappingPanel;
    private com.t_oster.visicut.gui.beans.PreviewPanel previewPanel;
    private com.t_oster.visicut.managers.ProfileManager profileManager1;
    private javax.swing.JProgressBar progressBar;
    private javax.swing.JMenuItem reloadMenuItem;
    private com.t_oster.visicut.gui.beans.ImageComboBox resolutionComboBox;
    private javax.swing.JMenuItem saveAsMenuItem;
    private javax.swing.JFileChooser saveFileChooser;
    private javax.swing.JMenuItem saveMenuItem;
    private javax.swing.JCheckBox showCuttingCb;
    private javax.swing.JCheckBox showEngravingCb;
    private javax.swing.JCheckBoxMenuItem showGridMenuItem;
    private javax.swing.JLabel timeLabel;
    private javax.swing.JMenu viewMenu;
    private com.t_oster.visicut.VisicutModel visicutModel1;
    private org.jdesktop.beansbinding.BindingGroup bindingGroup;
    // End of variables declaration//GEN-END:variables
}
