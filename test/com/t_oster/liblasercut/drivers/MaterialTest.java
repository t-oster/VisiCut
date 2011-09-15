/**
 * This file is part of VisiCut.
 * 
 *     VisiCut is free software: you can redistribute it and/or modify
 *     it under the terms of the Lesser GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *    VisiCut is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     Lesser GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with VisiCut.  If not, see <http://www.gnu.org/licenses/>.
 **/
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.liblasercut.drivers;

import com.t_oster.liblasercut.*;
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
    VectorPart vp = new VectorPart(new LaserProperty(10, 100, 5000));
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
    vp = new VectorPart(new LaserProperty(cutpower, cutspeed, 5000));
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
    VectorPart vp = new VectorPart(new LaserProperty(10, 100, 5000));
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
    vp = new VectorPart(new LaserProperty(cutpower, cutspeed, 5000));
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
