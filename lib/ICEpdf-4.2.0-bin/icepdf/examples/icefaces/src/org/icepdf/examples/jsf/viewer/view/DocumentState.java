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

import com.icesoft.faces.context.Resource;
import org.icepdf.core.exceptions.PDFException;
import org.icepdf.core.exceptions.PDFSecurityException;
import org.icepdf.core.pobjects.Document;
import org.icepdf.core.pobjects.Outlines;
import org.icepdf.core.pobjects.PDimension;
import org.icepdf.core.pobjects.Page;
import org.icepdf.core.util.GraphicsRenderingHints;
import org.icepdf.examples.jsf.viewer.util.FacesUtils;

import javax.faces.model.SelectItem;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.io.*;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * DocumentState stores the path, file name, zoom, rotation and page cursor for
 * a given PDF document.  The document state can close and open a document to
 * save server memory without clearig the document state parameters.
 *
 * @since 3.0
 */
public class DocumentState {

    private static final Logger logger =
            Logger.getLogger(DocumentState.class.toString());

    // ICEpdf document class
    private Document document;

    private final Object documentLock = new Object();

    // document outline if present
    private DefaultTreeModel outline;

    // document state parameters.
    private String documentName;
    private String documentPath;
    private float zoom = 1.0f;
    private float rotation = 0f;
    private int pageCursor = 1;
    private int maxPages;
    private PDimension pageSize;
    private boolean isStateChanged;

    // outline default expanded state
    private boolean outlineExpanded;

    // PDF resource for downloading uploaded file.
    private PDFResource pdfResource;

    // Document id is used to identify the image returned by the PDF rendering
    // servlet.  The id is updated when the document state changes which
    // forces the Browser to re-request the page image from the servlet.
    private String documentId;

    // is demo file flag, shared cached copy
    private boolean sharedSession;

    // default rotation factor increment.
    public static final float ROTATION_FACTOR = 90f;

    // list of zoom levels
    public static List<SelectItem> zoomLevels;

    static {
        zoomLevels = new ArrayList<SelectItem>();
        zoomLevels.add(new SelectItem(0.05f, "5%"));
        zoomLevels.add(new SelectItem(0.1f, "10%"));
        zoomLevels.add(new SelectItem(0.25f, "25%"));
        zoomLevels.add(new SelectItem(0.50f, "50%"));
        zoomLevels.add(new SelectItem(0.75f, "75%"));
        zoomLevels.add(new SelectItem(1.0f, "100%"));
        zoomLevels.add(new SelectItem(1.25f, "125%"));
        zoomLevels.add(new SelectItem(1.5f, "150%"));
        zoomLevels.add(new SelectItem(2.0f, "200%"));
        zoomLevels.add(new SelectItem(3.0f, "300%"));
    }

    public DocumentState(String documentPath ) {
        this(documentPath, false);
    }

    /**
     * Createa  new document state based on the given document path.
     *
     * @param documentPath path to PDF document.
     */
    public DocumentState(String documentPath, boolean sharedSession ) {
        documentName = documentPath.substring(documentPath.lastIndexOf(File.separatorChar) + 1);
        this.documentPath = documentPath;
        isStateChanged = true;
        this.sharedSession = sharedSession;
        // hock for file outputResource component/file download.
        pdfResource = new PDFResource();
    }

    /**
     * Open the PDF document wrapped by this object. If their is already a document
     * assigned to this document it is closed before the current documentPath
     * is loaded.
     */
    public void openDocument(DocumentCache test) throws PDFException, IOException, PDFSecurityException {

        // get reference to applciatoin scoped document cache.
        DocumentCache documentCache = (DocumentCache)
                FacesUtils.getManagedBean("documentCache");

        synchronized (documentLock) {

            if (document == null ) {

                if (sharedSession){
                    Document documentReference = documentCache.get(documentPath);
                    if (documentReference != null){
                        document = documentReference;
                    }else{
                        document = new Document();
                        document.setFile(documentPath);
                        documentCache.put(documentPath, document);
                    }
                }else{
                    document = new Document();
                    document.setFile(documentPath);
                }

            }

            // document length.
            maxPages = document.getPageTree().getNumberOfPages();
            // page size
            calculatePageImageSize();

            // build swing outlines.
            Outlines outlines = document.getCatalog().getOutlines();
            if (outlines != null && outlines.getRootOutlineItem() != null) {
                // root tree node
                OutlineItemTreeNode rootItem = new OutlineItemTreeNode(document.getPageTree(),
                        outlines.getRootOutlineItem());

                // expand root node
                ((OutlineItemTreeNode.NodeUserObject)
                        rootItem.getUserObject()).setExpanded(true);

                outline = new DefaultTreeModel(rootItem);
                // expand document outline.
                outlineExpanded = true;
            } else {
                outlineExpanded = false;
            }
        }
    }

