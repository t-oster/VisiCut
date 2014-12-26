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
 * ManageLasercuttersDialog.java
 *
 * Created on 06.09.2011, 21:24:37
 */
package com.t_oster.visicut.gui;

import com.t_oster.liblasercut.LaserCutter;
import com.t_oster.uicomponents.EditableTableProvider;
import com.t_oster.visicut.managers.PreferencesManager;
import com.t_oster.visicut.model.LaserDevice;
import java.beans.PropertyChangeSupport;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class ManageLasercuttersDialog extends javax.swing.JDialog implements EditableTableProvider
{

  protected List<LaserDevice> laserCutters = null;
  public static final String PROP_LASERCUTTERS = "laserCutters";

  /**
   * Get the value of laserCutters
   *
   * @return the value of laserCutters
   */
  public List<LaserDevice> getLaserCutters()
  {
    if (laserCutters == null) {
      return null;
    } else {
      return new LinkedList<LaserDevice>(laserCutters);
    }
  }

  /**
   * Set the value of laserCutters
   *
   * @param laserCutters new value of laserCutters
   */
  public void setLaserCutters(List<LaserDevice> laserCutters)
  {
    List<LaserDevice> oldLaserCutters = this.laserCutters;
    this.laserCutters = laserCutters;
    firePropertyChange(PROP_LASERCUTTERS, oldLaserCutters, laserCutters);
    List<LaserDevice> cur = new LinkedList<LaserDevice>();
    if (laserCutters != null)
    {
      for (LaserDevice l : laserCutters)
      {
        cur.add(((LaserDevice) l).clone());
      }
    }
    this.setCurrentLaserCutters(cur);
  }
  protected List<LaserDevice> currentLaserCutters = null;
  public static final String PROP_CURRENTLASERCUTTERS = "currentLaserCutters";

  /**
   * Get the value of currentLaserCutters
   *
   * @return the value of currentLaserCutters
   */
  public List<LaserDevice> getCurrentLaserCutters()
  {
    return currentLaserCutters;
  }

  /**
   * Set the value of currentLaserCutters
   *
   * @param currentLaserCutters new value of currentLaserCutters
   */
  public void setCurrentLaserCutters(List<LaserDevice> currentLaserCutters)
  {
    List<LaserDevice> oldCurrentLaserCutters = this.currentLaserCutters;
    this.currentLaserCutters = currentLaserCutters;
    firePropertyChange(PROP_CURRENTLASERCUTTERS, oldCurrentLaserCutters, currentLaserCutters);
  }
  private DefaultTableModel model = new DefaultTableModel()
  {

    private String[] columns = new String[]
    {
      java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/resources/ManageLasercuttersDialog").getString("NAME"), java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/resources/ManageLasercuttersDialog").getString("DRIVER")
    };

    @Override
    public Class getColumnClass(int i)
    {
      return String.class;
    }

    @Override
    public int getColumnCount()
    {
      return columns.length;
    }

    @Override
    public String getColumnName(int i)
    {
      return columns[i];
    }

    @Override
    public int getRowCount()
    {
      return ManageLasercuttersDialog.this.currentLaserCutters == null ? 0 : ManageLasercuttersDialog.this.currentLaserCutters.size();
    }

    @Override
    public void setValueAt(Object o, int y, int x)
    {
      LaserDevice c = ManageLasercuttersDialog.this.currentLaserCutters.get(y);
      switch (x)
      {
        case 0:
          c.setName((String) o);
          return;
      }
    }

    @Override
    public Object getValueAt(int y, int x)
    {
      LaserDevice c = ManageLasercuttersDialog.this.currentLaserCutters.get(y);
      switch (x)
      {
        case 0:
          return c.getName();
        case 1:
        {
          String cls = c.getLaserCutter().getModelName();
          String[] parts = cls.split("\\.");
          if (parts.length > 1)
          {

            cls = parts[parts.length - 1];
          }
          return cls;
        }

      }
      return null;
    }

    @Override
    public boolean isCellEditable(int y, int x)
    {
      return x == 0;
    }
  };

  /** Creates new form ManageLasercuttersDialog */
  public ManageLasercuttersDialog(java.awt.Frame parent, boolean modal)
  {
    super(parent, modal);
    initComponents();
    this.editableTablePanel1.setTableModel(model);
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

        jButton4 = new javax.swing.JButton();
        jButton5 = new javax.swing.JButton();
        editableTablePanel1 = new com.t_oster.uicomponents.EditableTablePanel();
        jLabel1 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/resources/ManageLasercuttersDialog"); // NOI18N
        setTitle(bundle.getString("TITLE")); // NOI18N
        setName("Form"); // NOI18N

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(com.t_oster.visicut.gui.VisicutApp.class).getContext().getResourceMap(ManageLasercuttersDialog.class);
        jButton4.setText(resourceMap.getString("jButton4.text")); // NOI18N
        jButton4.setName("jButton4"); // NOI18N
        jButton4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton4ActionPerformed(evt);
            }
        });

        jButton5.setText(resourceMap.getString("jButton5.text")); // NOI18N
        jButton5.setName("jButton5"); // NOI18N
        jButton5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton5ActionPerformed(evt);
            }
        });

        editableTablePanel1.setName("editableTablePanel1"); // NOI18N
        editableTablePanel1.setProvider(this);

        org.jdesktop.beansbinding.Binding binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, this, org.jdesktop.beansbinding.ELProperty.create("${currentLaserCutters}"), editableTablePanel1, org.jdesktop.beansbinding.BeanProperty.create("objects"), "Cutters");
        bindingGroup.addBinding(binding);

        jLabel1.setText(resourceMap.getString("mainHelptextLabel.text")); // NOI18N
        jLabel1.setName("mainHelptextLabel"); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(editableTablePanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 580, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(jButton5)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton4))
                    .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(editableTablePanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 277, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton4)
                    .addComponent(jButton5))
                .addContainerGap())
        );

        bindingGroup.bind();

        pack();
    }// </editor-fold>//GEN-END:initComponents

  private void jButton4ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButton4ActionPerformed
  {//GEN-HEADEREND:event_jButton4ActionPerformed
    this.setLaserCutters(this.getCurrentLaserCutters());
    this.setVisible(false);
  }//GEN-LAST:event_jButton4ActionPerformed

  private void jButton5ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jButton5ActionPerformed
  {//GEN-HEADEREND:event_jButton5ActionPerformed
    this.setLaserCutters(null);
    this.setVisible(false);
  }//GEN-LAST:event_jButton5ActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private com.t_oster.uicomponents.EditableTablePanel editableTablePanel1;
    private javax.swing.JButton jButton4;
    private javax.swing.JButton jButton5;
    private javax.swing.JLabel jLabel1;
    private org.jdesktop.beansbinding.BindingGroup bindingGroup;
    // End of variables declaration//GEN-END:variables

  public Object getNewInstance()
  {
    JComboBox driver = new JComboBox();
    Map<String, String> driverClassnames = new LinkedHashMap<String,String>();
    driver.setEditable(true);
    for (String s:PreferencesManager.getInstance().getPreferences().getAvailableLasercutterDrivers())
    {
      String modelName = s;
      //try to instanciate class to get readable name
      try
      {
        Class driverclass = Class.forName(s);
        LaserCutter cutter = (LaserCutter) driverclass.newInstance();
        modelName = cutter.getModelName();
      }
      catch (Exception e)
      {
      }
      driverClassnames.put(modelName, s);
      driver.addItem(modelName);
    }
    if (JOptionPane.showConfirmDialog(this, driver, java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/resources/ManageLasercuttersDialog").getString("PLEASE SELECT A DRIVER"), JOptionPane.OK_CANCEL_OPTION)==JOptionPane.CANCEL_OPTION)
    {
      return null;
    }
    LaserDevice result = new LaserDevice();
    try
    {
      Class driverclass = Class.forName(driverClassnames.get((String) driver.getSelectedItem()));
      LaserCutter cutter = (LaserCutter) driverclass.newInstance();
      result.setLaserCutter(cutter);
    }
    catch (Exception e)
    {

    }
    return (LaserDevice) this.editObject(result);
  }

  public Object editObject(Object o)
  {
    EditLaserDeviceDialog d = new EditLaserDeviceDialog(null, true);
    d.setLaserDevice((LaserDevice) o);
    d.setVisible(true);
    return d.getLaserDevice();
  }
}
