/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.jepilog.util;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
public class PDFConverterTest {
    
    public PDFConverterTest() {
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
     * Test of getSupportedFileExtensions method, of class PDFConverter.
     */
    @Test
    public void testGetSupportedFileExtensions() {
        System.out.println("getSupportedFileExtensions");
        PDFConverter instance = new PDFConverter();
        String[] expResult = new String[]{"pdf"};
        String[] result = instance.getSupportedFileExtensions();
        assertEquals(expResult.length, result.length);
        assertEquals(expResult[0], result[0]);
    }

    /**
     * Test of load method, of class PDFConverter.
     */
    @Test
    public void testLoad_File() throws Exception {
        System.out.println("load");
        File image = new File("test//files//sample.pdf");
        PDFConverter instance = new PDFConverter();
        instance.load(image);
    }

    /**
     * Test of load method, of class PDFConverter.
     */
    @Test
    public void testLoad_InputStream() throws Exception {
        System.out.println("load");
        InputStream stream = new FileInputStream(new File("test/files/sample.pdf"));
        PDFConverter instance = new PDFConverter();
        instance.load(stream);
    }

    /**
     * Test of convert method, of class PDFConverter.
     */
    @Test
    public void testConvert_File() throws Exception {
        System.out.println("convert");
        File output = new File("/tmp/JepilogPdfConversionTest.svg");
        PDFConverter instance = new PDFConverter();
        instance.load(new File("test/files/sample.pdf"));
        instance.convert(output);
        
    }

    /**
     * Test of convert method, of class PDFConverter.
     */
    @Test
    public void testConvert_OutputStream() throws FileNotFoundException, IOException {
        System.out.println("convert");
        OutputStream stream = new FileOutputStream(new File("/tmp/test.svg"));
        PDFConverter instance = new PDFConverter();
        instance.load(new File("test/files/sample.pdf"));
        instance.convert(stream);
    }
}
