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
 * The Original Code is ICEfaces 1.5 open source software code, released
 * November 5, 2006. The Initial Developer of the Original Code is ICEsoft
 * Technologies Canada, Corp. Portions created by ICEsoft are Copyright (C)
 * 2004-2006 ICEsoft Technologies Canada, Corp. All Rights Reserved.
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
package org.icepdf.examples.jsf.viewer.view;

import com.icesoft.faces.component.inputfile.FileInfo;
import com.icesoft.faces.component.inputfile.InputFile;
import com.icesoft.faces.context.DisposableBean;
import org.icepdf.core.pobjects.fonts.FontFactory;
import org.icepdf.examples.jsf.viewer.util.FacesUtils;

import javax.faces.event.ActionEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.ValueChangeEvent;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.awt.*;

/**
 * DocumentManager is a session scpoed bean responsible for managing the
 * state of PDF documents.  When a new document is loaded it is added to the
 * document history list.  The document state is stored for all documents
 * opened durign the user sesssion but only one document is actually kept
 * open in memory.
 *
 * @since 3.0
 */
public class DocumentManager implements DisposableBean {

    private static final Logger logger =
            Logger.getLogger(DocumentManager.class.toString());

    // state of current document, outline, annotations, page cursor, zoom, path
    // and image export type.
    private DocumentState currentDocumentState;

    // list of demo files if present
    private static final String DEMO_DIRECTORY = "/WEB-INF/demos/";
    private static ArrayList<DocumentState> demoFilePaths;
    private int fileUploadProgress;
    private boolean uploadDialogVisibility;

    // enable/disable font engine state.
    private boolean isFontEngine = true;
    private static boolean isDemo;

    // list of document history, we only keep one document open at at time
    // but we can keep a list of previous document states encase the document
    // is opened again.
    private ArrayList<DocumentState> documentStateHistory =
            new ArrayList<DocumentState>(10);

    // document cache, intended to lower memory consumption for files that
    // are open more then one session such as the demo files are.  Other
    // files are opened in a users session, so it best to clean when we can.
    private DocumentCache documentDemoCache;

    /**
     * Opens the PDF document specified by the request param "documentPath".
     *
     * @param event jsf action event.
     */
    public void openDocument(ActionEvent event) {
        try {
            // opens a document from based on the path information passed
            // in as a request parameter.  Called from either the document history
            // or demo folder list.
            String documentPath = FacesUtils.getRequestParameter("documentPath");
            documentPath = URLDecoder.decode(documentPath);

            // try to load the document
            loadFilePath(documentPath);

            // refresh current page state.
            refreshDocumentState();
        } catch (Throwable e) {
            logger.log(Level.WARNING, "Error opening document.", e);
        }
    }

    /**
     * Loads the specified document
     *
     * @param demoFileName name of demo file to load
     */
    public void setDocumentDemoFile(String demoFileName) {
        if (demoFilePaths == null) {
            try {
                loadDemoFiles();
            } catch (Throwable e) {
                logger.log(Level.WARNING, "Error loading demo files.", e);
            }
        }

        // check for demo file in path as an id, we reuse the document state
        for (DocumentState documentSate : demoFilePaths) {
            if (documentSate.getDocumentName().equals(demoFileName)) {
                currentDocumentState = documentSate;
            }
        }

        // see if we can open the document.
        try {
            currentDocumentState.openDocument(documentDemoCache);
        } catch (Throwable e) {
            logger.log(Level.WARNING, "Error loading file default file: ", e);
        }
    }

    /**
     * Gets the upload progress bar perscent complete.  Should only be
     * called be the progress bar component.
     *
     * @param event inputFile eventObject.
     */
    public void fileUploadProgress(EventObject event) {
        InputFile ifile = (InputFile) event.getSource();
        if (ifile != null) {
            fileUploadProgress = ifile.getFileInfo().getPercent();
        }
    }

    /**
     * File upload event, we only listen for document that have been saved and
     * are of type .pdf.
     *
     * @param event jsf action event.
     */
    public void fileUploadMonitor(ActionEvent event) {
        try {
            InputFile inputFile = (InputFile) event.getSource();
            FileInfo fileInfo = inputFile.getFileInfo();
            if (fileInfo.getStatus() == FileInfo.SAVED) {
                logger.info("File UPload Path " + fileInfo.getFileName());
                if (fileInfo.getFileName().toLowerCase().endsWith(".pdf")) {
                    loadFilePath(fileInfo.getPhysicalPath());
                    // refresh current page state.
                    refreshDocumentState();
                    FacesUtils.addInfoMessage("Successfully upload PDF Document.");
                }
            }
        } catch (Throwable e) {
            FacesUtils.addInfoMessage("Error during upload of PDF Document.");
            logger.log(Level.WARNING,
                    "Error opening PDF document that was uploaded."
                            + e.getMessage(), e);
        }
    }

    /**
     * Stats the upload process by showing an upload dialog.  Users can either
     * chose to open a document or close the dialog.
     *
     * @param event jsf action event.
     */
    public void uploadDocument(ActionEvent event) {

        // reset file progress.
        fileUploadProgress = 0;

        // show upload dialog
        uploadDialogVisibility = true;
    }

