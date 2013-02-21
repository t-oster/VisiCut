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

import com.t_oster.liblasercut.platform.Util;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import javax.swing.JPanel;

/**
 * A simple Image Panel, which displays a buffered image resized to the
 * panels current size
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class ImagePanel extends JPanel{

  private BufferedImage image = null;
  public static final String PROP_IMAGE = "image";

  public BufferedImage getImage()
  {
    return image;
  }

  public void setImage(BufferedImage image)
  {
    BufferedImage oldImage = this.image;
    this.image = image;
    if (Util.differ(oldImage, image))
    {
      firePropertyChange(PROP_IMAGE, oldImage, image);
      this.repaint();
    }
  }

  @Override
  protected void paintComponent(Graphics g)
  {
    super.paintComponent(g);
    if (this.image != null && g instanceof Graphics2D)
    {
      int x = 0;
      int y = 0;
      int w = this.getWidth();
      int h = this.getHeight();
      Insets i = this.getInsets();
      x += i.left;
      y += i.top;
      w -= x + i.right;
      h -= y + i.bottom;
      double factor = Math.min(w / (double) image.getWidth(), h / (double) image.getHeight());
      g.drawImage(image, x, y, (int) (image.getWidth()*factor), (int) (image.getHeight()*factor), null);
    }
  }
 
}
