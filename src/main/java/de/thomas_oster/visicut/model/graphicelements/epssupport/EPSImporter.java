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
package de.thomas_oster.visicut.model.graphicelements.epssupport;

import de.thomas_oster.liblasercut.platform.Util;
import de.thomas_oster.visicut.misc.ExtensionFilter;
import de.thomas_oster.visicut.model.graphicelements.AbstractImporter;
import de.thomas_oster.visicut.model.graphicelements.GraphicSet;
import de.thomas_oster.visicut.model.graphicelements.ImportException;
import de.thomas_oster.visicut.model.graphicelements.svgsupport.SVGImporter;
import java.awt.Dimension;
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
   * The DSC bounding boxes of an EPS file. "%%BoundingBox: x1 y1 x2 y2"
   * gives the corners (lower left and upper right) in whole points, not
   * x, y, width and height; "%%HiResBoundingBox" the same with decimals.
   */
  private static final class BoundingBoxes
  {
    /** Integer box (the page FreeHEP fits), or a default of 800 x 600. */
    Rectangle2D page = new Rectangle2D.Double(0, 0, 800, 600);
    /** HiResBoundingBox if present, else the integer box. */
    Rectangle2D hires;
  }

  private static Rectangle2D.Double parseBox(String line)
  {
    try
    {
      String[] elements = line.substring(line.indexOf(':') + 1).trim().split("\\s+");
      double x1 = Double.parseDouble(elements[0]);
      double y1 = Double.parseDouble(elements[1]);
      double x2 = Double.parseDouble(elements[2]);
      double y2 = Double.parseDouble(elements[3]);
      if (x2 > x1 && y2 > y1)
      {
        return new Rectangle2D.Double(x1, y1, x2 - x1, y2 - y1);
      }
    }
    catch (NumberFormatException | ArrayIndexOutOfBoundsException e)
    {
      // e.g. "(atend)"
    }
    return null;
  }

  private BoundingBoxes getBoundingBoxes(File epsfile)
  {
    BoundingBoxes result = new BoundingBoxes();
    Rectangle2D.Double lowres = null;
    Rectangle2D.Double hires = null;
    try (BufferedReader r = new BufferedReader(new FileReader(epsfile)))
    {
      String line;
      while ((line = r.readLine()) != null)
      {
        if (line.startsWith("%%HiResBoundingBox:"))
        {
          Rectangle2D.Double box = parseBox(line);
          if (box != null)
          {
            hires = box;
          }
        }
        else if (lowres == null && (line.startsWith("%%BoundingBox:") || line.startsWith("%%PageBoundingBox:")))
        {
          lowres = parseBox(line);
        }
      }
    }
    catch (Exception ex)
    {
      Logger.getLogger(EPSImporter.class.getName()).log(Level.SEVERE, null, ex);
    }
    if (lowres != null)
    {
      result.page = lowres;
    }
    else if (hires != null)
    {
      result.page = hires;
    }
    result.hires = hires != null ? hires : result.page;
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
      BoundingBoxes boxes = this.getBoundingBoxes(inputFile);
      // FreeHEP reads the integer %%BoundingBox itself, scales it uniformly
      // into the page (min of width and height ratio) with its lower left
      // corner at the page corner, and replaces the transform of the
      // Graphics2D by its own. A page of exactly the size of that box keeps
      // the scale at 1; rounding up only matters for fractional boxes.
      Rectangle2D page = boxes.page;
      Dimension d = new Dimension((int) Math.ceil(page.getWidth()), (int) Math.ceil(page.getHeight()));
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
      AffineTransform basic = AffineTransform.getScaleInstance(px2mm, px2mm);
      // The HiResBoundingBox names the exact corner of the drawing inside the
      // integer box (y runs upwards in PostScript, downwards on the page):
      // that corner becomes the origin of the imported graphic.
      Rectangle2D hires = boxes.hires;
      basic.translate(-(hires.getX() - page.getX()), -(page.getMaxY() - hires.getMaxY()));
      result.setBasicTransform(basic);
      return result;
    }
    catch (Exception ex)
    {
      Logger.getLogger(EPSImporter.class.getName()).log(Level.SEVERE, null, ex);
      throw new ImportException(ex);
    }
  }
}
