/**
 * This file is part of VisiCut.
 * Copyright (C) 2012 Thomas Oster <thomas.oster@rwth-aachen.de>
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
package com.t_oster.visicut.model;

import com.t_oster.liblasercut.LaserJob;
import com.t_oster.liblasercut.LaserProperty;
import com.t_oster.liblasercut.ProgressListener;
import com.t_oster.liblasercut.Raster3dPart;
import com.t_oster.liblasercut.platform.Point;
import com.t_oster.liblasercut.utils.BufferedImageAdapter;
import com.t_oster.visicut.misc.Helper;
import com.t_oster.visicut.model.graphicelements.GraphicObject;
import com.t_oster.visicut.model.graphicelements.GraphicSet;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
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

  @Override
  public void renderPreview(Graphics2D gg, GraphicSet objects, MaterialProfile material)
  {
    this.renderPreview(gg, objects, material, null);
  }

  public void renderPreview(Graphics2D gg, GraphicSet objects, MaterialProfile material, ProgressListener pl)
  {
    Rectangle2D bb = objects.getBoundingBox();
    if (bb != null && bb.getWidth() > 0 && bb.getHeight() > 0)
    {
      BufferedImage scaledImg = new BufferedImage((int) bb.getWidth(), (int) bb.getHeight(), BufferedImage.TYPE_INT_RGB);
      Graphics2D g = scaledImg.createGraphics();
      g.setColor(Color.white);
      g.fillRect(0, 0, scaledImg.getWidth(), scaledImg.getHeight());
      g.setClip(0, 0, scaledImg.getWidth(), scaledImg.getHeight());
      if (objects.getTransform() != null)
      {
        Rectangle2D origBB = objects.getOriginalBoundingBox();
        Rectangle2D targetBB = new Rectangle(0, 0, scaledImg.getWidth(), scaledImg.getHeight());
        g.setTransform(Helper.getTransform(origBB, targetBB));
      }
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
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
            gg.setColor(scaled);
            gg.drawLine((int) bb.getX() + x, (int) bb.getY() + y, (int) bb.getX() + x, (int) bb.getY() + y);
          }
        }
        if (pl != null)
        {
          pl.progressChanged(this, 100 * y / ad.getHeight());
        }
      }
    }
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
    for (GraphicSet objects : this.decompose(set))
    {
      Rectangle2D bb = objects.getBoundingBox();
      if (bb != null && bb.getWidth() > 0 && bb.getHeight() > 0)
      {
        BufferedImage scaledImg = new BufferedImage((int) bb.getWidth(), (int) bb.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = scaledImg.createGraphics();
        g.setColor(Color.white);
        g.fillRect(0, 0, scaledImg.getWidth(), scaledImg.getHeight());
        g.setClip(0, 0, scaledImg.getWidth(), scaledImg.getHeight());
        if (objects.getTransform() != null)
        {
          Rectangle2D origBB = objects.getOriginalBoundingBox();
          Rectangle2D targetBB = new Rectangle(0, 0, scaledImg.getWidth(), scaledImg.getHeight());
          g.setTransform(Helper.getTransform(origBB, targetBB));
        }
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
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
    return rp;
  }
}
