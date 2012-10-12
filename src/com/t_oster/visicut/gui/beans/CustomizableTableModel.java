package com.t_oster.visicut.gui.beans;

import com.t_oster.liblasercut.Customizable;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author thommy
 */
public class CustomizableTableModel extends DefaultTableModel
{
  final Customizable c;
  public CustomizableTableModel(Customizable c)
  {
    this.c = c;
  }

  private String[] cols = new String[]
  {
    java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/resources/EditLaserDeviceDialog").getString("ATTRIBUTE"), java.util.ResourceBundle.getBundle("com/t_oster/visicut/gui/resources/EditLaserDeviceDialog").getString("VALUE")
  };

  @Override
  public int getColumnCount()
  {
    return cols.length;
  }

  @Override
  public String getColumnName(int i)
  {
    return cols[i];
  }

  @Override
  public int getRowCount()
  {
    return c!= null ? c.getPropertyKeys().length : 0;
  }

  @Override
  public Object getValueAt(int y, int x)
  {
    if (c == null)
    {
      return null;
    }
    String attribute = c.getPropertyKeys()[y];
    if (x == 0)
    {
      return attribute;
    }
    else if (x == 1)
    {
      return c.getProperty(attribute);
    }
    return null;
  }

  @Override
  public boolean isCellEditable(int y, int x)
  {
    return x == 1;
  }

  @Override
  public void setValueAt(Object o, int y, int x)
  {
    if (c != null && x == 1)
    {
      String attribute = c.getPropertyKeys()[y];
      c.setProperty(attribute, o);
    }
  }

}
