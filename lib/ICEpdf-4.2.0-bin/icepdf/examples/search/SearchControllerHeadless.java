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

import org.icepdf.core.exceptions.PDFException;
import org.icepdf.core.exceptions.PDFSecurityException;
import org.icepdf.core.pobjects.Document;
import org.icepdf.core.pobjects.PDimension;
import org.icepdf.core.pobjects.Page;
import org.icepdf.core.search.DocumentSearchController;
import org.icepdf.core.util.GraphicsRenderingHints;
import org.icepdf.ri.common.search.DocumentSearchControllerImpl;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * The <code>SearchControllerHeadless</code> class is an example of how to
 * search a document and save page capture with search highlighting to disk.
 * A file specified at the command line is opened and every page in the document
 * is captured as an image and saved to disk as a PNG graphic file.
 *
 * @since 4.2
 */
public class SearchControllerHeadless {
    public static void main(String[] args) {

        // Get a file from the command line to open
        String filePath = args[0];

        // save page captures to file.
        float scale = 1.0f;
        float rotation = 0f;
        Object pageLock = new Object();

        // open the document
        Document document = new Document();
        try {
            document.setFile(filePath);
        } catch (PDFException e) {
            e.printStackTrace();
        } catch (PDFSecurityException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // get the search controller
        DocumentSearchController searchController =
                new DocumentSearchControllerImpl(document);
        // add a specified search terms.
        searchController.addSearchTerm("TABLE", true, false);
        searchController.addSearchTerm("CONTENTS", true, false);
        searchController.addSearchTerm("Chapter", true, false);

        // Paint each pages content to an image and write the image to file
        for (int i = 0; i < 5; i++) {

            Page page = document.getPageTree().getPage(i, pageLock);
            // initialize the page so we are using the same  WordText object
            // thar are used to paint the page.
            page.init();

            // search the page
            searchController.searchPage(i);

            // build the image for capture.
            PDimension sz = page.getSize(Page.BOUNDARY_CROPBOX, rotation, scale);
            int pageWidth = (int) sz.getWidth();
            int pageHeight = (int) sz.getHeight();
            BufferedImage image = new BufferedImage(pageWidth,
                    pageHeight,
                    BufferedImage.TYPE_INT_RGB);
            Graphics g = image.createGraphics();
            Graphics2D g2d = (Graphics2D) g;

            // capture current transform for graphics context.
            page.paint(g, GraphicsRenderingHints.SCREEN,
                    Page.BOUNDARY_CROPBOX, rotation, scale, null, true, true);
            g2d.dispose();
            document.getPageTree().releasePage(page, pageLock);

            // capture the page image to file
            try {
                File file = new File("imageCapture1_" + i + ".png");
                ImageIO.write(image, "png", file);

            } catch (IOException e) {
                e.printStackTrace();
            }
            image.flush();
        }
        // clean up resources
        document.dispose();
    }

}
