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

import com.t_oster.visicut.misc.Helper;
import java.awt.Color;
import java.awt.Component;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * A list Renderer, which allowes to render ImageListable Objects
 * whith their Image, Name and Tooltip and allowes disbeling
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class ImageListableList extends JList
{

  private DefaultListModel mappingListModel = new DefaultListModel();
  private DefaultListCellRenderer cellrenderer = new DefaultListCellRenderer()
  {

    @Override
    public Component getListCellRendererComponent(JList jlist, Object o, int i, boolean bln, boolean bln1)
    {
      Component c = super.getListCellRendererComponent(jlist, o, i, bln, bln1);
      if (c instanceof JLabel)
      {
        JLabel l = (JLabel) c;
        if (o instanceof ImageListable)
        {

          ImageListable item = (ImageListable) o;
          String label = "<html><table cellpadding=3><tr>";
//          if (item.getThumbnailPath() != null)
//          {
//            File f = new File(item.getThumbnailPath());
//            if (f.exists())
//            {
//              label += "<td height=80>"+Helper.imgTag(f, 64, 64)+"</td>";
//            }
//          }
          if (ImageListableList.this.isDisabled(o))
          {
            label += "<td width=3><td>";
            label += "<font color=" + Helper.toHtmlRGB((Color) UIManager.get("ComboBox.disabledForeground")) + ">";
            label += item.toString() + " (" + ImageListableList.this.disableReasons.get(o) + ")</font></td></tr></table></html>";
            l.setFocusable(false);
            l.setEnabled(false);
          }
          else
          {
            label += "</td><td width=3><td>" + item.toString() + "</td></tr></table></html>";
          }
          l.setText(label);
          l.setToolTipText(item.getDescription());
        }
        else
        {
          if (o == null)
          {
            l.setText("Please select");
          }
          else if (ImageListableList.this.isDisabled(o))
          {
            l.setText(o.toString() + " (" + ImageListableList.this.disableReasons.get(o) + ")");
            l.setFocusable(false);
            l.setEnabled(false);
          }
          else
          {
            l.setText(o.toString());
          }
        }
      }
      return c;
    }
  };

  public ImageListableList()
  {
    this.setModel(mappingListModel);
    this.setCellRenderer(cellrenderer);
    this.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    this.addListSelectionListener(new ListSelectionListener()
    {

      int oldSelectedIndex = -1;

      public void valueChanged(ListSelectionEvent lse)
      {
        if (ImageListableList.this.isDisabled(ImageListableList.this.getSelectedValue()))
        {
          ImageListableList.this.setSelectedIndex(oldSelectedIndex);
        }
        else
        {
          oldSelectedIndex = ImageListableList.this.getSelectedIndex();
        }
      }
    });
  }
  private Map<Object, String> disableReasons = new LinkedHashMap<Object, String>();

  public void setDisabled(Object o, boolean disabled, String reason)
  {
    if (disabled)
    {
      this.disableReasons.put(o, reason);
    }
    else
    {
      this.disableReasons.remove(o);
    }
  }

  public void setDisabled(Object o, boolean disabled)
  {
    this.setDisabled(o, disabled, "disabled");
  }

  public int getItemCount()
  {
    return mappingListModel.getSize();
  }

  public Object getItemAt(int i)
  {
    return mappingListModel.elementAt(i);
  }

  public boolean isDisabled(Object o)
  {
    return this.disableReasons.containsKey(o);
  }

  public void clearList()
  {
    this.mappingListModel.clear();
  }

  public void addItem(Object o)
  {
    this.mappingListModel.addElement(o);
  }
}
