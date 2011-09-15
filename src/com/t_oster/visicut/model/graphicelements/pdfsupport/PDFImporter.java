/**
 * This file is part of VisiCut.
 * 
 *     VisiCut is free software: you can redistribute it and/or modify
 *     it under the terms of the Lesser GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *    VisiCut is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     Lesser GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with VisiCut.  If not, see <http://www.gnu.org/licenses/>.
 **/
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
