/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

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
import com.t_oster.visicut.Helper;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.net.URL;
import javax.imageio.ImageIO;
import javax.swing.JOptionPane;

/**
 *
 * @author thommy
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
  public Point laserUpperLeft = new Point((int) Util.mm2px(0, 500), (int) Util.mm2px(0, 500));
  public Point laserLowerRight = new Point((int) Util.mm2px(600, 500), (int) Util.mm2px(300, 500));


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
  }
  protected LaserCutter laserCutter = null;

  /**
   * Get the value of laserCutter
   *
   * @return the value of laserCutter
   */
  public LaserCutter getLaserCutter()
  {
    return laserCutter;
  }

  /**
   * Set the value of laserCutter
   *
   * @param laserCutter new value of laserCutter
   */
  public void setLaserCutter(LaserCutter laserCutter)
  {
    this.laserCutter = laserCutter;
    if (laserCutter != null)
    {
      double width = this.laserCutter.getBedWidth();
      double height = this.laserCutter.getBedHeight();
      laserUpperLeft = new Point((int) Util.mm2px(width * 20 / 100, 500), (int) Util.mm2px(height * 20 / 100, 500));
      laserLowerRight = new Point((int) Util.mm2px(width * 80 / 100, 500), (int) Util.mm2px(height * 80 / 100, 500));
      refreshImagePoints();
    }
  }
  protected AffineTransform currentTransformation = currentTransformation = AffineTransform.getScaleInstance(0.01, 0.01);
  public static final String PROP_CURRENTTRANSFORMATION = "currentTransformation";

  private void refreshImagePoints()
  {
    Point imageUpperLeft = (Point) laserUpperLeft.clone();
    Point imageLowerRight = (Point) laserLowerRight.clone();
    if (this.getResultingTransformation() != null)
    {
      AffineTransform laser2img = this.getResultingTransformation();
      laser2img.transform(imageUpperLeft, imageUpperLeft);
      laser2img.transform(imageLowerRight, imageLowerRight);
    }
    this.calibrationPanel1.setPointList(new Point[]
      {
        imageUpperLeft, imageLowerRight
      });
  }

  /**
   * Get the value of currentTransformation
   *
   * @return the value of currentTransformation
   */
  public AffineTransform getCurrentTransformation()
  {
    Point[] img = this.calibrationPanel1.getPointList();
    return Helper.getTransform(
      new Rectangle(laserUpperLeft.x, laserUpperLeft.y, laserLowerRight.x - laserUpperLeft.x, laserLowerRight.y - laserUpperLeft.y),
      new Rectangle(img[0].x, img[0].y, img[1].x - img[0].x, img[1].y - img[0].y));
  }
  protected AffineTransform resultingTransformation = null;
  public static final String PROP_RESULTINGTRANSFORMATION = "resultingTransformation";

  /**
   * Get the value of resultingTransformation
   *
   * @return the value of resultingTransformation
   */
  public AffineTransform getResultingTransformation()
  {
    return resultingTransformation;
  }

  /**
   * Set the value of resultingTransformation
   *
   * @param resultingTransformation new value of resultingTransformation
   */
  public void setResultingTransformation(AffineTransform resultingTransformation)
  {
    AffineTransform oldResultingTransformation = this.resultingTransformation;
    this.resultingTransformation = resultingTransformation;
    firePropertyChange(PROP_RESULTINGTRANSFORMATION, oldResultingTransformation, resultingTransformation);
    this.refreshImagePoints();
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

    jPanel1 = new javax.swing.JPanel();
    calibrationPanel1 = new com.t_oster.visicut.gui.beans.CalibrationPanel();
    okButton = new javax.swing.JButton();
    cancelButton = new javax.swing.JButton();
    sendButton = new javax.swing.JButton();
    captureButton = new javax.swing.JButton();

    setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
    setName("Form"); // NOI18N

    org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(com.t_oster.visicut.gui.VisicutApp.class).getContext().getResourceMap(CamCalibrationDialog.class);
    jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(resourceMap.getString("jPanel1.border.title"))); // NOI18N
    jPanel1.setName("jPanel1"); // NOI18N

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
      .addGap(0, 824, Short.MAX_VALUE)
    );
    calibrationPanel1Layout.setVerticalGroup(
      calibrationPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGap(0, 377, Short.MAX_VALUE)
    );

    javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
    jPanel1.setLayout(jPanel1Layout);
    jPanel1Layout.setHorizontalGroup(
      jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(jPanel1Layout.createSequentialGroup()
        .addContainerGap()
        .addComponent(calibrationPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        .addContainerGap())
    );
    jPanel1Layout.setVerticalGroup(
      jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(jPanel1Layout.createSequentialGroup()
        .addComponent(calibrationPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        .addContainerGap())
    );

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

    captureButton.setText(resourceMap.getString("captureButton.text")); // NOI18N
    captureButton.setName("captureButton"); // NOI18N
    captureButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        captureButtonActionPerformed(evt);
      }
    });

    javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
    getContentPane().setLayout(layout);
    layout.setHorizontalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
        .addContainerGap(473, Short.MAX_VALUE)
        .addComponent(captureButton)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(sendButton)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(cancelButton)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(okButton, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
        .addGap(15, 15, 15))
      .addGroup(layout.createSequentialGroup()
        .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        .addContainerGap())
    );
    layout.setVerticalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(layout.createSequentialGroup()
        .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
          .addComponent(cancelButton)
          .addComponent(okButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
          .addComponent(sendButton)
          .addComponent(captureButton)))
    );

    bindingGroup.bind();

    pack();
  }// </editor-fold>//GEN-END:initComponents

