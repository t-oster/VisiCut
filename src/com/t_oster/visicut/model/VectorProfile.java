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
package com.t_oster.visicut.model;

import com.t_oster.liblasercut.LaserJob;
import com.t_oster.liblasercut.LaserProperty;
import com.t_oster.liblasercut.VectorPart;
import com.t_oster.liblasercut.platform.Util;
import com.t_oster.liblasercut.utils.ShapeConverter;
import com.t_oster.liblasercut.vectoroptimizers.VectorOptimizer;
import com.t_oster.liblasercut.vectoroptimizers.VectorOptimizer.OrderStrategy;
import com.t_oster.visicut.model.graphicelements.GraphicObject;
import com.t_oster.visicut.model.graphicelements.GraphicSet;
import com.t_oster.visicut.model.graphicelements.ShapeDecorator;
import com.t_oster.visicut.model.graphicelements.ShapeObject;
import com.t_oster.visicut.model.graphicelements.lssupport.LaserScriptShape;
import com.t_oster.visicut.model.graphicelements.lssupport.ScriptInterfaceLogUi;
import com.t_oster.liblasercut.laserscript.ScriptInterpreter;
import com.t_oster.liblasercut.laserscript.VectorPartScriptInterface;
import com.t_oster.visicut.managers.PreferencesManager;
import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.PathIterator;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.script.ScriptException;

