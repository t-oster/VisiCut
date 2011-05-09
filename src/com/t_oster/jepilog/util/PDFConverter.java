/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.jepilog.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.icepdf.core.exceptions.PDFException;
import org.icepdf.core.exceptions.PDFSecurityException;
import org.icepdf.core.pobjects.Document;
import org.icepdf.ri.util.SVG;

/**
 *
 * @author thommy
 */
public class PDFConverter implements FileConverter{

    private Document doc;
    /**
     * Returns a String array of lower-case
     * file extensions which are supported
     * @return 
     */
    public String[] getSupportedFileExtensions() {
        return new String[]{"pdf"};
    }

    public void load(File image) throws IOException {
        try {
            doc = new Document();
            doc.setFile(image.getAbsolutePath());
        } catch (PDFException ex) {
            Logger.getLogger(PDFConverter.class.getName()).log(Level.SEVERE, null, ex);
            throw new IOException(ex.getMessage());
        } catch (PDFSecurityException ex) {
            Logger.getLogger(PDFConverter.class.getName()).log(Level.SEVERE, null, ex);
            throw new IOException(ex.getMessage());
        }
        
    }

    public void load(InputStream stream) throws IOException {
        try {
            doc = new Document();
            doc.setInputStream(stream, null);
        } catch (PDFException ex) {
            Logger.getLogger(PDFConverter.class.getName()).log(Level.SEVERE, null, ex);
            throw new IOException(ex.getMessage());
        } catch (PDFSecurityException ex) {
            Logger.getLogger(PDFConverter.class.getName()).log(Level.SEVERE, null, ex);
            throw new IOException(ex.getMessage());
        }
        
    }

    public void convert(File output) throws IOException {
        SVG.createSVG(doc, 0, new FileWriter(output));
    }

    public void convert(OutputStream stream) {
        SVG.createSVG(doc, 0, new OutputStreamWriter(stream));
    }
    
}
