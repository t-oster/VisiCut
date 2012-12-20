package com.t_oster.visicut.gui;

import com.t_oster.visicut.VisicutModel;
import com.t_oster.visicut.model.graphicelements.GraphicSet;

/**
 * This class contains mehtods suitable for the PreviePanelKeyboardMouseHanlder
 * and the PositionPanelController
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class EditRectangleController
{
  protected GraphicSet getSelectedSet()
  {
    return VisicutModel.getInstance().getSelectedPart() == null ? null : VisicutModel.getInstance().getSelectedPart().getGraphicObjects();
  }
  
}
