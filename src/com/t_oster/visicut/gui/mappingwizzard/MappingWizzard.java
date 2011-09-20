/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.visicut.gui.mappingwizzard;

import com.t_oster.visicut.gui.MainView;
import com.t_oster.visicut.model.MaterialProfile;
import com.t_oster.visicut.model.graphicelements.GraphicSet;
import com.t_oster.visicut.model.mapping.MappingSet;

/**
 *
 * @author oster
 */
class MappingWizzard extends JDialog
{
  private GraphicSet obejects;
  private MaterialProfile material;
  MappingWizzard(GraphicSet graphicObjects, MaterialProfile material)
  {
    this.material=material;
    this.objects = objects;
  }

  MappingSet getMappingSet()
  {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  MappingWizzard showDialog(MainView aThis)
  {
    throw new UnsupportedOperationException("Not yet implemented");
  }
  
}
