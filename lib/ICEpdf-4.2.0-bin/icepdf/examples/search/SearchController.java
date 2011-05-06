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
import org.icepdf.core.pobjects.graphics.text.WordText;
import org.icepdf.core.search.DocumentSearchController;
import org.icepdf.ri.common.SwingController;
import org.icepdf.ri.common.SwingViewBuilder;

import javax.swing.*;
import java.util.ArrayList;

/**
 * The <code>SearchHighlight</code> class is an example of how to use
 * <code>DocumentSearchController</code> to highlight search terms in a
 * Document view.  A file specified at the command line is
 * opened in a JFrame which contains the viewer component and any number
 * of search terms can be specefied after the file name.
 * <p/>
 * Example:
 *   SearchHighlight "c:\DevelopersGuide.pdf" "PDF" "ICEsoft" "ICEfaces" "ICEsoft technologies"
 *
 * @since 4.0
 */
public class SearchController {
    public static void main(String[] args) {

        if (args.length < 2) {
            System.out.println("At leasts two command line arguments must " +
                    "be specified. ");
            System.out.println("<filename> <term1> ... <termN>");
        }

        // Get a file from the command line to open
        String filePath = args[0];

        // get search terms from command line
        String[] terms = new String[args.length - 1];
        for (int i = 1, max = args.length; i < max; i++) {
            terms[i - 1] = args[i];
        }

        // build a component controller
        SwingController controller = new SwingController();

        SwingViewBuilder factory = new SwingViewBuilder(controller);

        JPanel viewerComponentPanel = factory.buildViewerPanel();

        JFrame applicationFrame = new JFrame();
        applicationFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        applicationFrame.getContentPane().add(viewerComponentPanel);

        // Now that the GUI is all in place, we can try opening the PDF
        controller.openDocument(filePath);

        // show the component
        applicationFrame.pack();
        applicationFrame.setVisible(true);

        /**
         * Start of a simple search for the loaded file
         */
        // get the search controller
        DocumentSearchController searchController =
                controller.getDocumentSearchController();
        // add a specified search terms.
        for (String term : terms) {
            searchController.addSearchTerm(term, false, false);
        }
        // search the pages in the document or a subset
        Document document = controller.getDocument();
        // list of founds words to print out
        ArrayList<WordText> foundWords;
        for (int pageIndex = 0; pageIndex < document.getNumberOfPages();
             pageIndex++) {
            foundWords = searchController.searchPage(pageIndex);
            System.out.println("Page " + pageIndex);
            if (foundWords != null){
                for (WordText wordText : foundWords){
                    System.out.println("    found hit: " + wordText.toString());
                }
            }
        }

    }
}
