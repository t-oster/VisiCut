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
package com.frochr123.fabqr.gui;

import com.frochr123.fabqr.FabQRFunctions;
import com.frochr123.helper.PreviewImageExport;
import com.frochr123.icons.IconLoader;
import com.t_oster.visicut.gui.MainView;
import com.tur0kk.TakePhotoThread;
import com.frochr123.periodictasks.RefreshProjectorThread;
import com.t_oster.visicut.VisicutModel;
import com.t_oster.visicut.managers.PreferencesManager;
import com.t_oster.visicut.model.PlfFile;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.util.ResourceBundle;
import java.util.regex.Pattern;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JDialog;
import javax.swing.SwingUtilities;
import org.apache.http.client.ClientProtocolException;

/**
 * FabQRUploadDialog.java, uploads project data to the configured FabQR instance
 * @author Christian
 */
public class FabQRUploadDialog extends javax.swing.JDialog
{
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnClose;
    private javax.swing.JButton btnPhoto;
    private javax.swing.JButton btnPhotoRedo;
    private javax.swing.JButton btnPublish;
    private javax.swing.JCheckBox cb3DPrinter;
    private javax.swing.JCheckBox cbArduino;
    private javax.swing.JCheckBox cbCNCRouter;
    private javax.swing.JCheckBox cbLasercutter;
    private javax.swing.JCheckBox cbPCBSoldering;
    private javax.swing.JCheckBox cbRaspberryPi;
    private javax.swing.JComboBox cmbbxLicense;
    private javax.swing.ButtonGroup grpCams;
    private javax.swing.JLabel lblDescription;
    private javax.swing.JLabel lblEmail;
    private javax.swing.JLabel lblLicense;
    private javax.swing.JLabel lblName;
    private javax.swing.JLabel lblPhoto;
    private javax.swing.JLabel lblProjectName;
    private javax.swing.JLabel lblStatus;
    private javax.swing.JLabel lblTools;
    private javax.swing.JPanel pnlMain;
    private javax.swing.JPanel pnlPhoto;
    private javax.swing.JPanel pnlRight;
    private javax.swing.JPanel pnlSelectCamera;
    private javax.swing.JPanel pnlTop;
    private javax.swing.JRadioButton rdbtnVisicam;
    private javax.swing.JRadioButton rdbtnWebcam;
    private javax.swing.JScrollPane scrlDescription;
    private javax.swing.JTextArea txtDescription;
    private javax.swing.JTextField txtEmail;
    private javax.swing.JTextField txtName;
    private javax.swing.JTextField txtProjectName;
    // End of variables declaration//GEN-END:variables

  // Variables
  private TakePhotoThread cameraThread = null;
  private BufferedImage latestFullCameraImage = null;
  private ResourceBundle bundle = java.util.ResourceBundle.getBundle("com/frochr123/fabqr/gui/resources/FabQRUploadDialog");
  
  // Constructor
  public FabQRUploadDialog(java.awt.Frame parent, boolean modal)
  {
    super(parent, modal);
    
    // Initialization
    initComponents();
    this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    initWindowListener();

    // Set correct language for combobox license after initComponents()
    String[] localizedComboboxLicenseItems = new String[]
    {
      bundle.getString("CC0"),
      bundle.getString("CCBY"),
      bundle.getString("CCBYSA"),
      bundle.getString("CCBYNC"),
      bundle.getString("CCBYND"),
      bundle.getString("CCBYNCSA"),
      bundle.getString("CCBYNCND")
    };
    
    DefaultComboBoxModel localizedComboboxLicenseItemsModel = new javax.swing.DefaultComboBoxModel(localizedComboboxLicenseItems);
    cmbbxLicense.setModel(localizedComboboxLicenseItemsModel);

    // Change camera thread on switch buttons
    ItemListener buttonChangeListener = new ItemListener()
    {
      public void itemStateChanged(ItemEvent e)
      {
        // One button change causes SELECTED and DESELECTED, only react to SELECTED
        if (e.getStateChange() == ItemEvent.SELECTED)
        {
          // Stop old camera thread and start new one with new settings
          closeCamera();
          setupCamera();
        }
      }
    };

    rdbtnWebcam.addItemListener(buttonChangeListener);
    rdbtnVisicam.addItemListener(buttonChangeListener);
    
    // Set default status message
    showStatus(bundle.getString("INFO_WAITING_FOR_USER_INPUT"));
    
    // Start capturing
    setupCamera();
  }

