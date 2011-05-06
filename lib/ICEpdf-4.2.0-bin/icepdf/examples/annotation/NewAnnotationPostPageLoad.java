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

import org.icepdf.core.pobjects.Destination;
import org.icepdf.core.pobjects.Document;
import org.icepdf.core.pobjects.Reference;
import org.icepdf.core.pobjects.actions.ActionFactory;
import org.icepdf.core.pobjects.actions.GoToAction;
import org.icepdf.core.pobjects.actions.URIAction;
import org.icepdf.core.pobjects.annotations.*;
import org.icepdf.core.pobjects.graphics.text.WordText;
import org.icepdf.core.search.DocumentSearchController;
import org.icepdf.core.util.Library;
import org.icepdf.core.views.swing.AbstractPageViewComponent;
import org.icepdf.ri.common.SwingController;
import org.icepdf.ri.common.SwingViewBuilder;
import org.icepdf.ri.common.views.DocumentViewControllerImpl;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Vector;

/**
 * The <code>NewAnnotationPostPageLoad</code> class is an example of how to use
 * <code>DocumentSearchController</code> to find search terms in a
 * Document and convert the found words to annotations.
 * <p/>
 * A file specified at the command line is
 * opened in a JFrame which contains the viewer component and any number
 * of search terms can be specefied after the file name.
 * <p/>
 * Example:
 * SearchHighlight "c:\DevelopersGuide.pdf" "PDF" "ICEsoft" "ICEfaces" "ICEsoft technologies"
 * <p/>
 * The file that is opened in the Viewer RI will have the new annotations created
 * around the found search terms.  The example creates a URIActions for each
 * annotation but optionally can be compiled to build GotoActions to 'goto'
 * the last page of the document when executed.
 * <p/>
 * The annotation are created after the Document view is created so we
 * have to create new annotation slightly differently then if we where adding
 * them before teh view was created.
 *
 * @since 4.0
 */
public class NewAnnotationPostPageLoad {
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

        /**
         * Create a new instance so we can view the modified file.
         */
        SwingController controller = new SwingController();
        SwingViewBuilder factory = new SwingViewBuilder(controller);
        JPanel viewerComponentPanel = factory.buildViewerPanel();
        JFrame applicationFrame = new JFrame();
        applicationFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        applicationFrame.getContentPane().add(viewerComponentPanel);

        // add interactive mouse link annotation support via callback
        controller.getDocumentViewController().setAnnotationCallback(
                new org.icepdf.ri.common.MyAnnotationCallback(
                        controller.getDocumentViewController()));
        // set document view mode
        controller.getDocumentViewController().setViewType(
                DocumentViewControllerImpl.ONE_COLUMN_VIEW);

        // Now that the GUI is all in place, we can try opening the PDF
        controller.openDocument(filePath);

        /**
         * Start of a simple search for the loaded file and collect word
         * data for annotation creation.
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
        // set the max number of pages to search and create annotations for.
        int pageCount = 25;
        if (pageCount > document.getNumberOfPages()) {
            pageCount = document.getNumberOfPages();
        }

        // show the document and the new annotations.
        applicationFrame.pack();
        applicationFrame.setVisible(true);

        /**
         * Apply the search -> annotation resulst after the gui is build
         */
        // new annotation look and feel
        AnnotationState annotationState =
                new AnnotationState(Annotation.VISIBLE_RECTANGLE,
                        LinkAnnotation.HIGHLIGHT_INVERT, 1f,
                        BorderStyle.BORDER_STYLE_SOLID, Color.GRAY);

        // list of founds words to print out
        ArrayList<WordText> foundWords;
        java.util.List<AbstractPageViewComponent> pageComponents =
                controller.getDocumentViewController()
                        .getDocumentViewModel().getPageComponents();
        for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
            // get the search results for this page
            foundWords = searchController.searchPage(pageIndex);
            if (foundWords != null) {
                // get the current page lock and start adding the annotations
                AbstractPageViewComponent pageViewComponent =
                        pageComponents.get(pageIndex);
                for (WordText wordText : foundWords) {
                    // create a  new link annotation
                    LinkAnnotation linkAnnotation = (LinkAnnotation)
                            AnnotationFactory.buildAnnotation(
                                    document.getPageTree().getLibrary(),
                                    AnnotationFactory.LINK_ANNOTATION,
                                    wordText.getBounds().getBounds(),
                                    annotationState);
                    // create a new URI action
                    org.icepdf.core.pobjects.actions.Action action =
                            createURIAction(document.getPageTree().getLibrary(),
                                    "http://www.icepdf.org");
                    // or create a new goTo Annotation that links to the page
                    // number represented by pageCount.
//                    org.icepdf.core.pobjects.actions.Action action =
//                            createGoToAction(
//                                    document.getPageTree().getLibrary(),
//                                    document, document.getNumberOfPages() - 1);
                    // add the action to the annotation
                    linkAnnotation.addAction(action);
                    // add it to the pageComponent, not the page, as we won't
                    // see it until the page is re-initialized.
                    pageViewComponent.addAnnotation(linkAnnotation);
                }
            }
            // removed the search highlighting
            searchController.clearSearchHighlight(pageIndex);
        }

        // The save button can be used in the UI to save a copy of the
        // document.
    }

    /**
     * Utility for creation a URI action
     *
     * @param library document library reference
     * @param uri     uri that actin will launch
     * @return new URIAction object instance.
     */
    private static org.icepdf.core.pobjects.actions.Action createURIAction(
            Library library, String uri) {
        URIAction action = (URIAction)
                ActionFactory.buildAction(
                        library,
                        ActionFactory.URI_ACTION);
        action.setURI(uri);
        return action;
    }

    /**
     * Utility for creation a GoTo action
     *
     * @param library   document library reference
     * @param pageIndex page index to go to.
     * @return new GoToAction object instance.
     */
    private static org.icepdf.core.pobjects.actions.Action createGoToAction(
            Library library, Document document, int pageIndex) {
        GoToAction action = (GoToAction)
                ActionFactory.buildAction(
                        library,
                        ActionFactory.GOTO_ACTION);
        Reference pageReference = document.getPageTree()
                .getPageReference(pageIndex);
        Vector destVector = Destination.destinationSyntax(pageReference,
                Destination.TYPE_FIT);
        action.setDestination(new Destination(library, destVector));
        return action;
    }
}