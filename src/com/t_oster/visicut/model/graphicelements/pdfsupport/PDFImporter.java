/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.visicut.model.graphicelements.pdfsupport;

import com.sun.pdfview.PDFFile;
import com.sun.pdfview.PDFObject;
import com.t_oster.visicut.misc.ExtensionFilter;
import com.t_oster.visicut.model.graphicelements.GraphicSet;
import com.t_oster.visicut.model.graphicelements.ImportException;
import com.t_oster.visicut.model.graphicelements.Importer;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import javax.swing.filechooser.FileFilter;

/**
 *
 * @author thommy
 */
public class PDFImporter implements Importer
{

  public GraphicSet importFile(File inputFile) throws ImportException
  {
    GraphicSet result = new GraphicSet();
    try
    {
      
      FileInputStream istr = new FileInputStream(inputFile);
      byte[] buf = new byte[(int) inputFile.length()];
      int read = 0;
      int offset = 0;
      while (read >= 0)
      {
        read = istr.read(buf, offset, buf.length - offset);
      }
      istr.close();
      ByteBuffer byteBuf = ByteBuffer.allocate(buf.length);
      byteBuf.put(buf);
      PDFFile pdf = new PDFFile(byteBuf);
      PDFObject root = pdf.getRoot();
      PDFObject pagesObj = (PDFObject) root.getDictRef("Pages");
      Iterator i = pagesObj.getDictKeys();
      while (i.hasNext())
      {
        Object key = i.next();
        System.out.println("Key: "+key);
        System.out.println("Value: "+root.getDictRef((String) key));
      }
    }
    catch (IOException ex)
    {
      throw new ImportException(ex);
    }
    return result;
  }

  public FileFilter getFileFilter()
  {
    return new ExtensionFilter(".pdf", "Portable Document Format (*.pdf)");
  }
}
