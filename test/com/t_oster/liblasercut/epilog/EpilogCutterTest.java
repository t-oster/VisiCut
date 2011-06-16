/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.liblasercut.epilog;

import com.t_oster.util.Point;
import com.t_oster.liblasercut.*;
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
    VectorPart vp = new VectorPart(new CuttingProperty(50, 100, 5000));
    vp.moveto(0, 0);
    vp.lineto(100, 0);
    vp.lineto(100, 100);
    vp.lineto(0, 100);
    vp.lineto(0, 0);
    vp.moveto(200, 0);
    vp.setFocus(300);
    vp.lineto(300, 0);
    vp.lineto(300, 100);
    vp.lineto(400, 100);
    vp.lineto(400, 0);
    vp.lineto(300, 0);

    RasterPart rp = new RasterPart(new EngravingProperty(80, 100));
    GreyscaleRaster testimage = new GreyscaleRaster(){

            public int getWidth() {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            public int getGreyScale(int x, int y) {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            public void setGreyScale(int x, int y, int grey) {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            public int getHeight() {
                throw new UnsupportedOperationException("Not supported yet.");
            }

    };

    rp.addImage(bwr, new Point(0, 200));

    LaserJob job = new LaserJob("allparts", "666", "bla", 500, null, vp, null);
    instance.sendJob(job);
  }
}
