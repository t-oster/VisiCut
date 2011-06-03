/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.liblasercut;

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
public class BlackWhiteRasterTest {
    
    public BlackWhiteRasterTest() {
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
    public void testByteRepresentation(){
        BlackWhiteRaster ras = new BlackWhiteRaster(15,23);
        assertEquals(15, ras.getWidth());
        assertEquals(23, ras.getHeight());
        for (int x = 0;x<ras.getWidth();x++)
        {
            for (int y = 0;y<ras.getHeight();y++)
            {
                boolean black = ((Math.random()*10)%2 == 1);
                ras.setBlack(x, y, black);
                assertEquals(black, ras.isBlack(x,y));
            }
        }
    }
}
