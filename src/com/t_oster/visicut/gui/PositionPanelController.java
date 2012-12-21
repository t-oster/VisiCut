/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.visicut.gui;

import com.t_oster.visicut.VisicutModel;
import com.t_oster.visicut.gui.beans.PositionPanel;
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

  public void propertyChange(PropertyChangeEvent pce)
  {
    if (pce.getSource().equals(pp))
    {
      if (PositionPanel.PROP_RECTANGLE.equals(pce.getPropertyName()))
      {
        Rectangle2D src = getSelectedSet().getBoundingBox();
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
        ||VisicutModel.PROP_PLF_FILE_CHANGED.equals(pce.getPropertyName()))
      {
        if (getSelectedSet() != null)
        {
          this.pp.setRectangle(getSelectedSet().getBoundingBox());
          AffineTransform t = getSelectedSet().getTransform();
          this.pp.setAngle(t != null ? Helper.getRotationAngle(t) : 0);
        }
      }
    }
  }
}