    /**
     * Utility method which loads the PDF document specified by the document
     * path.
     *
     * @param documentPath path to PDF document.
     */
    private void loadFilePath(String documentPath) {
        // check the state history to see if this session has opened the document
        // in question before.  If so re-use document state.
        DocumentState documentState = null;
        for (DocumentState documentHistoryState : documentStateHistory) {
            if (documentPath.equals(documentHistoryState.getDocumentPath())) {
                documentState = documentHistoryState;
                break;
            }
        }
        // setup of current references so that the servlet can show the state
        // in question.
        if (documentState == null) {
            documentState = new DocumentState(documentPath);
            documentStateHistory.add(0, documentState);
        } else {
            // if the document changes then we'll close the previous one.
            // but only if it is not shared session.
            if (currentDocumentState != null &&
                    !currentDocumentState.isSharedSession() &&
                    !documentState.getDocumentName().equals(
                            currentDocumentState.getDocumentName()) ) {
                currentDocumentState.closeDocument();
            }
            // update history queue
            documentStateHistory.remove(documentState);
            documentStateHistory.add(0, documentState);
        }
        // see if we can open the document.
        try {
            documentState.openDocument(documentDemoCache);
            // assign the newly open document state.
            currentDocumentState = documentState;

        } catch (Throwable e) {
            logger.log(Level.WARNING, "Error loading file at path: " + documentPath, e);
            System.out.println("Error Loading file " + e.getMessage());
            FacesUtils.addInfoMessage("Could not open the PDF file." +
                    documentState.getDocumentName());
            // clean up and reset the viewer state. 
            if (!documentState.isSharedSession()){
                documentState.closeDocument();
            }
        }
    }

    /**
     * Updates the current document state page cursor to point to the next
     * logical page in the document.  Nothing happens if there is now documents
     * loaded or if the page cursor is at the end of the document.
     *
     * @param event jsf action event.
     */
    public void nextPage(ActionEvent event) {
        try {
            // if their is is a currentDocument then go to the next page.
            if (currentDocumentState != null) {
                int totalPages = currentDocumentState.getDocumentLength();
                int currentPage = currentDocumentState.getPageCursor();
                currentPage++;
                if (currentPage > totalPages) {
                    currentPage = totalPages;
                }
                currentDocumentState.setPageCursor(currentPage);

                // refresh current page state.
                refreshDocumentState();
            }
        } catch (Throwable e) {
            logger.log(Level.WARNING, "Error paging document.", e);
        }
    }

    /**
     * Updates the current document state page cursor to point to the previous
     * logical page in the document.  Nothing happens if there is now documents
     * loaded or if the page cursor is at the begining  of the document.
     *
     * @param event jsf action event.
     */
    public void previousPage(ActionEvent event) {
        try {
            // if their is is a currentDocument then go to the next page.
            if (currentDocumentState != null) {
                int currentPage = currentDocumentState.getPageCursor();
                currentPage--;
                if (currentPage < 1) {
                    currentPage = 1;
                }
                currentDocumentState.setPageCursor(currentPage);

                // refresh current page state.
                refreshDocumentState();
            }
        } catch (Throwable e) {
            logger.log(Level.WARNING, "Error paging document.", e);
        }
    }

    /**
     * Rotate the current document state by 90 degrees.
     *
     * @param event jsf action event.
     */
    public void rotateDocumentRight(ActionEvent event) {
        try {
            if (currentDocumentState != null) {
                float viewRotation = currentDocumentState.getRotation();
                viewRotation -= DocumentState.ROTATION_FACTOR;
                if (viewRotation < 0) {
                    viewRotation += 360;
                }
                currentDocumentState.setRotation(viewRotation);

                // refresh current page state.
                refreshDocumentState();
            }
        } catch (Throwable e) {
            logger.log(Level.WARNING,"Error rotating document.", e);
        }
    }

    /**
     * Rotate the current document state by -90 degrees.
     *
     * @param event jsf action event.
     */
    public void rotateDocumentLeft(ActionEvent event) {
        try {
            if (currentDocumentState != null) {
                float viewRotation = currentDocumentState.getRotation();
                viewRotation += DocumentState.ROTATION_FACTOR;
                viewRotation %= 360;
                currentDocumentState.setRotation(viewRotation);

                // refresh current page state.
                refreshDocumentState();
            }
        } catch (Throwable e) {
            logger.log(Level.WARNING, "Error rotating document.", e);
        }
    }

    /**
     * Gets a list of demo files located in the demo folder.
     *
     * @return list of PDF document paths in demo folder.
     */
    public ArrayList<DocumentState> getDemoFilePaths() {
        if (demoFilePaths == null) {
            try {
                loadDemoFiles();
            } catch (Throwable e) {
                logger.log(Level.WARNING, "Error loading demo files.", e);
            }
        }
        return demoFilePaths;
    }

