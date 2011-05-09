/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.liblasercut.epilog;

import com.t_oster.liblasercut.LaserJob;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author thommy
 */
public class EpilogCutterTest {
    
    public EpilogCutterTest() {
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

    /**
     * Test of sendJob method, of class EpilogCutter.
     */
    @Test
    public void testSendJob() {
        System.out.println("sendJob");
        LaserJob job = new LaserJob("peter", "parker", "bla", 600);
        EpilogCutter instance = new EpilogCutter("localhost");
        instance.sendJob(job);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getResolutions method, of class EpilogCutter.
     */
    @Test
    public void testGetResolutions() {
        System.out.println("getResolutions");
        EpilogCutter instance = null;
        int[] expResult = null;
        int[] result = instance.getResolutions();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getBedWidth method, of class EpilogCutter.
     */
    @Test
    public void testGetBedWidth() {
        System.out.println("getBedWidth");
        EpilogCutter instance = null;
        int expResult = 0;
        int result = instance.getBedWidth();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getBedHeight method, of class EpilogCutter.
     */
    @Test
    public void testGetBedHeight() {
        System.out.println("getBedHeight");
        EpilogCutter instance = null;
        int expResult = 0;
        int result = instance.getBedHeight();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
}
