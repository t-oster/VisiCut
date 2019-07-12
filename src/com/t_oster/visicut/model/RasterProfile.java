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
import com.t_oster.liblasercut.LaserCutter;
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
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import java.util.Hashtable;
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
    return ditherAlgorithm.clone(); // clone() required because otherwise we will get trouble with ProgressListener
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

  public BufferedImage getRenderedPreview(GraphicSet objects, MaterialProfile material, AffineTransform mm2px) throws InterruptedException
  {
    return this.getRenderedPreview(objects, material, mm2px, null);
  }

  private BufferedImage renderObjects(GraphicSet objects, AffineTransform mm2laserPx, Rectangle bb) {
    // Create an Image which fits the bounding box
    //
    // Unfortunately, we cannot use BufferedImage.TYPE_BYTE_GRAY here, mainly
    // because the SVG rendering refuses to render color gradients on a
    // grayscale buffer. (just throws an exception).
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
    // antialiasing doesn't make sense for 1-bit output, and makes font edges look jittery.
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
    g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    for (GraphicObject o : objects)
    {
      o.render(g);
    }
    return scaledImg;
  }

  public BufferedImage getRenderedPreview(GraphicSet objects, MaterialProfile material, AffineTransform mm2px, ProgressListener pl) throws InterruptedException
  {
    Rectangle bb = Helper.toRect(Helper.transform(objects.getBoundingBox(), mm2px));
    final Color engraveColor = material.getEngraveColor();
    if (bb != null && bb.width > 0 && bb.height > 0)
    {
      final BufferedImage scaledImg = renderObjects(objects, mm2px, bb);
      BufferedImageAdapter ad = new BufferedImageAdapter(scaledImg, invertColors);
      ad.setColorShift(this.getColorShift());
      DitheringAlgorithm alg = this.getDitherAlgorithm();
      if (pl != null)
      {
        alg.addProgressListener(pl);
      }
      alg.ditherDirect(ad);


      return convertToOneBitGrayscale(scaledImg, engraveColor);
    }
    return null;
  }
// adapted from https://stackoverflow.com/a/12860219
  /**
   * convert a black/white image to 1bit indexed palette,
   * optionally substituting $engraveColor for black.
   *
   * This saves about 75% of memory.
   * @param image
   * @param engraveColor the color which should be substituted for black (may be null)
   * @return
   */
  public static BufferedImage convertToOneBitGrayscale(BufferedImage image, Color engraveColor) {
  // black and white
  IndexColorModel whiteOrBlack = new IndexColorModel(1, 2, new byte[] {(byte) 255, (byte) 0}, new byte[] {(byte) 255, (byte) 0}, new byte[] {(byte) 255, (byte) 0}, new byte[] {(byte) 255, (byte) 255});
  IndexColorModel engravePreviewColored = new IndexColorModel(1, 2, new byte[] {(byte) 0, (byte) engraveColor.getRed()}, new byte[] {(byte) 0, (byte) engraveColor.getGreen()}, new byte[] {(byte) 0, (byte) engraveColor.getBlue()}, new byte[] {(byte) 0, (byte) 255});
  BufferedImage result = new BufferedImage(
            image.getWidth(),
            image.getHeight(),
            BufferedImage.TYPE_BYTE_INDEXED, whiteOrBlack);
  Graphics g = result.getGraphics();
  g.drawImage(image, 0, 0, null);
  g.dispose();
  if (engraveColor != null) {
    BufferedImage resultColored = new BufferedImage(engravePreviewColored, result.getRaster(), result.isAlphaPremultiplied(), new Hashtable());
    return resultColored;
  } else {
    return result;
  }
}

  @Override
  public void renderPreview(Graphics2D gg, GraphicSet objects, MaterialProfile material, AffineTransform mm2px) throws InterruptedException
  {
    Rectangle2D bb = Helper.transform(objects.getBoundingBox(), mm2px);
    if (bb != null && bb.getWidth() > 0 && bb.getHeight() > 0)
    {
      BufferedImage scaledImg = this.getRenderedPreview(objects, material, mm2px);
      gg.drawImage(scaledImg, null, (int) bb.getX(), (int) bb.getY());
    }
  }

  @Override
  public void addToLaserJob(LaserJob job, GraphicSet set, List<LaserProperty> laserProperties, LaserCutter cutter) throws InterruptedException
  {
    double factor = Util.dpi2dpmm(this.getDPI());
    AffineTransform mm2laserPx = AffineTransform.getScaleInstance(factor, factor);
    //Decompose Objects if their distance is big enough
    for (GraphicSet objects  : this.decompose(set))
    {
      Rectangle bb = Helper.toRect(Helper.transform(objects.getBoundingBox(), mm2laserPx));
      if (bb != null && bb.width > 0 && bb.height > 0)
      {
        // render into color image
        BufferedImage scaledImg = renderObjects(objects, mm2laserPx, bb);
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
