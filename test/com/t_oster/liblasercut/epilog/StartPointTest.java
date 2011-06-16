/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.t_oster.liblasercut.epilog;

import javax.swing.JOptionPane;
import com.t_oster.liblasercut.LaserJob;
import com.t_oster.liblasercut.VectorPart;
import com.t_oster.liblasercut.CuttingProperty;
import com.t_oster.liblasercut.IllegalJobException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

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
    VectorPart vp = new VectorPart(new CuttingProperty(50, 100, 5000));
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
    LaserJob job = new LaserJob("startPoint", "666", "bla", 500, null, vp, null);
    instance.sendJob(job);
    JOptionPane.showMessageDialog(null, "Please start the job 'startPoint' and then position your material");
  }

}