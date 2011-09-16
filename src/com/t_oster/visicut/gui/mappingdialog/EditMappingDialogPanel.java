/**
 * This file is part of VisiCut.
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
 * EditMappingDialogPanel.java
 *
 * Created on 08.09.2011, 11:45:35
 */
package com.t_oster.visicut.gui.mappingdialog;

import com.t_oster.visicut.gui.beans.EditableTableProvider;
import com.t_oster.visicut.misc.Helper;
import com.t_oster.visicut.model.LaserProfile;
import com.t_oster.visicut.model.MaterialProfile;
import com.t_oster.visicut.model.mapping.Mapping;
import com.t_oster.visicut.model.mapping.MappingFilter;
import java.awt.Color;
import java.awt.Component;
import java.util.List;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author thommy
 */
public class EditMappingDialogPanel extends javax.swing.JPanel implements EditableTableProvider
{

  /** Creates new form EditMappingDialogPanel */
  DefaultTableModel filterTableModel = new DefaultTableModel()
  {

    String[] columns = new String[]
    {
      "Attribute", " ", "Value"
    };

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
      return EditMappingDialogPanel.this.currentMapping == null ? 0 : EditMappingDialogPanel.this.currentMapping.getFilterSet().size();
    }

    @Override
    public Object getValueAt(int y, int x)
    {
      MappingFilter f = EditMappingDialogPanel.this.currentMapping.getFilterSet().get(y);
      switch (x)
      {
        case 0:
          return f.getAttribute();
        case 1:
          return f.isInverted() ? "IS NOT" : "IS";
        case 2:
          return f.getValue();
      }
      return null;
    }

    @Override
    public boolean isCellEditable(int y, int x)
    {
      return true;
    }

