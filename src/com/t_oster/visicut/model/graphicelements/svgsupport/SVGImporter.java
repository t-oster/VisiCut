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
package com.t_oster.visicut.model.graphicelements.svgsupport;

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
import com.t_oster.liblasercut.platform.Util;
import com.t_oster.visicut.misc.ExtensionFilter;
import com.t_oster.visicut.misc.Helper;
import com.t_oster.visicut.model.graphicelements.AbstractImporter;
import com.t_oster.visicut.model.graphicelements.GraphicObject;
import com.t_oster.visicut.model.graphicelements.GraphicSet;
import com.t_oster.visicut.model.graphicelements.ImportException;
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
import java.util.LinkedList;
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
      if (((ShapeElement) e).getShape() != null)
      {
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

  public GraphicSet importSetFromFile(InputStream in, String name, double svgResolution, final List<String> warnings) throws Exception
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
      result.setBasicTransform(determineTransformation(root, svgResolution));

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
   * @param n
   * @param dpi
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
   * check whether given NumberWithUnits n has an absolute unit
   * (directly related to millimeters, independent of DPI value, i.e. inch, mm etc.)
   * @param n NumberWithUnits
   * @return 
   */
  public static boolean isAbsoluteUnit(NumberWithUnits n) {
    if (n == null) {
      return false;
    }
    switch (n.getUnits())
    {
      case NumberWithUnits.UT_IN:
      case NumberWithUnits.UT_MM:
      case NumberWithUnits.UT_CM:
      case NumberWithUnits.UT_PT: // 1pt = 1/72 in
        return true;
      default:
        return false;
    }
  }

  public double determineReferenceResolution(File f, List<String> warnings) {
    // FIXME WORK IN PROGRESS
    
    // refactor so that dpiGuessInfo is discarded when determineTransformation() doesn't need the guessed DPI info.
    List<String> dpiGuessInfo = new LinkedList<String>();
    List<String> otherWarnings = new LinkedList<String>();
    double ret = determineReferenceResolution(f, dpiGuessInfo, otherWarnings);
    warnings.addAll(dpiGuessInfo);
    warnings.addAll(otherWarnings);
    return ret;
  }
  
  
  /**
   * Since different programs have a different idea of the reference resolution
   * in SVG, this method tries to determine it.
   * 
   * Tries to determine the Coordinate resolution in DPI.
   * Inkscape before 0.92 has 90 DPI. This is our fallback default for unknown files.
   * Adobe Illustrator has 72 DPI.
   * Corel X8 uses absolute units (DPI unknown but doesn't matter).
   * Inkscape 0.92 uses absolute units and internally 96 DPI.
   * The SVG standard says 96 DPI (early versions erroneously stated 90 DPI).
   *
   * @param dpiGuessInfo information issued about guessing the file DPI.
   *   This can be ignored if the file scaling is unambiguous.
   * @param warnings Other warnings issued about other SVG elements that might cause trouble
   * @return best guess of reference DPI
   */
  public double determineReferenceResolution(File f, List<String> dpiGuessInfo, List<String> warnings)
  {
    BufferedReader in = null;
    double result = 90;
    boolean AdobeIllustratorSeen = false;
    boolean WwwInkscapeComSeen = false;
    boolean InkscapeVersion092Seen = false;
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
          if (line.contains("inkscape:version="))
          {
	    // inkscape:version="0.92.0 ...."
	    // inkscape:version="0.91 r"

	    // FIXME: version number comparison needed here!
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
      warnings.add(java.util.ResourceBundle.getBundle("com/t_oster/visicut/model/graphicelements/svgsupport/resources/SVGImporter").getString("FLOWROOT_WARNING"));
    }

    // SVG files saved with Illustrator are 72 DPI,
    // SVG files loaded in inkscape retain the Illustrator comment, but are saved with 90 DPI.
    
    String guessedProgram = "unknown program";
    if (AdobeIllustratorSeen) {
      result = 72;
      guessedProgram = "Adobe Illustrator";
    }
    // inkscape wins over Illustrator
    if (WwwInkscapeComSeen) {
      result = 90;
      guessedProgram = "Inkscape older than version 0.92";
    }
    // inkscape with known version wins over anything else.
    if (InkscapeVersion092Seen) {
      result = 96;
      guessedProgram = "Inkscape version 0.92 or newer";
    }
    
    if (AdobeIllustratorSeen && (WwwInkscapeComSeen || InkscapeVersion092Seen)) {
      dpiGuessInfo.add("This file was edited with both Illustrator and Inkscape. Scaling may be wrong!");
    }
       dpiGuessInfo.add("File detected as: " + guessedProgram + " (" + result + " DPI) - Please check object size!");
    return result;
  }


  private AffineTransform determineTransformation(SVGRoot root, double svgResolutionGuess, List<String> warnings)
  {
    // Is the object size dependent on the assumed DPI?
    // If yes, a warning will be printed later.
    // Newer versions of Corel and Inkscape will add viewBox and absolute width/height to prevent this problem.
    boolean resolutionIsAmbiguous = true;
    try
    {
      StyleAttribute sty = new StyleAttribute();
      double x=0;
      double y=0;
      double width=0;
      double height=0;
      boolean widthHasAbsoluteUnit = false;
      boolean heightHasAbsoluteUnit = false;
      
      if (root.getPres(sty.setName("x")))
      {
        x = numberWithUnitsToMm(sty.getNumberWithUnits(), svgResolutionGuess);
      }
      if (root.getPres(sty.setName("y")))
      {
        y = numberWithUnitsToMm(sty.getNumberWithUnits(), svgResolutionGuess);
      }
      if (root.getPres(sty.setName("width")))
      {
        widthHasAbsoluteUnit = isAbsoluteUnit(sty.getNumberWithUnits());
        width = numberWithUnitsToMm(sty.getNumberWithUnits(), svgResolutionGuess);
        if (sty.getNumberWithUnits().getUnits() == NumberWithUnits.UT_PERCENT)
	  {
	    width = 0;	// cannot use percent here!
	  }
      }
      if (root.getPres(sty.setName("height")))
      {
        heightHasAbsoluteUnit = isAbsoluteUnit(sty.getNumberWithUnits());
        height = numberWithUnitsToMm(sty.getNumberWithUnits(), svgResolutionGuess);
        if (sty.getNumberWithUnits().getUnits() == NumberWithUnits.UT_PERCENT)
	  {
	    height = 0;	// cannot use percent here!
	  }
      }
      if (width != 0 && height != 0 && root.getPres(sty.setName("viewBox")))
      {
        float[] coords = sty.getFloatList();
        Rectangle2D coordinateBox = new Rectangle2D.Double(x,y,width,height);
        Rectangle2D viewBox = new Rectangle2D.Float(coords[0], coords[1], coords[2], coords[3]);
        if (heightHasAbsoluteUnit && widthHasAbsoluteUnit) {
          resolutionIsAmbiguous = false;
        }
        return Helper.getTransform(viewBox, coordinateBox);
      }
    }
    catch (SVGException ex)
    {
      Logger.getLogger(SVGImporter.class.getName()).log(Level.SEVERE, null, ex);
    }
    finally {
      // this check is called independent of where the function returns.
      if (resolutionIsAmbiguous) {
        warnings.add("Please check object scaling.");
      }
      
      // FIXME WORK IN PROGRESS -- instead of this, just skip the dpiGuessInfo warning output
      if (!resolutionIsAmbiguous) {
        warnings.add("Everything is fine, please ignore the warnings regarding object scaling! (FIXME)");
      }
    }
    double px2mm = Util.inch2mm(1/svgResolutionGuess);
    return AffineTransform.getScaleInstance(px2mm, px2mm);
  }

  @Override
  public GraphicSet importSetFromFile(File inputFile, List<String> warnings) throws ImportException
  {
    try
    {
      double svgResolution = determineReferenceResolution(inputFile, warnings);
      GraphicSet result = this.importSetFromFile(new FileInputStream(inputFile), inputFile.getName(), svgResolution, warnings);
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
