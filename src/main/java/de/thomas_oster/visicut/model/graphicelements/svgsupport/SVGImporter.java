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
package de.thomas_oster.visicut.model.graphicelements.svgsupport;

import com.kitfox.svg.Defs;
import com.kitfox.svg.Gradient;
import com.kitfox.svg.Group;
import com.kitfox.svg.ImageSVG;
import com.kitfox.svg.PatternSVG;
import com.kitfox.svg.SVGConst;
import com.kitfox.svg.SVGElement;
import com.kitfox.svg.SVGException;
import com.kitfox.svg.SVGRoot;
import com.kitfox.svg.SVGUniverse;
import com.kitfox.svg.ShapeElement;
import com.kitfox.svg.xml.NumberWithUnits;
import com.kitfox.svg.xml.StyleAttribute;
import de.thomas_oster.liblasercut.platform.Util;
import de.thomas_oster.visicut.misc.ExtensionFilter;
import de.thomas_oster.visicut.misc.Helper;
import de.thomas_oster.visicut.model.graphicelements.AbstractImporter;
import de.thomas_oster.visicut.model.graphicelements.GraphicObject;
import de.thomas_oster.visicut.model.graphicelements.GraphicSet;
import de.thomas_oster.visicut.model.graphicelements.ImportException;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import javax.swing.filechooser.FileFilter;

