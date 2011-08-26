/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.visicut.model.graphicelements.svgsupport;

import com.kitfox.svg.Group;
import com.t_oster.visicut.model.graphicelements.Importer;
import com.kitfox.svg.ImageSVG;
import com.kitfox.svg.SVGElement;
import com.kitfox.svg.SVGRoot;
import com.kitfox.svg.SVGUniverse;
import com.kitfox.svg.ShapeElement;
import com.t_oster.visicut.model.graphicelements.GraphicObject;
import com.t_oster.visicut.model.graphicelements.GraphicFileImporter;
import com.t_oster.visicut.model.graphicelements.ImportException;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author thommy
 */
public class SVGImporter implements Importer
{

  private SVGUniverse u = new SVGUniverse();
  
  private void importNode(SVGElement e, List<GraphicObject> result)
  {
    if (e instanceof ShapeElement && !(e instanceof Group))
    {
      result.add(new SVGShape((ShapeElement) e));
    }
    else if (e instanceof ImageSVG)
    {
      result.add(new SVGImage((ImageSVG) e));
    }
    for (int i=0;i< e.getNumChildren();i++)
    {
      importNode(e.getChild(i), result);
    }
  }
  
  @Override
  public List<GraphicObject> importFile(File inputFile) throws ImportException
  {
    try
    {
      URI svg = u.loadSVG(inputFile.toURI().toURL());
      SVGRoot root = u.getDiagram(svg).getRoot();
      List<GraphicObject> result = new LinkedList<GraphicObject>();
      importNode(root,result);
      return result;
    }
    catch (MalformedURLException ex)
    {
      throw new ImportException(ex);
    }
  }
}
