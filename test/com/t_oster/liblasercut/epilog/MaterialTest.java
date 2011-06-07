/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.liblasercut.epilog;

import com.kitfox.svg.SVGDiagram;
import com.kitfox.svg.SVGUniverse;
import com.t_oster.liblasercut.*;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.net.URI;
import javax.swing.JOptionPane;
import org.junit.Test;

/**
 *
 * @author oster
 */
public class MaterialTest
{

  @Test
  public void rects2() throws IllegalJobException, Exception
  {
    VectorPart vp = new VectorPart(new CuttingProperty(10, 100, 5000));
    int x = 0;
    for (int speed = 40; speed >= 10; speed -= 10)
    {
      vp.setSpeed(speed);
      for (int power = 10; power <= 100; power += 5)
      {
        vp.moveto(300 * x, 15 * power);
        vp.setPower(power);
        vp.lineto(300 * x + 200, 15 * power);
        vp.lineto(300 * x + 200, 15 * power + 50);
        vp.lineto(300 * x, 15 * power + 50);
        vp.lineto(300 * x, 15 * power);
      }
      x++;
    }
    LaserJob job = new LaserJob("rects40-10", "bla", "bla", 500, null, vp, null);
    EpilogCutter instance = new EpilogCutter("137.226.56.228");
    //instance.sendJob(job);
    JOptionPane.showConfirmDialog(null, "Please start the job with the name 'rects100-50' and STOP when cut through");
    int row = Integer.parseInt(JOptionPane.showInputDialog(null, "Please enter row of the first cut through (1...20)"));
    int column = Integer.parseInt(JOptionPane.showInputDialog(null, "Please enter column of the first cut through (1...5)"));
    int cutpower = 5 + row * 5;
    int cutspeed = 50 - column * 10;
    vp = new VectorPart(new CuttingProperty(cutpower, cutspeed, 5000));
    x = 0;
    for (int speed = 40; speed >= 10; speed -= 10)
    {
      //vp.setSpeed(speed);
      vp.moveto(300 * x, 0);
      vp.lineto(300 * x + 100, 0);
      vp.lineto(300 * x + 100, 1500);
      vp.lineto(300 * x, 1500);
      vp.lineto(300 * x, 0);
      x++;
    }
    job = new LaserJob("cutout", "bla", "bla", 500, null, vp, null);
    instance.sendJob(job);
    JOptionPane.showConfirmDialog(null, "please cut out");
  }

  @Test
  public void rects() throws IllegalJobException, Exception
  {
    VectorPart vp = new VectorPart(new CuttingProperty(10, 100, 5000));
    int x = 0;
    for (int speed = 100; speed >= 50; speed -= 10)
    {
      vp.setSpeed(speed);
      for (int power = 10; power <= 100; power += 5)
      {
        vp.moveto(300 * x, 15 * power);
        vp.setPower(power);
        vp.lineto(300 * x + 200, 15 * power);
        vp.lineto(300 * x + 200, 15 * power + 50);
        vp.lineto(300 * x, 15 * power + 50);
        vp.lineto(300 * x, 15 * power);
      }
      x++;
    }
    LaserJob job = new LaserJob("rects100-50", "bla", "bla", 500, null, vp, null);
    EpilogCutter instance = new EpilogCutter("137.226.56.228");
    instance.sendJob(job);
    JOptionPane.showConfirmDialog(null, "Please start the job with the name 'rects100-50' and STOP when cut through");
    int row = Integer.parseInt(JOptionPane.showInputDialog(null, "Please enter row of the first cut through (1...20)"));
    int column = Integer.parseInt(JOptionPane.showInputDialog(null, "Please enter column of the first cut through (1...5)"));
    int cutpower = 5 + row * 5;
    int cutspeed = 100 - column * 10;
    vp = new VectorPart(new CuttingProperty(cutpower, cutspeed, 5000));
    x = 0;
    for (int speed = 100; speed >= 50; speed -= 10)
    {
      //vp.setSpeed(speed);
      vp.moveto(300 * x, 0);
      vp.lineto(300 * x + 100, 0);
      vp.lineto(300 * x + 100, 1500);
      vp.lineto(300 * x, 1500);
      vp.lineto(300 * x, 0);
      x++;
    }
    job = new LaserJob("cutout", "bla", "bla", 500, null, vp, null);
    JOptionPane.showConfirmDialog(null, "please cut out");
  }
}