  // Window listener to close camera thread on window close, reset state in MainView
  private void initWindowListener()
  {
    this.addWindowListener
    (
      new WindowListener()
      {
        public void windowClosing(WindowEvent e)
        {
          closeDialogCleanup();
        }

        public void windowOpened(WindowEvent e)
        {
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
      }
    );
  }
    
  // Start camera capturing thread, updates lblPhoto frequently, deal with UI elements
  private void setupCamera()
  {
    // Reset image
    lblPhoto.setIcon(null);

    // Reset button states
    btnPhoto.setEnabled(false);
    btnPhotoRedo.setEnabled(false);
    btnPublish.setEnabled(false);

    // Check camera mode
    if ((rdbtnWebcam.isSelected() && TakePhotoThread.isWebCamDetected()) ||
       (rdbtnVisicam.isSelected() && TakePhotoThread.isVisiCamDetected()))
    {
      showStatus(bundle.getString("INFO_WAITING_FOR_USER_INPUT"));
      btnPhoto.setEnabled(true);

      cameraThread = new TakePhotoThread(lblPhoto, rdbtnWebcam.isSelected(), TakePhotoThread.PHOTO_RESOLUTION_HIGH);
      cameraThread.start();
    }
    else
    {
      showStatus(bundle.getString("ERROR_NO_CAMERA_FOUND"));
    }
  }

  // Interrupt thrread, freezes image
  private void closeCamera()
  {
    if (cameraThread != null)
    {
      cameraThread.interrupt();
      cameraThread = null;
    }
  }
  
  // Show status message
  private void showStatus(String message)
  {
    String finalMessage = bundle.getString("STATUS") + ": " + message;
    lblStatus.setText(finalMessage);
  }

  // Close window correctly
  private void closeDialogCleanup()
  {
    if (MainView.getInstance() != null)
    {
      MainView.getInstance().setFabqrUploadDialogOpened(false);
    }

    closeCamera();
  }

  private void handleGUIElements(final boolean enabled, final boolean disablePhotoButton)
  {
    if (enabled)
    {
      lblStatus.setIcon(null);
    }
    else
    {
      showStatus(bundle.getString("UPLOADING_MESSAGE"));
      lblStatus.setIcon(IconLoader.loadIcon(IconLoader.ICON_LOADING_CIRCLE_SMALL));
    }

    txtName.setEnabled(enabled);
    txtEmail.setEnabled(enabled);
    txtProjectName.setEnabled(enabled);
    cmbbxLicense.setEnabled(enabled);
    cbPCBSoldering.setEnabled(enabled);
    cbArduino.setEnabled(enabled);
    cb3DPrinter.setEnabled(enabled);
    cbCNCRouter.setEnabled(enabled);
    cbRaspberryPi.setEnabled(enabled);
    txtDescription.setEnabled(enabled);
    btnClose.setEnabled(enabled);
    btnPublish.setEnabled(enabled);

    rdbtnVisicam.setEnabled(enabled);
    rdbtnWebcam.setEnabled(enabled);
    btnPhotoRedo.setEnabled(enabled);
    btnPhoto.setEnabled(disablePhotoButton ? false : enabled);

    txtName.setEditable(enabled);
    txtName.setBackground(enabled ? Color.white : Color.lightGray);
    txtEmail.setEditable(enabled);
    txtEmail.setBackground(enabled ? Color.white : Color.lightGray);
    txtProjectName.setEditable(enabled);
    txtProjectName.setBackground(enabled ? Color.white : Color.lightGray);
    txtDescription.setEditable(enabled);
    txtDescription.setBackground(enabled ? Color.white : Color.lightGray);
  }
  
  // Function for drawing dialog elements
  // Generated by Form Editor
  @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        grpCams = new javax.swing.ButtonGroup();
        lblStatus = new javax.swing.JLabel();
        pnlMain = new javax.swing.JPanel();
        pnlTop = new javax.swing.JPanel();
        lblName = new javax.swing.JLabel();
        txtName = new javax.swing.JTextField();
        lblEmail = new javax.swing.JLabel();
        txtEmail = new javax.swing.JTextField();
        pnlPhoto = new javax.swing.JPanel();
        btnPhoto = new javax.swing.JButton();
        btnPhotoRedo = new javax.swing.JButton();
        pnlSelectCamera = new javax.swing.JPanel();
        rdbtnVisicam = new javax.swing.JRadioButton();
        rdbtnWebcam = new javax.swing.JRadioButton();
        lblPhoto = new javax.swing.JLabel();
        pnlRight = new javax.swing.JPanel();
        lblProjectName = new javax.swing.JLabel();
        txtProjectName = new javax.swing.JTextField();
        lblLicense = new javax.swing.JLabel();
        cmbbxLicense = new javax.swing.JComboBox();
        lblTools = new javax.swing.JLabel();
        cbLasercutter = new javax.swing.JCheckBox();
        cbPCBSoldering = new javax.swing.JCheckBox();
        cbArduino = new javax.swing.JCheckBox();
        cb3DPrinter = new javax.swing.JCheckBox();
        cbCNCRouter = new javax.swing.JCheckBox();
        cbRaspberryPi = new javax.swing.JCheckBox();
        lblDescription = new javax.swing.JLabel();
        scrlDescription = new javax.swing.JScrollPane();
        txtDescription = new javax.swing.JTextArea();
        btnClose = new javax.swing.JButton();
        btnPublish = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setName("Form"); // NOI18N
        setResizable(false);

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(com.t_oster.visicut.gui.VisicutApp.class).getContext().getResourceMap(FabQRUploadDialog.class);
        lblStatus.setText(resourceMap.getString("lblStatus.text")); // NOI18N
        lblStatus.setAlignmentY(0.0F);
        lblStatus.setName("lblStatus"); // NOI18N

        pnlMain.setBorder(javax.swing.BorderFactory.createLineBorder(resourceMap.getColor("pnlMain.border.lineColor"))); // NOI18N
        pnlMain.setName("pnlMain"); // NOI18N

        pnlTop.setName("pnlTop"); // NOI18N

        lblName.setText(resourceMap.getString("lblName.text")); // NOI18N
        lblName.setAlignmentY(0.0F);
        lblName.setName("lblName"); // NOI18N

        txtName.setText(resourceMap.getString("txtName.text")); // NOI18N
        txtName.setName("txtName"); // NOI18N

        lblEmail.setText(resourceMap.getString("lblEmail.text")); // NOI18N
        lblEmail.setAlignmentY(0.0F);
        lblEmail.setName("lblEmail"); // NOI18N

        txtEmail.setText(resourceMap.getString("txtEmail.text")); // NOI18N
        txtEmail.setName("txtEmail"); // NOI18N

        javax.swing.GroupLayout pnlTopLayout = new javax.swing.GroupLayout(pnlTop);
        pnlTop.setLayout(pnlTopLayout);
        pnlTopLayout.setHorizontalGroup(
            pnlTopLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlTopLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnlTopLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(lblName, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(lblEmail, javax.swing.GroupLayout.DEFAULT_SIZE, 97, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnlTopLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(txtEmail, javax.swing.GroupLayout.DEFAULT_SIZE, 221, Short.MAX_VALUE)
                    .addComponent(txtName, javax.swing.GroupLayout.DEFAULT_SIZE, 221, Short.MAX_VALUE))
                .addContainerGap())
        );
        pnlTopLayout.setVerticalGroup(
            pnlTopLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pnlTopLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(pnlTopLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblName, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnlTopLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtEmail, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblEmail, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        pnlPhoto.setBorder(javax.swing.BorderFactory.createLineBorder(resourceMap.getColor("pnlPhoto.border.lineColor"))); // NOI18N
        pnlPhoto.setName("pnlPhoto"); // NOI18N

        btnPhoto.setText(resourceMap.getString("btnPhoto.text")); // NOI18N
        btnPhoto.setName("btnPhoto"); // NOI18N
        btnPhoto.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPhotoActionPerformed(evt);
            }
        });

