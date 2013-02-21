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

import com.t_oster.uicomponents.ImageListable;
import com.t_oster.visicut.misc.Helper;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.UIManager;
import javax.swing.plaf.metal.MetalComboBoxUI;

/**
 *
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class ImageComboBox extends JComboBox
{

  private class ModifiedRenderer implements ListCellRenderer
  {

    private ListCellRenderer decoratee;

    public ModifiedRenderer(ListCellRenderer decoratee)
    {
      this.decoratee = decoratee;
    }

    public Component getListCellRendererComponent(JList jlist, Object o, int i, boolean bln, boolean bln1)
    {
      Component c = decoratee.getListCellRendererComponent(jlist, o, i, bln, bln1);
      if (c instanceof JLabel)
      {
        JLabel l = (JLabel) c;
        if (o instanceof ImageListable && (((ImageListable) o).getThumbnailPath() != null || ImageComboBox.this.isDisabled(o)))
        {

          ImageListable item = (ImageListable) o;
          String label = "<html><table cellpadding=3><tr>";
          if (item.getThumbnailPath() != null)
          {
            File f = new File(item.getThumbnailPath());
            if (f.exists())
            {
              label += "<td height=80>"+Helper.imgTag(f,64,64)+"</td>";
            }
          }
          if (ImageComboBox.this.isDisabled(o))
          {
            label += "<td width=3><td>";
            label += "<font color=" + Helper.toHtmlRGB((Color) UIManager.get("ComboBox.disabledForeground")) + ">";
            label += item.toString() + "<br/>" + ImageComboBox.this.disableReasons.get(o) + "</font></td></tr></table></html>";
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
            l.setText(java.util.ResourceBundle.getBundle("com/t_oster/uicomponents/resources/ImageComboBox").getString("PLEASE SELECT"));
          }
          else if (ImageComboBox.this.isDisabled(o))
          {
            l.setText(o.toString() + " (" + ImageComboBox.this.disableReasons.get(o) + ")");
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
    this.setDisabled(o, disabled, java.util.ResourceBundle.getBundle("com/t_oster/uicomponents/resources/ImageComboBox").getString("DISABLED"));
  }

  public boolean isDisabled(Object o)
  {
    return this.disableReasons.containsKey(o);
  }

  public ImageComboBox()
  {
    //For MAC Os displaying the correct size
    if (Helper.isMacOS())
    {
      this.setUI(new MetalComboBoxUI());
    }
    this.setRenderer(new ModifiedRenderer(this.getRenderer()));
    this.addActionListener(new ActionListener()
    {

      int oldSelectedIndex = -1;

      @Override
      public void actionPerformed(ActionEvent ae)
      {
        if (ImageComboBox.this.isDisabled(ImageComboBox.this.getSelectedItem()))
        {
          ImageComboBox.this.setSelectedIndex(oldSelectedIndex);
        }
        else
        {
          oldSelectedIndex = ImageComboBox.this.getSelectedIndex();
        }
      }
    });
  }
}
