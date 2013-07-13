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
package com.t_oster.visicut.gui.propertypanel;

import com.t_oster.liblasercut.LaserProperty;
import com.t_oster.visicut.VisicutModel;
import com.t_oster.visicut.managers.LaserPropertyManager;
import com.t_oster.visicut.model.LaserDevice;
import com.t_oster.visicut.model.LaserProfile;
import com.t_oster.visicut.model.MaterialProfile;
import com.t_oster.visicut.model.PlfPart;
import com.t_oster.visicut.model.Raster3dProfile;
import com.t_oster.visicut.model.RasterProfile;
import com.t_oster.visicut.model.VectorProfile;
import com.t_oster.visicut.model.mapping.Mapping;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class PropertiesPanel extends javax.swing.JPanel implements PropertyChangeListener
{

  private Map<LaserProfile, PropertyPanel> panels = new LinkedHashMap<LaserProfile, PropertyPanel>();
  /**
   * Creates new form PropertiesPanel
   */
  public PropertiesPanel()
  {
    initComponents();
    VisicutModel.getInstance().addPropertyChangeListener(this);
    this.setVisible(true);
  }
  
  private void reloadPanels()
  {
    panels.clear();
    this.updatePanels();
  }
  
  private ActionListener saveListener = new ActionListener()
  {
    public void actionPerformed(ActionEvent ae)
    {
      if (ae.getSource() instanceof PropertyPanel)
      {
        PropertyPanel p = (PropertyPanel) ae.getSource();
        for (Entry<LaserProfile, PropertyPanel> e : panels.entrySet())
        {
          if (p == e.getValue())
          {
            LaserDevice ld = VisicutModel.getInstance().getSelectedLaserDevice();
            MaterialProfile mp = VisicutModel.getInstance().getMaterial();
            float thickness = VisicutModel.getInstance().getMaterialThickness();
            LaserProfile lp = e.getKey();
            List<LaserProperty> props = p.getLaserProperties();
            try
            {
              LaserPropertyManager.getInstance().saveLaserProperties(ld, mp, lp, thickness, props);
              p.setModified(false);
            }
            catch (FileNotFoundException ex)
            {
              Logger.getLogger(PropertiesPanel.class.getName()).log(Level.SEVERE, null, ex);
            }
            catch (IOException ex)
            {
              Logger.getLogger(PropertiesPanel.class.getName()).log(Level.SEVERE, null, ex);
            }
            return;
          }
        }
      }
    }
  };
  
  private ActionListener revertListener = new ActionListener()
  {
    public void actionPerformed(ActionEvent ae)
    {
      if (ae.getSource() instanceof PropertyPanel)
      {
        PropertyPanel p = (PropertyPanel) ae.getSource();
        for (Entry<LaserProfile, PropertyPanel> e : panels.entrySet())
        {
          if (p == e.getValue())
          {
            LaserDevice ld = VisicutModel.getInstance().getSelectedLaserDevice();
            MaterialProfile mp = VisicutModel.getInstance().getMaterial();
            float thickness = VisicutModel.getInstance().getMaterialThickness();
            LaserProfile lp = e.getKey();
            try
            {
              p.setLaserProperties(LaserPropertyManager.getInstance().getLaserProperties(ld, mp, lp, thickness));
              p.setModified(false);
            }
            catch (FileNotFoundException ex)
            {
              Logger.getLogger(PropertiesPanel.class.getName()).log(Level.SEVERE, null, ex);
            }
            catch (IOException ex)
            {
              Logger.getLogger(PropertiesPanel.class.getName()).log(Level.SEVERE, null, ex);
            }
            return;
          }
        }
      }
    }
  };
  
  /**
   * Updates the view to show panels for the currently selected 
   * plf part. However if a laser-profile's properties have been changed,
   * the changed profiles are used.
   */
  private void updatePanels()
  {
    this.removeAll();
    LaserDevice ld = VisicutModel.getInstance().getSelectedLaserDevice();
    MaterialProfile mp = VisicutModel.getInstance().getMaterial();
    float thickness = VisicutModel.getInstance().getMaterialThickness();
    if (ld != null && VisicutModel.getInstance().getSelectedPart() != null && VisicutModel.getInstance().getSelectedPart().getMapping() != null)
    {
      for (Mapping m : VisicutModel.getInstance().getSelectedPart().getMapping())
      {
        if (m.getProfile() == null)
        {
          continue;
        }
        boolean unused = false;
        if (m.getFilterSet() != null)
        {
          unused = m.getFilterSet().getMatchingObjects(VisicutModel.getInstance().getSelectedPart().getGraphicObjects()).isEmpty();
        }
        else
        {
          unused = VisicutModel.getInstance().getSelectedPart().getUnmatchedObjects().isEmpty();
        }
        PropertyPanel p;
        if (panels.containsKey(m.getProfile()))
        {
          p = panels.get(m.getProfile());
          p.setMapping(m, unused);
        }
        else
        {
          p = new PropertyPanel();
          p.setMapping(m, unused);
          try
          {
            p.setLaserProperties(LaserPropertyManager.getInstance().getLaserProperties(ld, mp, m.getProfile(), thickness));
          }
          catch (Exception ex)
          {
            Logger.getLogger(PropertiesPanel.class.getName()).log(Level.SEVERE, null, ex);
          }
          p.addSaveListener(saveListener);
          p.addRevertListener(revertListener);
          panels.put(m.getProfile(), p);
        }
        p.setVisible(true);
        p.validate();
        if (!unused)
        {//only show panels with used mappings. See #157
          this.add(p);
        }
      }
      this.validate();
    }
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

    setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.LINE_AXIS));
  }// </editor-fold>//GEN-END:initComponents
  // Variables declaration - do not modify//GEN-BEGIN:variables
  // End of variables declaration//GEN-END:variables

  public void propertyChange(PropertyChangeEvent pce)
  {
    if (VisicutModel.PROP_MATERIAL.equals(pce.getPropertyName())
      || VisicutModel.PROP_MATERIALTHICKNESS.equals(pce.getPropertyName())
      || VisicutModel.PROP_SELECTEDLASERDEVICE.equals(pce.getPropertyName()))
    {
      reloadPanels();
    }
    else if(VisicutModel.PROP_SELECTEDPART.equals(pce.getPropertyName())
      || VisicutModel.PROP_PLF_PART_UPDATED.equals(pce.getPropertyName()))
    {
      updatePanels();
    }
  }
  
  public Map<LaserProfile, List<LaserProperty>> getPropertyMap()
  {
    Map<LaserProfile, List<LaserProperty>> result = new LinkedHashMap<LaserProfile, List<LaserProperty>>();
    //add all the properties from our panels
    for (Entry<LaserProfile, PropertyPanel> e : panels.entrySet())
    {
      result.put(e.getKey(), e.getValue().getLaserProperties());
    }
    //check if something is missing and try to load it from the manager
    LaserDevice ld = VisicutModel.getInstance().getSelectedLaserDevice();
    MaterialProfile mp = VisicutModel.getInstance().getMaterial();
    float thickness = VisicutModel.getInstance().getMaterialThickness();
    for(PlfPart p : VisicutModel.getInstance().getPlfFile())
    {
      if (p.getMapping() == null)
      {
        continue;
      }
      for (Mapping m : p.getMapping())
      {
        if (m != null && m.getProfile() != null)
        {
          LaserProfile lp = m.getProfile();
          if (!result.containsKey(lp))
          {
            List<LaserProperty> props = null;
            try
            {
              props = LaserPropertyManager.getInstance().getLaserProperties(ld, mp, lp, thickness);   
            }
            catch (Exception ex)
            {
            }
            if (props == null)
            {
              props = new LinkedList<LaserProperty>();
              if (lp instanceof VectorProfile)
              {
                props.add(ld.getLaserCutter().getLaserPropertyForVectorPart());
              }
              else if (lp instanceof RasterProfile)
              {
                props.add(ld.getLaserCutter().getLaserPropertyForRasterPart());
              }
              else if (lp instanceof Raster3dProfile)
              {
                props.add(ld.getLaserCutter().getLaserPropertyForRaster3dPart());
              }
            }
            result.put(lp, props);
          }
        }
      }
    }
    return result;
  }
}
