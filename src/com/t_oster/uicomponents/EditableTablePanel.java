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

import java.util.LinkedList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class EditableTablePanel extends javax.swing.JPanel
{

  /**
   * Creates new form EditableTablePanel2
   */
  public EditableTablePanel()
  {
    initComponents();
    this.setMoveButtonsVisible(false);
    this.setSaveButtonVisible(false);
    this.setLoadButtonVisible(false);
    this.setRevertButtonVisible(false);
    this.table.setModel(this.getTableModel());
    //make the table save data, when loosing focus
    this.table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
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
  protected boolean saveButtonVisible = false;
  public static final String PROP_SAVEBUTTONVISIBLE = "saveButtonVisible";

  /**
   * Get the value of saveButtonVisible
   *
   * @return the value of saveButtonVisible
   */
  public boolean isSaveButtonVisible()
  {
    return saveButtonVisible;
  }

  /**
   * Set the value of saveButtonVisible
   *
   * @param saveButtonVisible new value of saveButtonVisible
   */
  public void setSaveButtonVisible(boolean saveButtonVisible)
  {
    boolean oldSaveButtonVisible = this.saveButtonVisible;
    this.saveButtonVisible = saveButtonVisible;
    firePropertyChange(PROP_SAVEBUTTONVISIBLE, oldSaveButtonVisible, saveButtonVisible);
    this.btSave.setVisible(saveButtonVisible);
  }

  protected boolean loadButtonVisible = false;
  public static final String PROP_LOADBUTTONVISIBLE = "loadButtonVisible";

  /**
   * Get the value of loadButtonVisible
   *
   * @return the value of loadButtonVisible
   */
  public boolean isLoadButtonVisible()
  {
    return loadButtonVisible;
  }

  /**
   * Set the value of loadButtonVisible
   *
   * @param loadButtonVisible new value of loadButtonVisible
   */
  public void setLoadButtonVisible(boolean loadButtonVisible)
  {
    boolean oldLoadButtonVisible = this.loadButtonVisible;
    this.loadButtonVisible = loadButtonVisible;
    firePropertyChange(PROP_LOADBUTTONVISIBLE, oldLoadButtonVisible, loadButtonVisible);
    this.btLoad.setVisible(loadButtonVisible);
  }
  
  protected boolean revertButtonVisible = false;
  public static final String PROP_REVERTBUTTONVISIBLE = "revertButtonVisible";

  /**
   * Get the value of revertButtonVisible
   *
   * @return the value of revertButtonVisible
   */
  public boolean isRevertButtonVisible()
  {
    return revertButtonVisible;
  }

  /**
   * Set the value of revertButtonVisible
   *
   * @param revertButtonVisible new value of revertButtonVisible
   */
  public void setRevertButtonVisible(boolean revertButtonVisible)
  {
    boolean oldRevertButtonVisible = this.revertButtonVisible;
    this.revertButtonVisible = revertButtonVisible;
    firePropertyChange(PROP_REVERTBUTTONVISIBLE, oldRevertButtonVisible, revertButtonVisible);
    this.btRevert.setVisible(revertButtonVisible);
  }

  public JButton getSaveButton()
  {
    return this.btSave;
  }

  public JButton getLoadButton()
  {
    return this.btLoad;
  }
  
  public JButton getRevertButton()
  {
    return this.btRevert;
  }

  private boolean addButtonVisible = true;

  public boolean isAddButtonVisible()
  {
    return addButtonVisible;
  }

  public void setAddButtonVisible(boolean addButtonVisible)
  {
    this.addButtonVisible = addButtonVisible;
    this.btAdd.setVisible(addButtonVisible);
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
    this.btEdit.setVisible(editButtonVisible);
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
      java.util.ResourceBundle.getBundle("com/t_oster/uicomponents/resources/EditableTablePanel").getString("ELEMENTS")
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

  private TableModelListener updateButtonVisiblityListener = new TableModelListener(){
    public void tableChanged(TableModelEvent tme)
    {
      boolean moreThanOne = tableModel.getRowCount() > 1;
      btDown.setVisible(moreThanOne);
      btUp.setVisible(moreThanOne);
      btRemove.setVisible(moreThanOne);
    }
  };

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
    if (oldTableModel != null)
    {
      oldTableModel.removeTableModelListener(updateButtonVisiblityListener);
    }
    this.tableModel = tableModel;
    if (tableModel != null)
    {
      tableModel.addTableModelListener(updateButtonVisiblityListener);
    }
    this.table.setModel(tableModel);
    firePropertyChange(PROP_TABLEMODEL, oldTableModel, tableModel);
  }
  protected List<Object> objects = new LinkedList<Object>();
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


  public void setMoveButtonsVisible(boolean vis)
  {
    this.btUp.setVisible(vis);
    this.btDown.setVisible(vis);
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

    jScrollPane1 = new javax.swing.JScrollPane();
    table = new BetterJTable();
    btAdd = new javax.swing.JButton();
    btRemove = new javax.swing.JButton();
    btEdit = new javax.swing.JButton();
    btUp = new javax.swing.JButton();
    btDown = new javax.swing.JButton();
    btLoad = new javax.swing.JButton();
    btSave = new javax.swing.JButton();
    btRevert = new javax.swing.JButton();

    table.setModel(new javax.swing.table.DefaultTableModel(
      new Object [][]
      {
        {null, null, null, null},
        {null, null, null, null},
        {null, null, null, null},
        {null, null, null, null}
      },
      new String []
      {
        "Title 1", "Title 2", "Title 3", "Title 4"
      }
    ));
    jScrollPane1.setViewportView(table);

    btAdd.setIcon(PlatformIcon.get(PlatformIcon.ADD));
    java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("com/t_oster/uicomponents/resources/EditableTablePanel"); // NOI18N
    btAdd.setToolTipText(bundle.getString("+")); // NOI18N
    btAdd.addActionListener(new java.awt.event.ActionListener()
    {
      public void actionPerformed(java.awt.event.ActionEvent evt)
      {
        btAddActionPerformed(evt);
      }
    });

    btRemove.setIcon(PlatformIcon.get(PlatformIcon.REMOVE));
    btRemove.setToolTipText(bundle.getString("-")); // NOI18N
    btRemove.addActionListener(new java.awt.event.ActionListener()
    {
      public void actionPerformed(java.awt.event.ActionEvent evt)
      {
        btRemoveActionPerformed(evt);
      }
    });

    btEdit.setIcon(PlatformIcon.get(PlatformIcon.EDIT));
    btEdit.setToolTipText(bundle.getString("EDIT")); // NOI18N
    btEdit.addActionListener(new java.awt.event.ActionListener()
    {
      public void actionPerformed(java.awt.event.ActionEvent evt)
      {
        btEditActionPerformed(evt);
      }
    });

    btUp.setIcon(PlatformIcon.get(PlatformIcon.UP));
    btUp.setToolTipText(bundle.getString("UP")); // NOI18N
    btUp.addActionListener(new java.awt.event.ActionListener()
    {
      public void actionPerformed(java.awt.event.ActionEvent evt)
      {
        btUpActionPerformed(evt);
      }
    });

    btDown.setIcon(PlatformIcon.get(PlatformIcon.DOWN));
    btDown.setToolTipText(bundle.getString("DOWN")); // NOI18N
    btDown.addActionListener(new java.awt.event.ActionListener()
    {
      public void actionPerformed(java.awt.event.ActionEvent evt)
      {
        btDownActionPerformed(evt);
      }
    });

    btLoad.setIcon(PlatformIcon.get(PlatformIcon.LOAD));
    btLoad.setToolTipText(bundle.getString("LOAD")); // NOI18N
    btLoad.addActionListener(new java.awt.event.ActionListener()
    {
      public void actionPerformed(java.awt.event.ActionEvent evt)
      {
        btLoadActionPerformed(evt);
      }
    });

    btSave.setIcon(PlatformIcon.get(PlatformIcon.SAVE));
    btSave.setToolTipText(bundle.getString("SAVE")); // NOI18N

    btRevert.setIcon(PlatformIcon.get(PlatformIcon.UNDO));
    btRevert.setToolTipText(bundle.getString("REVERT")); // NOI18N
    btRevert.addActionListener(new java.awt.event.ActionListener()
    {
      public void actionPerformed(java.awt.event.ActionEvent evt)
      {
        btRevertActionPerformed(evt);
      }
    });

    javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
    this.setLayout(layout);
    layout.setHorizontalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(layout.createSequentialGroup()
        .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 475, Short.MAX_VALUE)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
          .addComponent(btLoad, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
          .addComponent(btDown, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
          .addComponent(btUp, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
          .addComponent(btEdit, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
          .addComponent(btRemove, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
          .addComponent(btAdd, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
          .addComponent(btSave, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
          .addComponent(btRevert, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
    );
    layout.setVerticalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(layout.createSequentialGroup()
        .addComponent(btAdd)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(btRemove)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(btEdit)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(btUp)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(btDown)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 176, Short.MAX_VALUE)
        .addComponent(btRevert)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(btLoad)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(btSave))
      .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
    );
  }// </editor-fold>//GEN-END:initComponents

  private void btRemoveActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btRemoveActionPerformed
  {//GEN-HEADEREND:event_btRemoveActionPerformed
    int idx = this.table.getSelectedRow();
    if (idx >= 0 && this.objects.size() > idx)
    {
      Object o = this.objects.get(idx);
      this.objects.remove(o);
      this.tableModel.fireTableRowsDeleted(idx, idx);
    }
  }//GEN-LAST:event_btRemoveActionPerformed

  private void btEditActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btEditActionPerformed
  {//GEN-HEADEREND:event_btEditActionPerformed
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
  }//GEN-LAST:event_btEditActionPerformed

  private void btAddActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btAddActionPerformed
  {//GEN-HEADEREND:event_btAddActionPerformed
    if (this.provider != null)
    {
      Object n = this.provider.getNewInstance();
      if (n != null)
      {
        this.objects.add(n);
        int idx = this.objects.indexOf(n);
        this.tableModel.fireTableRowsInserted(idx, idx);
      }
    }
  }//GEN-LAST:event_btAddActionPerformed

  private void btUpActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btUpActionPerformed
  {//GEN-HEADEREND:event_btUpActionPerformed
    int idx = this.table.getSelectedRow();
    if (idx > 0)
    {
      Object tmp = this.objects.get(idx-1);
      this.objects.set(idx-1, this.objects.get(idx));
      this.objects.set(idx, tmp);
      this.tableModel.fireTableRowsUpdated(idx-1, idx);
      this.table.getSelectionModel().setSelectionInterval(idx-1, idx-1);
    }
  }//GEN-LAST:event_btUpActionPerformed

  private void btDownActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btDownActionPerformed
  {//GEN-HEADEREND:event_btDownActionPerformed
    int idx = this.table.getSelectedRow();
    if (idx +1 < this.objects.size())
    {
      Object tmp = this.objects.get(idx+1);
      this.objects.set(idx+1, this.objects.get(idx));
      this.objects.set(idx, tmp);
      this.tableModel.fireTableRowsUpdated(idx, idx+1);
      this.table.getSelectionModel().setSelectionInterval(idx+1, idx+1);
    }
  }//GEN-LAST:event_btDownActionPerformed

  private void btLoadActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btLoadActionPerformed
  {//GEN-HEADEREND:event_btLoadActionPerformed
    // TODO add your handling code here:
  }//GEN-LAST:event_btLoadActionPerformed

  private void btRevertActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btRevertActionPerformed
  {//GEN-HEADEREND:event_btRevertActionPerformed
    // TODO add your handling code here:
  }//GEN-LAST:event_btRevertActionPerformed

  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JButton btAdd;
  private javax.swing.JButton btDown;
  private javax.swing.JButton btEdit;
  private javax.swing.JButton btLoad;
  private javax.swing.JButton btRemove;
  private javax.swing.JButton btRevert;
  private javax.swing.JButton btSave;
  private javax.swing.JButton btUp;
  private javax.swing.JScrollPane jScrollPane1;
  private javax.swing.JTable table;
  // End of variables declaration//GEN-END:variables
}
