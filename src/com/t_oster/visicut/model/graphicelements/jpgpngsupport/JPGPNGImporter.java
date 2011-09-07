/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.visicut.model.graphicelements.jpgpngsupport;

import com.t_oster.visicut.model.graphicelements.GraphicSet;
import com.t_oster.visicut.model.graphicelements.ImportException;
import com.t_oster.visicut.model.graphicelements.Importer;
import java.awt.geom.AffineTransform;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.filechooser.FileFilter;

/**
 *
 * @author thommy
 */
public class JPGPNGImporter implements Importer
{

  public GraphicSet importFile(File inputFile) throws ImportException
  {
    try
    {
      GraphicSet result = new GraphicSet();
      //TODO: Get Real Resolution
      result.setTransform(AffineTransform.getScaleInstance(500/72, 500/72));
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
