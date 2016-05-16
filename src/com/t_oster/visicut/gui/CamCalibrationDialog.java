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

/*
 * CamCalibrationDialog.java
 *
 * Created on 28.08.2011, 15:35:33
 */
package com.t_oster.visicut.gui;

import com.t_oster.liblasercut.LaserCutter;
import com.t_oster.liblasercut.LaserJob;
import com.t_oster.liblasercut.LaserProperty;
import com.t_oster.liblasercut.VectorPart;
import com.t_oster.liblasercut.platform.Util;
import com.t_oster.visicut.VisicutModel;
import com.t_oster.visicut.managers.LaserPropertyManager;
import com.t_oster.visicut.misc.Helper;
import com.t_oster.visicut.misc.Homography;
import com.t_oster.visicut.model.LaserDevice;
import com.t_oster.visicut.model.VectorProfile;
import com.t_oster.uicomponents.PlatformIcon;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.swing.JOptionPane;

/**
 *
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class CamCalibrationDialog extends javax.swing.JDialog
{

  protected String imageURL = null;

  /**
   * Get the value of imageURL
   *
   * @return the value of imageURL
   */
  public String getImageURL()
  {
    return imageURL;
  }

  /**
   * Set the value of imageURL
   *
   * @param imageURL new value of imageURL
   */
  public void setImageURL(String imageURL)
  {
    this.imageURL = imageURL;
  }

  protected BufferedImage backgroundImage = null;
  public static final String PROP_BACKGROUNDIMAGE = "backgroundImage";
  private int numAlignmentPoints = 8;
  private static Point2D.Double[] alignmentPoints;

  private static Point2D.Double[] alignmentPointsDefaults = {
    new Point2D.Double(0.2d, 0.2d), // upper left
    new Point2D.Double(0.8d, 0.8d), // lower right
    new Point2D.Double(0.8d, 0.2d), // upper right
    new Point2D.Double(0.2d, 0.8d), // lower left
    new Point2D.Double(0.5d, 0.2d), // upper mid
    new Point2D.Double(0.5d, 0.8d), // lower mid
    new Point2D.Double(0.3d, 0.5d), // mid left
    new Point2D.Double(0.7d, 0.5d), // mid right
  };


  /**
   * Get the value of backgroundImage
   *
   * @return the value of backgroundImage
   */
  public BufferedImage getBackgroundImage()
  {
    return backgroundImage;
  }

  /**
   * Set the value of backgroundImage
   *
   * @param backgroundImage new value of backgroundImage
   */
  public void setBackgroundImage(BufferedImage backgroundImage)
  {
    this.backgroundImage = backgroundImage;
    this.calibrationPanel1.setBackgroundImage(backgroundImage);
    //Check if a point is not in the Image (thus not reachable anymore)
    for (Point2D.Double p :this.calibrationPanel1.getPointList())
    {
      if (p.x >= backgroundImage.getWidth()) {
        p.x = backgroundImage.getWidth() - 1;
      }
      if (p.y >= backgroundImage.getHeight()) {
        p.y = backgroundImage.getHeight() - 1;
      }
    }
  }

  public void fetchFreshImage() {
        try
        {
          URL src = new URL(imageURL);
          if (src != null)
          {
            URLConnection conn = src.openConnection();

            // HTTP authentication
            if (VisicutModel.getInstance() != null && VisicutModel.getInstance().getSelectedLaserDevice() != null)
            {
              String encodedCredentials = Helper.getEncodedCredentials(VisicutModel.getInstance().getSelectedLaserDevice().getURLUser(), VisicutModel.getInstance().getSelectedLaserDevice().getURLPassword());
              if (!encodedCredentials.isEmpty())
              {
                conn.setRequestProperty("Authorization", "Basic " + encodedCredentials);
              }
            }

            ImageInputStream stream=new MemoryCacheImageInputStream(conn.getInputStream());
            BufferedImage back = ImageIO.read(stream);
            CamCalibrationDialog.this.setBackgroundImage(back);
          }
        }
        catch (Exception ex)
        {
          JOptionPane.showMessageDialog(CamCalibrationDialog.this, java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/resources/CamCalibrationDialog").getString("ERROR LOADING IMAGE:") + ex.getLocalizedMessage(), java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/resources/CamCalibrationDialog").getString("ERROR"), JOptionPane.ERROR_MESSAGE);
        }
  }

  /** These are the accepted correspondence points, either from initial values
   * or after OK has been pressed */
  private Point2D.Double[] confirmedImagePoints;
  /** These are points as modified by the dialog/panel, and are not confirmed
   * until OK has been pressed */
  private Point2D.Double[] modifiedImagePoints;

  private void refreshImagePoints()
  {
    int numPriorPoints = 0;
    if (modifiedImagePoints == null) {
      modifiedImagePoints = new Point2D.Double[numAlignmentPoints];
      if (confirmedImagePoints != null) {
        // Make a deep copy so alterations aren't applied until OK is pressed.
        numPriorPoints = Math.min(numAlignmentPoints, confirmedImagePoints.length);
        for (int i = 0; i < numPriorPoints; i++) {
          modifiedImagePoints[i] = new Point2D.Double(confirmedImagePoints[i].x,
              confirmedImagePoints[i].y);
        }
      }
    } else {
      numPriorPoints = modifiedImagePoints.length;
      if (modifiedImagePoints.length != numAlignmentPoints) {
        Point2D.Double[] temp = new Point2D.Double[numAlignmentPoints];
        for (int i = 0; i < Math.min(numAlignmentPoints, numPriorPoints); i++) {
          temp[i] = modifiedImagePoints[i];
        }
        modifiedImagePoints = temp;
      }
    }

    for (int i = numPriorPoints; i < numAlignmentPoints; i++) {
      modifiedImagePoints[i] = new Point2D.Double(alignmentPointsDefaults[i].x * backgroundImage.getWidth(),
          alignmentPointsDefaults[i].y * backgroundImage.getHeight());
    }
    this.calibrationPanel1.setPointList(modifiedImagePoints);
  }

  /**
   * Get the value of resultingTransformation
   *
   * @return the value of resultingTransformation
   */
  public Homography getResultingHomography()
  {
    Point2D.Double[] ap = new Point2D.Double[confirmedImagePoints.length];
    for (int i = 0; i < confirmedImagePoints.length; i++) {
      ap[i] = alignmentPoints[i];
    }
    return new Homography(ap, confirmedImagePoints);
  }

  private VectorProfile profile = null;
  public void setVectorProfile(VectorProfile p)
  {
    this.profile = p;
  }

  public void setCorrespondencePoints(Point2D.Double[] points) {
    confirmedImagePoints = points;
    modifiedImagePoints = null;
    numAlignmentPoints = points.length;
    alignmentPointsCombo.setSelectedItem(String.valueOf(numAlignmentPoints));
    refreshImagePoints();
  }

  public CamCalibrationDialog()
  {
    this(null, true);
  }

  /** Creates new form CamCalibrationDialog */
  public CamCalibrationDialog(java.awt.Frame parent, boolean modal)
  {
    super(parent, modal);
    initComponents();
    LaserCutter lc = VisicutModel.getInstance().getSelectedLaserDevice().getLaserCutter();
    alignmentPoints = new Point2D.Double[alignmentPointsDefaults.length];
    for (int i = 0; i < alignmentPointsDefaults.length; i++) {
      alignmentPoints[i] = new Point2D.Double(alignmentPointsDefaults[i].x * lc.getBedWidth(),
          alignmentPointsDefaults[i].y * lc.getBedHeight());
    }
    this.calibrationPanel1.setAreaSize(new Point2D.Double(lc.getBedWidth(), lc.getBedHeight()));
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

        okButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();
        sendButton = new javax.swing.JButton();
        captureButton = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        calibrationPanel1 = new com.t_oster.visicut.gui.beans.CalibrationPanel();
        btZoomIn = new javax.swing.JButton();
        btZoomOut = new javax.swing.JButton();
        alignmentPointsLabel = new javax.swing.JLabel();
        alignmentPointsCombo = new javax.swing.JComboBox();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/resources/CamCalibrationDialog"); // NOI18N
        setTitle(bundle.getString("TITLE")); // NOI18N
        setName("Form"); // NOI18N

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(com.t_oster.visicut.gui.VisicutApp.class).getContext().getResourceMap(CamCalibrationDialog.class);
        okButton.setText(resourceMap.getString("okButton.text")); // NOI18N
        okButton.setName("okButton"); // NOI18N
        okButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okButtonActionPerformed(evt);
            }
        });

        cancelButton.setText(resourceMap.getString("cancelButton.text")); // NOI18N
        cancelButton.setName("cancelButton"); // NOI18N
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });

        sendButton.setText(resourceMap.getString("sendButton.text")); // NOI18N
        sendButton.setName("sendButton"); // NOI18N
        sendButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sendButtonActionPerformed(evt);
            }
        });

        captureButton.setIcon(PlatformIcon.get(PlatformIcon.CAMERA));
        captureButton.setText(resourceMap.getString("captureButton.text")); // NOI18N
        captureButton.setName("captureButton"); // NOI18N
        captureButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                captureButtonActionPerformed(evt);
            }
        });

        jScrollPane1.setBorder(javax.swing.BorderFactory.createTitledBorder("Preview"));
        jScrollPane1.setName("jScrollPane1"); // NOI18N

        calibrationPanel1.setName("calibrationPanel1"); // NOI18N

        org.jdesktop.beansbinding.Binding binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ, this, org.jdesktop.beansbinding.ELProperty.create("${laserCam.capturedImage}"), calibrationPanel1, org.jdesktop.beansbinding.BeanProperty.create("backgroundImage"), "CamImageToPanel");
        bindingGroup.addBinding(binding);

        calibrationPanel1.addMouseWheelListener(new java.awt.event.MouseWheelListener() {
            public void mouseWheelMoved(java.awt.event.MouseWheelEvent evt) {
                calibrationPanel1MouseWheelMoved(evt);
            }
        });

        javax.swing.GroupLayout calibrationPanel1Layout = new javax.swing.GroupLayout(calibrationPanel1);
        calibrationPanel1.setLayout(calibrationPanel1Layout);
        calibrationPanel1Layout.setHorizontalGroup(
            calibrationPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1021, Short.MAX_VALUE)
        );
        calibrationPanel1Layout.setVerticalGroup(
            calibrationPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 412, Short.MAX_VALUE)
        );

        jScrollPane1.setViewportView(calibrationPanel1);

        btZoomIn.setIcon(PlatformIcon.get(PlatformIcon.ZOOM_IN));
        btZoomIn.setText(resourceMap.getString("btZoomIn.text")); // NOI18N
        btZoomIn.setName("btZoomIn"); // NOI18N
        btZoomIn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btZoomInActionPerformed(evt);
            }
        });

        btZoomOut.setIcon(PlatformIcon.get(PlatformIcon.ZOOM_OUT));
        btZoomOut.setText(resourceMap.getString("btZoomOut.text")); // NOI18N
        btZoomOut.setName("btZoomOut"); // NOI18N
        btZoomOut.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btZoomOutActionPerformed(evt);
            }
        });

        alignmentPointsLabel.setText(resourceMap.getString("alignmentPointsLabel.text")); // NOI18N
        alignmentPointsLabel.setName("alignmentPointsLabel"); // NOI18N

        alignmentPointsCombo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "2", "4", "6", "8" }));
        alignmentPointsCombo.setName("alignmentPointsCombo"); // NOI18N
        alignmentPointsCombo.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                alignmentPointsComboItemStateChanged(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(btZoomOut, javax.swing.GroupLayout.PREFERRED_SIZE, 51, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btZoomIn, javax.swing.GroupLayout.PREFERRED_SIZE, 54, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(6, 6, 6)
                .addComponent(alignmentPointsLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(alignmentPointsCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(captureButton)
                .addGap(552, 552, 552))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(sendButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(cancelButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(okButton, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 1035, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btZoomIn, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btZoomOut, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(alignmentPointsLabel)
                    .addComponent(alignmentPointsCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(captureButton, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 444, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(okButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(cancelButton)
                    .addComponent(sendButton))
                .addGap(17, 17, 17))
        );

        bindingGroup.bind();

        pack();
    }// </editor-fold>//GEN-END:initComponents

private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonActionPerformed
  this.setCorrespondencePoints(this.calibrationPanel1.getPointList());
  this.setVisible(false);
}//GEN-LAST:event_okButtonActionPerformed

private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
  this.refreshImagePoints();
  this.setVisible(false);
}//GEN-LAST:event_cancelButtonActionPerformed

private void sendButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sendButtonActionPerformed

  try
  {
    VisicutModel vm = VisicutModel.getInstance();
    LaserDevice laserDevice = vm.getSelectedLaserDevice();
    if (laserDevice == null)
    {
      throw new Exception(java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/resources/CamCalibrationDialog").getString("NO LASERCUTTER SELECTED"));
    }
    VectorPart vp = null;
    for (LaserProperty lp : LaserPropertyManager.getInstance().getLaserProperties(laserDevice, vm.getMaterial(), profile, vm.getMaterialThickness()))
    {
      if (vp == null)
      {
        vp = new VectorPart(lp, profile.getDPI());
      }
      else
      {
        vp.setProperty(lp);
      }
      int size = (int) Util.mm2px(10, profile.getDPI());
      int mm = (int) Util.mm2px(1, profile.getDPI());
      for (int i = 0; i < numAlignmentPoints; i++)
      {
        Point2D p = alignmentPoints[i];
        int x = (int) Util.mm2px(p.getX(), profile.getDPI());
        int y = (int) Util.mm2px(p.getY(), profile.getDPI());
        vp.moveto(x - size / 2, y);
        vp.lineto(x + size / 2, y);
        vp.moveto(x, y - size / 2);
        vp.lineto(x, y + size / 2);
        for (int j = 0; j <= i; j++) {
          // label counts with short ticks underneath
          if ((j+1) % 5 == 0) {
	    vp.moveto(x - i*mm + (j-5)*mm*2, y + size);
          } else {
	    vp.moveto(x - i*mm + j*mm*2, y + size);
          }
	    vp.lineto(x - i*mm + j*mm*2, y + size+4*mm);
        }
      }
    }
    LaserJob job = new LaserJob(java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/resources/CamCalibrationDialog").getString("CALIBRATION"), java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/resources/CamCalibrationDialog").getString("VISICUT CALIBRATION PAGE"), "visicut");
    job.addPart(vp);
    laserDevice.getLaserCutter().sendJob(job);
    JOptionPane.showMessageDialog(this, java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/resources/CamCalibrationDialog").getString("PLEASE PRESS 'START' ON THE LASERCUTTER"));
  }
  catch (Exception e)
  {
    JOptionPane.showMessageDialog(this, java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/resources/CamCalibrationDialog").getString("ERROR SENDING PAGE: ") + e.getLocalizedMessage(), java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/resources/CamCalibrationDialog").getString("ERROR"), JOptionPane.ERROR_MESSAGE);
  }
}//GEN-LAST:event_sendButtonActionPerformed

private void calibrationPanel1MouseWheelMoved(java.awt.event.MouseWheelEvent evt) {//GEN-FIRST:event_calibrationPanel1MouseWheelMoved
  if (evt.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL)
  {
    this.calibrationPanel1.setZoom(this.calibrationPanel1.getZoom() - (evt.getUnitsToScroll() * this.calibrationPanel1.getZoom() / 32));
  }
}//GEN-LAST:event_calibrationPanel1MouseWheelMoved

private void captureButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_captureButtonActionPerformed
new Thread()
    {

      @Override
      public void run()
      {
        CamCalibrationDialog.this.captureButton.setEnabled(false);
        fetchFreshImage();
        CamCalibrationDialog.this.captureButton.setEnabled(true);
      }
    }.start();
}//GEN-LAST:event_captureButtonActionPerformed

  private void btZoomInActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btZoomInActionPerformed
  {//GEN-HEADEREND:event_btZoomInActionPerformed
    calibrationPanel1.setZoom(calibrationPanel1.getZoom() - (-2 * calibrationPanel1.getZoom() / 32));
  }//GEN-LAST:event_btZoomInActionPerformed

  private void btZoomOutActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btZoomOutActionPerformed
  {//GEN-HEADEREND:event_btZoomOutActionPerformed
    calibrationPanel1.setZoom(calibrationPanel1.getZoom() - (2 * calibrationPanel1.getZoom() / 32));
  }//GEN-LAST:event_btZoomOutActionPerformed

private void alignmentPointsComboItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_alignmentPointsComboItemStateChanged
  this.numAlignmentPoints = Integer.parseInt((String)alignmentPointsCombo.getSelectedItem());
  this.refreshImagePoints();
}//GEN-LAST:event_alignmentPointsComboItemStateChanged

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox alignmentPointsCombo;
    private javax.swing.JLabel alignmentPointsLabel;
    private javax.swing.JButton btZoomIn;
    private javax.swing.JButton btZoomOut;
    private com.t_oster.visicut.gui.beans.CalibrationPanel calibrationPanel1;
    private javax.swing.JButton cancelButton;
    private javax.swing.JButton captureButton;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JButton okButton;
    private javax.swing.JButton sendButton;
    private org.jdesktop.beansbinding.BindingGroup bindingGroup;
    // End of variables declaration//GEN-END:variables
}
