/**
 * This file is part of VisiCut.
 * 
 *     VisiCut is free software: you can redistribute it and/or modify
 *     it under the terms of the Lesser GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *    VisiCut is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     Lesser GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with VisiCut.  If not, see <http://www.gnu.org/licenses/>.
 **/
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * EditRasterProfileDialog.java
 *
 * Created on 06.09.2011, 10:34:59
 */
package com.t_oster.visicut.gui;

import com.t_oster.liblasercut.BlackWhiteRaster;
import com.t_oster.liblasercut.BlackWhiteRaster.DitherAlgorithm;
import com.t_oster.liblasercut.LaserProperty;
import com.t_oster.visicut.gui.beans.EditableTableProvider;
import com.t_oster.visicut.model.LaserPropertyBean;
import com.t_oster.visicut.model.RasterProfile;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author thommy
 */
public class EditRasterProfileDialog extends javax.swing.JDialog implements EditableTableProvider
{

  public Object getNewInstance()
  {
    return new LaserProperty();
  }

  public Object editObject(Object o)
  {
    if (o instanceof LaserProperty)
    {
      EditLaserPropertyPanel d = new EditLaserPropertyPanel();
      d.setLaserProperty(new LaserPropertyBean(((LaserProperty) o).clone()));
      d.setShowFrequency(false);
      if (JOptionPane.showConfirmDialog(this, d, "Edit Laser Property", JOptionPane.OK_CANCEL_OPTION)== JOptionPane.OK_OPTION)
      {
        return d.getLaserProperty().getLaserProperty();
      }
      else
      {
        return null;
      }
    }
    return o;
  }

  private class LaserPropertiesTableModel extends DefaultTableModel
  {

    public void setRasterProfile(RasterProfile rp)
    {
      this.rp = rp;
    }
    private RasterProfile rp = null;
    private String[] columnNames = new String[]
    {
      "Power", "Speed", "Focus"
    };

    @Override
    public int getColumnCount()
    {
      return columnNames.length;
    }

    @Override
    public String getColumnName(int i)
    {
      return columnNames[i];
    }

    @Override
    public Object getValueAt(int y, int x)
    {
      switch (x)
      {
        case 0:
          return rp.getLaserProperties().get(y).getPower();
        case 1:
          return rp.getLaserProperties().get(y).getSpeed();
        case 2:
          return rp.getLaserProperties().get(y).getFocus();
      }
      return null;
    }

    @Override
    public boolean isCellEditable(int i, int i1)
    {
      return true;
    }

    @Override
    public void setValueAt(Object o, int y, int x)
    {
      switch (x)
      {
        case 0:
          rp.getLaserProperties().get(y).setPower(Integer.parseInt(o.toString()));
          return;
        case 1:
          rp.getLaserProperties().get(y).setSpeed(Integer.parseInt(o.toString()));
          return;
        case 2:
          rp.getLaserProperties().get(y).setFocus(Float.parseFloat(o.toString()));
          return;
      }
    }

    @Override
    public int getRowCount()
    {
      return rp == null ? 0 : rp.getLaserProperties().size();
    }
  }
  protected RasterProfile rasterProfile = null;
  public static final String PROP_RASTERPROFILE = "rasterProfile";

  /**
   * Get the value of RasterProfile
   *
   * @return the value of RasterProfile
   */
  public RasterProfile getRasterProfile()
  {
    return rasterProfile;
  }

  /**
   * Set the value of RasterProfile
   *
   * @param rasterProfile new value of RasterProfile
   */
  public void setRasterProfile(RasterProfile rasterProfile)
  {
    RasterProfile oldRasterProfile = this.rasterProfile;
    this.rasterProfile = rasterProfile;
    firePropertyChange(PROP_RASTERPROFILE, oldRasterProfile, rasterProfile);
    if (rasterProfile == null)
    {
      this.setCurrentRasterProfile(new RasterProfile());
    }
    else
    {
      this.setCurrentRasterProfile((RasterProfile) rasterProfile.clone());
    }
  }
  protected RasterProfile currentRasterProfile = null;
  public static final String PROP_CURRENTRASTERPROFILE = "currentRasterProfile";

  /**
   * Get the value of currentRasterProfile
   *
   * @return the value of currentRasterProfile
   */
  public RasterProfile getCurrentRasterProfile()
  {
    return currentRasterProfile;
  }

  /**
   * Set the value of currentRasterProfile
   *
   * @param currentRasterProfile new value of currentRasterProfile
   */
  public void setCurrentRasterProfile(RasterProfile currentRasterProfile)
  {
    RasterProfile oldCurrentRasterProfile = this.currentRasterProfile;
    this.currentRasterProfile = currentRasterProfile;
    firePropertyChange(PROP_CURRENTRASTERPROFILE, oldCurrentRasterProfile, currentRasterProfile);
    this.listModel.setRasterProfile(currentRasterProfile);
  }
  private LaserPropertiesTableModel listModel = new LaserPropertiesTableModel();

