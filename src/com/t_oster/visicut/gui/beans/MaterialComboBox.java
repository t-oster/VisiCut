/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.visicut.gui.beans;

import com.t_oster.visicut.model.MaterialProfile;
import java.awt.Component;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;

/**
 *
 * @author thommy
 */
public class MaterialComboBox extends JComboBox
{

  private DefaultListCellRenderer cellrenderer = new DefaultListCellRenderer()
  {

    @Override
    public Component getListCellRendererComponent(JList<?> jlist, Object o, int i, boolean bln, boolean bln1)
    {
      Component c = super.getListCellRendererComponent(jlist, o, i, bln, bln1);
      if (o instanceof MaterialProfile && c instanceof JLabel)
      {
        JLabel l = (JLabel) c;
        MaterialProfile p = (MaterialProfile) o;
        String label = "<html><table cellpadding=3><tr><td>";
        if (p.getThumbnailPath() != null)
        {
          label += "<img width=64 height=64 src=file://" + p.getThumbnailPath() + "/> ";
        }
        label += "</td><td width=3><td>" + p.getName() + "<br>"+ p.getDepth()+" mm</td></tr></table></html>";
        l.setText(label);
        l.setToolTipText(p.getDescription());
      }
      return c;
    }
  };
  
  public MaterialComboBox()
  {
    this.setRenderer(cellrenderer);
  }
}