/**
 * This class represents a Line Profile,
 * which means a kind of line which can be
 * cut with the lasercutten in a specified
 * Material
 *
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class VectorProfile extends LaserProfile
{

  public VectorProfile()
  {
    this.setName("cut");
  }

  protected OrderStrategy orderStrategy = OrderStrategy.INNER_FIRST;

  /**
   * Get the value of orderStrategy
   *
   * @return the value of orderStrategy
   */
  public OrderStrategy getOrderStrategy()
  {
    if (orderStrategy == null)
    {
      orderStrategy = OrderStrategy.INNER_FIRST;
    }
    return orderStrategy;
  }

  /**
   * Set the value of orderStrategy
   *
   * @param orderStrategy new value of orderStrategy
   */
  public void setOrderStrategy(OrderStrategy orderStrategy)
  {
    this.orderStrategy = orderStrategy;
  }


  protected boolean useOutline = false;

  /**
   * Get the value of useOutline
   *
   * @return the value of useOutline
   */
  public boolean isUseOutline()
  {
    return useOutline;
  }

  /**
   * Set the value of useOutline
   *
   * @param useOutline new value of useOutline
   */
  public void setUseOutline(boolean useOutline)
  {
    this.useOutline = useOutline;
  }

  protected boolean isCut = true;

  /**
   * Get the value of isCut
   *
   * @return the value of isCut
   */
  public boolean isIsCut()
  {
    return isCut;
  }

  /**
   * Set the value of isCut
   *
   * @param isCut new value of isCut
   */
  public void setIsCut(boolean isCut)
  {
    this.isCut = isCut;
  }
  protected float width = 1;

  /**
   * Get the value of width
   *
   * @return the value of width
   */
  public float getWidth()
  {
    return width;
  }

  /**
   * Set the value of width
   *
   * @param width new value of width
   */
  public void setWidth(float width)
  {
    this.width = width;
  }

  private GraphicSet calculateOuterShape(GraphicSet objects)
  {
    final Area outerShape = new Area();
    for (GraphicObject o : objects)
    {
      if (o instanceof ShapeObject)
      {
        outerShape.add(new Area(((ShapeObject) o).getShape()));
      }
      else
      {
        outerShape.add(new Area(o.getBoundingBox()));
      }
    }
    GraphicSet result = new GraphicSet();
    result.setBasicTransform(objects.getBasicTransform());
    result.setTransform(objects.getTransform());
    result.add(new ShapeDecorator(outerShape));
    return result;
  }

  @Override
  public void renderPreview(Graphics2D gg, GraphicSet objects, MaterialProfile material, AffineTransform mm2px)
  {
    //TODO calculate outline
    gg.setColor(this.isCut ? material.getCutColor() : material.getEngraveColor());
    Stroke bak = gg.getStroke();
    Stroke s = new BasicStroke((float) ((mm2px.getScaleX()+mm2px.getScaleY())*this.getWidth()/2), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    gg.setStroke(s);
    if (this.isUseOutline())
    {
      objects = this.calculateOuterShape(objects);
    }
    for (GraphicObject e : objects)
    {
      try
      {
        Shape sh = (e instanceof ShapeObject) ? ((ShapeObject) e).getShape() : e.getBoundingBox();
        if (objects.getTransform() != null)
        {
          sh = objects.getTransform().createTransformedShape(sh);
        }
        //all coordinates are assumed to be milimeters, so we transform to desired resolution
        double factor = Util.dpi2dpmm(this.getDPI());
        AffineTransform mm2laserPx = AffineTransform.getScaleInstance(factor, factor);
        sh = mm2laserPx.createTransformedShape(sh);

        AffineTransform laserPx2PreviewPx = mm2laserPx.createInverse();
        laserPx2PreviewPx.concatenate(mm2px);
        if (sh == null)
        {
          //WTF??
          System.out.println("Error extracting Shape from: " + ((ShapeObject) e).toString());
        }
        else
        {
          PathIterator iter = sh.getPathIterator(null, 1);
          int startx = 0;
          int starty = 0;
          int lastx = 0;
          int lasty = 0;
          while (!iter.isDone())
          {
            double[] test = new double[8];
            int result = iter.currentSegment(test);
            //transform coordinates to preview-coordinates
            laserPx2PreviewPx.transform(test, 0, test, 0, 1);
            if (result == PathIterator.SEG_MOVETO)
            {
              startx = (int) test[0];
              starty = (int) test[1];
              lastx = (int) test[0];
              lasty = (int) test[1];
            }
            else if (result == PathIterator.SEG_LINETO)
            {
              gg.drawLine(lastx, lasty, (int) test[0], (int) test[1]);
              lastx = (int) test[0];
              lasty = (int) test[1];
            }
            else if (result == PathIterator.SEG_CLOSE)
            {
              gg.drawLine(lastx, lasty, startx, starty);
            }
            iter.next();
          }
        }
      }
      catch (NoninvertibleTransformException ex)
      {
        Logger.getLogger(VectorProfile.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
    gg.setStroke(bak);
  }

  @Override
  public void addToLaserJob(LaserJob job, GraphicSet objects, List<LaserProperty> laserProperties)
  {
    if (this.isUseOutline())
    {
      objects = this.calculateOuterShape(objects);
    }
    double factor = Util.dpi2dpmm(this.getDPI());
    AffineTransform mm2laserpx = AffineTransform.getScaleInstance(factor, factor);
    VectorPart part = new VectorPart(laserProperties.get(0), this.getDPI());
    boolean optimize = true;
    for (LaserProperty prop : laserProperties)
    {
      part.setProperty(prop);
      ShapeConverter conv = new ShapeConverter();
      for (GraphicObject e : objects)
      {
        if (e instanceof LaserScriptShape)
        {
          //since the profiles are enumerated file-wise and a laserscript is always
          //one file and creates one shape, this whole file should not be optimized
          optimize = false;
          ScriptInterpreter i = new ScriptInterpreter();
          AffineTransform mm2laser = new AffineTransform(objects.getTransform());
          mm2laser.preConcatenate(mm2laserpx);
          try
          {
            i.execute(new FileReader(((LaserScriptShape) e).getScriptSource()), 
              new ScriptInterfaceLogUi(new VectorPartScriptInterface(part, mm2laser)),
              !PreferencesManager.getInstance().getPreferences().isDisableSandbox());
          }
          catch (ScriptException exx)
          {
            throw new RuntimeException(exx);
          }
          catch (IOException ex)
          {
            Logger.getLogger(VectorProfile.class.getName()).log(Level.SEVERE, null, ex);
          }
        }
        else
        {
          Shape sh = (e instanceof ShapeObject) ? ((ShapeObject) e).getShape() : e.getBoundingBox();
          if (objects.getTransform() != null)
          {
            sh = objects.getTransform().createTransformedShape(sh);
          }
          sh = mm2laserpx.createTransformedShape(sh);
          conv.addShape(sh, part);
        }
      }
    }
    if (optimize)
    {
      VectorOptimizer vo = VectorOptimizer.create(this.getOrderStrategy());
      job.addPart(vo.optimize(part));
    }
    else
    {
      job.addPart(part);
    }
  }

  @Override
  public LaserProfile clone()
  {
    VectorProfile cp = new VectorProfile();
    cp.description = description;
    cp.isCut = isCut;
    cp.name = name;
    cp.orderStrategy = orderStrategy;
    cp.thumbnailPath = thumbnailPath;
    cp.width = width;
    cp.useOutline = useOutline;
    cp.setDPI(getDPI());
    return cp;
  }

  @Override
  public int hashCode()
  {
    return super.hashCodeBase() * 31 + orderStrategy.hashCode()*7 + (useOutline?1:0) + (isCut?3:0) + (int)width;
  }

  @Override
  public boolean equals(Object obj)
  {
    if (obj == null)
    {
      return false;
    }
    if (getClass() != obj.getClass())
    {
      return false;
    }
    final VectorProfile other = (VectorProfile) obj;
    if (this.orderStrategy != other.orderStrategy)
    {
      return false;
    }
    if (this.useOutline != other.useOutline)
    {
      return false;
    }
    if (this.isCut != other.isCut)
    {
      return false;
    }
    if (Float.floatToIntBits(this.width) != Float.floatToIntBits(other.width))
    {
      return false;
    }
    return super.equalsBase(obj);
  }
}