  /** Creates new form EditRasterProfileDialog */
  public EditRasterProfileDialog(java.awt.Frame parent, boolean modal)
  {
    super(parent, modal);
    initComponents();
    jComboBox1.removeAllItems();
    for (DitherAlgorithm a: BlackWhiteRaster.DitherAlgorithm.values())
    {
      jComboBox1.addItem(a);
    }
    this.editableTablePanel1.setTableModel(listModel);
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

    jLabel3 = new javax.swing.JLabel();
    jButton4 = new javax.swing.JButton();
    jButton3 = new javax.swing.JButton();
    jLabel5 = new javax.swing.JLabel();
    jLabel4 = new javax.swing.JLabel();
    jTextField2 = new javax.swing.JTextField();
    jLabel2 = new javax.swing.JLabel();
    jLabel1 = new javax.swing.JLabel();
    jTextField1 = new javax.swing.JTextField();
    jComboBox1 = new javax.swing.JComboBox();
    selectThumbnailButton1 = new com.t_oster.visicut.gui.beans.SelectThumbnailButton();
    chooseColorButton1 = new com.t_oster.visicut.gui.beans.ChooseColorButton();
    editableTablePanel1 = new com.t_oster.visicut.gui.beans.EditableTablePanel();
    jCheckBox1 = new javax.swing.JCheckBox();
    jSlider1 = new javax.swing.JSlider();
    jLabel6 = new javax.swing.JLabel();

    setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
    setName("Form"); // NOI18N

    org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(com.t_oster.visicut.gui.VisicutApp.class).getContext().getResourceMap(EditRasterProfileDialog.class);
    jLabel3.setText(resourceMap.getString("jLabel3.text")); // NOI18N
    jLabel3.setName("jLabel3"); // NOI18N

    jButton4.setText(resourceMap.getString("jButton4.text")); // NOI18N
    jButton4.setName("jButton4"); // NOI18N
    jButton4.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        jButton4ActionPerformed(evt);
      }
    });

    jButton3.setText(resourceMap.getString("jButton3.text")); // NOI18N
    jButton3.setName("jButton3"); // NOI18N
    jButton3.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        jButton3ActionPerformed(evt);
      }
    });

    jLabel5.setText(resourceMap.getString("jLabel5.text")); // NOI18N
    jLabel5.setName("jLabel5"); // NOI18N

    jLabel4.setText(resourceMap.getString("jLabel4.text")); // NOI18N
    jLabel4.setName("jLabel4"); // NOI18N

    jTextField2.setName("jTextField2"); // NOI18N

    org.jdesktop.beansbinding.Binding binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, this, org.jdesktop.beansbinding.ELProperty.create("${currentRasterProfile.description}"), jTextField2, org.jdesktop.beansbinding.BeanProperty.create("text"), "desc");
    bindingGroup.addBinding(binding);

    jLabel2.setText(resourceMap.getString("jLabel2.text")); // NOI18N
    jLabel2.setName("jLabel2"); // NOI18N

    jLabel1.setText(resourceMap.getString("jLabel1.text")); // NOI18N
    jLabel1.setName("jLabel1"); // NOI18N

    jTextField1.setName("jTextField1"); // NOI18N

    binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, this, org.jdesktop.beansbinding.ELProperty.create("${currentRasterProfile.name}"), jTextField1, org.jdesktop.beansbinding.BeanProperty.create("text"), "Name");
    bindingGroup.addBinding(binding);

    jComboBox1.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
    jComboBox1.setName("jComboBox1"); // NOI18N

    binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, this, org.jdesktop.beansbinding.ELProperty.create("${currentRasterProfile.ditherAlgorithm}"), jComboBox1, org.jdesktop.beansbinding.BeanProperty.create("selectedItem"));
    bindingGroup.addBinding(binding);

    selectThumbnailButton1.setName("selectThumbnailButton1"); // NOI18N

    binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, this, org.jdesktop.beansbinding.ELProperty.create("${currentRasterProfile.thumbnailPath}"), selectThumbnailButton1, org.jdesktop.beansbinding.BeanProperty.create("thumbnailPath"), "thumbnailbt");
    bindingGroup.addBinding(binding);

    chooseColorButton1.setName("chooseColorButton1"); // NOI18N

    binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, this, org.jdesktop.beansbinding.ELProperty.create("${currentRasterProfile.color}"), chooseColorButton1, org.jdesktop.beansbinding.BeanProperty.create("selectedColor"), "color");
    bindingGroup.addBinding(binding);

    editableTablePanel1.setName("editableTablePanel1"); // NOI18N
    editableTablePanel1.setProvider(this);

    binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, this, org.jdesktop.beansbinding.ELProperty.create("${currentRasterProfile.laserProperties}"), editableTablePanel1, org.jdesktop.beansbinding.BeanProperty.create("objects"), "laserprops");
    bindingGroup.addBinding(binding);

    jCheckBox1.setText(resourceMap.getString("jCheckBox1.text")); // NOI18N
    jCheckBox1.setName("jCheckBox1"); // NOI18N

    binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, this, org.jdesktop.beansbinding.ELProperty.create("${currentRasterProfile.invertColors}"), jCheckBox1, org.jdesktop.beansbinding.BeanProperty.create("selected"), "invcolors");
    bindingGroup.addBinding(binding);

    jSlider1.setMaximum(255);
    jSlider1.setMinimum(-255);
    jSlider1.setToolTipText(resourceMap.getString("jSlider1.toolTipText")); // NOI18N
    jSlider1.setValue(0);
    jSlider1.setName("jSlider1"); // NOI18N

    binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, this, org.jdesktop.beansbinding.ELProperty.create("${currentRasterProfile.colorShift}"), jSlider1, org.jdesktop.beansbinding.BeanProperty.create("value"), "colorshift");
    bindingGroup.addBinding(binding);

    jLabel6.setText(resourceMap.getString("jLabel6.text")); // NOI18N
    jLabel6.setName("jLabel6"); // NOI18N

    javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
    getContentPane().setLayout(layout);
    layout.setHorizontalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(layout.createSequentialGroup()
        .addContainerGap()
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
          .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
              .addComponent(jLabel2)
              .addComponent(jLabel4)
              .addComponent(selectThumbnailButton1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGap(28, 28, 28)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
              .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(jComboBox1, 0, 201, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel5)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(chooseColorButton1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
              .addComponent(jTextField1, javax.swing.GroupLayout.DEFAULT_SIZE, 329, Short.MAX_VALUE)
              .addComponent(jTextField2, javax.swing.GroupLayout.PREFERRED_SIZE, 300, javax.swing.GroupLayout.PREFERRED_SIZE)
              .addComponent(jLabel1)))
          .addComponent(editableTablePanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 439, Short.MAX_VALUE)
          .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
            .addComponent(jButton4)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(jButton3))
          .addComponent(jLabel3)
          .addGroup(layout.createSequentialGroup()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
              .addComponent(jLabel6)
              .addComponent(jCheckBox1))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(jSlider1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
        .addContainerGap())
    );
    layout.setVerticalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(layout.createSequentialGroup()
        .addContainerGap()
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
          .addGroup(layout.createSequentialGroup()
            .addComponent(jLabel1)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
          .addComponent(selectThumbnailButton1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
          .addComponent(jLabel2)
          .addComponent(jTextField2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        .addGap(8, 8, 8)
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
          .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
          .addComponent(jLabel4)
          .addComponent(jLabel5)
          .addComponent(chooseColorButton1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        .addGap(18, 18, 18)
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
          .addGroup(layout.createSequentialGroup()
            .addComponent(jCheckBox1)
            .addGap(18, 18, 18)
            .addComponent(jLabel6))
          .addComponent(jSlider1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        .addGap(19, 19, 19)
        .addComponent(jLabel3)
        .addGap(18, 18, 18)
        .addComponent(editableTablePanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 203, Short.MAX_VALUE)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
          .addComponent(jButton3)
          .addComponent(jButton4))
        .addContainerGap())
    );

    bindingGroup.bind();

    pack();
  }// </editor-fold>//GEN-END:initComponents

  private void jButton4ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButton4ActionPerformed
  {//GEN-HEADEREND:event_jButton4ActionPerformed

    this.setRasterProfile(null);     this.setVisible(false);   }//GEN-LAST:event_jButton4ActionPerformed

  private void jButton3ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButton3ActionPerformed
  {//GEN-HEADEREND:event_jButton3ActionPerformed

    this.setRasterProfile(this.getCurrentRasterProfile());     this.setVisible(false);   }//GEN-LAST:event_jButton3ActionPerformed
  /**
   * @param args the command line arguments
   */
  // Variables declaration - do not modify//GEN-BEGIN:variables
  private com.t_oster.visicut.gui.beans.ChooseColorButton chooseColorButton1;
  private com.t_oster.visicut.gui.beans.EditableTablePanel editableTablePanel1;
  private javax.swing.JButton jButton3;
  private javax.swing.JButton jButton4;
  private javax.swing.JCheckBox jCheckBox1;
  private javax.swing.JComboBox jComboBox1;
  private javax.swing.JLabel jLabel1;
  private javax.swing.JLabel jLabel2;
  private javax.swing.JLabel jLabel3;
  private javax.swing.JLabel jLabel4;
  private javax.swing.JLabel jLabel5;
  private javax.swing.JLabel jLabel6;
  private javax.swing.JSlider jSlider1;
  private javax.swing.JTextField jTextField1;
  private javax.swing.JTextField jTextField2;
  private com.t_oster.visicut.gui.beans.SelectThumbnailButton selectThumbnailButton1;
  private org.jdesktop.beansbinding.BindingGroup bindingGroup;
  // End of variables declaration//GEN-END:variables
}
