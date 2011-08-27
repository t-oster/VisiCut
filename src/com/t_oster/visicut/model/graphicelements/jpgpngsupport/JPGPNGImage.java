/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.visicut.model.graphicelements.jpgpngsupport;

import com.t_oster.visicut.model.graphicelements.GraphicObject;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.image.RenderedImage;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author thommy
 */
public class JPGPNGImage implements GraphicObject
{

  private RenderedImage decoratee;
  public JPGPNGImage(RenderedImage img)
  {
    decoratee = img;
  }
  public List<Object> getAttributeValues(String name)
  {
    List<Object> result = new LinkedList<Object>();
    if (name.equals("ObjectType"))
    {
      result.add("Image");
    }
    return result;
  }

  public List<String> getAttributes()
  {
    List<String> result = new LinkedList<String>();
    result.add("ObjectType");
    return result;
  }

  public void render(Graphics2D g)
  {
    g.drawRenderedImage(decoratee, null);
  }

  public Rectangle2D getBoundingBox()
  {
    return new Rectangle(decoratee.getMinX(), decoratee.getMinY(), decoratee.getWidth(), decoratee.getHeight());
  }
  
}
