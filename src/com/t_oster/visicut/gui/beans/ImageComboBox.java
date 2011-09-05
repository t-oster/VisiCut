/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.visicut.gui.beans;

import com.t_oster.visicut.gui.ImageListable;
import java.awt.Component;
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
      if (o instanceof ImageListable && c instanceof JLabel)
      {
        JLabel l = (JLabel) c;
        ImageListable item = (ImageListable) o;
        String label = "<html><table cellpadding=3><tr><td>";
        if (item.getThumbnailPath() != null)
        {
          label += "<img width=64 height=64 src=file://" + item.getThumbnailPath() + "/> ";
        }
        label += "</td><td width=3><td>" + item.getName() + "</td></tr></table></html>";
        l.setText(label);
        l.setToolTipText(item.getDescription());
      }
      return c;
    }
  };

  public ImageComboBox()
  {
    this.setRenderer(cellrenderer);
  }
}
