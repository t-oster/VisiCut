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
package org.icepdf.examples.jsf.viewer.servlet;

import org.icepdf.examples.jsf.viewer.view.BeanNames;
import org.icepdf.examples.jsf.viewer.view.DocumentManager;

import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * PDF Rendering servlet responsible for rendering the current document state
 * image.  The document state keeps track of the current page, zoom and rotation
 * informaiton.  
 *
 * @since 3.0
 */
public class PdfRenderer extends HttpServlet {

    private static final Logger logger =
            Logger.getLogger(PdfRenderer.class.toString());

    /**
     * @param request  incoming request
     * @param response outgoing response
     * @throws javax.servlet.ServletException
     * @throws java.io.IOException
     */
    public void doGet(HttpServletRequest request,
                                   HttpServletResponse response)
            throws ServletException, IOException {
        BufferedImage bi = null;
        try {
            // get the document manager from the session map.
            DocumentManager documentManager = (DocumentManager)
                    request.getSession().getAttribute(BeanNames.DOCUMENT_MANAGER);

            if (documentManager != null) {
                // get the page image a write it out to the response stream
                bi = (BufferedImage)
                        documentManager.getCurrentPageImage();
                if (bi != null) {
                    response.setContentType("image/png");
                    OutputStream os1 = response.getOutputStream();
                    ImageIO.write(bi, "png", os1);
                    os1.close();
                    bi.flush();
                }
            }
        } catch (Throwable e) {
            logger.log(Level.FINE, "Error writing image stream.", e);
            if (bi != null){
                bi.flush();
            }
        }
    }

}

