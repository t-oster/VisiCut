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
package com.t_oster.visicut.gui.beans;

import com.t_oster.liblasercut.platform.Tuple;
import com.t_oster.visicut.gui.ImageListable;
import com.t_oster.visicut.misc.Helper;
import com.t_oster.visicut.model.LaserProfile;
import com.t_oster.visicut.model.graphicelements.GraphicObject;
import com.t_oster.visicut.model.graphicelements.GraphicSet;
import com.t_oster.visicut.model.mapping.FilterSet;
import com.t_oster.visicut.model.mapping.Mapping;
import com.t_oster.visicut.model.mapping.MappingFilter;
import com.t_oster.visicut.model.mapping.MappingSet;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.File;
import java.util.LinkedList;
import java.util.List;
import javax.swing.DefaultCellEditor;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class CustomMappingTable extends JTable
{

  protected String attribute = null;

  /**
   * Get the value of attribute
   *
   * @return the value of attribute
   */
  public String getAttribute()
  {
    return attribute;
  }

  /**
   * Set the value of attribute
   *
   * @param attribute new value of attribute
   */
  public void setAttribute(String attribute)
  {
    this.attribute = attribute;
    this.getColumnModel().getColumn(0).setHeaderValue(attribute);
    this.refreshListContent();
  }
  protected GraphicSet objects = null;

  /**
   * Get the value of objects
   *
   * @return the value of objects
   */
  public GraphicSet getObjects()
  {
    return objects;
  }

  /**
   * Set the value of objects
   *
   * @param objects new value of objects
   */
  public void setObjects(GraphicSet objects)
  {
    this.objects = objects;
    this.refreshListContent();
  }

  private void refreshListContent()
  {
    this.values = new LinkedList<Tuple<Object, LaserProfile>>();
    if (this.attribute != null)
    {
      List<Object> visitedValues = new LinkedList<Object>();
      if (objects != null)
      {
        for (GraphicObject g : objects)
        {
          for (Object value : g.getAttributeValues(attribute))
          {
            if (!visitedValues.contains(value))
            {
              visitedValues.add(value);
              this.values.add(new Tuple(value, null));
            }
          }
        }
      }
    }
    this.model.fireTableDataChanged();
  }
  protected List<LaserProfile> laserProfiles = null;

  /**
   * Get the value of laserProfiles
   *
   * @return the value of laserProfiles
   */
  public List<LaserProfile> getLaserProfiles()
  {
    return laserProfiles;
  }

  /**
   * Set the value of laserProfiles
   *
   * @param laserProfiles new value of laserProfiles
   */
  public void setLaserProfiles(List<LaserProfile> laserProfiles)
  {
    this.laserProfiles = laserProfiles;
    profilesCb = new ImageComboBox();
    profilesCb.addItem(null);
    if (laserProfiles != null)
    {
      for (LaserProfile lp : laserProfiles)
      {
        profilesCb.addItem(lp);
      }
    }
    this.getColumnModel().getColumn(1).setCellEditor(
      new DefaultCellEditor(profilesCb));
  }
  private ImageComboBox profilesCb;
  private List<Tuple<Object, LaserProfile>> values;

  /**
   * Sets the mapped Laserprofile for a value, if the
   * value exits in the table
   */
  void setProfileForValue(Object value, LaserProfile profile)
  {
    for (Tuple<Object, LaserProfile> tup : values)
    {
      if (tup.getA().equals(value))
      {
        tup.setB(profile);
        break;
      }
    }
  }
  private DefaultTableModel model = new DefaultTableModel()
  {

    @Override
    public int getColumnCount()
    {
      return 2;
    }

    @Override
    public String getColumnName(int i)
    {
      return i == 0 ? CustomMappingTable.this.getAttribute() : "Laser Profile";
    }

    @Override
    public int getRowCount()
    {
      return CustomMappingTable.this.values == null ? 0 : CustomMappingTable.this.values.size();
    }

    @Override
    public Object getValueAt(int y, int x)
    {
      if (x == 0)
      {
        return CustomMappingTable.this.values.get(y).getA();
      }
      else
      {
        return CustomMappingTable.this.values.get(y).getB();
      }
    }

    @Override
    public boolean isCellEditable(int y, int x)
    {
      return x == 1;
    }

    @Override
    public void setValueAt(Object o, int y, int x)
    {
      CustomMappingTable.this.values.get(y).setB((LaserProfile) o);
      this.fireTableCellUpdated(y, x);
    }
  };

  public CustomMappingTable()
  {
    this.values = new LinkedList<Tuple<Object, LaserProfile>>();
    this.setModel(model);
    this.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); 
    this.addFocusListener(new FocusListener(){

      public void focusGained(FocusEvent fe)
      {
      }

      public void focusLost(FocusEvent fe)
      {
        if (!fe.isTemporary() && !(fe.getOppositeComponent() instanceof ImageComboBox))
        {
          CustomMappingTable.this.clearSelection();
        }
      }
      
    });
    DefaultTableCellRenderer renderer = new DefaultTableCellRenderer()
    {

      private int minRowHeight = 16;

      @Override
      public Component getTableCellRendererComponent(JTable jtable, Object o, boolean bln, boolean bln1, int i, int i1)
      {
        Component c = super.getTableCellRendererComponent(jtable, o, bln, bln1, i, i1);
        if (c instanceof JLabel)
        {
          JLabel l = (JLabel) c;
          if (o == null)
          {
            jtable.setRowHeight(i, minRowHeight);
            l.setText("Ignore");
          }
          else if (o instanceof Color)
          {
            minRowHeight = 32;
            ((JLabel) c).setText("<html><table border=1><tr><td bgcolor=" + Helper.toHtmlRGB((Color) o) + ">&nbsp;&nbsp;&nbsp;&nbsp;</td></tr></table></html>");
          }
          else if (o instanceof ImageListable)
          {
            jtable.setRowHeight(i, 80);
            ImageListable item = (ImageListable) o;
            String label = "<html><table cellpadding=3><tr>";
            if (item.getThumbnailPath() != null)
            {
              File f = new File(item.getThumbnailPath());
              if (f.exists())
              {
                label += Helper.imgTag(f, 64, 64)+"</td>";
              }
            }
            label += "</td><td width=3><td>" + item.toString() + "</td></tr></table></html>";
            l.setText(label);
            l.setToolTipText(item.getDescription());
          }
        }
        return c;
      }
    };
    this.getColumnModel().getColumn(1).setPreferredWidth(200);
    this.getColumnModel().getColumn(0).setCellRenderer(renderer);
    this.getColumnModel().getColumn(1).setCellRenderer(renderer);
  }

  public MappingSet getResultingMappingSet()
  {
    MappingSet result = new MappingSet();
    for (Tuple<Object, LaserProfile> t : this.values)
    {
      if (t.getB() != null)
      {
        FilterSet fs = new FilterSet();
        fs.add(new MappingFilter(this.attribute, t.getA()));
        result.add(new Mapping(fs, t.getB().getName()));
      }
    }
    return result;
  }

  /**
   * Returns the MappingSet resulting of all selected Rows.
   * If A row is mapped to Ignore, the a Mapping with
   * target null is added.
   * If no row is selected, it returns the MappingSet of
   * all Mapped Rows.
   * 
   * @return 
   */
  public MappingSet getSelectionMappingSet()
  {
    int[] rows = this.getSelectedRows();
    if (rows.length == 0)
    {
      return this.getResultingMappingSet();
    }
    else
    {
      MappingSet result = new MappingSet();
      for (int i : rows)
      {
        Tuple<Object, LaserProfile> t = values.get(i);
        FilterSet fs = new FilterSet();
        fs.add(new MappingFilter(this.attribute, t.getA()));
        result.add(new Mapping(fs, t.getB() == null ? null : t.getB().getName()));
      }
      return result;
    }
  }
}
