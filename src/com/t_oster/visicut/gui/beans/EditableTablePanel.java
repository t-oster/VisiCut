/**
 * This file is part of VisiCut.
 * Copyright (C) 2011 Thomas Oster <thomas.oster@rwth-aachen.de>
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
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * EditableTablePanel.java
 *
 * Created on 07.09.2011, 18:31:49
 */
package com.t_oster.visicut.gui.beans;

import java.util.LinkedList;
import java.util.List;
import javax.swing.JTable;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author thommy
 */
public class EditableTablePanel extends javax.swing.JPanel
{

  /** Creates new form EditableTablePanel */
  public EditableTablePanel()
  {
    initComponents();
    this.table.setModel(this.getTableModel());
  }
  
  public void addListSelectionListener(ListSelectionListener l)
  {
    this.table.getSelectionModel().addListSelectionListener(l);
  }
  
  public void removeListSelectionListener(ListSelectionListener l)
  {
    this.table.getSelectionModel().removeListSelectionListener(l);
  }
  
  public JTable getTable()
  {
    return table;
  }
  
  public void clearSelection()
  {
    this.table.clearSelection();
  }
  
  public void setSelectedRow(int i)
  {
    this.table.getSelectionModel().setSelectionInterval(i, i);
  }
  
  public int getSelectedRow()
  {
    return this.table.getSelectedRow();
  }
  protected boolean editButtonVisible = true;
  public static final String PROP_EDITBUTTONVISIBLE = "editButtonVisible";

  /**
   * Get the value of editButtonVisible
   *
   * @return the value of editButtonVisible
   */
  public boolean isEditButtonVisible()
  {
    return editButtonVisible;
  }

  /**
   * Set the value of editButtonVisible
   * If the Edit button is visible, all Add-Button pushes
   * will be resulting in editing the new Object before adding
   * it to the list. If editButtonVisible is false, it will directly
   * be added to the list
   *
   * @param editButtonVisible new value of editButtonVisible
   */
  public void setEditButtonVisible(boolean editButtonVisible)
  {
    boolean oldEditButtonVisible = this.editButtonVisible;
    this.editButtonVisible = editButtonVisible;
    firePropertyChange(PROP_EDITBUTTONVISIBLE, oldEditButtonVisible, editButtonVisible);
    this.editButton.setVisible(editButtonVisible);
  }

  protected EditableTableProvider provider = null;
  public static final String PROP_PROVIDER = "provider";

  /**
   * Get the value of provider
   *
   * @return the value of provider
   */
  public EditableTableProvider getProvider()
  {
    return provider;
  }

  /**
   * Set the value of provider
   *
   * @param provider new value of provider
   */
  public void setProvider(EditableTableProvider provider)
  {
    EditableTableProvider oldProvider = this.provider;
    this.provider = provider;
    firePropertyChange(PROP_PROVIDER, oldProvider, provider);
  }
  protected DefaultTableModel tableModel = new DefaultTableModel()
  {

    private String[] columns = new String[]
    {
      "Elements"
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
      return EditableTablePanel.this.objects == null ? 0 : EditableTablePanel.this.objects.size();
    }

    @Override
    public Object getValueAt(int y, int x)
    {
      return EditableTablePanel.this.objects.get(y);
    }

    @Override
    public boolean isCellEditable(int i, int i1)
    {
      return false;
    }
  };
  public static final String PROP_TABLEMODEL = "tableModel";

  /**
   * Get the value of tableModel
   *
   * @return the value of tableModel
   */
  public DefaultTableModel getTableModel()
  {
    return tableModel;
  }

  /**
   * Set the value of tableModel.
   * The TableModel is used to generate the contents
   * of the table. If an Object is set to be deleted,
   * the Models fireProperty Change method will be used.
   *
   * @param tableModel new value of tableModel
   */
  public void setTableModel(DefaultTableModel tableModel)
  {
    DefaultTableModel oldTableModel = this.tableModel;
    this.tableModel = tableModel;
    firePropertyChange(PROP_TABLEMODEL, oldTableModel, tableModel);
    this.table.setModel(tableModel);
  }
  protected List<Object> objects = new LinkedList<Object>();
  ;

