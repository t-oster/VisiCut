package com.t_oster.visicut.model;

import com.t_oster.liblasercut.BlackWhiteRaster;
import com.t_oster.liblasercut.BlackWhiteRaster.DitherAlgorithm;
import com.t_oster.liblasercut.LaserJob;
import com.t_oster.liblasercut.LaserProperty;
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
import java.util.LinkedList;

/**
 * This Class represents a profile, describing
 * how the Lasercutter generates RasterData
 * from given GraphicElements
 * 
 * @author thommy
 */
public class RasterProfile extends LaserProfile
{

  public RasterProfile()
  {
    this.setName("average");
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
  protected DitherAlgorithm ditherAlgorithm = DitherAlgorithm.AVERAGE;

  /**
   * Get the value of ditherAlgorithm
   *
   * @return the value of ditherAlgorithm
   */
  public DitherAlgorithm getDitherAlgorithm()
  {
    return ditherAlgorithm;
  }

  /**
   * Set the value of ditherAlgorithm
   *
   * @param ditherAlgorithm new value of ditherAlgorithm
   */
  public void setDitherAlgorithm(DitherAlgorithm ditherAlgorithm)
  {
    this.ditherAlgorithm = ditherAlgorithm;
  }

  @Override
  public void renderPreview(Graphics2D gg, GraphicSet objects)
  {
    Rectangle2D bb = objects.getBoundingBox();
    if (bb != null && bb.getWidth() > 0 && bb.getHeight() > 0)
    {
      BufferedImage scaledImg = new BufferedImage((int) bb.getWidth(), (int) bb.getHeight(), BufferedImage.TYPE_INT_RGB);
      Graphics2D g = scaledImg.createGraphics();
      g.setColor(Color.white);
      g.fillRect(0, 0, scaledImg.getWidth(), scaledImg.getHeight());
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
      BlackWhiteRaster bwr = new BlackWhiteRaster(ad, this.getDitherAlgorithm());
      gg.setColor(this.getColor());
      for (int y = 0; y < bwr.getHeight(); y++)
      {
        for (int x = 0; x < bwr.getWidth(); x++)
        {
          if (bwr.isBlack(x, y))
          {
            gg.drawLine((int) bb.getX() + x, (int) bb.getY() + y, (int) bb.getX() + x, (int) bb.getY() + y);
          }
        }
      }
    }
  }

  @Override
  public void addToLaserJob(LaserJob job, GraphicSet objects)
  {
    for (LaserProperty prop : this.getLaserProperties())
    {
      Rectangle2D bb = objects.getBoundingBox();
      if (bb != null && bb.getWidth() > 0 && bb.getHeight() > 0)
      {
        BufferedImage scaledImg = new BufferedImage((int) bb.getWidth(), (int) bb.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = scaledImg.createGraphics();
        g.setColor(Color.white);
        g.fillRect(0, 0, scaledImg.getWidth(), scaledImg.getHeight());
        if (objects.getTransform() != null)
        {
          Rectangle2D origBB = objects.getOriginalBoundingBox();
          Rectangle2D targetBB = new Rectangle(0, 0, scaledImg.getWidth(), scaledImg.getHeight());
          g.setTransform(Helper.getTransform(origBB, targetBB));
        }
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        for (GraphicObject o : objects)
        {
          o.render(g);
        }
        BufferedImageAdapter ad = new BufferedImageAdapter(scaledImg, invertColors);
        ad.setColorShift(this.getColorShift());
        BlackWhiteRaster bw = new BlackWhiteRaster(ad, this.getDitherAlgorithm());
        job.getRasterPart().addImage(bw, prop, new Point((int) bb.getX(), (int) bb.getY()));
      }
    }
  }

  @Override
  public LaserProfile clone()
  {
    RasterProfile rp = new RasterProfile();
    rp.color = this.color;
    rp.colorShift = this.colorShift;
    rp.description = this.description;
    rp.ditherAlgorithm = this.ditherAlgorithm;
    rp.invertColors = this.invertColors;
    rp.name = this.name;
    rp.thumbnailPath = this.thumbnailPath;
    //rp.laserProperties = new LinkedList<LaserProperty>();
    for (LaserProperty l: this.getLaserProperties())
    {
      rp.laserProperties.add(l.clone());
    }
    return rp;
  }
}
