/**
 * This file is part of VisiCut.
 * Copyright (C) 2011 Thomas Oster <thomas.oster@rwth-aachen.de>
 * RWTH Aachen University - 52062 Aachen, Germany
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
package com.t_oster.visicut.gui.beans;

import com.t_oster.visicut.VisicutModel;
import com.t_oster.visicut.model.PlfPart;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.LinkedList;
import java.util.List;
import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

/**
 *
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class SelectedObjectComboBox extends JComboBox
{
  private final List<ListDataListener> listeners = new LinkedList<ListDataListener>();
  private ComboBoxModel model = new ComboBoxModel(){

    public void setSelectedItem(Object o)
    {
      VisicutModel.getInstance().setSelectedPart((PlfPart) o);
    }

    public Object getSelectedItem()
    {
      return VisicutModel.getInstance().getSelectedPart();
    }

    public int getSize()
    {
      return VisicutModel.getInstance().getPlfFile().size();
    }

    public Object getElementAt(int i)
    {
      return VisicutModel.getInstance().getPlfFile().get(i);
    }

    public void addListDataListener(ListDataListener ll)
    {
      listeners.add(ll);
    }

    public void removeListDataListener(ListDataListener ll)
    {
      listeners.remove(ll);
    }

  };

  public SelectedObjectComboBox()
  {
    this.setModel(model);
    VisicutModel.getInstance().addPropertyChangeListener(new PropertyChangeListener(){

      public void propertyChange(PropertyChangeEvent pce)
      {
        if (VisicutModel.PROP_PLF_FILE_CHANGED.equals(pce.getPropertyName()))
        {
          fireAllChanged();
        }
        else if (VisicutModel.PROP_PLF_PART_ADDED.equals(pce.getPropertyName()))
        {
          firePartAdded();
        }
        else if (VisicutModel.PROP_PLF_PART_REMOVED.equals(pce.getPropertyName()))
        {
          firePartRemoved();
        }
        else if (VisicutModel.PROP_PLF_PART_UPDATED.equals(pce.getPropertyName()))
        {
          firePartUpdated();
        }
      }

      private void fireAllChanged()
      {
        ListDataEvent ev = new ListDataEvent(model, ListDataEvent.CONTENTS_CHANGED, 0, 0);
        for (ListDataListener l : listeners)
        {
          l.contentsChanged(ev);
        }
      }

      private void firePartAdded()
      {
        //TODO
      }

      private void firePartRemoved()
      {
        //TODO
      }

      private void firePartUpdated()
      {
        //TODO
      }
    });
  }

}