    @Override
    public void setValueAt(Object o, int y, int x)
    {
      MappingFilter f = EditMappingDialogPanel.this.currentMapping.getFilterSet().get(y);
      switch (x)
      {
        case 0:
          f.setAttribute(o.toString());
          break;
        case 1:
          f.setInverted(o.toString().equals("IS NOT"));
          break;
        case 2:
          f.setValue(o);
          break;
      }
    }
  };

  public EditMappingDialogPanel()
  {
    initComponents();
    this.editableTablePanel1.setTableModel(filterTableModel);
    JComboBox attributeCb = new JComboBox();
    attributeCb.setEditable(true);
    for (String a:new String[]{"Type", "Fill Color", "Line Color", "Line Width", "Group"})
    {
      attributeCb.addItem(a);
    }
    this.editableTablePanel1.getTable().getColumnModel().getColumn(0).setCellEditor(new DefaultCellEditor(attributeCb));
    JComboBox invertedCb = new JComboBox();
    invertedCb.addItem("IS NOT");
    invertedCb.addItem("IS");
    this.editableTablePanel1.getTable().getColumnModel().getColumn(1).setCellEditor(new DefaultCellEditor(invertedCb));
    this.editableTablePanel1.getTable().getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer()
    {

      @Override
      public Component getTableCellRendererComponent(JTable jtable, Object o, boolean bln, boolean bln1, int i, int i1)
      {
        Component c = super.getTableCellRendererComponent(jtable, o, bln, bln1, i, i1);
        if (c instanceof JLabel && o instanceof Color)
        {
          ((JLabel) c).setText("<html><table border=1><tr><td bgcolor=" + Helper.toHtmlRGB((Color) o) + ">&nbsp;&nbsp;&nbsp;&nbsp;</td></tr></table></html>");
        }
        return c;
      }
    });
    this.editableTablePanel1.getTable().getColumnModel().getColumn(2).setCellEditor(new FilterValueEditor());
  }
  protected MaterialProfile material = null;
  public static final String PROP_MATERIAL = "material";

  /**
   * Get the value of material
   *
   * @return the value of material
   */
  public MaterialProfile getMaterial()
  {
    return material;
  }

  /**
   * Set the value of material.
   * The Material is used for filling the ComboBox
   * of Target Profiles
   *
   * @param material new value of material
   */
  public void setMaterial(MaterialProfile material)
  {
    MaterialProfile oldMaterial = this.material;
    this.material = material;
    firePropertyChange(PROP_MATERIAL, oldMaterial, material);
    if (material != null)
    {
      this.imageComboBox1.removeAllItems();
      for (LaserProfile p : material.getLaserProfiles())
      {
        this.imageComboBox1.addItem(p);
      }
      //For adding eventually missing Laserprofile  
      this.setCurrentMapping(this.getCurrentMapping());
    }
  }
  protected Mapping currentMapping = new Mapping();
  ;

    public static final String PROP_CURRENTMAPPING = "currentMapping";

  /**
   * Get the value of currentMapping
   *
   * @return the value of currentMapping
   */
  public Mapping getCurrentMapping()
  {
    if (this.imageComboBox1.getSelectedItem() instanceof LaserProfile)
    {
      currentMapping.setProfileName(((LaserProfile) this.imageComboBox1.getSelectedItem()).getName());
    }
    else
    {
      currentMapping.setProfileName(imageComboBox1.getSelectedItem().toString());
    }
    currentMapping.getFilterSet().setUseOuterShape(jCheckBox1.isSelected());
    return currentMapping;
  }

  /**
   * Set the value of currentMapping
   *
   * @param currentMapping new value of currentMapping
   */
  public void setCurrentMapping(Mapping currentMapping)
  {
    Mapping oldCurrentMapping = this.currentMapping;
    this.currentMapping = currentMapping;
    firePropertyChange(PROP_CURRENTMAPPING, oldCurrentMapping, currentMapping);

    //Fill ComboBox
    if (currentMapping != null)
    {
      this.editableTablePanel1.setObjects((List) currentMapping.getFilterSet());
      this.jCheckBox1.setSelected(currentMapping.getFilterSet().isUseOuterShape());
      boolean found = false;
      String prof = currentMapping.getProfileName();
      for (int i = 0; i < this.imageComboBox1.getItemCount(); i++)
      {
        if (this.imageComboBox1.getItemAt(i) instanceof LaserProfile)
        {
          if (((LaserProfile) (this.imageComboBox1.getItemAt(i))).getName().equals(prof))
          {
            found = true;
            this.imageComboBox1.setSelectedIndex(i);
            break;
          }
        }
        else if (this.imageComboBox1.getItemAt(i) instanceof String)
        {
          if (((String) (this.imageComboBox1.getItemAt(i))).equals(prof))
          {
            found = true;
            this.imageComboBox1.setSelectedIndex(i);
            break;
          }
        }
      }
      if (!found)
      {
        this.imageComboBox1.addItem(prof);
        this.imageComboBox1.setSelectedItem(prof);
      }
      this.filterTableModel.fireTableDataChanged();
    }
  }

  /** This method is called from within the constructor to
   * initialize the form.
   * WARNING: Do NOT modify this code. The content of this method is
   * always regenerated by the Form Editor.
   */
  @SuppressWarnings("unchecked")
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents() {

    jLabel1 = new javax.swing.JLabel();
    editableTablePanel1 = new com.t_oster.visicut.gui.beans.EditableTablePanel();
    imageComboBox1 = new com.t_oster.visicut.gui.beans.ImageComboBox();
    jLabel2 = new javax.swing.JLabel();
    jCheckBox1 = new javax.swing.JCheckBox();

    setName("Form"); // NOI18N

    org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(com.t_oster.visicut.gui.VisicutApp.class).getContext().getResourceMap(EditMappingDialogPanel.class);
    jLabel1.setText(resourceMap.getString("jLabel1.text")); // NOI18N
    jLabel1.setName("jLabel1"); // NOI18N

    editableTablePanel1.setEditButtonVisible(false);
    editableTablePanel1.setName("editableTablePanel1"); // NOI18N
    editableTablePanel1.setProvider(this);

    imageComboBox1.setEditable(true);
    imageComboBox1.setName("imageComboBox1"); // NOI18N

    jLabel2.setText(resourceMap.getString("jLabel2.text")); // NOI18N
    jLabel2.setName("jLabel2"); // NOI18N

    jCheckBox1.setText(resourceMap.getString("jCheckBox1.text")); // NOI18N
    jCheckBox1.setName("jCheckBox1"); // NOI18N

    javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
    this.setLayout(layout);
    layout.setHorizontalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
        .addContainerGap()
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
          .addComponent(editableTablePanel1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 376, Short.MAX_VALUE)
          .addComponent(imageComboBox1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 376, Short.MAX_VALUE)
          .addComponent(jLabel2, javax.swing.GroupLayout.Alignment.LEADING)
          .addComponent(jCheckBox1, javax.swing.GroupLayout.Alignment.LEADING)
          .addComponent(jLabel1, javax.swing.GroupLayout.Alignment.LEADING))
        .addContainerGap())
    );
    layout.setVerticalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(layout.createSequentialGroup()
        .addContainerGap()
        .addComponent(jLabel2)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(imageComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, 73, javax.swing.GroupLayout.PREFERRED_SIZE)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 9, Short.MAX_VALUE)
        .addComponent(jCheckBox1)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(jLabel1)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(editableTablePanel1, javax.swing.GroupLayout.PREFERRED_SIZE, 159, javax.swing.GroupLayout.PREFERRED_SIZE)
        .addContainerGap())
    );
  }// </editor-fold>//GEN-END:initComponents
  // Variables declaration - do not modify//GEN-BEGIN:variables
  private com.t_oster.visicut.gui.beans.EditableTablePanel editableTablePanel1;
  private com.t_oster.visicut.gui.beans.ImageComboBox imageComboBox1;
  private javax.swing.JCheckBox jCheckBox1;
  private javax.swing.JLabel jLabel1;
  private javax.swing.JLabel jLabel2;
  // End of variables declaration//GEN-END:variables

  public Object getNewInstance()
  {
    return new MappingFilter("Line Width", null);
  }

  public Object editObject(Object o)
  {
    return o;
  }
}
