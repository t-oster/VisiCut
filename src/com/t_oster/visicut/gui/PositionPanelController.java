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
package com.t_oster.visicut.gui;

import com.t_oster.visicut.VisicutModel;
import com.t_oster.uicomponents.PositionPanel;
import com.t_oster.visicut.misc.Helper;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 *
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class PositionPanelController extends EditRectangleController implements PropertyChangeListener
{

  private PositionPanel pp;
  private VisicutModel vm;

  public PositionPanelController(PositionPanel p, VisicutModel v)
  {
    this.pp = p;
    this.vm = v;
    this.vm.addPropertyChangeListener(this);
    this.pp.addPropertyChangeListener(this);
  }

  private boolean ignorePpUpdates = false;
  
  public void propertyChange(PropertyChangeEvent pce)
  {
    if (getSelectedSet() != null)
    {
      if (pce.getSource().equals(pp) && !ignorePpUpdates)
      {
        if (PositionPanel.PROP_RECTANGLE.equals(pce.getPropertyName()))
        {
          Rectangle2D src = VisicutModel.getInstance().getSelectedPart().getBoundingBox();
          AffineTransform t = getSelectedSet().getTransform();
          t.preConcatenate(Helper.getTransform(src, this.pp.getRectangle()));
          getSelectedSet().setTransform(t);
          this.vm.firePartUpdated(VisicutModel.getInstance().getSelectedPart());
        }
        else if (PositionPanel.PROP_ANGLE.equals(pce.getPropertyName()))
        {
          getSelectedSet().rotateAbsolute(pp.getAngle());
          this.vm.firePartUpdated(VisicutModel.getInstance().getSelectedPart());
        }
      }
      else if (pce.getSource().equals(vm))
      {
        if (VisicutModel.PROP_SELECTEDPART.equals(pce.getPropertyName())
          || VisicutModel.PROP_PLF_FILE_CHANGED.equals(pce.getPropertyName())
          || VisicutModel.PROP_PLF_PART_UPDATED.equals(pce.getPropertyName()))
        {
          this.ignorePpUpdates = true;
          this.pp.setRectangle(VisicutModel.getInstance().getSelectedPart().getBoundingBox());
          AffineTransform t = getSelectedSet().getTransform();
          this.pp.setAngle(t != null ? Helper.getRotationAngle(t) : 0);
          this.ignorePpUpdates = false;
        }
      }
    }
  }
}
