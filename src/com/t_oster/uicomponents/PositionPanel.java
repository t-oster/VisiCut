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
package com.t_oster.uicomponents;

import com.t_oster.liblasercut.platform.Util;
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
      if (cbProportional.isSelected())
      {
        ignoreTextfieldUpdates = true;
        if (this.rectangle.getWidth() != 0 || this.getRectangle().getHeight() != 0)
        {
          double whfactor = this.rectangle.getHeight()/this.rectangle.getWidth();
          if (pce.getSource().equals(tfWidth))
          {
            tfHeight.setValue(tfWidth.getValue()*whfactor);
          }
          else if (pce.getSource().equals(tfHeight))
          {
            tfWidth.setValue(tfHeight.getValue()/whfactor);
          }
        }
        ignoreTextfieldUpdates = false;
      }
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
      this.angle = checkNaN(angle);
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
          x = this.rectangle.getWidth() != 0 ? this.rectangle.getCenterX() : this.rectangle.getMinX();
          y = this.rectangle.getMinY();
          break;
        case TOP_RIGHT:
          x = this.rectangle.getMaxX();
          y = this.rectangle.getMinY();
          break;
        case CENTER_LEFT:
          x = this.rectangle.getMinX();
          y = this.rectangle.getHeight() != 0 ? this.rectangle.getCenterY() : this.rectangle.getMinY();
          break;
        case CENTER_CENTER:
          x = this.rectangle.getWidth() != 0 ? this.rectangle.getCenterX() : this.rectangle.getMinX();
          y = this.rectangle.getHeight() != 0 ? this.rectangle.getCenterY() : this.rectangle.getMinY();
          break;
        case CENTER_RIGHT:
          x = this.rectangle.getMaxX();
          y = this.rectangle.getHeight() != 0 ? this.rectangle.getCenterY() : this.rectangle.getMinY();
          break;
          case BOTTOM_LEFT:
          x = this.rectangle.getMinX();
          y = this.rectangle.getMaxY();
          break;
        case BOTTOM_CENTER:
          x = this.rectangle.getWidth() != 0 ? this.rectangle.getCenterX() : this.rectangle.getMinX();
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
    boolean oldIgnoreTextfieldUpdates = ignoreTextfieldUpdates;
    ignoreTextfieldUpdates = true;
    tfX.setValue(checkNaN(x));
    tfY.setValue(checkNaN(y));
    ignoreTextfieldUpdates = oldIgnoreTextfieldUpdates;
  }

  /**
   * Checks d if it's = NaN and
   * returns 0 in this case, else d
   * @param d
   * @return d or 0 if d == NaN
   */
  private double checkNaN(double d)
  {
    return Double.isNaN(d) ? 0 : d;
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
      this.rectangle = new Rectangle2D.Double(checkNaN(rectangle.getX()), checkNaN(rectangle.getY()), checkNaN(rectangle.getWidth()), checkNaN(rectangle.getHeight()));
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
    ancorPointPanel1 = new com.t_oster.uicomponents.AncorPointPanel();
    tfX = new com.t_oster.uicomponents.LengthTextfield();
    tfY = new com.t_oster.uicomponents.LengthTextfield();
    tfHeight = new com.t_oster.uicomponents.LengthTextfield();
    tfAngle = new com.t_oster.uicomponents.AngleTextfield();
    tfWidth = new com.t_oster.uicomponents.LengthTextfield();
    cbProportional = new javax.swing.JCheckBox();

    java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("com/t_oster/uicomponents/resources/PositionPanel"); // NOI18N
    lbY1.setText(bundle.getString("HEIGHT")); // NOI18N

    lbX1.setText(bundle.getString("WIDTH")); // NOI18N

    lbY2.setText(bundle.getString("ANGLE")); // NOI18N

    jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(bundle.getString("REFERENCE"))); // NOI18N

    lbX.setText(bundle.getString("X")); // NOI18N

    lbY.setText(bundle.getString("Y")); // NOI18N

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
        .addGap(0, 21, Short.MAX_VALUE))
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

    cbProportional.setText(bundle.getString("PROPORTIONAL")); // NOI18N

    javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
    this.setLayout(layout);
    layout.setHorizontalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(layout.createSequentialGroup()
        .addContainerGap()
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
          .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
          .addGroup(layout.createSequentialGroup()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
              .addComponent(lbY1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
              .addComponent(lbX1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
              .addComponent(lbY2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
              .addComponent(tfAngle, javax.swing.GroupLayout.PREFERRED_SIZE, 191, javax.swing.GroupLayout.PREFERRED_SIZE)
              .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                  .addComponent(tfHeight, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                  .addComponent(tfWidth, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 85, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(cbProportional)))))
        .addContainerGap(25, Short.MAX_VALUE))
    );
    layout.setVerticalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(layout.createSequentialGroup()
        .addContainerGap()
        .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
          .addGroup(layout.createSequentialGroup()
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
              .addComponent(lbX1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
              .addComponent(tfWidth, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
              .addComponent(lbY1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
              .addComponent(tfHeight, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
          .addGroup(layout.createSequentialGroup()
            .addGap(23, 23, 23)
            .addComponent(cbProportional)))
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
          .addComponent(lbY2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
          .addComponent(tfAngle, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        .addContainerGap(74, Short.MAX_VALUE))
    );
  }// </editor-fold>//GEN-END:initComponents

  private void ancorPointPanel1PropertyChange(java.beans.PropertyChangeEvent evt)//GEN-FIRST:event_ancorPointPanel1PropertyChange
  {//GEN-HEADEREND:event_ancorPointPanel1PropertyChange
    this.updateXYText();
  }//GEN-LAST:event_ancorPointPanel1PropertyChange

  // Variables declaration - do not modify//GEN-BEGIN:variables
  private com.t_oster.uicomponents.AncorPointPanel ancorPointPanel1;
  private javax.swing.JCheckBox cbProportional;
  private javax.swing.JPanel jPanel1;
  private java.awt.Label lbX;
  private java.awt.Label lbX1;
  private java.awt.Label lbY;
  private java.awt.Label lbY1;
  private java.awt.Label lbY2;
  private com.t_oster.uicomponents.AngleTextfield tfAngle;
  private com.t_oster.uicomponents.LengthTextfield tfHeight;
  private com.t_oster.uicomponents.LengthTextfield tfWidth;
  private com.t_oster.uicomponents.LengthTextfield tfX;
  private com.t_oster.uicomponents.LengthTextfield tfY;
  // End of variables declaration//GEN-END:variables

}
