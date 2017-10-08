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
package com.t_oster.visicut.model.graphicelements.jpgpngsupport;

import com.t_oster.liblasercut.platform.Util;
import com.t_oster.visicut.model.graphicelements.AbstractImporter;
import com.t_oster.visicut.model.graphicelements.GraphicSet;
import com.t_oster.visicut.model.graphicelements.ImportException;
import java.awt.geom.AffineTransform;
import java.io.File;
import java.io.IOException;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.filechooser.FileFilter;

/**
 *
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class JPGPNGImporter extends AbstractImporter
{

  public GraphicSet importSetFromFile(File inputFile, List<String> warnings) throws ImportException
  {
    try
    {
      GraphicSet result = new GraphicSet();
      //TODO: Get Real Resolution
      double px2mm = Util.inch2mm(1d/72d);
      result.setBasicTransform(AffineTransform.getScaleInstance(px2mm, px2mm));
      result.add(new JPGPNGImage(ImageIO.read(inputFile)));
      return result;
    }
    catch (IOException ex)
    {
      throw new ImportException(ex);
    }
  }

  public FileFilter getFileFilter()
  {
    return new FileFilter()
    {

      @Override
      public boolean accept(File file)
      {
        String name = file.getAbsolutePath().toLowerCase();
        return file.isDirectory() || name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".bmp") || name.endsWith(".jpeg");
      }

      @Override
      public String getDescription()
      {
        return "Rastergrafiken (jpg,png,bmp)";
      }

    };
  }

}
