/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.visicut.model.graphicelements.dxfsupport;

import com.t_oster.visicut.model.graphicelements.GraphicSet;
import com.t_oster.visicut.model.graphicelements.ImportException;
import com.t_oster.visicut.model.graphicelements.Importer;
import java.io.File;
import java.io.FileInputStream;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.kabeja.dxf.DXFBlock;
import org.kabeja.dxf.DXFDocument;
import org.kabeja.dxf.DXFEntity;
import org.kabeja.parser.DXFParser;
import org.kabeja.parser.ParseException;
import org.kabeja.parser.Parser;
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
    try
    {
      Parser p = ParserBuilder.createDefaultParser();
      p.parse(new FileInputStream(inputFile), DXFParser.DEFAULT_ENCODING);
      DXFDocument dxf = p.getDocument();
      Iterator blockIter = dxf.getDXFBlockIterator();
      while (blockIter.hasNext())
      {
        Object o = blockIter.next();
        if (o instanceof DXFBlock)
        {
          DXFBlock b = (DXFBlock) o;
          Iterator entityIter = b.getDXFEntitiesIterator();
          while (entityIter.hasNext())
          {
            Object oo = entityIter.next();
            if (oo instanceof DXFEntity)
            {
              DXFEntity e = (DXFEntity) oo;
              System.out.println(e);
            }
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
  
}
