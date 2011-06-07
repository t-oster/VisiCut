/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.liblasercut.epilog;

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
   * Test of sendJob method, of class EpilogCutter.
   */
  @Test
  public void testVectorJob() throws IllegalJobException, Exception
  {
    EpilogCutter.SIMULATE_COMMUNICATION = false;
    System.out.println("sendJob");

    EpilogCutter instance = new EpilogCutter("137.226.56.228");
    VectorPart vp = new VectorPart(new CuttingProperty(50, 100, 5000));
    vp.moveto(0, 0);
    for (int i = 0; i <= 500; i++)
    {
      vp.setFocus(i);
      vp.lineto(2 * i, 0);
    }
    LaserJob job = new LaserJob("vector", "666", "bla", 500, null, vp, null);
    instance.sendJob(job);
  }
}
