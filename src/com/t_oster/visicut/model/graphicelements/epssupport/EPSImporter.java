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
package com.t_oster.visicut.model.graphicelements.epssupport;

import com.t_oster.liblasercut.platform.Util;
import com.t_oster.visicut.misc.ExtensionFilter;
import com.t_oster.visicut.model.graphicelements.AbstractImporter;
import com.t_oster.visicut.model.graphicelements.GraphicSet;
import com.t_oster.visicut.model.graphicelements.ImportException;
import com.t_oster.visicut.model.graphicelements.svgsupport.SVGImporter;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Writer;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.filechooser.FileFilter;
import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.freehep.postscript.PSInputFile;
import org.freehep.postscript.Processor;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;

/**
 *
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class EPSImporter extends AbstractImporter
{

  public FileFilter getFileFilter()
  {
    return new ExtensionFilter(".eps", "Encapsulated PostScript (*.eps)");
  }

  /**
   * Tries to read the BoundingBox out of the EPS file.
   * If not successful, it returns a default BoundingBox
   * @param epsfile
   * @return
   */
  private Rectangle2D getBoundingBox(File epsfile)
  {
    Rectangle.Double result = new Rectangle.Double(0, 0, 800, 600);
    try
    {
      BufferedReader r = new BufferedReader(new FileReader(epsfile));
      String line = null;
      while ((line = r.readLine()) != null)
      {
        //TODO: Get HighRes BoundingBox
        if (line.startsWith("%%BoundingBox:") || line.startsWith("%%PageBoundingBox:"))
        {
          try
          {
            String[] elements = line.split(" ");
            result = new Rectangle.Double(
              Integer.parseInt(elements[1]),
              Integer.parseInt(elements[2]),
              Integer.parseInt(elements[3]),
              Integer.parseInt(elements[4]));
            break;
          }
          catch (NumberFormatException e)
          {
          }
        }
      }
      r.close();
    }
    catch (Exception ex)
    {
      Logger.getLogger(EPSImporter.class.getName()).log(Level.SEVERE, null, ex);
    }
    return result;
  }

  public GraphicSet importSetFromFile(File inputFile, List<String> warnings) throws ImportException
  {
    Writer out = null;
    try
    {
      // Get a DOMImplementation
      DOMImplementation domImpl =
        GenericDOMImplementation.getDOMImplementation();
      // Create an instance of org.w3c.dom.Document
      Document document = domImpl.createDocument(null, "svg", null);
      // Create an instance of the SVG Generator
      final SVGGraphics2D svgGenerator = new SVGGraphics2D(document);
      svgGenerator.setTransform(new AffineTransform());
      // Open input file
      PSInputFile in = new PSInputFile(inputFile.getAbsolutePath());
      Rectangle2D bb = this.getBoundingBox(inputFile);
      svgGenerator.setTransform(AffineTransform.getTranslateInstance(-bb.getX(), -bb.getY()));
      Dimension d = new Dimension((int) bb.getWidth(), (int) bb.getHeight());
      // Create processor and associate to input and output file
      Processor processor = new Processor(svgGenerator, d, false);
      processor.setData(in);

      // Process
      processor.process();
      File tmp = File.createTempFile("temp", "svg");
      tmp.deleteOnExit();
      svgGenerator.stream(new FileWriter(tmp));
      GraphicSet result = new SVGImporter().importSetFromFile(tmp, warnings);
      //Assume the EPS has been created with 72DPI (from Inkscape)
      double px2mm = Util.inch2mm(1d/72d);
      result.setBasicTransform(AffineTransform.getScaleInstance(px2mm, px2mm));
      return result;
    }
    catch (Exception ex)
    {
      Logger.getLogger(EPSImporter.class.getName()).log(Level.SEVERE, null, ex);
      throw new ImportException(ex);
    }
  }
}
