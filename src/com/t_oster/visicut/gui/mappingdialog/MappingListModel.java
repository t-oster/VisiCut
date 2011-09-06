/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.visicut.gui.mappingdialog;

import com.t_oster.visicut.model.MaterialProfile;
import com.t_oster.visicut.model.VectorProfile;
import com.t_oster.visicut.model.mapping.Mapping;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.List;
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
    }
  }

}
