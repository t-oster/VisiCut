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
package com.t_oster.visicut.gui.mappingwizzard;

import com.t_oster.liblasercut.platform.Tuple;
import com.t_oster.visicut.gui.ImageListable;
import com.t_oster.visicut.gui.beans.ImageComboBox;
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
import java.io.File;
import java.util.LinkedList;
import java.util.List;
import javax.swing.DefaultCellEditor;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author thommy
 */
public class MappingWizzardTable extends JTable
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
    this.values = new LinkedList<Tuple<Object, LaserProfile>>();
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
    for (Tuple<Object,LaserProfile> tup:values)
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
      return i == 0 ? MappingWizzardTable.this.getAttribute() : "Laser Profile";
    }

    @Override
    public int getRowCount()
    {
      return MappingWizzardTable.this.values == null ? 0 : MappingWizzardTable.this.values.size();
    }

    @Override
    public Object getValueAt(int y, int x)
    {
      if (x == 0)
      {
        return MappingWizzardTable.this.values.get(y).getA();
      }
      else
      {
        return MappingWizzardTable.this.values.get(y).getB();
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
      MappingWizzardTable.this.values.get(y).setB((LaserProfile) o);
      this.fireTableCellUpdated(y, x);
    }
  };

  public MappingWizzardTable()
  {
    this.values = new LinkedList<Tuple<Object, LaserProfile>>();
    this.setModel(model);
    DefaultTableCellRenderer renderer = new DefaultTableCellRenderer()
    {

      @Override
      public Component getTableCellRendererComponent(JTable jtable, Object o, boolean bln, boolean bln1, int i, int i1)
      {
        Component c = super.getTableCellRendererComponent(jtable, o, bln, bln1, i, i1);
        if (c instanceof JLabel)
        {
          JLabel l = (JLabel) c;
          if (o == null)
          {
            l.setText("Ignore");
          }
          else if (o instanceof Color)
          {
            ((JLabel) c).setText("<html><table border=1><tr><td bgcolor=" + Helper.toHtmlRGB((Color) o) + ">&nbsp;&nbsp;&nbsp;&nbsp;</td></tr></table></html>");
          }
          else if (o instanceof ImageListable)
          {

            ImageListable item = (ImageListable) o;
            String label = "<html><table cellpadding=3><tr>";
            if (item.getThumbnailPath() != null)
            {
              File f = new File(item.getThumbnailPath());
              if (f.exists())
              {
                label += "<td height=80><img width=64 height=64 src=file://" + f.getAbsolutePath() + "/></td>";
              }
            }
            label += "</td><td width=3><td>" + item.getName() + "</td></tr></table></html>";
            l.setText(label);
            l.setToolTipText(item.getDescription());
          }
        }
        return c;
      }
    };
    this.getColumnModel().getColumn(0).setCellRenderer(renderer);
    this.getColumnModel().getColumn(1).setCellRenderer(renderer);
  }

  MappingSet getMappingSet()
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
}
