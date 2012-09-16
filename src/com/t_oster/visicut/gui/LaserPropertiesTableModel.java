/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.visicut.gui;

import com.t_oster.liblasercut.LaserProperty;
import com.t_oster.visicut.model.LaserProfile;
import com.t_oster.visicut.model.VectorProfile;
import java.util.List;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author thommy
 */
class LaserPropertiesTableModel extends DefaultTableModel
{

  public void setLaserProperties(List<LaserProperty> lp, boolean showFrequency)
  {
    this.lp = lp;
    if (showFrequency)
    {
      columnNames = new String[]{"Power", "Speed", "Focus", "Frequency"};
    }
    else
    {
      columnNames = new String[]{"Power", "Speed", "Focus"};
    }
    this.fireTableStructureChanged();
  }
  private List<LaserProperty> lp = null;
  private String[] columnNames = new String[]{"Power", "Speed", "Focus", "Frequency"};

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
        return lp.get(y).getPower();
      case 1:
        return lp.get(y).getSpeed();
      case 2:
        return lp.get(y).getFocus();
      case 3:
        return lp.get(y).getFrequency();
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
        lp.get(y).setPower(Integer.parseInt(o.toString()));
        return;
      case 1:
        lp.get(y).setSpeed(Integer.parseInt(o.toString()));
        return;
      case 2:
        lp.get(y).setFocus(Float.parseFloat(o.toString()));
        return;
      case 3:
        lp.get(y).setFrequency(Integer.parseInt(o.toString()));
        return;
    }
  }

  @Override
  public int getRowCount()
  {
    return lp == null ? 0 : lp.size();
  }
  
}
