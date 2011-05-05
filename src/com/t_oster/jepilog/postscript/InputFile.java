/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.jepilog.postscript;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.ghost4j.converter.ConverterException;
import net.sf.ghost4j.converter.PDFConverter;
import net.sf.ghost4j.document.Document;
import net.sf.ghost4j.document.DocumentException;
import net.sf.ghost4j.document.PDFDocument;
import net.sf.ghost4j.document.PSDocument;

/**
 * This class represents a File which
 * Jepilog can use. This will be a
 * Postscript, PDF or maybe SVG file.
 * @author thommy
 */
public class InputFile {

    private File sourcefile;
    private Document doc;

    public static InputFile importFile(File file) throws IOException {
        //TODO: Check if file is in correct format etc
        if (file.getName().endsWith("pdf")) {
            PDFDocument pdf = new PDFDocument();
            pdf.load(file);
            return new InputFile(file, pdf);
        } else if (file.getName().endsWith("ps")) {
            //load the Postscript file
            final PSDocument ps = new PSDocument();
            ps.load(file);
            return new InputFile(file,ps);
        }
        else{
            throw new IllegalArgumentException("File is not PDF or PS file");
        }

    }

    private InputFile(File sourcefile, Document doc) {
        this.sourcefile = sourcefile;
        this.doc = doc;
    }

    public Document getDocument(){
        return doc;
    }
}
