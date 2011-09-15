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
package com.t_oster.visicut.model.graphicelements;

import com.t_oster.visicut.misc.MultiFilter;
import java.io.File;
import java.io.FileNotFoundException;
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
