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

import javax.swing.JOptionPane;
import com.t_oster.liblasercut.LaserJob;
import com.t_oster.liblasercut.VectorPart;
import com.t_oster.liblasercut.LaserProperty;
import com.t_oster.liblasercut.IllegalJobException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author oster
 */
public class StartPointTest {

    public StartPointTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
  public void testMovingAtTheEnd() throws IllegalJobException, Exception
  {
    EpilogCutter.SIMULATE_COMMUNICATION = false;
    System.out.println("sendJob");

    EpilogCutter instance = new EpilogCutter("137.226.56.228");
    VectorPart vp = new VectorPart(new LaserProperty(50, 100, 5000));
    vp.moveto(2000, 1000);
    vp.setFocus(200);
    vp.setFocus(0);
    vp.moveto(0, 100);
    vp.lineto(200, 100);
    vp.moveto(100, 0);
    vp.lineto(100,200);

    vp.moveto(2000, 1100);
    vp.lineto(2200, 1100);
    vp.moveto(2100, 1000);
    vp.lineto(2100,1200);
    LaserJob job = new LaserJob("startPoint", "123", "bla", 500, null, vp, null);
    instance.sendJob(job);
    JOptionPane.showMessageDialog(null, "Please start the job 'startPoint' and then position your material");
  }

}