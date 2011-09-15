/**
 * This file is part of VisiCut.
 * 
 *     VisiCut is free software: you can redistribute it and/or modify
 *     it under the terms of the Lesser GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *    VisiCut is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     Lesser GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with VisiCut.  If not, see <http://www.gnu.org/licenses/>.
 **/

package com.t_oster.visicut.gui.mappingdialog;

import com.t_oster.visicut.model.LaserProfile;
import com.t_oster.visicut.model.MaterialProfile;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedList;
import java.util.List;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
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
  private List<JRadioButton> buttons = new LinkedList<JRadioButton>();

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
      for (int i = 0; i < this.getMaterial().getLaserProfiles().size(); i++)
      {
        if (this.getMaterial().getLaserProfiles().get(i).equals(selectedCuttingProfile))
        {
          this.buttons.get(i).setSelected(true);
          break;
        }
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
      int i = 0;
      for (LaserProfile l : this.getMaterial().getLaserProfiles())
      {
        JiconRadioButton b = new JiconRadioButton();
        b.setText(l.getName());
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
      this.setVisible(true);
      this.validate();
    }
  }

  public void actionPerformed(ActionEvent ae)
  {
    if (ae.getSource() instanceof JiconRadioButton)
    {
      JiconRadioButton b = (JiconRadioButton) ae.getSource();
      if (b.isSelected())
      {
        this.setSelectedLaserProfile(this.material.getLaserProfiles().get(
          this.buttons.indexOf(b)));
      }
    }
  }
}