        btnPhotoRedo.setText(resourceMap.getString("btnPhotoRedo.text")); // NOI18N
        btnPhotoRedo.setName("btnPhotoRedo"); // NOI18N
        btnPhotoRedo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPhotoRedoActionPerformed(evt);
            }
        });

        pnlSelectCamera.setBorder(javax.swing.BorderFactory.createTitledBorder(resourceMap.getString("pnlSelectCamera.border.title"))); // NOI18N
        pnlSelectCamera.setName("pnlSelectCamera"); // NOI18N
        pnlSelectCamera.setLayout(new java.awt.GridLayout(1, 2));

        grpCams.add(rdbtnVisicam);
        rdbtnVisicam.setSelected(true);
        rdbtnVisicam.setText(resourceMap.getString("rdbtnVisicam.text")); // NOI18N
        rdbtnVisicam.setName("rdbtnVisicam"); // NOI18N
        pnlSelectCamera.add(rdbtnVisicam);

        grpCams.add(rdbtnWebcam);
        rdbtnWebcam.setText(resourceMap.getString("rdbtnWebcam.text")); // NOI18N
        rdbtnWebcam.setName("rdbtnWebcam"); // NOI18N
        pnlSelectCamera.add(rdbtnWebcam);

        lblPhoto.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblPhoto.setText(resourceMap.getString("lblPhoto.text")); // NOI18N
        lblPhoto.setBorder(javax.swing.BorderFactory.createLineBorder(resourceMap.getColor("lblPhoto.border.lineColor"))); // NOI18N
        lblPhoto.setName("lblPhoto"); // NOI18N

        javax.swing.GroupLayout pnlPhotoLayout = new javax.swing.GroupLayout(pnlPhoto);
        pnlPhoto.setLayout(pnlPhotoLayout);
        pnlPhotoLayout.setHorizontalGroup(
            pnlPhotoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlPhotoLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnlPhotoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(lblPhoto, javax.swing.GroupLayout.PREFERRED_SIZE, 320, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(pnlPhotoLayout.createSequentialGroup()
                        .addGroup(pnlPhotoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(btnPhotoRedo, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(btnPhoto, javax.swing.GroupLayout.DEFAULT_SIZE, 120, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(pnlSelectCamera, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );
        pnlPhotoLayout.setVerticalGroup(
            pnlPhotoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlPhotoLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnlPhotoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pnlPhotoLayout.createSequentialGroup()
                        .addComponent(btnPhoto)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnPhotoRedo))
                    .addComponent(pnlSelectCamera, javax.swing.GroupLayout.PREFERRED_SIZE, 52, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(lblPhoto, javax.swing.GroupLayout.PREFERRED_SIZE, 240, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        pnlRight.setName("pnlRight"); // NOI18N

        lblProjectName.setText(resourceMap.getString("lblProjectName.text")); // NOI18N
        lblProjectName.setAlignmentY(0.0F);
        lblProjectName.setName("lblProjectName"); // NOI18N

        txtProjectName.setText(resourceMap.getString("txtProjectName.text")); // NOI18N
        txtProjectName.setName("txtProjectName"); // NOI18N

        lblLicense.setText(resourceMap.getString("lblLicense.text")); // NOI18N
        lblLicense.setAlignmentY(0.0F);
        lblLicense.setName("lblLicense"); // NOI18N

        cmbbxLicense.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "CC0 1.0: No restrictions", "CC BY 4.0: Author attribution", "CC BY-SA 4.0: Author attribution; Share with same license", "CC BY-NC 4.0: Author attribution; Non-commercial", "CC BY-ND 4.0: Author attribution; No derivatives allowed", "CC BY-NC-SA 4.0: Author attribution; Non-commercial; Share with same license", "CC BY-NC-ND 4.0: Author attribution; Non-commercial; No derivatives allowed" }));
        cmbbxLicense.setName("cmbbxLicense"); // NOI18N

        lblTools.setText(resourceMap.getString("lblTools.text")); // NOI18N
        lblTools.setAlignmentY(0.0F);
        lblTools.setName("lblTools"); // NOI18N

        cbLasercutter.setSelected(true);
        cbLasercutter.setText(resourceMap.getString("cbLasercutter.text")); // NOI18N
        cbLasercutter.setEnabled(false);
        cbLasercutter.setName("cbLasercutter"); // NOI18N

        cbPCBSoldering.setText(resourceMap.getString("cbPCBSoldering.text")); // NOI18N
        cbPCBSoldering.setName("cbPCBSoldering"); // NOI18N

        cbArduino.setText(resourceMap.getString("cbArduino.text")); // NOI18N
        cbArduino.setName("cbArduino"); // NOI18N

        cb3DPrinter.setText(resourceMap.getString("cb3DPrinter.text")); // NOI18N
        cb3DPrinter.setName("cb3DPrinter"); // NOI18N

        cbCNCRouter.setText(resourceMap.getString("cbCNCRouter.text")); // NOI18N
        cbCNCRouter.setName("cbCNCRouter"); // NOI18N

        cbRaspberryPi.setText(resourceMap.getString("cbRaspberryPi.text")); // NOI18N
        cbRaspberryPi.setName("cbRaspberryPi"); // NOI18N

        lblDescription.setText(resourceMap.getString("lblDescription.text")); // NOI18N
        lblDescription.setAlignmentY(0.0F);
        lblDescription.setName("lblDescription"); // NOI18N

        scrlDescription.setName("scrlDescription"); // NOI18N

        txtDescription.setColumns(20);
        txtDescription.setLineWrap(true);
        txtDescription.setRows(5);
        txtDescription.setAlignmentX(0.0F);
        txtDescription.setAlignmentY(0.0F);
        txtDescription.setBorder(javax.swing.BorderFactory.createLineBorder(resourceMap.getColor("txtDescription.border.lineColor"))); // NOI18N
        txtDescription.setName("txtDescription"); // NOI18N
        scrlDescription.setViewportView(txtDescription);

        javax.swing.GroupLayout pnlRightLayout = new javax.swing.GroupLayout(pnlRight);
        pnlRight.setLayout(pnlRightLayout);
        pnlRightLayout.setHorizontalGroup(
            pnlRightLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pnlRightLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnlRightLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(scrlDescription, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 361, Short.MAX_VALUE)
                    .addComponent(txtProjectName, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 361, Short.MAX_VALUE)
                    .addComponent(lblProjectName, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 361, Short.MAX_VALUE)
                    .addComponent(lblLicense, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 361, Short.MAX_VALUE)
                    .addComponent(cmbbxLicense, javax.swing.GroupLayout.Alignment.LEADING, 0, 361, Short.MAX_VALUE)
                    .addComponent(lblTools, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 361, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, pnlRightLayout.createSequentialGroup()
                        .addGroup(pnlRightLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(cb3DPrinter, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(cbLasercutter, javax.swing.GroupLayout.DEFAULT_SIZE, 96, Short.MAX_VALUE))
                        .addGap(18, 18, 18)
                        .addGroup(pnlRightLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(cbCNCRouter, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(cbPCBSoldering, javax.swing.GroupLayout.DEFAULT_SIZE, 130, Short.MAX_VALUE))
                        .addGap(18, 18, 18)
                        .addGroup(pnlRightLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addGroup(pnlRightLayout.createSequentialGroup()
                                .addComponent(cbArduino)
                                .addGap(36, 36, 36))
                            .addComponent(cbRaspberryPi, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .addComponent(lblDescription, javax.swing.GroupLayout.Alignment.LEADING))
                .addContainerGap())
        );
        pnlRightLayout.setVerticalGroup(
            pnlRightLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlRightLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lblProjectName, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txtProjectName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(lblLicense, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(cmbbxLicense, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(lblTools, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnlRightLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cbLasercutter)
                    .addComponent(cbPCBSoldering)
                    .addComponent(cbArduino))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnlRightLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cb3DPrinter)
                    .addComponent(cbCNCRouter)
                    .addComponent(cbRaspberryPi))
                .addGap(18, 18, 18)
                .addComponent(lblDescription, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(scrlDescription, javax.swing.GroupLayout.DEFAULT_SIZE, 94, Short.MAX_VALUE)
                .addContainerGap())
        );

        btnClose.setText(resourceMap.getString("btnClose.text")); // NOI18N
        btnClose.setToolTipText(resourceMap.getString("btnClose.toolTipText")); // NOI18N
        btnClose.setName("btnClose"); // NOI18N
        btnClose.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCloseActionPerformed(evt);
            }
        });

        btnPublish.setText(resourceMap.getString("btnPublish.text")); // NOI18N
        btnPublish.setToolTipText(resourceMap.getString("btnPublish.toolTipText")); // NOI18N
        btnPublish.setName("btnPublish"); // NOI18N
        btnPublish.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPublishActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout pnlMainLayout = new javax.swing.GroupLayout(pnlMain);
        pnlMain.setLayout(pnlMainLayout);
        pnlMainLayout.setHorizontalGroup(
            pnlMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlMainLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnlMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(pnlTop, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(pnlPhoto, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(pnlMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(pnlMainLayout.createSequentialGroup()
                        .addComponent(btnClose, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(btnPublish, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(pnlRight, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        pnlMainLayout.setVerticalGroup(
            pnlMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pnlMainLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(pnlMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pnlMainLayout.createSequentialGroup()
                        .addComponent(pnlTop, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(pnlPhoto, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pnlMainLayout.createSequentialGroup()
                        .addComponent(pnlRight, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(18, 18, 18)
                        .addGroup(pnlMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(btnPublish)
                            .addComponent(btnClose))))
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(lblStatus, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 755, Short.MAX_VALUE)
                    .addComponent(pnlMain, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lblStatus, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(pnlMain, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

private void btnPhotoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPhotoActionPerformed
  
  // Get latest full camera image (without resize)
  latestFullCameraImage = null;

  if (cameraThread != null)
  {
    Image image = cameraThread.getLatestRawImage();
    
    // Convert image to bufferedimage
    if (image != null)
    {
      BufferedImage bufferedImage = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_ARGB);
      Graphics2D graphics = bufferedImage.createGraphics();
      graphics.drawImage(image, 0, 0, null);
      graphics.dispose();

      latestFullCameraImage = bufferedImage;
    }
  }

  // Stop camera, freeze image
  closeCamera();

  // Set correct UI button states
  btnPhoto.setEnabled(false);
  btnPhotoRedo.setEnabled(true);
  btnPublish.setEnabled(true);
}//GEN-LAST:event_btnPhotoActionPerformed

  private void btnPhotoRedoActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btnPhotoRedoActionPerformed
  {//GEN-HEADEREND:event_btnPhotoRedoActionPerformed
    // Restart camera thread
    closeCamera();
    setupCamera();
  }//GEN-LAST:event_btnPhotoRedoActionPerformed

  private void btnPublishActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btnPublishActionPerformed
  {//GEN-HEADEREND:event_btnPublishActionPerformed
    // Disable all GUI elements and set loading status
    handleGUIElements(false, true);

    // Start new thread to prepare and upload data
    new Thread(new Runnable()
    {
      public void run()
      {
        try
        {
          // Check for valid situation, otherwise abort
          if (MainView.getInstance() == null || VisicutModel.getInstance() == null || VisicutModel.getInstance().getPlfFile() == null
              || PreferencesManager.getInstance() == null || PreferencesManager.getInstance().getPreferences() == null
              || !FabQRFunctions.isFabqrActive() || FabQRFunctions.getFabqrPrivateURL() == null
              || FabQRFunctions.getFabqrPrivateURL().isEmpty())
          {
            throw new Exception(bundle.getString("ERROR_CRITICAL"));
          }
          
          // Texts
          String name = txtName.getText();
          String email = txtEmail.getText();
          String projectName = txtProjectName.getText();
          name = ((name == null) ? "" : name.trim());
          email = ((email == null) ? "" : email.trim());
          projectName = ((projectName == null) ? "" : projectName.trim());

          // Check valid project name
          if (projectName.isEmpty())
          {
            throw new Exception(bundle.getString("ERROR_EMPTY_PROJECT_NAME"));
          }
          
          if (projectName.length() < 3)
          {
            throw new Exception(bundle.getString("ERROR_TOO_SHORT_PROJECT_NAME"));
          }
          
          // Get license index, index -1 = error, index 0 = requires no name / mail, index > 0 requires name and email
          int licenseIndex = cmbbxLicense.getSelectedIndex();

          // Check valid license
          if (licenseIndex < 0)
          {
            throw new Exception(bundle.getString("ERROR_INVALID_LICENSE"));
          }
          // Check for valid name and email, needed for these licenses
          else if (licenseIndex > 0)
          {
            if (name.isEmpty())
            {
              throw new Exception(bundle.getString("ERROR_LICENSE_NEEDS_NAME"));
            }

            if (email.isEmpty())
            {
              throw new Exception(bundle.getString("ERROR_LICENSE_NEEDS_EMAIL"));
            }
          }

          // For these cases email must be checked
          if (licenseIndex > 0 || !email.isEmpty())
          {
            // Simple and inaccurate check for valid email with regex
            Pattern emailPattern = Pattern.compile("^.+@.+\\..+$");

            if (!emailPattern.matcher(email).find())
            {
              throw new Exception(bundle.getString("ERROR_INVALID_EMAIL"));
            }
          }

          // Build string for selected tools
          String tools = "Laser cutter";
          
          if (cbPCBSoldering.isSelected())
          {
            tools = tools + ",PCB / Soldering";
          }
          
          if (cb3DPrinter.isSelected())
          {
            tools = tools + ",3D printer";
          }
          
          if (cbCNCRouter.isSelected())
          {
            tools = tools + ",CNC router";
          }
          
          if (cbArduino.isSelected())
          {
            tools = tools + ",Arduino";
          }
          
          if (cbRaspberryPi.isSelected())
          {
            tools = tools + ",Raspberry Pi";
          }

          // Check valid description
          String description = txtDescription.getText();
          description = ((description == null) ? "" : description.trim());

          if (description.isEmpty())
          {
            throw new Exception(bundle.getString("ERROR_EMPTY_DESCRIPTION"));
          }
          
          // Images, real image is allowed to be null, scheme image must not be null
          BufferedImage imageReal = latestFullCameraImage;
          BufferedImage imageScheme = PreviewImageExport.generateImage(RefreshProjectorThread.getProjectorWidth(), RefreshProjectorThread.getProjectorHeight(), true);

          if (imageScheme == null)
          {
            throw new Exception(bundle.getString("ERROR_EMPTY_SCHEME_IMAGE"));
          }
          
          // Get PLF data
          PlfFile plfFile = VisicutModel.getInstance().getPlfFile();

          // Check PLF data
          if (plfFile == null)
          {
            throw new Exception(bundle.getString("ERROR_INVALID_PLF_DATA"));
          }

          // Get internal data: lasercutter name
          String lasercutterName = "";

          if (VisicutModel.getInstance().getSelectedLaserDevice() != null
              && VisicutModel.getInstance().getSelectedLaserDevice().getLaserCutter() != null)
          {
            lasercutterName = VisicutModel.getInstance().getSelectedLaserDevice().getLaserCutter().getModelName();
            lasercutterName = ((lasercutterName == null) ? "" : lasercutterName.trim());
          }

          // Check lasercutter name
          if (lasercutterName.isEmpty())
          {
            throw new Exception(bundle.getString("ERROR_EMPTY_LASERCUTTER_NAME"));
          }

          // Get internal data: lasercutter material string
          String lasercutterMaterial = "";

          if (VisicutModel.getInstance().getMaterial() != null)
          {
            String lasercutterMaterialName = VisicutModel.getInstance().getMaterial().getName();
            lasercutterMaterialName = ((lasercutterMaterialName == null) ? "" : lasercutterMaterialName.trim());
            
            // Check material name
            if (lasercutterMaterialName.isEmpty())
            {
              throw new Exception(bundle.getString("ERROR_EMPTY_MATERIAL_NAME"));
            }
            
            float lasercutterMaterialThickness = VisicutModel.getInstance().getMaterialThickness();
            lasercutterMaterial = lasercutterMaterialName + ", " + new Float(lasercutterMaterialThickness).toString() + " mm";
          }
          
          // Get internal data: Location, FabLab name
          String location = PreferencesManager.getInstance().getPreferences().getLabName();

          if (location == null || location.isEmpty())
          {
            throw new Exception(bundle.getString("ERROR_INVALID_FABLAB_NAME"));
          }
          
          // Upload data
          FabQRFunctions.uploadFabQRProject(name, email, projectName, licenseIndex, tools, description, location, imageReal, imageScheme, plfFile, lasercutterName, lasercutterMaterial);

          // On success show suscess message and disable loading icon
          showStatus(bundle.getString("SUCCESS_MESSAGE"));
          lblStatus.setIcon(null);

          // On success enable cancel button again to close dialog
          btnClose.setEnabled(true);
        }
        catch (ClientProtocolException e)
        {
          // Enable all GUI elements and disable loading status
          handleGUIElements(true, true);
          
          // Show error message
          if (e.getCause() != null && e.getCause().getMessage() != null && !e.getCause().getMessage().isEmpty())
          {
            showStatus("HTTP exception: " + e.getCause().getMessage());
          }
          else
          {
            showStatus(e.getMessage());
          }
        }
        catch (Exception e)
        {
          // Enable all GUI elements and disable loading status
          handleGUIElements(true, true);
          
          // Show error message
          showStatus(e.getMessage());
        }
      }
    }).start();
  }//GEN-LAST:event_btnPublishActionPerformed

  private void btnCloseActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btnCloseActionPerformed
  {//GEN-HEADEREND:event_btnCloseActionPerformed
    // Just close dialog
    closeDialogCleanup();
    dispose();
  }//GEN-LAST:event_btnCloseActionPerformed
}
