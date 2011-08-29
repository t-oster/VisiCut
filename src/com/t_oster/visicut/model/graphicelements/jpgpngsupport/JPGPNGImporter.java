/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.visicut.model.graphicelements.jpgpngsupport;

import com.t_oster.visicut.model.graphicelements.GraphicObject;
import com.t_oster.visicut.model.graphicelements.GraphicSet;
import com.t_oster.visicut.model.graphicelements.ImportException;
import com.t_oster.visicut.model.graphicelements.Importer;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import javax.imageio.ImageIO;

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
      result.add(new JPGPNGImage(ImageIO.read(inputFile)));
      return result;
    }
    catch (IOException ex)
    {
      throw new ImportException(ex);
    }
  }
  
}
