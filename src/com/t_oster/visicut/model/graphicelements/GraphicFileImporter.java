/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.visicut.model.graphicelements;

import com.t_oster.visicut.misc.ExtensionFilter;
import com.t_oster.visicut.misc.MultiFilter;
import com.t_oster.visicut.model.graphicelements.dxfsupport.DXFImporter;
import com.t_oster.visicut.model.graphicelements.jpgpngsupport.JPGPNGImporter;
import com.t_oster.visicut.model.graphicelements.pdfsupport.PDFImporter;
import com.t_oster.visicut.model.graphicelements.svgsupport.SVGImporter;
import java.io.File;
import java.io.FileNotFoundException;
import java.lang.Class;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.filechooser.FileFilter;

/**
 * The Importer class Takes an InputFile and returns
 * a List of GraphicObjects
 * 
 * @author thommy
 */
public class GraphicFileImporter implements Importer
{

  private List<Importer> importers = new LinkedList<Importer>();

  /**
   * Tries to load all importerClasses and creates a GraphicFileImporter
   * which uses all given Importers to import files
   * @param importerClasses 
   */
  public GraphicFileImporter(String[] importerClasses)
  {
    for (String className:importerClasses)
    {
      try
      {
        Class c = Class.forName(className);
        this.importers.add((Importer) c.newInstance());
      }
      catch (InstantiationException ex)
      {
        Logger.getLogger(GraphicFileImporter.class.getName()).log(Level.SEVERE, null, ex);
      }
      catch (IllegalAccessException ex)
      {
        Logger.getLogger(GraphicFileImporter.class.getName()).log(Level.SEVERE, null, ex);
      }      
      catch (ClassNotFoundException ex)
      {
        Logger.getLogger(GraphicFileImporter.class.getName()).log(Level.SEVERE, null, ex);
      }
      
    }
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
    else
    {
      for (Importer i:this.importers)
      {
        if (i.getFileFilter().accept(inputFile))
        {
          GraphicSet gs = i.importFile(inputFile);
          return gs;
        }
      }
      throw new ImportException("Unsupported File Format");
    }
  }

  public FileFilter[] getFileFilters()
  {
    List<FileFilter> result = new LinkedList<FileFilter>();
    for (Importer i : this.importers)
    {
      result.add(i.getFileFilter());
    }
    return result.toArray(new FileFilter[0]);
  }

  public FileFilter getFileFilter()
  {
    return new MultiFilter(this.getFileFilters(), "Alle supported Files");
  }
}
