/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.liblasercut.epilog;

import com.t_oster.liblasercut.LaserJob;
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
    public void testSendJob() {
        EpilogCutter.SIMULATE_COMMUNICATION = false;
        System.out.println("sendJob");
        LaserJob job = new LaserJob("jepilog", "666", "bla", 500);
        EpilogCutter instance = new EpilogCutter("137.226.56.228");
        instance.sendJob(job);
    }
    
    @Test
    public void testSendJobSimulated() {
        EpilogCutter.SIMULATE_COMMUNICATION = true;
        System.out.println("sendJob  (simulation)");
        LaserJob job = new LaserJob("Drucken Neues Dokument 1", "666", "bla", 500);
        EpilogCutter instance = new EpilogCutter("137.226.56.228");
        instance.sendJob(job);
    }
    
}
