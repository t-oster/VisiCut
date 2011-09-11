/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.visicut.gui.beans;

import com.t_oster.visicut.gui.ImageListable;
import java.awt.Component;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;

/**
 *
 * @author thommy
 */
public class ImageComboBox extends JComboBox
{

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
          String label = "<html><table cellpadding=3><tr><td>";
          if (item.getThumbnailPath() != null)
          {
            label += "<img width=64 height=64 src=file://" + item.getThumbnailPath() + "/> ";
          }
          if (ImageComboBox.this.isDisabled(o))
          {
            label += "</td><td width=3><td>" + item.getName() + "<br/>" + ImageComboBox.this.disableReasons.get(o) + "</td></tr></table></html>";
            l.setFocusable(false);
            l.setEnabled(false);
          }
          else
          {
            label += "</td><td width=3><td>" + item.getName() + "</td></tr></table></html>";
          }
          l.setText(label);
          l.setToolTipText(item.getDescription());
        }
        else
        {
          if (o==null)
          {
            l.setText("Please select");
          }
          else if (ImageComboBox.this.isDisabled(o))
          {
            l.setText(o.toString()+" ("+ImageComboBox.this.disableReasons.get(o)+")");
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

  public boolean isDisabled(Object o)
  {
    return this.disableReasons.containsKey(o);
  }

  public ImageComboBox()
  {
    this.setRenderer(cellrenderer);
  }
}