    /**
     * Go to the page number specified by the request param "pageNumber".
     *
     * @param event JSF action event.
     */
    public void goToDestination(ActionEvent event) {
        try {
            int pageNumber = Integer.parseInt(
                    FacesUtils.getRequestParameter("pageNumber"));
            currentDocumentState.setPageCursor(pageNumber + 1);

            // refresh current page state.
            refreshDocumentState();
        } catch (Throwable e) {
            logger.log(Level.WARNING, "Error goign to specified page number.");
        }
    }

    /**
     * Go to the page number specifed by the current document state.  If the
     * page number is not in the range of the documents pages it is altered to
     * the nearest bound.
     *
     * @param event jsf action event.
     */
    public void goToPage(ActionEvent event) {
        try {
            if (currentDocumentState != null) {

                int totalPages = currentDocumentState.getDocumentLength();
                int currentPage = currentDocumentState.getPageCursor();

                if (currentPage > totalPages) {
                    currentDocumentState.setPageCursor(totalPages);
                }
                if (currentPage < 1) {
                    currentDocumentState.setPageCursor(1);
                }
                // refresh current page state.
                refreshDocumentState();
            }
        } catch (Throwable e) {
            logger.log(Level.WARNING, "Error paging document.", e);
        }
    }

     /**
     * Gets the image associated with the current document state.
     *
     * @return image represented by the pageCursor, rotation and zoom.
     */
    public Image getCurrentPageImage() {
        if (currentDocumentState != null) {
            FontFactory.getInstance().setAwtFontSubstitution(!isFontEngine);
            // invalidate the content streams, so we are paint with as close
            // to as possible the correct awt font state.
            if(isDemo){
                currentDocumentState.invalidate();
            }
            return currentDocumentState.getPageImage();
        }
        return null;
    }


    /**
     * Toggle the font engine functionality and refresht he current page view
     *
     * @param event jsf action event.
     */
    public void toggleFontEngine(ActionEvent event) {
        try {
            if (currentDocumentState != null) {

                // toggle flag.
                isFontEngine = !isFontEngine;

                // refresh current page state.
                refreshDocumentState();
            }
        } catch (Throwable e) {
            logger.log(Level.WARNING, "Error enable/disabling document.", e);
        }
    }

    /**
     * Read all PDF document that can be found in the Servlet context path +
     * DEMO_DIRECTORY .
     *
     * @throws MalformedURLException error getting file path.
     * @throws URISyntaxException    error getting file path.
     */
    public static void loadDemoFiles() throws MalformedURLException, URISyntaxException {

        // Loading of the resource must be done the "JSF way" so that
        // it is agnostic about it's environment (portlet vs servlet).
        HttpSession session = FacesUtils.getHttpSession(false);
        ServletContext context = session.getServletContext();
        String demoFilesPath = context.getRealPath(DEMO_DIRECTORY);

        // get listing of pdf files that might be in this folder.
        File directory = new File(demoFilesPath);
        String[] fontPaths;
        demoFilePaths = new ArrayList<DocumentState>(5);
        if (directory.canRead()) {
            fontPaths = directory.list();
            for (String pdfFile : fontPaths) {
                if (pdfFile != null &&
                        pdfFile.endsWith(".pdf")) {
                    demoFilePaths.add(new DocumentState(
                            directory.getAbsolutePath() + File.separatorChar +
                                    pdfFile, true));
                }
            }
        }
    }

    private void refreshDocumentState() {
        if (currentDocumentState != null) {
            // setup page size
            currentDocumentState.calculatePageImageSize();
            // refresh current image.
            currentDocumentState.generateDocumentID();
        }
    }

    public int getFileUploadProgress() {
        return fileUploadProgress;
    }

    public void setFileUploadProgress(int fileUploadProgress) {
        this.fileUploadProgress = fileUploadProgress;
    }

    public void documentZoomLevelChange(ValueChangeEvent event) {
        if (event.getPhaseId() != PhaseId.INVOKE_APPLICATION) {
            event.setPhaseId(PhaseId.INVOKE_APPLICATION);
            event.queue();
        } else {
            // refresh current page state.
            refreshDocumentState();
        }
    }

    public void dispose() throws Exception {
        if (currentDocumentState != null &&
                !currentDocumentState.isSharedSession()){
            currentDocumentState.closeDocument();
        }
    }

    public ArrayList<DocumentState> getDocumentStateHistory() {
        return documentStateHistory;
    }

    public DocumentState getCurrentDocumentState() {
        return currentDocumentState;
    }

    public void toggleUploadDialogVisibility(ActionEvent event) {
        uploadDialogVisibility = !uploadDialogVisibility;
    }

    public boolean isUploadDialogVisibility() {
        return uploadDialogVisibility;
    }

    public void setUploadDialogVisibility(boolean uploadDialogVisibility) {
        this.uploadDialogVisibility = uploadDialogVisibility;
    }

    public boolean isFontEngine() {
        return isFontEngine;
    }

    public void setFontEngine(boolean fontEngine) {
        isFontEngine = fontEngine;
    }

    public boolean isDemo() {
        return isDemo;
    }

    public void setDemo(boolean demo) {
        isDemo = demo;
    }

    public void setDocumentDemoCache(DocumentCache documentDemoCache) {
        this.documentDemoCache = documentDemoCache;
    }
}
