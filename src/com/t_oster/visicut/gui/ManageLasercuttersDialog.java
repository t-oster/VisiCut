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
 * ManageLasercuttersDialog.java
 *
 * Created on 06.09.2011, 21:24:37
 */
package com.t_oster.visicut.gui;

import com.t_oster.liblasercut.LaserCutter;
import com.t_oster.liblasercut.drivers.EpilogCutter;  //needed for a line 329
import com.t_oster.visicut.gui.beans.EditableTableProvider;
import com.t_oster.visicut.managers.PreferencesManager;
import com.t_oster.visicut.model.LaserDevice;
import java.beans.PropertyChangeSupport;
import java.util.LinkedList;
import java.util.List;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class ManageLasercuttersDialog extends javax.swing.JDialog implements EditableTableProvider
{

  public static final String PROP_DEFAULTLASERCUTTER = "defaultLaserCutter";

  /**
   * Get the value of defaultLaserCutter
   *
   * @return the value of defaultLaserCutter
   */
  public int getDefaultLaserCutter()
  {
    return defaultIndex;
  }

  /**
   * Set the value of defaultLaserCutter
   *
   * @param defaultLaserCutter new value of defaultLaserCutter
   */
  public void setDefaultLaserCutter(int idx)
  {
    int oldIdx = this.defaultIndex;
    this.defaultIndex = idx;
    firePropertyChange(PROP_DEFAULTLASERCUTTER, oldIdx, idx);
  }
  private int defaultIndex;
  private PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);
  protected List<LaserDevice> laserCutters = null;
  public static final String PROP_LASERCUTTERS = "laserCutters";

  /**
   * Get the value of laserCutters
   *
   * @return the value of laserCutters
   */
  public List<LaserDevice> getLaserCutters()
  {
    return laserCutters;
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
      "Name", "Driver", "Default"
    };

    @Override
    public Class<?> getColumnClass(int i)
    {
      return i < 2 ? String.class : Boolean.class;
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
        case 2:
          if ((Boolean) o.equals(true))
          {
            int old = ManageLasercuttersDialog.this.defaultIndex;
            ManageLasercuttersDialog.this.defaultIndex = y;
            this.fireTableCellUpdated(old, 2);
            this.fireTableCellUpdated(y, 2);
          }
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
          String cls = c.getLaserCutter().getClass().toString();
          String[] parts = cls.split("\\.");
          if (parts.length > 1)
          {

            cls = parts[parts.length - 1];
          }
          return cls;
        }
        case 2:
          return (y == ManageLasercuttersDialog.this.defaultIndex);

      }
      return null;
    }

    @Override
    public boolean isCellEditable(int y, int x)
    {
      return x == 0 || x == 2;
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
    editableTablePanel1 = new com.t_oster.visicut.gui.beans.EditableTablePanel();

    setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
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

    javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
    getContentPane().setLayout(layout);
    layout.setHorizontalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
        .addContainerGap()
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
          .addComponent(editableTablePanel1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 333, Short.MAX_VALUE)
          .addGroup(layout.createSequentialGroup()
            .addComponent(jButton5)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(jButton4)))
        .addContainerGap())
    );
    layout.setVerticalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
        .addContainerGap()
        .addComponent(editableTablePanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 311, Short.MAX_VALUE)
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
  private com.t_oster.visicut.gui.beans.EditableTablePanel editableTablePanel1;
  private javax.swing.JButton jButton4;
  private javax.swing.JButton jButton5;
  private org.jdesktop.beansbinding.BindingGroup bindingGroup;
  // End of variables declaration//GEN-END:variables

  public Object getNewInstance()
  {
    JComboBox driver = new JComboBox();
    driver.setEditable(true);
    for (String s:PreferencesManager.getInstance().getPreferences().getAvailableLasercutterDrivers())
    {
      driver.addItem(s);
    }
    if (JOptionPane.showConfirmDialog(this, driver, "Please select a driver", JOptionPane.OK_CANCEL_OPTION)==JOptionPane.CANCEL_OPTION)
    {
      return null;
    }
    LaserDevice result = new LaserDevice();
    try
    {
      Class driverclass = Class.forName((String) driver.getSelectedItem());
      if (driver.getSelectedItem()=="com.t_oster.liblasercut.drivers.EpilogCutter")
      {
        
        EpilogCutter cutter = (EpilogCutter) driverclass.newInstance();
        // set some default values so that they are present in the settings.xml file
        // from the beginning on. If they are present from the beginning on then they
        // can be edited in the gui. (Axel)
        
        //todo: move this to the constructor of EpilogCutter
        /*
        cutter.setDpi(250.0);
        cutter.setBedHeight(350.0);
        cutter.setBedWidth(599.0);
        cutter.setHostname("localhost");
        cutter.setPort(515);
        cutter.setModel("ZING")
        */
        result.setLaserCutter(cutter);
      }
      else
      {
        LaserCutter cutter = (LaserCutter) driverclass.newInstance();
        result.setLaserCutter(cutter);
      }
        
    }
    catch (Exception e)
    {
      
    }   
    return result;
  }

  public Object editObject(Object o)
  {
    EditLaserDeviceDialog d = new EditLaserDeviceDialog(null, true);
    d.setLaserDevice((LaserDevice) o);
    d.setVisible(true);
    return d.getLaserDevice();
  }
}
