/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.visicut.gui;

import com.t_oster.liblasercut.LaserProperty;
import java.util.List;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author thommy
 */
class LaserPropertiesTableModel extends DefaultTableModel
{

  public void setLaserProperties(List<LaserProperty> lp)
  {
    this.lp = lp;
    if (lp.isEmpty())
    {
      columnNames = new String[]{"no property given ;("};
    }
    else
    {
      columnNames = lp.get(0).getPropertyKeys();
    }
    this.fireTableStructureChanged();
  }
  private List<LaserProperty> lp = null;
  private String[] columnNames = new String[]{"No property given ;("};

  @Override
  public Class getColumnClass(int x)
  {
    if (lp.isEmpty())
    {
      return String.class;
    }
    else
    {
      return lp.get(0).getProperty(this.columnNames[x]).getClass();
    }
  }
  
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
    return lp.get(y).getProperty(this.columnNames[x]);
  }

  @Override
  public boolean isCellEditable(int i, int i1)
  {
    return true;
  }

  @Override
  public void setValueAt(Object o, int y, int x)
  {
    lp.get(y).setProperty(this.columnNames[x], o);
  }

  @Override
  public int getRowCount()
  {
    return lp == null ? 0 : lp.size();
  }
  
}
