/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.liblasercut.drivers;

import java.awt.Font;
import com.t_oster.liblasercut.utils.BufferedImageAdapter;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import com.t_oster.liblasercut.platform.Point;
import com.t_oster.liblasercut.*;
import java.awt.Color;
import java.util.LinkedList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author thommy
 */
public class EpilogCutterTest
{

  @Test
  public void testEncode()
  {
    EpilogCutter instance = new EpilogCutter(null);
    List<Byte> in = new LinkedList<Byte>();
    for (int k = 0; k < 100; k++)
    {
      in.add((byte) 7);
    }
    for (int k = 0; k < 50; k++)
    {
      in.add((byte) k);
    }
    in = instance.encode(in);
    assertEquals((byte) -99, (byte) in.get(0));
    assertEquals((byte) 7, (byte) in.get(1));
    assertEquals((byte) 49, (byte) in.get(2));
    for (int k = 0; k < 50; k++)
    {
      assertEquals((byte) k, (byte) in.get(3 + k));
    }
    assertEquals((byte) -128, (byte) 0x80);
  }

  private BufferedImage getTestImage()
    {
      BufferedImage testbild = new BufferedImage(300,300, BufferedImage.TYPE_INT_RGB);
    Graphics g = testbild.getGraphics();
    g.setColor(Color.WHITE);
    g.fillRect(0, 0, 300, 300);
    g.setColor(Color.BLACK);
    g.drawRect(0, 0, 299, 299);
    g.setFont(new Font("sansserif", Font.BOLD, 12));
    g.drawString("The quick brown fox jumps", 0, 20);
    g.drawString("over the lazy dog.", 0, 40);
    g.fillRect(0, 150, 75, 75);
    g.setColor(Color.WHITE);
    g.fillRect(75, 150, 75, 75);
    g.setColor(Color.DARK_GRAY);
    g.fillRect(150, 150, 75, 75);
    g.setColor(Color.LIGHT_GRAY);
    g.fillRect(3*75, 150, 75, 75);
    for (int x=0;x<300;x++){
        g.setColor(new Color(255*x/300,255*x/300,255*x/300));
        g.drawLine(x, 200, x, 300);
    }
    return testbild;
  }

  /**
   * This method sends a job which contains all three parts (raster,raster3d,vector)
   * and also tests focus change on every part
   */
  @Test
  public void testFullJob() throws IllegalJobException, Exception
  {
    EpilogCutter.SIMULATE_COMMUNICATION = false;
    System.out.println("sendJob");

    EpilogCutter instance = new EpilogCutter("137.226.56.228");
    System.out.println("Creating VP");
    VectorPart vp = new VectorPart(new LaserProperty(50, 100, 5000));
    vp.moveto(0, 0);
    vp.lineto(100, 0);
    vp.lineto(100, 100);
    vp.lineto(0, 100);
    vp.lineto(0, 0);
    vp.moveto(200, 0);
    vp.setFocus(300);
    vp.lineto(300, 0);
    vp.lineto(300, 100);
    vp.lineto(200, 100);
    vp.lineto(200, 0);
    System.out.println("Creating RP");
    RasterPart rp = new RasterPart(new LaserProperty(80, 100));
    rp.addImage(new BlackWhiteRaster(new BufferedImageAdapter(getTestImage()), BlackWhiteRaster.DitherAlgorithm.FLOYD_STEINBERG), new Point(0, 200));
    System.out.println("Creating R3dP");
    Raster3dPart r3p = new Raster3dPart(new LaserProperty(80, 100));
    r3p.addImage(new BufferedImageAdapter(getTestImage()), new Point(0, 600));
    System.out.println("Creating Job");
    LaserJob job = new LaserJob("allparts", "666", "bla", 500, r3p, vp, rp);
    System.out.println("Sending Job");
    instance.sendJob(job);
    System.out.println("Done.");
  }
}
