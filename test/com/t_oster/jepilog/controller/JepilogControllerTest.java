/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.jepilog.controller;

import com.t_oster.jepilog.controller.JepilogController.StartingPosition;
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
public class JepilogControllerTest {
    
    public JepilogControllerTest() {
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
    public void testSetStartingPosition() throws IOException {
        System.out.println("setStartingPosition");
        JepilogController instance = new JepilogController();
        instance.importSvg(new File("butterfly.svg"));
        instance.setStartingPosition(JepilogController.StartingPosition.BOTTOM_LEFT);
        assertEquals(JepilogController.StartingPosition.BOTTOM_LEFT, instance.getStartingPosition());
        instance.setStartingPosition(JepilogController.StartingPosition.CENTER);
        assertEquals(JepilogController.StartingPosition.CENTER, instance.getStartingPosition());
        instance.setStartPoint(10,10);
        assertEquals(JepilogController.StartingPosition.CUSTOM, instance.getStartingPosition());
    }

}
