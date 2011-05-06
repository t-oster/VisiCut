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
import org.icepdf.core.pobjects.PInfo;

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * The <code>PageMetaDataExtraction</code> class is an example of how to extract
 * meta-data from a PDF document.   A file specified at the command line is opened
 * and the document's information is displayed on the command line.
 *
 * @since 2.0
 */
public class PageMetaDataExtraction {
    public static void main(String[] args) {

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

        // data to collect from document information entry
        String title = "";
        String author = "";
        String subject = "";
        String keyWords = "";
        String creator = "";
        String producer = "";
        String creationDate = "";
        String modDate = "";

        // get document information values if available
        PInfo documentInfo = document.getInfo();
        if (documentInfo != null) {
            title = documentInfo.getTitle();
            author = documentInfo.getAuthor();
            subject = documentInfo.getSubject();
            keyWords = documentInfo.getKeywords();
            creator = documentInfo.getCreator() != null ?
                    documentInfo.getCreator() : "Not Available";
            producer = documentInfo.getProducer() != null ?
                    documentInfo.getProducer() : "Not Available";
            creationDate = documentInfo.getCreationDate() != null ?
                    documentInfo.getCreationDate().toString() : "Not Available";
            modDate = documentInfo.getModDate() != null ?
                    documentInfo.getModDate().toString() : "Not Available";
        }

        // Output the captured document information
        System.out.println("Title:    " + title);
        System.out.println("Subject:  " + subject);
        System.out.println("Author:   " + author);
        System.out.println("Keywords: " + keyWords);
        System.out.println("Creator:  " + creator);
        System.out.println("Producer: " + producer);
        System.out.println("Created:  " + creationDate);
        System.out.println("Modified: " + modDate);

        // clean up resources
        document.dispose();
    }
}
