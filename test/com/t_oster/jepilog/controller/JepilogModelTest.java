/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.jepilog.controller;

import java.awt.Point;
import com.t_oster.jepilog.model.JepilogModel;
import java.io.File;
import java.io.IOException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author thommy
 */
public class JepilogModelTest {
    
    public JepilogModelTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    /**
     * Test of setStartingPosition method, of class JepilogController.
     */
    @Test
    public void testSetStartingPosition() throws IOException, Exception {
        System.out.println("setStartingPosition");
        JepilogModel instance = new JepilogModel();
        instance.importSVG(new File("butterfly.svg"));
        instance.setStartPosition("bottom left");
        assertEquals("bottom left", instance.getStartPosition());
        instance.setStartPosition("center");
        assertEquals("center", instance.getStartPosition());
        instance.setStartPoint(new Point(10,10));
        assertEquals("custom", instance.getStartPosition());
    }

}
