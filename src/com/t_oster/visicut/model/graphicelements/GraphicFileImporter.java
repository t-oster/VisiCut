/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.visicut.model.graphicelements;

import com.t_oster.visicut.model.graphicelements.jpgpngsupport.JPGPNGImporter;
import com.t_oster.visicut.model.graphicelements.svgsupport.SVGImporter;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

/**
 * The Importer class Takes an InputFile and returns
 * a List of GraphicObjects
 * 
 * @author thommy
 */
public class GraphicFileImporter implements Importer
{
  public List<GraphicObject> importFile(File inputFile) throws ImportException
  {
    if (inputFile == null)
    {
      throw new ImportException(new NullPointerException());
    }
    if (!inputFile.exists())
    {
      throw new ImportException(new FileNotFoundException());
    }
    String name = inputFile.getAbsolutePath().toLowerCase();
    if (name.endsWith(".svg"))
    {
      return (new SVGImporter()).importFile(inputFile);
    }
    else if (name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg"))
    {
      return (new JPGPNGImporter()).importFile(inputFile);
    }
    else
    {
      throw new ImportException("Unsupported File Format");
    }
  }
}
