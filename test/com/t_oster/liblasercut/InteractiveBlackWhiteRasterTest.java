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
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.liblasercut;

import com.kitfox.svg.SVGUniverse;
import com.kitfox.svg.app.beans.SVGIcon;
import com.t_oster.liblasercut.BlackWhiteRaster.DitherAlgorithm;
import com.t_oster.liblasercut.utils.BufferedImageAdapter;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

/**
 *
 * @author thommy
 */
public class InteractiveBlackWhiteRasterTest
{

  private final ProgressWindow pw = new ProgressWindow();
  
  public static void main(String[] args) throws MalformedURLException, IOException
  {
    new InteractiveBlackWhiteRasterTest().runInteractiveTest();
  }

  private void runInteractiveTest() throws MalformedURLException, IOException
  {
    JComboBox cb = new JComboBox();

    for (DitherAlgorithm da : BlackWhiteRaster.DitherAlgorithm.values())
    {
      cb.addItem(da);
    }
    int image = 0;


    while (JOptionPane.showConfirmDialog(
      null, cb, "Waehlen Sie einen Algorithmus aus", JOptionPane.OK_CANCEL_OPTION)
      == JOptionPane.OK_OPTION)
    {
      DitherAlgorithm da = (DitherAlgorithm) cb.getSelectedItem();
      BufferedImage test;
      if (image < 1)
      {
        SVGUniverse univ = new SVGUniverse();
        URI svg = univ.loadSVG(new File("test/files/tux.svg").toURI().toURL());
        SVGIcon icon = new SVGIcon();
        icon.setSvgURI(svg);
        icon.setAntiAlias(false);
        icon.setClipToViewbox(false);
        icon.setScaleToFit(false);

        test = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics g = test.getGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0,0,test.getWidth(), test.getHeight());
        icon.paintIcon(null, g, 0, 0);
      }
      else
      {
        test = ImageIO.read(new File("test/files/rastertest.png"));
      }
      
      ProgressListener l = new ProgressListener(){

        public void progressChanged(Object source, int percent)
        {
          pw.setState(percent);
        }

        public void taskChanged(Object source, String taskName)
        {
          throw new UnsupportedOperationException("Not supported yet.");
        }
        
      };
      pw.setVisible(true);
      BlackWhiteRaster bwr = new BlackWhiteRaster(
        new BufferedImageAdapter(test), da, l);
      for (int x = 0; x < bwr.getWidth(); x++)
      {
        for (int y = 0; y < bwr.getHeight(); y++)
        {
          test.setRGB(x, y, (bwr.isBlack(x, y) ? Color.BLACK : Color.WHITE).getRGB());
        }
      }
      pw.setVisible(false);
      JOptionPane.showConfirmDialog(null, new JLabel(new ImageIcon(test)), "Tada", JOptionPane.OK_OPTION);
      image = (image + 1) % 2;
    }
  }
}
