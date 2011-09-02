/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.visicut.model.graphicelements;

import com.t_oster.visicut.ExtensionFilter;
import com.t_oster.visicut.model.graphicelements.dxfsupport.DXFImporter;
import com.t_oster.visicut.model.graphicelements.jpgpngsupport.JPGPNGImporter;
import com.t_oster.visicut.model.graphicelements.pdfsupport.PDFImporter;
import com.t_oster.visicut.model.graphicelements.svgsupport.SVGImporter;
import java.io.File;
import java.io.FileNotFoundException;
import javax.swing.filechooser.FileFilter;

/**
 * The Importer class Takes an InputFile and returns
 * a List of GraphicObjects
 * 
 * @author thommy
 */
public class GraphicFileImporter implements Importer
{

  private static FileFilter[] fileTypes;

  public static FileFilter[] getFileFilters()
  {
    if (fileTypes == null)
    {
      fileTypes = new FileFilter[]
      {
        new ExtensionFilter(".plf", "VisiCut Portable Laser File (*.plf)"),
        new ExtensionFilter(".svg", "Scalable Vector Graphics (*.svg)"),
        new ExtensionFilter(".png", "Portable Network Graphic (*.png)"),
        new ExtensionFilter(".jpg", "JPEG (*.jpg)"),
      };
    }
    return fileTypes;
  }

  public GraphicSet importFile(File inputFile) throws ImportException
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
    else if (name.endsWith(".dxf"))
    {
      return (new DXFImporter()).importFile(inputFile);
    }
    else if (name.endsWith(".pdf"))
    {
      return (new PDFImporter()).importFile(inputFile);
    }
    else
    {
      throw new ImportException("Unsupported File Format");
    }
  }
}
