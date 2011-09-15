/**
 * This file is part of VisiCut.
 * 
 *     VisiCut is free software: you can redistribute it and/or modify
 *     it under the terms of the Lesser GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *    VisiCut is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     Lesser GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with VisiCut.  If not, see <http://www.gnu.org/licenses/>.
 **/
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.visicut.gui.mappingdialog;

import com.t_oster.liblasercut.platform.Util;
import com.t_oster.visicut.gui.beans.EditableTablePanel;
import com.t_oster.visicut.gui.beans.EditableTableProvider;
import com.t_oster.visicut.misc.Helper;
import com.t_oster.visicut.model.MaterialProfile;
import com.t_oster.visicut.model.mapping.FilterSet;
import com.t_oster.visicut.model.mapping.Mapping;
import com.t_oster.visicut.model.mapping.MappingFilter;
import java.awt.Color;
import java.awt.Component;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;

/**
 *
 * @author thommy
 */
public class MappingTable extends EditableTablePanel implements EditableTableProvider, ListSelectionListener
{

  public MappingTable()
  {
    this.setTableModel(model);
    this.setProvider(this);
    this.addListSelectionListener(this);
    this.getTable().getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer()
    {

      @Override
      public Component getTableCellRendererComponent(JTable jtable, Object o, boolean bln, boolean bln1, int i, int i1)
      {
        Component c = super.getTableCellRendererComponent(jtable, o, bln, bln1, i, i1);
        if (o instanceof FilterSet && c instanceof JLabel)
        {
          String text = "";
          if (((FilterSet) o).isEmpty())
          {
            text = "Everything";
          }
          else
          {
            for (MappingFilter f : (FilterSet) o)
            {
              if (!"".equals(text))
              {
                text += "<td>AND</td>";
              }
              text += "<td>"+f.getAttribute().replace("_", " ") + (f.isInverted() ? " IS NOT" : " IS")+"</td>";
              if (f.getValue() instanceof Color)
              {
                text += "<td bgcolor=" + Helper.toHtmlRGB((Color) f.getValue()) + ">&nbsp;&nbsp;</td>";
              }
              else
              {
                text += "<td>"+f.getValue().toString()+"</td>";
              }
            }
          }
          ((JLabel) c).setText("<html><table><tr>" + text + "</tr></table></html>");
        }
        return c;
      }
    });
  }
  private MappingListModel model = new MappingListModel();
  protected Mapping selectedMapping = null;
  public static final String PROP_SELECTEDMAPPING = "selectedMapping";

  /**
   * Get the value of selectedMapping
   *
   * @return the value of selectedMapping
   */
  public Mapping getSelectedMapping()
  {
    return selectedMapping;
  }

  /**
   * Set the value of selectedMapping
   *
   * @param selectedMapping new value of selectedMapping
   */
  public void setSelectedMapping(Mapping selectedMapping)
  {
    Mapping oldSelectedMapping = this.selectedMapping;
    this.selectedMapping = selectedMapping;
    firePropertyChange(PROP_SELECTEDMAPPING, oldSelectedMapping, selectedMapping);
    if (Util.differ(oldSelectedMapping, selectedMapping))
    {
      if (selectedMapping == null)
      {
        this.clearSelection();
      }
      else
      {
        this.setSelectedRow(mappings.indexOf(selectedMapping));
      }
    }
  }

  @Override
  public void valueChanged(ListSelectionEvent lse)
  {
    int idx = this.getSelectedRow();
    if (idx >= 0)
    {
      this.setSelectedMapping(mappings.get(idx));
    }
  }
  protected List<Mapping> mappings = null;
  public static final String PROP_MAPPINGS = "mappings";

  /**
   * Get the value of mappings
   *
   * @return the value of mappings
   */
  public List<Mapping> getMappings()
  {
    return mappings;
  }

  /**
   * Set the value of mappings
   *
   * @param mappings new value of mappings
   */
  public void setMappings(List<Mapping> mappings)
  {
    List<Mapping> oldMappings = this.mappings;
    this.mappings = mappings;
    firePropertyChange(PROP_MAPPINGS, oldMappings, mappings);
    this.model.setMappings(mappings);
    this.setObjects((List) mappings);
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
   * Set the value of material
   *
   * @param material new value of material
   */
  public void setMaterial(MaterialProfile material)
  {
    MaterialProfile oldMaterial = this.material;
    this.material = material;
    firePropertyChange(PROP_MATERIAL, oldMaterial, material);
  }

  public MappingListModel getModel()
  {
    return this.model;
  }

  public Object getNewInstance()
  {
    Mapping m = new Mapping();
    m.setFilterSet(new FilterSet());
    m.setProfileName("cut line");
    return m;
  }

  public Object editObject(Object o)
  {
    if (o instanceof Mapping)
    {
      EditMappingDialogPanel d = new EditMappingDialogPanel();
      d.setCurrentMapping(((Mapping) o).clone());
      d.setMaterial(this.getMaterial());
      if (JOptionPane.showConfirmDialog(this, d, "Edit Mapping", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION)
      {
        return d.getCurrentMapping();
      }
    }
    return null;
  }
}
