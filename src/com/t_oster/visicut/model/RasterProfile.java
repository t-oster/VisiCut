package com.t_oster.visicut.model;

import com.t_oster.liblasercut.BlackWhiteRaster;
import com.t_oster.liblasercut.BlackWhiteRaster.DitherAlgorithm;
import com.t_oster.liblasercut.LaserJob;
import com.t_oster.liblasercut.platform.Point;
import com.t_oster.liblasercut.utils.BufferedImageAdapter;
import com.t_oster.visicut.model.graphicelements.GraphicObject;
import com.t_oster.visicut.model.graphicelements.GraphicSet;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

/**
 * This Class represents a profile, describing
 * how the Lasercutter generates RasterData
 * from given GraphicElements
 * 
 * @author thommy
 */
public class RasterProfile extends LaserProfile
{

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
    if (bb.getWidth() > 0 && bb.getHeight() > 0)
    {
      BufferedImage scaledImg = new BufferedImage((int) bb.getWidth(), (int) bb.getHeight(), BufferedImage.TYPE_INT_RGB);
      Graphics2D g = scaledImg.createGraphics();
      g.setColor(Color.white);
      g.fillRect(0, 0, scaledImg.getWidth(), scaledImg.getHeight());
      if (objects.getTransform() != null)
      {
        g.setTransform(objects.getTransform());
      }
      for (GraphicObject o : objects)
      {
        o.render(g);
      }
      BufferedImageAdapter ad = new BufferedImageAdapter(scaledImg);
      //ad.setColorShift(this.getColorShift());
      BlackWhiteRaster bw = new BlackWhiteRaster(ad, this.getDitherAlgorithm());
      gg.setColor(this.getColor());
      for (int y = 0; y < bw.getHeight(); y++)
      {
        for (int x = 0; x < bw.getWidth(); x++)
        {
          if (bw.isBlack(x, y))
          {
            gg.drawLine(x, y, x, y);
          }
        }
      }
    }
  }

  @Override
  public void addToLaserJob(LaserJob job, GraphicSet objects)
  {
    Rectangle2D bb = objects.getBoundingBox();
    if (bb.getWidth() > 0 && bb.getHeight() > 0)
    {

      BufferedImage scaledImg = new BufferedImage((int) bb.getWidth(), (int) bb.getHeight(), BufferedImage.TYPE_INT_RGB);
      Graphics2D g = scaledImg.createGraphics();
      g.setTransform(objects.getTransform());
      for (GraphicObject o : objects)
      {
        o.render(g);
      }
      BufferedImageAdapter ad = new BufferedImageAdapter(scaledImg);
      ad.setColorShift(this.getColorShift());
      BlackWhiteRaster bw = new BlackWhiteRaster(ad, this.getDitherAlgorithm());
      job.getRasterPart().addImage(bw, this.getCuttingProperty(), new Point((int) bb.getX(), (int) bb.getY()));
    }
  }
}
