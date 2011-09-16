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
package com.t_oster.visicut.gui.mappingdialog;

import com.t_oster.visicut.model.MaterialProfile;
import com.t_oster.visicut.model.VectorProfile;
import com.t_oster.visicut.model.mapping.Mapping;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.List;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author thommy
 */
public class MappingListModel extends DefaultTableModel
{

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
    propertyChangeSupport.firePropertyChange(PROP_MATERIAL, oldMaterial, material);
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
    propertyChangeSupport.firePropertyChange(PROP_MAPPINGS, oldMappings, mappings);
  }
  private PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

  /**
   * Add PropertyChangeListener.
   *
   * @param listener
   */
  public void addPropertyChangeListener(PropertyChangeListener listener)
  {
    propertyChangeSupport.addPropertyChangeListener(listener);
  }

  /**
   * Remove PropertyChangeListener.
   *
   * @param listener
   */
  public void removePropertyChangeListener(PropertyChangeListener listener)
  {
    propertyChangeSupport.removePropertyChangeListener(listener);
  }


  private String[] columns = new String[]{"Filters", "Profile", "Use Outline"};
  
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
    return this.mappings == null ? 0 : mappings.size();
  }

  @Override
  public Object getValueAt(int y, int x)
  {
    switch (x)
    {
      case 0:
        return mappings.get(y).getFilterSet();
      case 1:
        return (mappings.get(y).getProfileName());
      case 2:
        return mappings.get(y).getFilterSet().isUseOuterShape();
    }
    return null;
  }

  @Override
  public Class<?> getColumnClass(int i)
  {
    return i == 2 ? Boolean.class : String.class;
  }
  
  
  @Override
  public boolean isCellEditable(int y, int x)
  {
    return x==2 && material != null && material.getLaserProfile(mappings.get(y).getProfileName()) instanceof VectorProfile;
  }

  @Override
  public void setValueAt(Object o, int y, int x)
  {
    if (x==2)
    {
      this.getMappings().get(y).getFilterSet().setUseOuterShape((Boolean) o);
      this.fireTableChanged(new TableModelEvent(this, 0x0));
    }
  }

}
