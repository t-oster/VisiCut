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
package com.t_oster.visicut.model.graphicelements.dxfsupport;

import com.t_oster.liblasercut.platform.Util;
import com.t_oster.visicut.misc.ExtensionFilter;
import com.t_oster.visicut.model.graphicelements.AbstractImporter;
import com.t_oster.visicut.model.graphicelements.GraphicSet;
import com.t_oster.visicut.model.graphicelements.ImportException;
import com.t_oster.visicut.model.graphicelements.svgsupport.SVGImporter;
import java.io.File;
import java.io.FileInputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.filechooser.FileFilter;

import org.kabeja.dxf.DXFDocument;
import org.kabeja.parser.Parser;
import org.kabeja.parser.DXFParser;
import org.kabeja.parser.ParserBuilder;
import org.kabeja.svg.SVGGenerator;
import org.kabeja.xml.SAXGenerator;
import org.kabeja.xml.SAXPrettyOutputter;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 *
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class DXFImporter extends AbstractImporter
{

  public GraphicSet importSetFromFile(File inputFile, List<String> warnings) throws ImportException
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
      //TODO Check which resolution it exports and adapt it to mm
      result = svgimp.importSetFromFile(in, inputFile.getName(), 1/Util.mm2inch(1), warnings);
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
