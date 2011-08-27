/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.visicut.gui.mappingdialog;

import com.t_oster.visicut.model.LaserProfile;
import com.t_oster.visicut.model.MaterialProfile;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
  private JRadioButton[] buttons = new JRadioButton[0];

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
      for (int i = 0; i < this.getMaterial().getLaserProfiles().length; i++)
      {
        if (this.getMaterial().getLaserProfile(i).equals(selectedCuttingProfile))
        {
          this.buttons[i].setSelected(true);
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
    this.buttons = new JRadioButton[0];
    if (this.getMaterial() != null && this.getMaterial().getLaserProfiles() != null)
    {
      this.buttons = new JRadioButton[this.getMaterial().getLaserProfiles().length];
      this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
      int i = 0;
      for (LaserProfile l : this.getMaterial().getLaserProfiles())
      {
        JiconRadioButton b = new JiconRadioButton();
        b.setText(l.getName());
        b.setLabelIcon(l.getPreviewThumbnail());
        b.addActionListener(this);
        this.group.add(b);
        b.setVisible(true);
        this.add(b);
        this.buttons[i++] = b;
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
        for (int i = 0; i < this.buttons.length; i++)
        {
          if (this.buttons[i].equals(b))
          {
            this.setSelectedLaserProfile(this.material.getLaserProfile(i));
          }
        }
      }
    }
  }
}
