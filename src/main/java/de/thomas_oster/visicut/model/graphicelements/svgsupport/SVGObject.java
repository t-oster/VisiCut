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

import com.kitfox.svg.*;
import com.kitfox.svg.xml.StyleAttribute;
import de.thomas_oster.visicut.misc.Helper;
import de.thomas_oster.visicut.model.graphicelements.GraphicObject;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public abstract class SVGObject implements GraphicObject
{

  public enum Attribute
  {
    Stroke_Color,
    Fill_Color,
    Group,
    Stroke_Width,
    Color,
    Type,
    Id,
  }

  /**
   * Returns a List of SVGElements representing the Path
   * from the current Decorated Element to the root Node
   */
  public List<SVGElement> getPathToRoot()
  {
    List<SVGElement> result = new LinkedList<SVGElement>();
    SVGElement current = this.getDecoratee();
    while (current != null)
    {
      result.add(current);
      current = current.getParent();
    }
    return result;
  }

  public abstract RenderableElement getDecoratee();

  /**
   * This 
   * applies all transformations in the Path of the SVGShape
   * and returns the Transformed Shape, which can be displayed
   * or printed on the position it appears in the original image.
   */
  public AffineTransform getAbsoluteTransformation() throws SVGException
  {
    if (this.getDecoratee() != null)
    {
      AffineTransform tr = new AffineTransform();
      List<SVGElement> pathToRoot = this.getPathToRoot();
      Collections.reverse(pathToRoot);
      for (Object o : pathToRoot)
      {
        if (o instanceof Group)
        {
          StyleAttribute sty = new StyleAttribute("transform");
          if (((SVGElement) o).getPres(sty))
          {
            String value = sty.getStringValue();
            for (String v : value.split("\\)"))
            {
              v = v.trim();
              if (!"".equals(v))
              {
                AffineTransform trans = SVGElement.parseSingleTransform(v+")");
                tr.concatenate(trans);
              }
            }
          }
        }
      }
      return tr;
    }
    return null;
  }

  @Override
  public void render(Graphics2D g)
  {
    AffineTransform bak = g.getTransform();
    try
    {
      AffineTransform trans = g.getTransform();
      trans.concatenate(this.getAbsoluteTransformation());
      g.setTransform(trans);
      this.getDecoratee().render(g);
    }
    catch (Exception ex)
    {
      Logger.getLogger(SVGShape.class.getName()).log(Level.SEVERE, null, ex);
    }
    g.setTransform(bak);
  }
  
  // Cache for attribute values. Needs to be threadsafe, as getAttributeValues()
  // is used both in the GUI (via PropertyMappingPanelTable) and the preview
  // and calculation of the lasercut file (via MappingFilter), which is in a
  // separate thread.
  private Map<String, List<Object>> attributeValues = new ConcurrentHashMap<String, List<Object>>();

  public List<Object> getAttributeValues(String name)
  {
    if (attributeValues.containsKey(name))
    {
      return attributeValues.get(name);
    }
    List<Object> result = new LinkedList<Object>();
    try
    {
      switch (Attribute.valueOf(name.replace(" ", "_")))
      {
        case Id:
        {
          if (this.getDecoratee().getId() != null)
          {
            result.add(this.getDecoratee().getId());
          }
          break;
        }
        case Group:
        {
          for (SVGElement e : this.getPath(this.getDecoratee()))
          {
            //Exclude SVGRoot, because is not a real group
            if (e instanceof Group && !(e instanceof SVGRoot))
            {
              String id = ((Group) e).getId();
              //Use Inkscape-Labels instead of IDs for Groups (e.g. on Layers)
              StyleAttribute s = ((Group) e).getPresAbsolute("inkscape:label");
              if (s != null && s.getStringValue() != null)
              {
                id = s.getStringValue();
              }
              if (id != null)
              {
                result.add(id);
              }
            }
          }
          break;
        }
      }
    }
    catch (IllegalArgumentException e)
    {
      //Attribute not supported in SVG => Ignore
    }
    attributeValues.put(name, result);
    return result;
  }

  // cached return value of getAttributes()
  private List<String> attributesCache = null;

  public List<String> getAttributes()
  {
    if (attributesCache != null)
    {
      return attributesCache;
    }
    LinkedList<String> attributes = new LinkedList<String>();
    for (Attribute a : Attribute.values())
    {
      if (this.getAttributeValues(a.toString()).size() > 0)
      {
        attributes.add(a.toString().replace("_", " "));
      }
    }
    // late assignment for thread-safety:
    // prevent other threads from accessing the list while it is being constructed
    attributesCache = attributes;
    return attributesCache;
  }

  /**
   * Returns the path from root to the given Element
   */
  protected List<SVGElement> getPath(SVGElement e)
  {
    List<SVGElement> result = new LinkedList<SVGElement>();
    while (e != null)
    {
      result.add(0, e);
      e = e.getParent();
    }
    return result;
  }

  public Rectangle2D getBoundingBox()
  {
    try
    {
      Rectangle2D result = getDecoratee().getBoundingBox();
      if (result == null)
      {
        //Return boundingbox of parent
        SVGElement p = this.getDecoratee();
        do
        {
          p = p.getParent();
          if (p != null && p instanceof RenderableElement)
          {
            result = ((RenderableElement) p).getBoundingBox();
          }
        }
        while (result == null && p != null);
      }
      AffineTransform tr = this.getAbsoluteTransformation();
      return Helper.transform(result, tr);
    }
    catch (SVGException ex)
    {
      Logger.getLogger(SVGObject.class.getName()).log(Level.SEVERE, null, ex);
      return new Rectangle(0, 0, 0, 0);
    }
  }
}