private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonActionPerformed
  this.setResultingTransformation(this.getCurrentTransformation());
  this.setVisible(false);
}//GEN-LAST:event_okButtonActionPerformed

private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
  this.refreshImagePoints();
  this.setVisible(false);
}//GEN-LAST:event_cancelButtonActionPerformed

private void sendButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sendButtonActionPerformed

  try
  {
    if (laserCutter == null)
    {
      throw new Exception("No Lasercutter selected");
    }
    VectorPart vp = new VectorPart(new LaserProperty());
    int size = 10;
    for (Point p : new Point[]
      {
        laserUpperLeft, laserLowerRight
      })
    {
      vp.moveto(p.x - size / 2, p.y);
      vp.lineto(p.x + size / 2, p.y);
      vp.moveto(p.x, p.y - size / 2);
      vp.lineto(p.x, p.y + size / 2);
    }
    LaserJob job = new LaserJob("Calibration", "VisiCut Calibration Page", "visicut", 500, null, vp, null);
    this.laserCutter.sendJob(job);
    JOptionPane.showMessageDialog(this, "Please press 'START' on the Lasercutter");
  }
  catch (Exception e)
  {
    JOptionPane.showMessageDialog(this, "Error sending Page:\n" + e.getLocalizedMessage(), "Error", JOptionPane.ERROR_MESSAGE);
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
        try
        {
          URL src = new URL(imageURL);
          if (src != null)
          {
            BufferedImage back = ImageIO.read(src);
            CamCalibrationDialog.this.setBackgroundImage(back);
          }
        }
        catch (Exception ex)
        {
          JOptionPane.showMessageDialog(CamCalibrationDialog.this, "Error loading Image:" + ex.getLocalizedMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
        CamCalibrationDialog.this.captureButton.setEnabled(true);
      }
    }.start();
}//GEN-LAST:event_captureButtonActionPerformed

  // Variables declaration - do not modify//GEN-BEGIN:variables
  private com.t_oster.visicut.gui.beans.CalibrationPanel calibrationPanel1;
  private javax.swing.JButton cancelButton;
  private javax.swing.JButton captureButton;
  private javax.swing.JPanel jPanel1;
  private javax.swing.JButton okButton;
  private javax.swing.JButton sendButton;
  private org.jdesktop.beansbinding.BindingGroup bindingGroup;
  // End of variables declaration//GEN-END:variables
}
