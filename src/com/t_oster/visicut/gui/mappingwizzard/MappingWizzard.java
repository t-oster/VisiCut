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

import com.t_oster.visicut.model.MaterialProfile;
import com.t_oster.visicut.model.graphicelements.GraphicObject;
import com.t_oster.visicut.model.graphicelements.GraphicSet;
import com.t_oster.visicut.model.mapping.MappingSet;
import java.awt.Frame;
import java.util.LinkedList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JOptionPane;

/**
 *
 * @author oster
 */
public class MappingWizzard
{

  private GraphicSet objects;
  private MaterialProfile material;
  private JButton ok;
  private JButton cancel;
  private MappingWizzardTable tb;

  public MappingWizzard(GraphicSet graphicObjects, MaterialProfile material)
  {
    this.material = material;
    this.objects = graphicObjects;
  }
  protected MappingSet mappingSet = null;

  /**
   * Get the value of mappingSet
   *
   * @return the value of mappingSet
   */
  public MappingSet getMappingSet()
  {
    return mappingSet;
  }

  /**
   * Set the value of mappingSet
   *
   * @param mappingSet new value of mappingSet
   */
  private void setMappingSet(MappingSet mappingSet)
  {
    this.mappingSet = mappingSet;
  }


  public void showDialog(Frame parent)
  {
    List<String> attributes = new LinkedList<String>();
    for (GraphicObject g : objects)
    {
      for (String attribute : g.getAttributes())
      {
        if (!attributes.contains(attribute))
        {
          attributes.add(attribute);
        }
      }
    }
    SelectAttributeDialog d = new SelectAttributeDialog();
    d.setValues(attributes);
    if (JOptionPane.showConfirmDialog(parent, d, "Please select...", JOptionPane.OK_CANCEL_OPTION)==JOptionPane.CANCEL_OPTION)
    {
      tb = null;
      return;
    }
    String selectedAttribute = (String) d.getSelectedValue();
    MappingWizzardDialog mwd = new MappingWizzardDialog(parent, true);
    mwd.setAttribute(selectedAttribute);
    mwd.setMaterialProfile(this.material);
    mwd.setGraphicObjects(objects);
    mwd.setVisible(true);
    this.setMappingSet(mwd.getMappingSet());
  }
}
