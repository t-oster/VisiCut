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
import com.t_oster.liblasercut.ProgressListener;
import com.t_oster.liblasercut.Raster3dPart;
import com.t_oster.liblasercut.platform.Point;
import com.t_oster.liblasercut.utils.BufferedImageAdapter;
import com.t_oster.liblasercut.platform.Util;
import com.t_oster.visicut.misc.Helper;
import com.t_oster.visicut.model.graphicelements.GraphicObject;
import com.t_oster.visicut.model.graphicelements.GraphicSet;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * This Class represents a profile, describing
 * how the Lasercutter generates RasterData
 * from given GraphicElements
 * 
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class Raster3dProfile extends LaserProfile
{

  public Raster3dProfile()
  {
    this.setName("engrave 3d");
  }
  protected boolean invertColors = false;

  /**
   * Get the value of invertColors
   *
   * @return the value of invertColors
   */
  public boolean isInvertColors()
  {
    return invertColors;
  }

  /**
   * Set the value of invertColors
   *
   * @param invertColors new value of invertColors
   */
  public void setInvertColors(boolean invertColors)
  {
    this.invertColors = invertColors;
  }
  protected int colorShift = 0;

  /**
   * Get the value of colorShift
   *
   * @return the value of colorShift
   */
  public int getColorShift()
  {
    return colorShift;
  }

  /**
   * Set the value of colorShift
   *
   * @param colorShift new value of colorShift
   */
  public void setColorShift(int colorShift)
  {
    this.colorShift = colorShift;
  }

  public BufferedImage getRenderedPreview(GraphicSet objects, MaterialProfile material, AffineTransform mm2px, ProgressListener pl)
  {
    Rectangle bb = Helper.toRect(Helper.transform(objects.getBoundingBox(), mm2px));
    if (bb != null && bb.width > 0 && bb.height > 0)
    {
      BufferedImage scaledImg = new BufferedImage(bb.width, bb.height, BufferedImage.TYPE_INT_ARGB);
      Graphics2D g = scaledImg.createGraphics();
      g.setColor(Color.white);
      g.fillRect(0, 0, scaledImg.getWidth(), scaledImg.getHeight());
      g.setClip(0, 0, scaledImg.getWidth(), scaledImg.getHeight());
      AffineTransform pipe = AffineTransform.getTranslateInstance(-bb.x, -bb.y);
      pipe.concatenate(mm2px);
      if (objects.getTransform() != null)
      {
        pipe.concatenate(objects.getTransform());
      }
      g.setTransform(pipe);
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
      for (GraphicObject o : objects)
      {
        o.render(g);
      }
      BufferedImageAdapter ad = new BufferedImageAdapter(scaledImg, invertColors);
      ad.setColorShift(this.getColorShift());
      for (int y = 0; y < ad.getHeight(); y++)
      {
        for (int x = 0; x < ad.getWidth(); x++)
        {
          if (ad.getGreyScale(x, y) < 255)
          {
            double f = (double) ad.getGreyScale(x, y) / 255;
            Color scaled = getColorBetween(material.getEngraveColor(), material.getColor(), f);
            scaledImg.setRGB(x, y, scaled.getRGB());
          }
          else
          {
            scaledImg.getAlphaRaster().setPixel(x, y, new int[]
              {
                0, 0, 0
              });
          }
        }
        if (pl != null)
        {
          pl.progressChanged(this, 100 * y / ad.getHeight());
        }
      }
      return scaledImg;
    }
    return null;
  }
  
  @Override
  public void renderPreview(Graphics2D gg, GraphicSet objects, MaterialProfile material, AffineTransform mm2px)
  {
    this.renderPreview(gg, objects, material, mm2px, null);
  }

  public void renderPreview(Graphics2D gg, GraphicSet objects, MaterialProfile material, AffineTransform mm2px, ProgressListener pl)
  {
    Rectangle bb = Helper.toRect(Helper.transform(objects.getBoundingBox(), mm2px));
    BufferedImage scaledImg = this.getRenderedPreview(objects, material, mm2px, pl);
    gg.drawImage(scaledImg, null, bb.x, bb.y);
  }

  /**
   * Returns the color between a and b depending on factor.
   * factor 0 means a, factor 1 means b
   * @param a
   * @param b
   * @param factor
   * @return 
   */
  private Color getColorBetween(Color a, Color b, double factor)
  {
    int ra = a.getRed();
    int rb = b.getRed();
    int ga = a.getGreen();
    int gb = b.getGreen();
    int ba = a.getBlue();
    int bb = b.getBlue();
    return new Color(
      (int) (ra + factor * (rb - ra)),
      (int) (ga + factor * (gb - ga)),
      (int) (ba + factor * (bb - ba)));
  }

  @Override
  public void addToLaserJob(LaserJob job, GraphicSet set, List<LaserProperty> laserProperties)
  {
    double factor = Util.dpi2dpmm(this.getDPI());
    AffineTransform mm2laserPx = AffineTransform.getScaleInstance(factor, factor);
    for (GraphicSet objects : this.decompose(set))
    {
      Rectangle bb = Helper.toRect(Helper.transform(objects.getBoundingBox(), mm2laserPx));
      if (bb != null && bb.width > 0 && bb.height > 0)
      {
        BufferedImage scaledImg = new BufferedImage(bb.width, bb.height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = scaledImg.createGraphics();
        //fill it with white background for dithering
        g.setColor(Color.white);
        g.fillRect(0, 0, scaledImg.getWidth(), scaledImg.getHeight());
        g.setClip(0, 0, scaledImg.getWidth(), scaledImg.getHeight());
        //render all objects onto the image, moved to the images origin
        AffineTransform pipe = AffineTransform.getTranslateInstance(-bb.x, -bb.y);
        pipe.concatenate(mm2laserPx);
        pipe.concatenate(objects.getTransform());
        g.setTransform(pipe);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        for (GraphicObject o : objects)
        {
          o.render(g);
        }
        BufferedImageAdapter ad = new BufferedImageAdapter(scaledImg, invertColors);
        ad.setColorShift(this.getColorShift());
        for (LaserProperty prop : laserProperties)
        {
          Raster3dPart part = new Raster3dPart(ad, prop, new Point((int) bb.getX(), (int) bb.getY()), getDPI());
          job.addPart(part);
        }
      }
    }
  }

  @Override
  public LaserProfile clone()
  {
    Raster3dProfile rp = new Raster3dProfile();
    rp.colorShift = this.colorShift;
    rp.description = this.description;
    rp.invertColors = this.invertColors;
    rp.name = this.name;
    rp.thumbnailPath = this.thumbnailPath;
    rp.setDPI(getDPI());
    return rp;
  }

  @Override
  public int hashCode()
  {
    return super.hashCodeBase() * 31 + (invertColors?1:0) *17 + colorShift;
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
    final Raster3dProfile other = (Raster3dProfile) obj;
    if (this.invertColors != other.invertColors)
    {
      return false;
    }
    if (this.colorShift != other.colorShift)
    {
      return false;
    }
    return super.equalsBase(other);
  }
}
