/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.visicut.model.graphicelements.dxfsupport;

import com.t_oster.visicut.misc.ExtensionFilter;
import com.t_oster.visicut.model.graphicelements.GraphicSet;
import com.t_oster.visicut.model.graphicelements.ImportException;
import com.t_oster.visicut.model.graphicelements.Importer;
import com.t_oster.visicut.model.graphicelements.svgsupport.SVGImporter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.HashMap;
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
import org.kabeja.svg.SVGGenerator;
import org.kabeja.xml.SAXGenerator;
import org.kabeja.xml.SAXPrettyOutputter;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

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
      Parser parser = ParserBuilder.createDefaultParser();
      parser.parse(new FileInputStream(inputFile), DXFParser.DEFAULT_ENCODING);
      final DXFDocument doc = parser.getDocument();

      PipedInputStream in = new PipedInputStream();
      PipedOutputStream out = new PipedOutputStream(in);
      

      //the SVG will be emitted as SAX-Events
      //see org.xml.sax.ContentHandler for more information 
      final ContentHandler myhandler = new SAXPrettyOutputter(out);

      //the output - create first a SAXGenerator (SVG here)
      final SAXGenerator generator = new SVGGenerator();

      //setup properties
      generator.setProperties(new HashMap());

      new Thread(
        new Runnable()
        {

          public void run()
          {
          try
          {
            generator.generate(doc, myhandler, new HashMap());
          }
          catch (SAXException ex)
          {
            Logger.getLogger(DXFImporter.class.getName()).log(Level.SEVERE, null, ex);
          }
          }
        }).start();
      SVGImporter svgimp = new SVGImporter();
      return svgimp.importFile(in, inputFile.getName());
      //start the output
      

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