    public static final String PROP_OBJECTS = "objects";

  /**
   * Get the value of objects
   *
   * @return the value of objects
   */
  public List<Object> getObjects()
  {
    return objects;
  }

  /**
   * Set the value of objects
   *
   * @param objects new value of objects
   */
  public void setObjects(List<Object> objects)
  {
    List<Object> oldObjects = this.objects;
    this.objects = objects;
    firePropertyChange(PROP_OBJECTS, oldObjects, objects);
    this.tableModel.fireTableDataChanged();
  }

  /** This method is called from within the constructor to
   * initialize the form.
   * WARNING: Do NOT modify this code. The content of this method is
   * always regenerated by the Form Editor.
   */
  @SuppressWarnings("unchecked")
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents() {

    jScrollPane1 = new javax.swing.JScrollPane();
    table = new javax.swing.JTable();
    addButton = new javax.swing.JButton();
    removeButton = new javax.swing.JButton();
    editButton = new javax.swing.JButton();

    setName("Form"); // NOI18N

    jScrollPane1.setName("jScrollPane1"); // NOI18N

    table.setModel(new javax.swing.table.DefaultTableModel(
      new Object [][] {
        {null, null, null, null},
        {null, null, null, null},
        {null, null, null, null},
        {null, null, null, null}
      },
      new String [] {
        "Title 1", "Title 2", "Title 3", "Title 4"
      }
    ));
    table.setName("table"); // NOI18N
    jScrollPane1.setViewportView(table);

    org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(com.t_oster.visicut.gui.VisicutApp.class).getContext().getResourceMap(EditableTablePanel.class);
    addButton.setText(resourceMap.getString("addButton.text")); // NOI18N
    addButton.setName("addButton"); // NOI18N
    addButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        addButtonActionPerformed(evt);
      }
    });

    removeButton.setText(resourceMap.getString("removeButton.text")); // NOI18N
    removeButton.setName("removeButton"); // NOI18N
    removeButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        removeButtonActionPerformed(evt);
      }
    });

    editButton.setText(resourceMap.getString("editButton.text")); // NOI18N
    editButton.setName("editButton"); // NOI18N
    editButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        editButtonActionPerformed(evt);
      }
    });

    javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
    this.setLayout(layout);
    layout.setHorizontalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
        .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 238, Short.MAX_VALUE)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
          .addComponent(editButton)
          .addComponent(addButton, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
          .addComponent(removeButton, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)))
    );
    layout.setVerticalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(layout.createSequentialGroup()
        .addComponent(addButton)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(removeButton)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(editButton)
        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
      .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 108, Short.MAX_VALUE)
    );
  }// </editor-fold>//GEN-END:initComponents

private void removeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeButtonActionPerformed
  int idx = this.table.getSelectedRow();
  if (idx >= 0)
  {
    Object o = this.objects.get(idx);
    this.objects.remove(o);
    this.tableModel.fireTableRowsDeleted(idx, idx);
  }
}//GEN-LAST:event_removeButtonActionPerformed

private void editButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_editButtonActionPerformed
  int idx = this.table.getSelectedRow();
  if (idx >= 0 && this.provider != null)
  {
    Object result = this.provider.editObject(this.objects.get(idx));
    if (result != null)
    {
      this.objects.set(idx, result);
      this.tableModel.fireTableRowsUpdated(idx, idx);
    }
  }
}//GEN-LAST:event_editButtonActionPerformed

private void addButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addButtonActionPerformed
  if (this.provider != null)
  {
    Object n = this.provider.getNewInstance();
    if (n != null)
    {
      Object o = editButtonVisible ? this.provider.editObject(n) : n;
      if (o != null)
      {
        this.objects.add(o);
        int idx = this.objects.indexOf(o);
        this.tableModel.fireTableRowsInserted(idx, idx);
      }
    }
  }
}//GEN-LAST:event_addButtonActionPerformed
  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JButton addButton;
  private javax.swing.JButton editButton;
  private javax.swing.JScrollPane jScrollPane1;
  private javax.swing.JButton removeButton;
  private javax.swing.JTable table;
  // End of variables declaration//GEN-END:variables
}