    /**
     * Disposed of the ICEpdf document object freeing up server resources.
     */
    public void closeDocument() {
        synchronized (documentLock) {
            try {
                if (document != null) {
                    document.dispose();
                }
                document = null;
                outline = null;
                maxPages = -1;
                isStateChanged = true;
            } catch (Throwable e) {
                logger.log(Level.FINE, "Could not close document.", e);
            }
        }
    }

    /**
     * Gets the total number of pages in the document.
     *
     * @return number of pages in document, -1 if the number of pages could
     *         not be determined.
     */
    public int getDocumentLength() {
        return maxPages;
    }

    /**
     * Gets the image associated with the current document state.
     *
     * @return image represented by the pageCursor, rotation and zoom.
     */
    protected Image getPageImage() {
        synchronized (documentLock) {
            if (document != null) {

                if (logger.isLoggable(Level.FINE)){
                    logger.fine("Capturing " + documentName + " " + pageCursor );
                }

                // check page bounds just encase.
                if (pageCursor < 1) {
                    pageCursor = 1;
                } else if (pageCursor > document.getPageTree().getNumberOfPages()) {
                    pageCursor = document.getPageTree().getNumberOfPages();
                }

                return document.getPageImage(pageCursor - 1, GraphicsRenderingHints.SCREEN,
                        Page.BOUNDARY_CROPBOX, rotation, zoom);
            }
            return null;
        }
    }

    /**
     * Gets the page size associated with the current document state.
     *
     * @return page sized specified by the attributes pageCursor, rotation and zoom.
     */
    public void calculatePageImageSize() {
        synchronized (documentLock) {
            if (document != null && document.getCatalog() != null) {
                pageSize = document.getPageDimension(pageCursor - 1, rotation, zoom);
            } else {
                pageSize = new PDimension(1f, 1f);
            }
        }
    }

    public int getPageWidth() {
        return (int) pageSize.getWidth();
    }

    public int getPageHieght() {
        return (int) pageSize.getHeight();
    }

    /**
     * Invalidates the current page content stream so that
     */
    public void invalidate() {
        synchronized (documentLock) {
            if (document != null) {
                Page page;
                // quickly invalidate content streams so we can swap font
                // implementations.
                for (int i = 0, max = getDocumentLength(); i < max; i++) {
                    page = document.getPageTree().getPage(i, this);
                    if (page.isInitiated()) {
                        page.getLibrary().disposeFontResources();
                        page.reduceMemory();
                    }
                    document.getPageTree().releasePage(page, this);
                }
                // mark state as dirty
                isStateChanged = true;
            }
        }
    }

    public float getZoom() {
        return zoom;
    }

    public void setZoom(float zoom) {
        if (this.zoom != zoom) {
            isStateChanged = true;
        }
        this.zoom = zoom;
    }

    public float getRotation() {
        return rotation;
    }

    public void setRotation(float rotation) {
        if (rotation != this.rotation) {
            isStateChanged = true;
        }
        this.rotation = rotation;
    }

    public int getPageCursor() {
        return pageCursor;
    }

    public void setPageCursor(int pageCursor) {
        if (pageCursor != this.pageCursor) {
            isStateChanged = true;
        }
        this.pageCursor = pageCursor;
    }

    public List<SelectItem> getZoomLevels() {
        return zoomLevels;
    }

    public static float getRotationFactor() {
        return ROTATION_FACTOR;
    }

    public String getDocumentName() {
        return documentName;
    }

    public String getDocumentPath() {
        return documentPath;
    }

    public String getDocumentEncodedPath() {
        return URLEncoder.encode(documentPath);
    }

    public String getDocumentId() {
        return documentId;
    }

    public void generateDocumentID() {
        if (isStateChanged) {
            isStateChanged = false;
            documentId = documentName + System.currentTimeMillis();
        }
    }

    public DefaultTreeModel getOutline() {
        return outline;
    }

    public void setStateChanged(boolean stateChanged) {
        isStateChanged = stateChanged;
    }

    public boolean isOutlineExpanded() {
        return outlineExpanded;
    }

    public void setOutlineExpanded(boolean outlineExpanded) {
        this.outlineExpanded = outlineExpanded;
    }

    public PDFResource getPdfResource() {
        return pdfResource;
    }

    public boolean isSharedSession() {
        return sharedSession;
    }

    public void setSharedSession(boolean sharedSession) {
        this.sharedSession = sharedSession;
    }

    /**
     * Simple PDF resource to allow easy download of uploaded files.
     */
    public class PDFResource implements Resource, Serializable {

        private final Date lastModified;

        public PDFResource() {
            this.lastModified = new Date();
        }

        public InputStream open() throws IOException {
            return new FileInputStream(documentPath);
        }

        public String calculateDigest() {
            return documentName;
        }

        public Date lastModified() {
            return lastModified;
        }

        public void withOptions(Options arg0) throws IOException {
        }
    }
}
