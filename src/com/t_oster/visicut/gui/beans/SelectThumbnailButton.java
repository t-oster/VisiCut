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
package com.t_oster.visicut.gui.beans;

import com.t_oster.visicut.misc.ExtensionFilter;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import javax.swing.JButton;
import javax.swing.JFileChooser;

/**
 * This Button displays a Thumbnail if a thumbnailPath String is given.
 * If clicked, it displays a File Selection Dialog, which can select
 * PNG files and sets its Thumbnail if a PNG file is selected.
 * @author thommy
 */
public class SelectThumbnailButton extends JButton implements ActionListener
{

  public SelectThumbnailButton(String path)
  {
    this.setThumbnailPath(path);
    this.addActionListener(this);
  }

  public SelectThumbnailButton()
  {
    this(null);
  }
  protected String thumbnailPath = null;
  public static final String PROP_THUMBNAILPATH = "thumbnailPath";

  /**
   * Get the value of thumbnailPath
   *
   * @return the value of thumbnailPath
   */
  public String getThumbnailPath()
  {
    return thumbnailPath;
  }

  /**
   * Set the value of thumbnailPath
   * This will repaint the Button to display 
   * the Thubnnail found on the given path
   *
   * @param thumbnailPath new value of thumbnailPath
   */
  public final void setThumbnailPath(String thumbnailPath)
  {
    String oldThumbnailPath = this.thumbnailPath;
    this.thumbnailPath = thumbnailPath;
    firePropertyChange(PROP_THUMBNAILPATH, oldThumbnailPath, thumbnailPath);
    this.setText("<html><table cellpadding=3><tr><td><img width=64 height=64 src=file://" + getThumbnailPath() + "/></td></tr></table></html>");
  }

  public void actionPerformed(ActionEvent ae)
  {
    JFileChooser fc = new JFileChooser();
    fc.setAcceptAllFileFilterUsed(false);
    fc.addChoosableFileFilter(new ExtensionFilter(".png", "PNG Files (*.png)"));
    if (getThumbnailPath() != null)
    {
      fc.setSelectedFile(new File(getThumbnailPath()));
      fc.setCurrentDirectory(new File(getThumbnailPath()).getParentFile());
    }
    fc.setDialogType(JFileChooser.OPEN_DIALOG);
    if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
    {
      File thumb = fc.getSelectedFile();
      setThumbnailPath(thumb.getAbsolutePath());
    }
  }
}
