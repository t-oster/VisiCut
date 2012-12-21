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

  private String lengthUnit = "mm";
  public static final String PROP_LENGTHUNIT = "lengthUnit";

  public String getLengthUnit()
  {
    return lengthUnit;
  }

  public void setLengthUnit(String lengthUnit)
  {
    String oldLengthUnit = this.lengthUnit;
    this.lengthUnit = lengthUnit;
    this.updateRectanlgeText();
    firePropertyChange(PROP_LENGTHUNIT, oldLengthUnit, lengthUnit);
  }

  private String angleUnit = "deg";
  public static final String PROP_ANGLEUNIT = "angleUnit";

  public String getAngleUnit()
  {
    return angleUnit;
  }

  public void setAngleUnit(String angleUnit)
  {
    String oldAngleUnit = this.angleUnit;
    this.angleUnit = angleUnit;
    this.updateAngleText();
    firePropertyChange(PROP_ANGLEUNIT, oldAngleUnit, angleUnit);
  }

  private String angleToString(double angle)
  {
    if ("deg".equals(this.getAngleUnit()))
    {
      angle = Helper.angle2degree(angle);
    }
    return this.doubleToString(angle);
  }

  private String lengthToString(double mm)
  {
    double valueInUnit = mm;
    if ("cm".equals(this.getLengthUnit()))
    {
      valueInUnit /= 10;
    }
    else if ("in".equals(this.getLengthUnit()))
    {
      valueInUnit = Util.mm2inch(mm);
    }
    return doubleToString(valueInUnit);
  }

  private double stringToLength(String txt)
  {
    double valueInUnit = Double.parseDouble(txt);
    if ("cm".equals(this.getLengthUnit()))
    {
      valueInUnit *= 10;
    }
    else if ("in".equals(this.getLengthUnit()))
    {
      valueInUnit = Util.inch2mm(valueInUnit);
    }
    return valueInUnit;
  }

  private String doubleToString(double d)
  {
    String result = (""+d).replace(",", ".");
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

  private void updateAngleText()
  {
    this.tfAngle.setText(angleToString(this.angle));
  }

  private void updateRectanlgeText()
  {
    tfWidth.setText(lengthToString(this.rectangle.getWidth()));
    tfHeight.setText(lengthToString(this.rectangle.getHeight()));
    updateXYText();
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
    this.updateAngleText();
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
    tfX.setText(lengthToString(x));
    tfY.setText(lengthToString(y));
  }

  private Rectangle2D getRectangleFromTextfields()
  {
    try
    {
      double w = stringToLength(this.tfWidth.getText());
      double h = stringToLength(this.tfHeight.getText());
      double x = stringToLength(this.tfX.getText());
      double y = stringToLength(this.tfY.getText());
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
    bindingGroup = new org.jdesktop.beansbinding.BindingGroup();

    edgeRadioGroup = new javax.swing.ButtonGroup();
    tfHeight = new java.awt.TextField();
    tfWidth = new java.awt.TextField();
    lbY1 = new java.awt.Label();
    lbX1 = new java.awt.Label();
    label2 = new java.awt.Label();
    label3 = new java.awt.Label();
    tfAngle = new java.awt.TextField();
    lbY2 = new java.awt.Label();
    label5 = new java.awt.Label();
    jPanel1 = new javax.swing.JPanel();
    lbX = new java.awt.Label();
    lbY = new java.awt.Label();
    tfX = new java.awt.TextField();
    tfY = new java.awt.TextField();
    ancorPointPanel1 = new com.t_oster.visicut.gui.beans.AncorPointPanel();
    label1 = new java.awt.Label();
    label4 = new java.awt.Label();
    cbLengthUnit = new javax.swing.JComboBox();
    cbAngleUnit = new javax.swing.JComboBox();
    jLabel1 = new javax.swing.JLabel();
    jLabel2 = new javax.swing.JLabel();

    tfHeight.setText("textField2");

    tfWidth.setText("textField1");

    lbY1.setText("height:");

    lbX1.setText("width:");

    org.jdesktop.beansbinding.Binding binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbLengthUnit, org.jdesktop.beansbinding.ELProperty.create("${selectedItem}"), label2, org.jdesktop.beansbinding.BeanProperty.create("text"));
    bindingGroup.addBinding(binding);

    binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbLengthUnit, org.jdesktop.beansbinding.ELProperty.create("${selectedItem}"), label3, org.jdesktop.beansbinding.BeanProperty.create("text"));
    bindingGroup.addBinding(binding);

    tfAngle.setText("textField2");

    lbY2.setText("angle:");

    binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbAngleUnit, org.jdesktop.beansbinding.ELProperty.create("${selectedItem}"), label5, org.jdesktop.beansbinding.BeanProperty.create("text"));
    bindingGroup.addBinding(binding);

    java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/beans/resources/PositionPanel"); // NOI18N
    jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(bundle.getString("REFERENCE"))); // NOI18N

    lbX.setText("x:");

    lbY.setText("y:");

    tfX.setText("textField1");

    tfY.setText("textField2");

    ancorPointPanel1.addPropertyChangeListener(new java.beans.PropertyChangeListener()
    {
      public void propertyChange(java.beans.PropertyChangeEvent evt)
      {
        ancorPointPanel1PropertyChange(evt);
      }
    });

    binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbLengthUnit, org.jdesktop.beansbinding.ELProperty.create("${selectedItem}"), label1, org.jdesktop.beansbinding.BeanProperty.create("text"));
    bindingGroup.addBinding(binding);

    binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cbLengthUnit, org.jdesktop.beansbinding.ELProperty.create("${selectedItem}"), label4, org.jdesktop.beansbinding.BeanProperty.create("text"));
    bindingGroup.addBinding(binding);

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
            .addComponent(tfY, javax.swing.GroupLayout.DEFAULT_SIZE, 94, Short.MAX_VALUE)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(label4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
          .addGroup(jPanel1Layout.createSequentialGroup()
            .addComponent(lbY, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(tfX, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(label1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
        .addContainerGap())
    );
    jPanel1Layout.setVerticalGroup(
      jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(jPanel1Layout.createSequentialGroup()
        .addComponent(ancorPointPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        .addGap(0, 19, Short.MAX_VALUE))
      .addGroup(jPanel1Layout.createSequentialGroup()
        .addContainerGap()
        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
          .addComponent(tfY, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
          .addComponent(label4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
          .addComponent(lbX, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
          .addComponent(label1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
          .addComponent(tfX, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
          .addComponent(lbY, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
    );

    cbLengthUnit.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "mm", "cm", "in" }));

    binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, this, org.jdesktop.beansbinding.ELProperty.create("${lengthUnit}"), cbLengthUnit, org.jdesktop.beansbinding.BeanProperty.create("selectedItem"));
    bindingGroup.addBinding(binding);

    cbAngleUnit.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "deg", "rad" }));

    binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, this, org.jdesktop.beansbinding.ELProperty.create("${angleUnit}"), cbAngleUnit, org.jdesktop.beansbinding.BeanProperty.create("selectedItem"));
    bindingGroup.addBinding(binding);

    jLabel1.setText("Length Unit");

    jLabel2.setText("Angle Unit");

    javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
    this.setLayout(layout);
    layout.setHorizontalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(layout.createSequentialGroup()
        .addContainerGap()
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
          .addGroup(layout.createSequentialGroup()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
              .addComponent(lbY1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
              .addComponent(lbX1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
              .addComponent(lbY2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
              .addComponent(tfWidth, javax.swing.GroupLayout.DEFAULT_SIZE, 153, Short.MAX_VALUE)
              .addComponent(tfHeight, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
              .addComponent(tfAngle, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
              .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(label2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(label3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
              .addComponent(label5, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
          .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
              .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(jLabel1)
                .addComponent(jLabel2))
              .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
              .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                .addComponent(cbLengthUnit, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(cbAngleUnit, 0, 152, Short.MAX_VALUE)))
            .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
        .addContainerGap(35, Short.MAX_VALUE))
    );
    layout.setVerticalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
        .addContainerGap()
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
          .addComponent(cbLengthUnit, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
          .addComponent(jLabel1))
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
          .addComponent(cbAngleUnit, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
          .addComponent(jLabel2))
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 11, Short.MAX_VALUE)
        .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
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
          .addComponent(label5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        .addContainerGap())
    );

    bindingGroup.bind();
  }// </editor-fold>//GEN-END:initComponents

  private void ancorPointPanel1PropertyChange(java.beans.PropertyChangeEvent evt)//GEN-FIRST:event_ancorPointPanel1PropertyChange
  {//GEN-HEADEREND:event_ancorPointPanel1PropertyChange
    this.updateXYText();
  }//GEN-LAST:event_ancorPointPanel1PropertyChange

  // Variables declaration - do not modify//GEN-BEGIN:variables
  private com.t_oster.visicut.gui.beans.AncorPointPanel ancorPointPanel1;
  private javax.swing.JComboBox cbAngleUnit;
  private javax.swing.JComboBox cbLengthUnit;
  private javax.swing.ButtonGroup edgeRadioGroup;
  private javax.swing.JLabel jLabel1;
  private javax.swing.JLabel jLabel2;
  private javax.swing.JPanel jPanel1;
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
  private org.jdesktop.beansbinding.BindingGroup bindingGroup;
  // End of variables declaration//GEN-END:variables

  public void actionPerformed(ActionEvent ae)
  {
    if (ae.getSource().equals(tfAngle))
    {
      try
      {
        this.setAngle(textToAngle(tfAngle.getText()));
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

  private double textToAngle(String text)
  {
    double valueInUnit = Double.parseDouble(text);
    if ("deg".equals(this.getAngleUnit()))
    {
      valueInUnit = Helper.degree2angle(valueInUnit);
    }
    return valueInUnit;
  }
}
