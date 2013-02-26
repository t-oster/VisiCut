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

package com.t_oster.uicomponents;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;

/**
 * This class provides a default-cell-editor with an JComboBox component.
 * The difference to a default-cell editor is, that the combo-box
 * will show it's pop-up on the first click.
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class ComboBoxCellEditor extends DefaultCellEditor
{
  private JComboBox box;
  public JComboBox getComboBox()
  {
    return box;
  }
  
  public ComboBoxCellEditor()
  {
    super(new JComboBox());
    box = (JComboBox) this.getComponent();
    box.addFocusListener(new FocusListener(){
      public void focusGained(FocusEvent fe)
      {
        box.showPopup();
      }
      public void focusLost(FocusEvent fe){}
    });
  }
}
