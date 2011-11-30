/**
 * This file is part of VisiCut.
 * Copyright (C) 2011 Thomas Oster <thomas.oster@rwth-aachen.de>
 * RWTH Aachen University - 52062 Aachen, Germany
 * 
 *     VisiCut is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *    VisiCut is distributed in the hope that it will be useful,
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
import com.kitfox.svg.SVGException;
import com.t_oster.visicut.model.graphicelements.Importer;
import com.kitfox.svg.ImageSVG;
import com.kitfox.svg.PatternSVG;
import com.kitfox.svg.SVGElement;
import com.kitfox.svg.SVGRoot;
import com.kitfox.svg.SVGUniverse;
import com.kitfox.svg.ShapeElement;
import com.kitfox.svg.xml.StyleAttribute;
import com.t_oster.visicut.misc.ExtensionFilter;
import com.t_oster.visicut.misc.Helper;
import com.t_oster.visicut.model.graphicelements.GraphicObject;
import com.t_oster.visicut.model.graphicelements.GraphicSet;
import com.t_oster.visicut.model.graphicelements.ImportException;
import com.t_oster.visicut.model.CONSTANT;
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
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.filechooser.FileFilter;

/**
 *
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class SVGImporter implements Importer
{

  private SVGUniverse u = new SVGUniverse();
  private SVGRoot root;
  //can be overwritten by setDpi() later
  private int dpi = CONSTANT.PROP_SVG_DPI;  

  private void importNode(SVGElement e, List<GraphicObject> result)
  {
    if (e instanceof PatternSVG || e instanceof Gradient || e instanceof Defs)
    {//Ignore Patterns,Gradients and Children
      return;
    }
    if (e instanceof ShapeElement && !(e instanceof Group))
    {
      if (((ShapeElement) e).getShape() != null)
      {
        result.add(new SVGShape((ShapeElement) e));
      }
      else
      {
        System.err.println("Ignoring SVGShape: " + e + " because can't get Shape");
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
      importNode(e.getChild(i), result);
    }
  }

  public GraphicSet importFile(InputStream in, String name) throws Exception
  {
    try
    {
      u.clear();
      URI svg = u.loadSVG(in, name);
      root = u.getDiagram(svg).getRoot();
      GraphicSet result = new GraphicSet();
      //Inkscape SVG Units are 1/90 inch
      //result.setBasicTransform(AffineTransform.getScaleInstance(500d / 90, 500d / 90));
      //CONSTANT.PROP_SVG_DPI
      //result.setBasicTransform(AffineTransform.getScaleInstance(1200d / 90, 1200d / 90));
      System.out.println("Importing svg at "+this.getDpi()+" dpi");
      result.setBasicTransform(AffineTransform.getScaleInstance(this.getDpi() / 90, this.getDpi() / 90));
      
      importNode(root, result);
      return result;
    }
    catch (Exception e)
    {
      throw new ImportException(e);
    }
  }

  /*
   * Tries to determine the Coordinate resolution in DPI.
   * SVG default is 90, but AI generates 72??
   * 
   */
  private AffineTransform determineTransformation(SVGRoot root, File f)
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
        x = Helper.numberWithUnitsToPx(sty.getNumberWithUnits(), (int)this.getDpi());
      }

      if (root.getPres(sty.setName("y")))
      {
        y = Helper.numberWithUnitsToPx(sty.getNumberWithUnits(), (int)this.getDpi());
      }

      if (root.getPres(sty.setName("width")))
      {
        width = Helper.numberWithUnitsToPx(sty.getNumberWithUnits(), (int)this.getDpi());
      }
      if (root.getPres(sty.setName("height")))
      {
        height = Helper.numberWithUnitsToPx(sty.getNumberWithUnits(), (int)this.getDpi());
      }
      if (width != 0 && height != 0 && root.getPres(sty.setName("viewBox")))
      {
        float[] coords = sty.getFloatList();
        Rectangle2D coordinateBox = new Rectangle2D.Double(x,y,width,height);
        Rectangle2D viewBox = new Rectangle2D.Float(coords[0], coords[1], coords[2], coords[3]);
        return Helper.getTransform(viewBox, coordinateBox);
      }
    }
    catch (SVGException ex)
    {
      Logger.getLogger(SVGImporter.class.getName()).log(Level.SEVERE, null, ex);
    }
    BufferedReader in = null;
    int result = 90;
    try
    {
      in = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
      try
      {
        for (int i = 0; i < 2; i++)
        {
          if (in.readLine().startsWith("<!-- Generator: Adobe Illustrator"))
          {
            result = 72;
            break;
          }
        }
        in.close();
      }
      catch (IOException ex)
      {
        Logger.getLogger(SVGImporter.class.getName()).log(Level.SEVERE, null, ex);
      }
      //return AffineTransform.getScaleInstance(500d / result, 500d / result);
      //CONSTANT.PROP_SVG_DPI
      return AffineTransform.getScaleInstance(this.getDpi() / result, this.getDpi() / result);
    }
    catch (FileNotFoundException ex)
    {
      Logger.getLogger(SVGImporter.class.getName()).log(Level.SEVERE, null, ex);
    }
    finally
    {
      try
      {
        in.close();
      }
      catch (IOException ex)
      {
        Logger.getLogger(SVGImporter.class.getName()).log(Level.SEVERE, null, ex);
      }
    }  
    //CONSTANT.PROP_SVG_DPI
    //return AffineTransform.getScaleInstance(500d / result, 500d / result);
    return AffineTransform.getScaleInstance(this.getDpi() / result, this.getDpi() / result);
  }

  @Override
  public GraphicSet importFile(File inputFile) throws ImportException
  {
    try
    {
      GraphicSet result = this.importFile(new FileInputStream(inputFile), inputFile.getName());
      result.setBasicTransform(determineTransformation(root, inputFile));
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
  
  public double getDpi()
  {
    return this.dpi;
  }
  public void setDpi(int dpi)
  {
    this.dpi = dpi;
  }
}
