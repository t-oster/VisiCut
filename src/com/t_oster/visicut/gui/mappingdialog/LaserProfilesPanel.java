/**
 * This file is part of VisiCut.
 * Copyright (C) 2011 Thomas Oster <thomas.oster@rwth-aachen.de>
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
package com.t_oster.visicut.gui.mappingdialog;

import com.t_oster.visicut.model.LaserProfile;
import com.t_oster.visicut.model.MaterialProfile;
import com.t_oster.visicut.model.RasterProfile;
import com.t_oster.visicut.model.VectorProfile;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedList;
import java.util.List;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

/**
 *
 * @author thommy
 */
public class LaserProfilesPanel extends JPanel implements ActionListener
{

  protected MaterialProfile material = new MaterialProfile();
  private ButtonGroup group = new ButtonGroup();
  private List<JiconRadioButton> buttons = new LinkedList<JiconRadioButton>();

  public LaserProfilesPanel()
  {
  }

  /**
   * Get the value of material
   *
   * @return the value of material
   */
  public MaterialProfile getMaterial()
  {
    return material;
  }

  /**
   * Set the value of material
   *
   * @param material new value of material
   */
  public void setMaterial(MaterialProfile material)
  {
    this.material = material;
    this.refresh();
  }
  protected LaserProfile selectedLaserProfile = null;
  public static final String PROP_SELECTEDLASERPROFILE = "selectedLaserProfile";

  /**
   * Get the value of selectedLaserProfile
   *
   * @return the value of selectedLaserProfile
   */
  public LaserProfile getSelectedLaserProfile()
  {
    return selectedLaserProfile;
  }

  /**
   * Set the value of selectedLaserProfile
   *
   * @param selectedLaserProfile new value of selectedLaserProfile
   */
  public void setSelectedLaserProfile(LaserProfile selectedCuttingProfile)
  {
    LaserProfile oldSelectedCuttingProfile = this.selectedLaserProfile;
    this.selectedLaserProfile = selectedCuttingProfile;
    if (selectedCuttingProfile == null)
    {
      group.clearSelection();
    }
    else
    {
      boolean found = false;
      for (JiconRadioButton b:this.buttons){
        if (b.getLabelText().equals(selectedCuttingProfile.getName())){
          b.setSelected(true);
          found=true;
          break;
        }
      }
      if (!found)
      {
        group.clearSelection();
      }
    }
    firePropertyChange(PROP_SELECTEDLASERPROFILE, oldSelectedCuttingProfile, selectedCuttingProfile);
    this.repaint();
  }

  private void refresh()
  {
    for (JRadioButton b : this.buttons)
    {
      this.group.remove(b);
      b.removeActionListener(this);
      this.remove(b);
    }
    this.buttons.clear();
    if (this.getMaterial() != null && this.getMaterial().getLaserProfiles() != null)
    {
      this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
      List<LaserProfile> rasterProfiles = new LinkedList<LaserProfile>();
      boolean lineProfileHere = false;
      for (LaserProfile l : this.getMaterial().getLaserProfiles())
      {
        if (!(l instanceof VectorProfile))
        {//Sort Vector from Raster Profiles
          rasterProfiles.add(l);
          continue;
        }
        if (!lineProfileHere)
        {
          lineProfileHere=true;
          JLabel lab = new JLabel("Line Profiles:");
          lab.setToolTipText("Line Profiles can only use Vectorgraphics.\nThe Laser will follow the lines in the graphic and cut or engrave,\nindependant of the line width");
          this.add(lab);
        }
        this.addProfileButton(l);
      }
      if (rasterProfiles.size()>0)
      {
        JLabel l = new JLabel("Raster Profiles");
        l.setToolTipText("Raster Profiles can use any kind of graphics.\nThe Graphic will be rastered and lasered line by line.");
        this.add(l);
      }
      for (LaserProfile l:rasterProfiles)
      {
        this.addProfileButton(l);
      }
      this.setVisible(true);
      this.validate();
    }
  }

  private void addProfileButton(LaserProfile l)
  {
    JiconRadioButton b = new JiconRadioButton();
    b.setLabelText(l.getName());
    if (l.getPreviewThumbnail().exists())
    {
      b.setLabelIcon(l.getPreviewThumbnail());
    }
    b.addActionListener(this);
    b.setToolTipText(l.getDescription());
    this.group.add(b);
    b.setVisible(true);
    this.add(b);
    this.buttons.add(b);
  }

  public void actionPerformed(ActionEvent ae)
  {
    if (ae.getSource() instanceof JiconRadioButton)
    {
      JiconRadioButton b = (JiconRadioButton) ae.getSource();
      if (b.isSelected())
      {
        this.setSelectedLaserProfile(this.material.getLaserProfile(b.getLabelText()));
      }
    }
  }
}
