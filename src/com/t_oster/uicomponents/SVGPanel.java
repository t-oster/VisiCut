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

import com.kitfox.svg.SVGDiagram;
import com.kitfox.svg.SVGException;
import com.kitfox.svg.SVGRoot;
import com.kitfox.svg.SVGUniverse;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JPanel;

/**
 *
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class SVGPanel extends JPanel {

  private File svgFile = null;

  public File getSvgFile()
  {
    return svgFile;
  }

  public void setSvgFile(File svgFile)
  {
    this.svgFile = svgFile;
    this.root = null;
  }
  
  private SVGRoot root;
  
  @Override
  protected void paintComponent(Graphics g)
  {
    super.paintComponent(g);
    if (this.svgFile != null && g instanceof Graphics2D)
    {
      try
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
        if (root == null)
        {
          SVGUniverse u = new SVGUniverse();
          root = u.getDiagram(u.loadSVG(new FileInputStream(svgFile), svgFile.getName())).getRoot(); 
        }
        Rectangle2D bb = root.getBoundingBox();
        double factor = Math.min(w / (double) bb.getWidth(), h / (double) bb.getHeight());
        Graphics2D gg = (Graphics2D) g;
        AffineTransform bak = gg.getTransform();
        gg.translate(x-bb.getX(), y-bb.getY());
        gg.scale(factor, factor);
        root.render(gg);
        gg.setTransform(bak);
      }
      catch (SVGException ex)
      {
        Logger.getLogger(SVGPanel.class.getName()).log(Level.SEVERE, null, ex);
      }
      catch (IOException ex)
      {
        Logger.getLogger(SVGPanel.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
  }

}
