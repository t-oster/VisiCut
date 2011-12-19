/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.visicut.gui;

import com.t_oster.visicut.model.LaserProfile;
import com.t_oster.visicut.model.VectorProfile;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author thommy
 */
class LaserProfileTableModel extends DefaultTableModel
{

  public void setLaserProfile(LaserProfile lp)
  {
    this.lp = lp;
    if (lp instanceof VectorProfile)
    {
      columnNames = new String[]{"Power", "Speed", "Frequency", "Focus"};
    }
    else
    {
      columnNames = new String[]{"Power", "Speed", "Frequency"};
    }
  }
  private LaserProfile lp = null;
  private String[] columnNames = new String[]{"Power", "Speed", "Frequency", "Focus"};

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
        return lp.getLaserProperties().get(y).getPower();
      case 1:
        return lp.getLaserProperties().get(y).getSpeed();
      case 2:
        return lp.getLaserProperties().get(y).getFrequency();
      case 3:
        return lp.getLaserProperties().get(y).getFocus();
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
        lp.getLaserProperties().get(y).setPower(Integer.parseInt(o.toString()));
        return;
      case 1:
        lp.getLaserProperties().get(y).setSpeed(Integer.parseInt(o.toString()));
        return;
      case 2:
        lp.getLaserProperties().get(y).setFrequency(Integer.parseInt(o.toString()));
        return;
      case 3:
        lp.getLaserProperties().get(y).setFocus(Float.parseFloat(o.toString()));
        return;
    }
  }

  @Override
  public int getRowCount()
  {
    return lp == null ? 0 : lp.getLaserProperties().size();
  }
  
}
