/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.liblasercut.epilog;

import com.t_oster.liblasercut.IllegalJobException;
import com.t_oster.liblasercut.LaserJob;
import com.t_oster.liblasercut.VectorPart;
import org.junit.Test;

/**
 *
 * @author thommy
 */
public class EpilogCutterTest {
   

    /**
     * Test of sendJob method, of class EpilogCutter.
     */
    @Test
    public void testSendJob() throws IllegalJobException, Exception {
        EpilogCutter.SIMULATE_COMMUNICATION = false;
        System.out.println("sendJob");
        
        EpilogCutter instance = new EpilogCutter("137.226.56.228");
        VectorPart vp = new VectorPart(5000,20,100);
        vp.line(0,0,0,100);
        vp.line(0,100,100,100);
        vp.line(100,100,100,0);
        vp.line(100,0,0,0);
        LaserJob job = new LaserJob("testpilog", "666", "bla", 500, vp);
        instance.sendJob(job);
    }
    
}
