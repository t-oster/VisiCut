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
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
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
  
  protected File defaultDirectory = null;

  /**
   * Get the value of defaultDirectory
   *
   * @return the value of defaultDirectory
   */
  public File getDefaultDirectory()
  {
    return defaultDirectory;
  }

  /**
   * Set the value of defaultDirectory
   *
   * @param defaultDirectory new value of defaultDirectory
   */
  public void setDefaultDirectory(File defaultDirectory)
  {
    this.defaultDirectory = defaultDirectory;
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
    if (thumbnailPath == null)
    {
      this.setText("No Image");
    }
    else
    {
      File f = new File(thumbnailPath);
      if (f.exists())
      {
        this.setText("<html><table cellpadding=3><tr><td><img width=64 height=64 src=file://" + f.getAbsolutePath() + "/></td></tr></table></html>");
      }
      else
      {
        this.setText("File not found");
      }
    }
  }

  public void actionPerformed(ActionEvent ae)
  {
    JFileChooser fc = new JFileChooser();
    fc.setAcceptAllFileFilterUsed(false);
    fc.addChoosableFileFilter(new ExtensionFilter(".png", "PNG Files (*.png)"));
    if (getDefaultDirectory() != null)
    {
      fc.setCurrentDirectory(getDefaultDirectory());
    }
    if (getThumbnailPath() != null)
    {
      fc.setSelectedFile(new File(getThumbnailPath()));
      fc.setCurrentDirectory(new File(getThumbnailPath()).getParentFile());
    }
    fc.setDialogType(JFileChooser.OPEN_DIALOG);
    if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
    {
      File thumb = fc.getSelectedFile();
      setThumbnailPath(thumb.getPath());
    }
  }
}
