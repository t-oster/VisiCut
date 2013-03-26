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

import com.t_oster.liblasercut.BlackWhiteRaster;
import com.t_oster.liblasercut.LaserJob;
import com.t_oster.liblasercut.LaserProperty;
import com.t_oster.liblasercut.ProgressListener;
import com.t_oster.liblasercut.RasterPart;
import com.t_oster.liblasercut.dithering.DitheringAlgorithm;
import com.t_oster.liblasercut.dithering.FloydSteinberg;
import com.t_oster.liblasercut.platform.Point;
import com.t_oster.liblasercut.platform.Util;
import com.t_oster.liblasercut.utils.BufferedImageAdapter;
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
public class RasterProfile extends LaserProfile
{

  public RasterProfile()
  {
    this.setName("engrave");
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
  protected DitheringAlgorithm ditherAlgorithm = new FloydSteinberg();

  /**
   * Get the value of ditherAlgorithm
   *
   * @return the value of ditherAlgorithm
   */
  public DitheringAlgorithm getDitherAlgorithm()
  {
    if (ditherAlgorithm == null)
    {
      ditherAlgorithm = new FloydSteinberg();
    }
    return ditherAlgorithm;
  }

  /**
   * Set the value of ditherAlgorithm
   *
   * @param ditherAlgorithm new value of ditherAlgorithm
   */
  public void setDitherAlgorithm(DitheringAlgorithm ditherAlgorithm)
  {
    this.ditherAlgorithm = ditherAlgorithm;
  }

  public BufferedImage getRenderedPreview(GraphicSet objects, MaterialProfile material, AffineTransform mm2px)
  {
    return this.getRenderedPreview(objects, material, mm2px, null);
  }

  public BufferedImage getRenderedPreview(GraphicSet objects, MaterialProfile material, AffineTransform mm2px, ProgressListener pl)
  {
    Rectangle bb = Helper.toRect(Helper.transform(objects.getBoundingBox(), mm2px));
    final Color engraveColor = material.getEngraveColor();
    if (bb != null && bb.width > 0 && bb.height > 0)
    {//Create an Image which fits the bounding box
      final BufferedImage scaledImg = new BufferedImage(bb.width, bb.height, BufferedImage.TYPE_INT_ARGB);
      Graphics2D g = scaledImg.createGraphics();
      //fill it with black or white background for dithering depending on invert flag
      g.setColor( (invertColors ? Color.black : Color.white) );
      g.fillRect(0, 0, scaledImg.getWidth(), scaledImg.getHeight());
      g.setClip(0, 0, scaledImg.getWidth(), scaledImg.getHeight());
      //render all objects onto the image, moved to the images origin
      AffineTransform pipe = AffineTransform.getTranslateInstance(-bb.x, -bb.y);
      pipe.concatenate(mm2px);
      pipe.concatenate(objects.getTransform());
      g.setTransform(pipe);
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
      g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
      for (GraphicObject o : objects)
      {
        o.render(g);
      }
      BufferedImageAdapter ad = new BufferedImageAdapter(scaledImg, invertColors)
      {
        //TODO: Gefahr, dass man das Dithering ergebnis verändert, falls
        //der Algorithmus einen Pixel 2x ausliest
      @Override
        public void setGreyScale(int x, int y, int greyscale)
        {
          if (greyscale == 255)
          {
            scaledImg.getAlphaRaster().setPixel(x, y, new int[]
              {
                0, 0, 0
              });
          }
          else if (greyscale == 0)
          {
            scaledImg.setRGB(x, y, engraveColor.getRGB());
          }
        }
      };
      ad.setColorShift(this.getColorShift());
      DitheringAlgorithm alg = this.getDitherAlgorithm();
      if (pl != null)
      {
        alg.addProgressListener(pl);
      }
      alg.ditherDirect(ad);
      return scaledImg;
    }
    return null;
  }

  @Override
  public void renderPreview(Graphics2D gg, GraphicSet objects, MaterialProfile material, AffineTransform mm2px)
  {
    Rectangle2D bb = Helper.transform(objects.getBoundingBox(), mm2px);
    if (bb != null && bb.getWidth() > 0 && bb.getHeight() > 0)
    {
      BufferedImage scaledImg = this.getRenderedPreview(objects, material, mm2px);
      gg.drawImage(scaledImg, null, (int) bb.getX(), (int) bb.getY());
    }
  }

  @Override
  public void addToLaserJob(LaserJob job, GraphicSet set, List<LaserProperty> laserProperties)
  {
    double factor = Util.dpi2dpmm(this.getDPI());
    AffineTransform mm2laserPx = AffineTransform.getScaleInstance(factor, factor);
    //Decompose Objects if their distance is big enough
    for (GraphicSet objects  : this.decompose(set))
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
        //Then dither this image
        BufferedImageAdapter ad = new BufferedImageAdapter(scaledImg, invertColors);
        ad.setColorShift(this.getColorShift());
        BlackWhiteRaster bw = new BlackWhiteRaster(ad, this.getDitherAlgorithm());
        for (LaserProperty prop : laserProperties)
        {
          RasterPart part = new RasterPart(bw, prop, new Point(bb.x, bb.y), getDPI());
          job.addPart(part);
        }
      }
    }
  }

  @Override
  public LaserProfile clone()
  {
    RasterProfile rp = new RasterProfile();
    rp.colorShift = this.colorShift;
    rp.description = this.description;
    rp.ditherAlgorithm = this.ditherAlgorithm;
    rp.invertColors = this.invertColors;
    rp.name = this.name;
    rp.thumbnailPath = this.thumbnailPath;
    rp.setDPI(getDPI());
    return rp;
  }
  
  public int hashCode()
  {
    return super.hashCodeBase() * 31 + (invertColors?1:0) *17 + colorShift;
  }
  
  public boolean equals(Object obj)
  {
    if (obj == null)
    {
      return false;
    }
    if (!getClass().equals(obj.getClass()))
    {
      return false;
    }
    final RasterProfile other = (RasterProfile) obj;
    if (this.invertColors != other.invertColors)
    {
      return false;
    }
    if (this.colorShift != other.colorShift)
    {
      return false;
    }
    if (Util.differ(this.ditherAlgorithm, other.ditherAlgorithm))
    {
      return false;
    }
    return super.equalsBase(other);
  }
}
