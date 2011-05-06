/*
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * "The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations under
 * the License.
 *
 * The Original Code is ICEpdf 3.0 open source software code, released
 * May 1st, 2009. The Initial Developer of the Original Code is ICEsoft
 * Technologies Canada, Corp. Portions created by ICEsoft are Copyright (C)
 * 2004-2011 ICEsoft Technologies Canada, Corp. All Rights Reserved.
 *
 * Contributor(s): _____________________.
 *
 * Alternatively, the contents of this file may be used under the terms of
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"
 * License), in which case the provisions of the LGPL License are
 * applicable instead of those above. If you wish to allow use of your
 * version of this file only under the terms of the LGPL License and not to
 * allow others to use your version of this file under the MPL, indicate
 * your decision by deleting the provisions above and replace them with
 * the notice and other provisions required by the LGPL License. If you do
 * not delete the provisions above, a recipient may use your version of
 * this file under either the MPL or the LGPL License."
 *
 */


import org.icepdf.core.pobjects.Document;
import org.icepdf.core.pobjects.Page;
import org.icepdf.core.pobjects.PDimension;
import org.icepdf.core.exceptions.PDFException;
import org.icepdf.core.exceptions.PDFSecurityException;
import org.icepdf.core.util.GraphicsRenderingHints;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.IIOImage;
import javax.imageio.ImageWriteParam;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.image.DataBuffer;
import java.awt.*;
import java.io.*;
import java.util.Iterator;

/**
 * The <code>MultiPageCapture</code> class is an example of how to save page
 * captures to disk.  A PDF, specified at the command line, is opened, and
 * every page in the document is captured as an image and saved into one
 * multi-page group 4 fax TIFF graphics file.
 *
 * @since 4.0
 */
public class MultiPageCapture {
    public static final double FAX_RESOLUTION = 200.0;
    public static final double PRINTER_RESOLUTION = 300.0;
    
    // This compression type may be wpecific to JAI ImageIO Tools
    public static final String COMPRESSION_TYPE_GROUP4FAX = "CCITT T.6";
    
    public static void main(String[] args) {
        // Verify that ImageIO can output TIFF
        Iterator<ImageWriter> iterator = ImageIO.getImageWritersByFormatName("tiff");
        if (!iterator.hasNext()) {
            System.out.println(
                "ImageIO missing required plug-in to write TIFF files. " +
                "You can download the JAI ImageIO Tools from: " +
                "https://jai-imageio.dev.java.net/");
            return;
        }
        boolean foundCompressionType = false;
        for(String type : iterator.next().getDefaultWriteParam().getCompressionTypes()) {
            if (COMPRESSION_TYPE_GROUP4FAX.equals(type)) {
                foundCompressionType = true;
                break;
            }
        }
        if (!foundCompressionType) {
            System.out.println(
                "TIFF ImageIO plug-in does not support Group 4 Fax " +
                "compression type ("+COMPRESSION_TYPE_GROUP4FAX+")");
            return;
        }
        
        // Get a file from the command line to open
        String filePath = args[0];

        // open the url
        Document document = new Document();
        try {
            document.setFile(filePath);
        } catch (PDFException ex) {
            System.out.println("Error parsing PDF document " + ex);
        } catch (PDFSecurityException ex) {
            System.out.println("Error encryption not supported " + ex);
        } catch (FileNotFoundException ex) {
            System.out.println("Error file not found " + ex);
        } catch (IOException ex) {
            System.out.println("Error handling PDF document " + ex);
        }

        try {
            // save page caputres to file.
            File file = new File("imageCapture.tif");
            ImageOutputStream ios = ImageIO.createImageOutputStream(file);
            ImageWriter writer = ImageIO.getImageWritersByFormatName("tiff").next();
            writer.setOutput(ios);

            // Paint each pages content to an image and write the image to file
            for (int i = 0; i < document.getNumberOfPages(); i++) {
                final double targetDPI = PRINTER_RESOLUTION;
                float scale = 1.0f;
                float rotation = 0f;
                
                // Given no initial zooming, calculate our natural DPI when
                // printed to standard US Letter paper
                PDimension size = document.getPageDimension(i, rotation, scale);
                double dpi = Math.sqrt((size.getWidth()*size.getWidth()) +
                                       (size.getHeight()*size.getHeight()) ) /
                             Math.sqrt((8.5*8.5)+(11*11));
                
                // Calculate scale required to achieve at least our target DPI
                if (dpi < (targetDPI-0.1)) {
                    scale = (float) (targetDPI / dpi);
                    size = document.getPageDimension(i, rotation, scale);
                }
                
                int pageWidth = (int) size.getWidth();
                int pageHeight = (int) size.getHeight();
                int[] cmap = new int[] { 0xFF000000, 0xFFFFFFFF };
                IndexColorModel cm = new IndexColorModel(
                    1, cmap.length,  cmap, 0, false, Transparency.BITMASK,
                    DataBuffer.TYPE_BYTE);
                BufferedImage image = new BufferedImage(
                    pageWidth, pageHeight, BufferedImage.TYPE_BYTE_BINARY, cm);
                Graphics g = image.createGraphics();
                document.paintPage(
                    i, g, GraphicsRenderingHints.PRINT, Page.BOUNDARY_CROPBOX,
                    rotation, scale);
                g.dispose();
                
                // capture the page image to file
                IIOImage img = new IIOImage(image, null, null);
                ImageWriteParam param = writer.getDefaultWriteParam();
                param.setCompressionMode(param.MODE_EXPLICIT);
                param.setCompressionType(COMPRESSION_TYPE_GROUP4FAX);
                if (i == 0) {
                    writer.write(null, img, param);
                }
                else {
                    writer.writeInsert(-1, img, param);
                }
                image.flush();
            }
            
            ios.flush();
            ios.close();
            writer.dispose();
        }
        catch(IOException e) {
            System.out.println("Error saving file  " + e);
            e.printStackTrace();
        }
        
        // clean up resources
        document.dispose();
    }
}
