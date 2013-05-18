/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.visicut.model.graphicelements.psvgsupport;

import com.t_oster.visicut.model.graphicelements.ImportException;
import java.io.File;
import java.util.LinkedList;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class PSVGImporterTest
{
  
  public PSVGImporterTest()
  {
  }

  @Test
  public void testSomeMethod() throws ImportException
  {
    PSVGImporter i = new PSVGImporter();
    i.importFile(new File("/tmp/Candleholder.psvg"), new LinkedList<String>());
  }
}