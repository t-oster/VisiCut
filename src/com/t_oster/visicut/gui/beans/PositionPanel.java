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

/**
 *
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class PositionPanel extends javax.swing.JPanel implements ActionListener
{

  private String doubleToString(double mm)
  {
    String result = (""+mm).replace(",", ".");
    if (result.contains("."))
    {
      String[] parts = result.split("\\.");
      result = parts[0]+"."+parts[1].substring(0, Math.min(parts[1].length(),2));
    }
    if (result.equals("-0.0"))
    {
      result = "0.0";
    }
    return result;
  }
  
  /**
   * Creates new form PositionPanel
   */
  public PositionPanel()
  {
    initComponents();
    tfX.addActionListener(this);
    tfY.addActionListener(this);
    tfWidth.addActionListener(this);
    tfHeight.addActionListener(this);
    tfAngle.addActionListener(this);
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

  /**
   * Set the value of angle
   *
   * @param angle new value of angle
   */
  public void setAngle(double angle)
  {
    double oldAngle = this.angle;
    this.angle = angle;
    this.tfAngle.setText(doubleToString(Helper.angle2degree(this.angle)));
    firePropertyChange(PROP_ANGLE, oldAngle, angle);
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

  private void updateXandYtoPosition(AncorPointPanel.Position p)
  {
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
    tfX.setText(doubleToString(x));
    tfY.setText(doubleToString(y));
  }
  
  private Rectangle2D getRectangleFromTextfields()
  {
    try
    {
      double w = Double.parseDouble(this.tfWidth.getText());
      double h = Double.parseDouble(this.tfHeight.getText());
      double x = Double.parseDouble(this.tfX.getText());
      double y = Double.parseDouble(this.tfY.getText());
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
      tfWidth.setText(doubleToString(this.rectangle.getWidth()));
      tfHeight.setText(doubleToString(this.rectangle.getHeight()));
      updateXandYtoPosition(this.ancorPointPanel1.getPosition());
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

    edgeRadioGroup = new javax.swing.ButtonGroup();
    ancorPointPanel1 = new com.t_oster.visicut.gui.beans.AncorPointPanel();
    lbX = new java.awt.Label();
    lbY = new java.awt.Label();
    tfX = new java.awt.TextField();
    tfY = new java.awt.TextField();
    tfHeight = new java.awt.TextField();
    tfWidth = new java.awt.TextField();
    lbY1 = new java.awt.Label();
    lbX1 = new java.awt.Label();
    label1 = new java.awt.Label();
    label2 = new java.awt.Label();
    label3 = new java.awt.Label();
    label4 = new java.awt.Label();
    tfAngle = new java.awt.TextField();
    lbY2 = new java.awt.Label();
    label5 = new java.awt.Label();

    ancorPointPanel1.addPropertyChangeListener(new java.beans.PropertyChangeListener()
    {
      public void propertyChange(java.beans.PropertyChangeEvent evt)
      {
        ancorPointPanel1PropertyChange(evt);
      }
    });

    lbX.setText("x:");

    lbY.setText("y:");

    tfX.setText("textField1");

    tfY.setText("textField2");

    tfHeight.setText("textField2");

    tfWidth.setText("textField1");

    lbY1.setText("height:");

    lbX1.setText("width:");

    label1.setText("mm");

    label2.setText("mm");

    label3.setText("mm");

    label4.setText("mm");

    tfAngle.setText("textField2");

    lbY2.setText("angle:");

    label5.setText("deg");

    javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
    this.setLayout(layout);
    layout.setHorizontalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(layout.createSequentialGroup()
        .addContainerGap()
        .addComponent(ancorPointPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
          .addComponent(lbY, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
          .addComponent(lbX, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
          .addComponent(lbY1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
          .addComponent(lbX1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
          .addComponent(lbY2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        .addGap(28, 28, 28)
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
          .addComponent(tfWidth, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
          .addComponent(tfHeight, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
          .addComponent(tfX, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
          .addComponent(tfY, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
          .addComponent(tfAngle, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
          .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(label1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addComponent(label2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addComponent(label3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addComponent(label4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
          .addComponent(label5, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        .addContainerGap(20, Short.MAX_VALUE))
    );
    layout.setVerticalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(layout.createSequentialGroup()
        .addContainerGap()
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
          .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
              .addComponent(lbX, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
              .addComponent(tfX, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
              .addComponent(label1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
              .addComponent(lbY, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
              .addComponent(tfY, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
              .addComponent(label4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
              .addComponent(lbX1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
              .addComponent(tfWidth, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
              .addComponent(label2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
              .addComponent(lbY1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
              .addComponent(tfHeight, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
              .addComponent(label3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
              .addComponent(lbY2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
              .addComponent(tfAngle, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
              .addComponent(label5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
          .addComponent(ancorPointPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
    );
  }// </editor-fold>//GEN-END:initComponents

  private void ancorPointPanel1PropertyChange(java.beans.PropertyChangeEvent evt)//GEN-FIRST:event_ancorPointPanel1PropertyChange
  {//GEN-HEADEREND:event_ancorPointPanel1PropertyChange
    this.updateXandYtoPosition(this.ancorPointPanel1.getPosition());
  }//GEN-LAST:event_ancorPointPanel1PropertyChange

  // Variables declaration - do not modify//GEN-BEGIN:variables
  private com.t_oster.visicut.gui.beans.AncorPointPanel ancorPointPanel1;
  private javax.swing.ButtonGroup edgeRadioGroup;
  private java.awt.Label label1;
  private java.awt.Label label2;
  private java.awt.Label label3;
  private java.awt.Label label4;
  private java.awt.Label label5;
  private java.awt.Label lbX;
  private java.awt.Label lbX1;
  private java.awt.Label lbY;
  private java.awt.Label lbY1;
  private java.awt.Label lbY2;
  private java.awt.TextField tfAngle;
  private java.awt.TextField tfHeight;
  private java.awt.TextField tfWidth;
  private java.awt.TextField tfX;
  private java.awt.TextField tfY;
  // End of variables declaration//GEN-END:variables

  public void actionPerformed(ActionEvent ae)
  {
    if (ae.getSource().equals(tfAngle))
    {
      try
      {
        this.setAngle(Helper.degree2angle(Double.parseDouble(tfAngle.getText())));
      }
      catch (NumberFormatException e)
      {
        this.tfAngle.setText(doubleToString(this.angle));
      }
    }
    else
    {
      this.setRectangle(this.getRectangleFromTextfields());
    }
  }
}
