/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.visicut.model.graphicelements.pdfsupport;

import com.t_oster.visicut.model.graphicelements.GraphicSet;
import java.io.File;
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
public class PDFImporterTest
{
  
  public PDFImporterTest()
  {
  }

  @BeforeClass
  public static void setUpClass() throws Exception
  {
  }

  @AfterClass
  public static void tearDownClass() throws Exception
  {
  }
  
  @Before
  public void setUp()
  {
  }
  
  @After
  public void tearDown()
  {
  }

  /**
   * Test of importFile method, of class PDFImporter.
   */
  @Test
  public void testImportFile() throws Exception
  {
    System.out.println("importFile");
    File inputFile = new File("test/files/sample.pdf");
    PDFImporter instance = new PDFImporter();
    instance.importFile(inputFile);
    
  }
}
