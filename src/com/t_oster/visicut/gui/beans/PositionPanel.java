/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.visicut.gui.beans;

import com.t_oster.liblasercut.platform.Util;
import com.t_oster.visicut.misc.Helper;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 *
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class PositionPanel extends javax.swing.JPanel implements PropertyChangeListener
{

  /**
   * Creates new form PositionPanel
   */
  public PositionPanel()
  {
    initComponents();
    tfX.addPropertyChangeListener(UnitTextfield.PROP_VALUE, this);
    tfY.addPropertyChangeListener(UnitTextfield.PROP_VALUE, this);
    tfWidth.addPropertyChangeListener(UnitTextfield.PROP_VALUE, this);
    tfHeight.addPropertyChangeListener(UnitTextfield.PROP_VALUE, this);
    tfAngle.addPropertyChangeListener(UnitTextfield.PROP_VALUE, this);
  }

  private boolean ignoreTextfieldUpdates = false;
  public void propertyChange(PropertyChangeEvent pce)
  {
    if (pce.getSource().equals(tfAngle))
    {
      double oldAngle = this.angle;
      this.angle = tfAngle.getValue();
      firePropertyChange(PROP_ANGLE, oldAngle, angle);
    }
    else if (!ignoreTextfieldUpdates)
    {
      Rectangle2D oldRectangle = this.rectangle;
      this.rectangle = this.getRectangleFromTextfields();
      firePropertyChange(PROP_RECTANGLE, oldRectangle, this.rectangle);
    }
  }

  private double angle = 0;
  public static final String PROP_ANGLE = "angle";

  /**
   * Get the value of angle
   *
   * @return the value of angle
   */
  public double getAngle()
  {
    return angle;
  }

  private void updateAngleText()
  {
    this.tfAngle.setValue(this.angle);
  }

  private void updateRectanlgeText()
  {
    this.ignoreTextfieldUpdates = true;
    tfWidth.setValue(this.rectangle.getWidth());
    tfHeight.setValue(this.rectangle.getHeight());
    updateXYText();
    this.ignoreTextfieldUpdates = false;
  }

  /**
   * Set the value of angle
   *
   * @param angle new value of angle
   */
  public void setAngle(double angle)
  {
    double oldAngle = this.angle;
    if (oldAngle != angle)
    {
      this.angle = angle;
      this.updateAngleText();
      firePropertyChange(PROP_ANGLE, oldAngle, angle);
    }
  }

  private Rectangle2D rectangle = null;
  public static final String PROP_RECTANGLE = "rectangle";

  /**
   * Get the value of rectangle
   *
   * @return the value of rectangle
   */
  public Rectangle2D getRectangle()
  {
    return rectangle;
  }

  private void updateXYText()
  {
    AncorPointPanel.Position p = this.ancorPointPanel1.getPosition();
    double x,y;
    if (this.rectangle == null)
    {
     x = 0;
     y = 0;
    }
    else
    {
      switch (p)
      {
        case TOP_LEFT:
          x = this.rectangle.getMinX();
          y = this.rectangle.getMinY();
          break;
        case TOP_CENTER:
          x = this.rectangle.getCenterX();
          y = this.rectangle.getMinY();
          break;
        case TOP_RIGHT:
          x = this.rectangle.getMaxX();
          y = this.rectangle.getMinY();
          break;
        case CENTER_LEFT:
          x = this.rectangle.getMinX();
          y = this.rectangle.getCenterY();
          break;
        case CENTER_CENTER:
          x = this.rectangle.getCenterX();
          y = this.rectangle.getCenterY();
          break;
        case CENTER_RIGHT:
          x = this.rectangle.getMaxX();
          y = this.rectangle.getCenterY();
          break;
          case BOTTOM_LEFT:
          x = this.rectangle.getMinX();
          y = this.rectangle.getMaxY();
          break;
        case BOTTOM_CENTER:
          x = this.rectangle.getCenterX();
          y = this.rectangle.getMaxY();
          break;
        case BOTTOM_RIGHT:
          x = this.rectangle.getMaxX();
          y = this.rectangle.getMaxY();
          break;
        default:
          x = 0;
          y = 0;
          break;
      }
    }
    tfX.setValue(x);
    tfY.setValue(y);
  }

  private Rectangle2D getRectangleFromTextfields()
  {
    try
    {
      double w = tfWidth.getValue();
      double h = tfHeight.getValue();
      double x = tfX.getValue();
      double y = tfY.getValue();
      switch (this.ancorPointPanel1.getPosition())
      {
        case TOP_LEFT:
          break;
        case TOP_CENTER:
          x -= w/2;
          break;
        case TOP_RIGHT:
          x -= w;
          break;
        case CENTER_LEFT:
          y -= h/2;
          break;
        case CENTER_CENTER:
          y -= h/2;
          x -= w/2;
          break;
        case CENTER_RIGHT:
          y -= h/2;
          x -= w;
          break;
        case BOTTOM_LEFT:
          y -= h;
          break;
        case BOTTOM_CENTER:
          y -= h;
          x -= w/2;
          break;
        case BOTTOM_RIGHT:
          y -= h;
          x -= w;
          break;
        default:
          x = 0;
          y = 0;
      }
      return new Rectangle2D.Double(x,y,w,h);
    }
    catch (NumberFormatException e)
    {
      return this.rectangle;
    }
  }
  /**
   * Set the value of rectangle
   *
   * @param rectangle new value of rectangle
   */
  public void setRectangle(Rectangle2D rectangle)
  {
    Rectangle2D oldRectangle = this.rectangle;
    if (Util.differ(oldRectangle, rectangle))
    {
      this.rectangle = new Rectangle2D.Double(rectangle.getX(), rectangle.getY(), rectangle.getWidth(), rectangle.getHeight());
      this.updateRectanlgeText();
      firePropertyChange(PROP_RECTANGLE, oldRectangle, this.rectangle);
    }
  }


  /**
   * This method is called from within the constructor to initialize the form.
   * WARNING: Do NOT modify this code. The content of this method is always
   * regenerated by the Form Editor.
   */
  @SuppressWarnings("unchecked")
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents()
  {

    lbY1 = new java.awt.Label();
    lbX1 = new java.awt.Label();
    lbY2 = new java.awt.Label();
    jPanel1 = new javax.swing.JPanel();
    lbX = new java.awt.Label();
    lbY = new java.awt.Label();
    ancorPointPanel1 = new com.t_oster.visicut.gui.beans.AncorPointPanel();
    tfX = new com.t_oster.visicut.gui.beans.LengthTextfield();
    tfY = new com.t_oster.visicut.gui.beans.LengthTextfield();
    tfHeight = new com.t_oster.visicut.gui.beans.LengthTextfield();
    tfAngle = new com.t_oster.visicut.gui.beans.AngleTextfield();
    tfWidth = new com.t_oster.visicut.gui.beans.LengthTextfield();

    lbY1.setText("height:");

    lbX1.setText("width:");

    lbY2.setText("angle:");

    java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/beans/resources/PositionPanel"); // NOI18N
    jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(bundle.getString("REFERENCE"))); // NOI18N

    lbX.setText("x:");

    lbY.setText("y:");

    ancorPointPanel1.addPropertyChangeListener(new java.beans.PropertyChangeListener()
    {
      public void propertyChange(java.beans.PropertyChangeEvent evt)
      {
        ancorPointPanel1PropertyChange(evt);
      }
    });

    javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
    jPanel1.setLayout(jPanel1Layout);
    jPanel1Layout.setHorizontalGroup(
      jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(jPanel1Layout.createSequentialGroup()
        .addComponent(ancorPointPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
          .addGroup(jPanel1Layout.createSequentialGroup()
            .addComponent(lbX, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(tfX, javax.swing.GroupLayout.DEFAULT_SIZE, 128, Short.MAX_VALUE))
          .addGroup(jPanel1Layout.createSequentialGroup()
            .addComponent(lbY, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(tfY, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
        .addContainerGap())
    );
    jPanel1Layout.setVerticalGroup(
      jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(jPanel1Layout.createSequentialGroup()
        .addComponent(ancorPointPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        .addGap(0, 20, Short.MAX_VALUE))
      .addGroup(jPanel1Layout.createSequentialGroup()
        .addContainerGap()
        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
          .addComponent(lbX, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
          .addGroup(jPanel1Layout.createSequentialGroup()
            .addComponent(tfX, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addGap(1, 1, 1)))
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
          .addComponent(lbY, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
          .addGroup(jPanel1Layout.createSequentialGroup()
            .addGap(1, 1, 1)
            .addComponent(tfY, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
    );

    javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
    this.setLayout(layout);
    layout.setHorizontalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(layout.createSequentialGroup()
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
          .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
          .addGroup(layout.createSequentialGroup()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
              .addComponent(lbY1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
              .addComponent(lbX1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
              .addComponent(lbY2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
              .addComponent(tfHeight, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
              .addComponent(tfAngle, javax.swing.GroupLayout.DEFAULT_SIZE, 191, Short.MAX_VALUE)
              .addGroup(layout.createSequentialGroup()
                .addComponent(tfWidth, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(2, 2, 2)))))
        .addGap(0, 47, Short.MAX_VALUE))
    );
    layout.setVerticalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
        .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
          .addComponent(lbX1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
          .addComponent(tfWidth, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
          .addComponent(lbY1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
          .addComponent(tfHeight, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
          .addComponent(lbY2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
          .addComponent(tfAngle, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        .addContainerGap(87, Short.MAX_VALUE))
    );
  }// </editor-fold>//GEN-END:initComponents

  private void ancorPointPanel1PropertyChange(java.beans.PropertyChangeEvent evt)//GEN-FIRST:event_ancorPointPanel1PropertyChange
  {//GEN-HEADEREND:event_ancorPointPanel1PropertyChange
    this.updateXYText();
  }//GEN-LAST:event_ancorPointPanel1PropertyChange

  // Variables declaration - do not modify//GEN-BEGIN:variables
  private com.t_oster.visicut.gui.beans.AncorPointPanel ancorPointPanel1;
  private javax.swing.JPanel jPanel1;
  private java.awt.Label lbX;
  private java.awt.Label lbX1;
  private java.awt.Label lbY;
  private java.awt.Label lbY1;
  private java.awt.Label lbY2;
  private com.t_oster.visicut.gui.beans.AngleTextfield tfAngle;
  private com.t_oster.visicut.gui.beans.LengthTextfield tfHeight;
  private com.t_oster.visicut.gui.beans.LengthTextfield tfWidth;
  private com.t_oster.visicut.gui.beans.LengthTextfield tfX;
  private com.t_oster.visicut.gui.beans.LengthTextfield tfY;
  // End of variables declaration//GEN-END:variables

}