/**
 *
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class SVGImporter extends AbstractImporter
{

  private SVGUniverse u = new SVGUniverse();
  private SVGRoot root;

  private void importNode(SVGElement e, List<GraphicObject> result, double svgResolution, List<String> warnings)
  {
    if (e instanceof PatternSVG || e instanceof Gradient || e instanceof Defs)
    {//Ignore Patterns,Gradients and Children
      return;
    }
    StyleAttribute display = e.getStyleAbsolute("display");
    if (display != null && "none".equals(display.getStringValue()))
    {
      return;
    }
    StyleAttribute visibility = e.getStyleAbsolute("visibility");
    if (visibility != null && "hidden".equals(visibility.getStringValue()))
    {
      return;
    }
    if (e instanceof ShapeElement && !(e instanceof Group))
    {
      var shape = ((ShapeElement) e).getShape();
      if (shape != null)
      {
        if (Helper.shapeIsEmpty(shape)) {
          // ignore empty paths (only MOVETO segments, no actual path)
          return;
        }
        result.add(new SVGShape((ShapeElement) e, svgResolution));
      }
      else
      {
        //warnings.add("Ignoring SVGShape: " + e + " because can't get Shape");
      }
    }
    else
    {
      if (e instanceof ImageSVG)
      {
        result.add(new SVGImage((ImageSVG) e));
      }
    }
    for (int i = 0; i < e.getNumChildren(); i++)
    {
      importNode(e.getChild(i), result, svgResolution, warnings);
    }
  }

  public GraphicSet importSetFromFile(InputStream in, String name, double svgResolution, boolean originIsBottomLeft, double bedHeightInMm, final List<String> warnings) throws Exception
  {
    Handler svgImportLoggerHandler = new Handler()
    {
      @Override
      public void publish(LogRecord lr)
      {
        warnings.add(lr.getMessage());
      }

      @Override
      public void flush(){}

      @Override
      public void close() throws SecurityException{}

    };

    try
    {
      u.clear();
      Logger.getLogger(SVGConst.SVG_LOGGER).addHandler(svgImportLoggerHandler);
      URI svg = u.loadSVG(in, Helper.toPathName(name));   
      root = u.getDiagram(svg).getRoot();
      GraphicSet result = new GraphicSet();
      result.setBasicTransform(determineTransformation(root, svgResolution, originIsBottomLeft, bedHeightInMm));

      // The resulting transformation is the mapping of "SVG pixels" to real millimeters.
      // If viewBox, width and height are set, this scaling can be different from
      // the implicit svgResolution before, so we recalculate the svgResolution.
      //
      // Note: Modifying svgResolution at this point only affects the values
      // of stroke-width shown in the mapping table, and nothing else, especially
      // not the rendering or the laser engraving result.
      double mmPerPx = (result.getBasicTransform().getScaleX() + result.getBasicTransform().getScaleY()) / 2;
      svgResolution = 1/Util.mm2inch(mmPerPx);
      importNode(root, result, svgResolution, warnings);
      Logger.getLogger(SVGConst.SVG_LOGGER).removeHandler(svgImportLoggerHandler);
      return result;
    }
    catch (Exception e)
    {
      throw new ImportException(e);
    }
  }

  /**
   * Calculates the size in mm (with repolution dpi)
   * of a NumberWithUnits element (SVG-Element)
   */
  public static double numberWithUnitsToMm(NumberWithUnits n, double dpi)
  {
    switch (n.getUnits())
    {
      case NumberWithUnits.UT_MM:
        return n.getValue();
      case NumberWithUnits.UT_CM:
        return 10.0 * n.getValue();
      case NumberWithUnits.UT_PT:
        return Util.inch2mm(n.getValue()/72.0);
      case NumberWithUnits.UT_PX:
      case NumberWithUnits.UT_UNITLESS:
        return Util.px2mm(n.getValue(), dpi);
      case NumberWithUnits.UT_IN:
        return Util.inch2mm(n.getValue());
      default:
        return n.getValue();
    }
  }

  /**
   * Since different programs have a different idea of the reference resolution
   * in SVG, this method tries to determine it.
   */
  public double determineResolution(File f, List<String> warnings)
  {
    BufferedReader in = null;;
    double result = 90;
    boolean AdobeIllustratorSeen = false;
    boolean WwwInkscapeComSeen = false;
    boolean InkscapeVersion092Seen = false;
    boolean InkscapeVersionSeen = false;
    boolean ViewBoxSeen = false;
    boolean usesFlowRoot = false;
    try
    {
      in = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
      try
      {
        String line = in.readLine();
        while (line != null)
        {
          if (line.startsWith("<!-- Generator: Adobe Illustrator"))
          {
            AdobeIllustratorSeen = true;
          }
          if (line.contains("www.inkscape.org/namespaces"))
          {
            WwwInkscapeComSeen = true;
          }
          if (line.contains("viewBox="))
          {
            ViewBoxSeen = true;
            // viewBox="0 0 210 300"
          }
          if (line.contains("inkscape:version="))
          {
            InkscapeVersionSeen = true;
	    // inkscape:version="0.92.0 ...."
	    // inkscape:version="0.91 r"

	    Pattern versionPattern = Pattern.compile("inkscape:version\\s*=\\s*[\"']?([0-9]+)\\.([0-9]+)");
	    Matcher matcher = versionPattern.matcher(line);
	    if (matcher.find())
	      {
	        String vers = matcher.group();
		// vers = "inkscape:version=\"0.92"

	        int vers_maj;
		try { vers_maj = Integer.parseInt(matcher.group(1)); }
		catch (NumberFormatException e) { vers_maj = -1; }

	        int vers_min;
		try { vers_min = Integer.parseInt(matcher.group(2)); }
		catch (NumberFormatException e) { vers_min = -1; }

		if (((vers_maj == 0) && (vers_min >= 92)) || (vers_maj > 0))
		  {
		    InkscapeVersion092Seen = true;
		  }
              }
          }
          if (line.contains("</flowRoot>"))
          {
            usesFlowRoot = true;
          }
          line = in.readLine();
        }
        in.close();
      }
      catch (IOException ex)
      {
        Logger.getLogger(SVGImporter.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
    catch (FileNotFoundException ex)
    {
      Logger.getLogger(SVGImporter.class.getName()).log(Level.SEVERE, null, ex);
    }
    finally
    {
      try
      {
        if (in != null)
        {
          in.close();
        }
      }
      catch (IOException ex)
      {
        Logger.getLogger(SVGImporter.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
    if (usesFlowRoot)
    {
      warnings.add(java.util.ResourceBundle.getBundle("de.thomas_oster/visicut/model/graphicelements/svgsupport/resources/SVGImporter").getString("FLOWROOT_WARNING"));
    }

    // SVG files saved with Illustrator are 72 DPI,
    // SVG files loaded in inkscape retain the Illustrator comment, but are saved with 90 DPI.
    if (AdobeIllustratorSeen) { result = 72; }
    if (WwwInkscapeComSeen) { result = 90; }	// inkscape wins over Illustrator
    if (InkscapeVersion092Seen) { result = 96; }	// inkscape with known version wins over anything else.
    if ((result != 96) && !ViewBoxSeen)
    {
      if (AdobeIllustratorSeen)
        {
          warnings.add("Adobe Illustrator comment seen in SVG. No viewBox. Using "+result+" dpi.");
        }
      if (InkscapeVersion092Seen)
        {
          warnings.add("Inkscape Version 0.92+ comment seen in SVG. No viewBox. Using "+result+" dpi.");
        }
      if (WwwInkscapeComSeen && !InkscapeVersionSeen)
        {
          warnings.add("Old inkscape header without version seen in SVG. No viewBox. Using "+result+" dpi.");
        }
      if (InkscapeVersionSeen && !InkscapeVersion092Seen)
        {
          warnings.add("Old inkscape version (< 0.92) seen in SVG. No viewBox. Using "+result+" dpi.");
        }
    }
    return result;
  }

  /*
   * Tries to determine the Coordinate resolution in DPI.
   * SVG default is 90, but AI generates 72??
   * Since inkscape 0.92 SVG default is 96 DPI.
   */
  private AffineTransform determineTransformation(SVGRoot root, double svgResolution, boolean originBottomLeft, double bedHeightInMm)
  {
    try
    {
      StyleAttribute sty = new StyleAttribute();
      double x=0;
      double y=0;
      double width=0;
      double height=0;
      if (root.getPres(sty.setName("x")))
      {
        x = numberWithUnitsToMm(sty.getNumberWithUnits(), svgResolution);
      }
      if (root.getPres(sty.setName("y")))
      {
        y = numberWithUnitsToMm(sty.getNumberWithUnits(), svgResolution);
      }
      if (root.getPres(sty.setName("width")))
      {
        width = numberWithUnitsToMm(sty.getNumberWithUnits(), svgResolution);
        if (sty.getNumberWithUnits().getUnits() == NumberWithUnits.UT_PERCENT)
	  {
	    width = 0;	// cannot use percent here!
	  }
      }
      if (root.getPres(sty.setName("height")))
      {
        height = numberWithUnitsToMm(sty.getNumberWithUnits(), svgResolution);
        if (sty.getNumberWithUnits().getUnits() == NumberWithUnits.UT_PERCENT)
	  {
	    height = 0;	// cannot use percent here!
	  }
      }
      if (width != 0 && height != 0 && root.getPres(sty.setName("viewBox")))
      {
        float[] coords = sty.getFloatList();
        /**
         * https://github.com/t-oster/VisiCut/issues/633 If origin is bottom left
         * and a viewbox is given, use the bottom-left corner for placement.
         */
        if (originBottomLeft && height < bedHeightInMm) {
          y += bedHeightInMm - height;
        }
        Rectangle2D coordinateBox = new Rectangle2D.Double(x,y,width,height);
        Rectangle2D viewBox = new Rectangle2D.Float(coords[0], coords[1], coords[2], coords[3]);
        return Helper.getTransform(viewBox, coordinateBox);
      }
    }
    catch (SVGException ex)
    {
      Logger.getLogger(SVGImporter.class.getName()).log(Level.SEVERE, null, ex);
    }
    double px2mm = Util.inch2mm(1/svgResolution);
    return AffineTransform.getScaleInstance(px2mm, px2mm);
  }

  @Override
  public GraphicSet importSetFromFile(File inputFile, boolean originIsBottomLeft, double bedHeightInMm, List<String> warnings) throws ImportException
  {
    try
    {
      double svgResolution = determineResolution(inputFile, warnings);
      GraphicSet result = this.importSetFromFile(new FileInputStream(inputFile), inputFile.getName(), svgResolution, originIsBottomLeft, bedHeightInMm, warnings);
      return result;
    }
    catch (Exception ex)
    {
      throw new ImportException(ex);
    }
  }

  public FileFilter getFileFilter()
  {
    return new ExtensionFilter(".svg", "Scalable Vector Graphic (*.svg)");
  }
}
