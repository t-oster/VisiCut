/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.visicut.model.graphicelements.dxfsupport;

import com.t_oster.visicut.misc.ExtensionFilter;
import com.t_oster.visicut.model.graphicelements.GraphicSet;
import com.t_oster.visicut.model.graphicelements.ImportException;
import com.t_oster.visicut.model.graphicelements.Importer;
import java.io.File;
import java.io.FileInputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.filechooser.FileFilter;
import java.util.Iterator;

import java.util.List;
import org.kabeja.dxf.DXFDocument;
import org.kabeja.dxf.DXFLayer;
import org.kabeja.dxf.DXFPolyline;
import org.kabeja.dxf.DXFConstants;
import org.kabeja.parser.Parser;
import org.kabeja.parser.DXFParser;
import org.kabeja.parser.ParserBuilder;

/**
 *
 * @author thommy
 */
public class DXFImporter implements Importer
{

  public GraphicSet importFile(File inputFile) throws ImportException
  {
    GraphicSet result = new GraphicSet();
    Parser parser = ParserBuilder.createDefaultParser();
    try
    {
      //parse
      parser.parse(new FileInputStream(inputFile), DXFParser.DEFAULT_ENCODING);
      //get the documnet and the layer
      DXFDocument doc = parser.getDocument();
      Iterator i = doc.getDXFLayerIterator();
      while (i.hasNext())
      {
        DXFLayer layer = (DXFLayer) i.next();
        //get all polylines from the layer
        List<DXFPolyline> list = layer.getDXFEntities(DXFConstants.ENTITY_TYPE_POLYLINE);
        if (list != null)
        {
          for (Object o : list)
          {
            result.add(new Polyline((DXFPolyline) o));
          }
        }
      }
    }
    catch (Exception ex)
    {
      Logger.getLogger(DXFImporter.class.getName()).log(Level.SEVERE, null, ex);
    }
    return result;
  }

  public FileFilter getFileFilter()
  {
    return new ExtensionFilter(".dxf", "AutoCAD DXF Files (*.dxf)");
  }
}
