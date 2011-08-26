/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.visicut.gui.mappingdialog;

import com.t_oster.visicut.model.VectorProfile;
import com.t_oster.visicut.model.MaterialProfile;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Enumeration;
import javax.swing.AbstractButton;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

/**
 *
 * @author thommy
 */
public class CuttingProfilesPanel extends JPanel implements ActionListener
{

  protected MaterialProfile material = new MaterialProfile();
  private ButtonGroup group = new ButtonGroup();
  private JRadioButton[] buttons = new JRadioButton[0];

  public CuttingProfilesPanel()
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
  protected VectorProfile selectedCuttingProfile = null;
  public static final String PROP_SELECTEDCUTTINGPROFILE = "selectedCuttingProfile";

  /**
   * Get the value of selectedCuttingProfile
   *
   * @return the value of selectedCuttingProfile
   */
  public VectorProfile getSelectedCuttingProfile()
  {
    return selectedCuttingProfile;
  }

  /**
   * Set the value of selectedCuttingProfile
   *
   * @param selectedCuttingProfile new value of selectedCuttingProfile
   */
  public void setSelectedCuttingProfile(VectorProfile selectedCuttingProfile)
  {
    VectorProfile oldSelectedCuttingProfile = this.selectedCuttingProfile;
    this.selectedCuttingProfile = selectedCuttingProfile;
    firePropertyChange(PROP_SELECTEDCUTTINGPROFILE, oldSelectedCuttingProfile, selectedCuttingProfile);
    if (selectedCuttingProfile == null)
    {
      for (JRadioButton b : this.buttons)
      {
        b.setSelected(false);
      }
    }
    else
    {
      for (int i = 0; i < this.getMaterial().getLineProfiles().length; i++)
      {
        if (this.getMaterial().getLineProfile(i).equals(selectedCuttingProfile))
        {
          this.buttons[i].setSelected(true);
        }
      }
    }
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
    if (this.getMaterial() != null && this.getMaterial().getLineProfiles() != null)
    {
      this.buttons = new JRadioButton[this.getMaterial().getLineProfiles().length];
      this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
      int i = 0;
      for (VectorProfile l : this.getMaterial().getLineProfiles())
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
            this.setSelectedCuttingProfile(this.material.getLineProfile(i));
          }
        }
      }
    }
  }
}
