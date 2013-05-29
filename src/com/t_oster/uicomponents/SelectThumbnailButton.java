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

import com.t_oster.visicut.misc.ExtensionFilter;
import com.t_oster.visicut.misc.Helper;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.FilenameFilter;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.filechooser.FileFilter;

/**
 * This Button displays a Thumbnail if a thumbnailPath String is given.
 * If clicked, it displays a File Selection Dialog, which can select
 * PNG files and sets its Thumbnail if a PNG file is selected.
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class SelectThumbnailButton extends JButton implements ActionListener
{

  FileFilter filter;
  
  public SelectThumbnailButton(String path)
  {
    filter = new ExtensionFilter(new String[]{".png",".jpg",".jpeg"}, java.util.ResourceBundle.getBundle("com/t_oster/uicomponents/resources/SelectThumbnailButton").getString("ICON FILES (*.PNG,*.JPG)"));
    this.setThumbnailPath(path);
    this.addActionListener(this);
    this.prepareMenu();
  }

  private void prepareMenu()
  {
    final JPopupMenu menu = new JPopupMenu();
    JMenuItem mi = new JMenuItem("remove");
    mi.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent ae)
      {
        SelectThumbnailButton.this.setThumbnailPath(null);
      }
    });
    menu.add(mi);
    this.addMouseListener(new MouseListener(){

      public void mouseClicked(MouseEvent me)
      {
        if (me.getButton() == MouseEvent.BUTTON3 && SelectThumbnailButton.this.getThumbnailPath() != null)
        {
          menu.show(SelectThumbnailButton.this, me.getX(), me.getY());
        }
      }

      public void mousePressed(MouseEvent me)
      {
      }

      public void mouseReleased(MouseEvent me)
      {
      }

      public void mouseEntered(MouseEvent me)
      {
      }

      public void mouseExited(MouseEvent me)
      {
      }
    });
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
      this.setText("<html><table cellpadding=3><tr><td>"+Helper.imgTag(this.getClass().getResource("resources/no-image.png"), 64, 64)+"</td></tr></table></html>");
    }
    else
    {
      File f = new File(thumbnailPath);
      if (f.exists())
      {
        this.setText("<html><table cellpadding=3><tr><td>"+Helper.imgTag(f, 64, 64)+"</td></tr></table></html>");
      }
      else
      {
        this.setText(java.util.ResourceBundle.getBundle("com/t_oster/uicomponents/resources/SelectThumbnailButton").getString("FILE NOT FOUND"));
      }
    }
  }

  public void actionPerformed(ActionEvent ae)
  {
    if (Helper.isMacOS())
    {
      FileDialog fd = new FileDialog((Frame) null, java.util.ResourceBundle.getBundle("com/t_oster/uicomponents/resources/SelectThumbnailButton").getString("PLEASE SELECT A THUMBNAIL"));
      fd.setMode(FileDialog.LOAD);
      fd.setFilenameFilter(new FilenameFilter()
      {

        public boolean accept(File file, String string)
        {
          return filter.accept(new File(file, string));
        }

      });
      if (getDefaultDirectory() != null)
      {
        fd.setDirectory(getDefaultDirectory().getAbsolutePath());
      }
      if (getThumbnailPath() != null)
      {
        File tb = new File(getThumbnailPath());
        fd.setDirectory(tb.getParent());
        fd.setFile(tb.getName());
      }
      fd.setVisible(true);
      if (fd.getFile() != null)
      {
        File tb = new File(fd.getDirectory(), fd.getFile());
        setThumbnailPath(tb.getAbsolutePath());
      }
    }
    else
    {
      JFileChooser fc = new JFileChooser();
      fc.setAcceptAllFileFilterUsed(false);
      fc.addChoosableFileFilter(filter);
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
}
