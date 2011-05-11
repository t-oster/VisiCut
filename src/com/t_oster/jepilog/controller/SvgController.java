/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.jepilog.controller;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import org.apache.batik.dom.svg.SAXSVGDocumentFactory;
import org.apache.batik.util.XMLResourceDescriptor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 *
 * @author thommy
 */
public class SvgController {
    
    private Document doc;
    private Element svg;
    
    public void importSvg(File svgDocument) throws IOException{
            // Parse the barChart.svg file into a Document.
            String parser = XMLResourceDescriptor.getXMLParserClassName();
            SAXSVGDocumentFactory f = new SAXSVGDocumentFactory(parser);
            doc = f.createDocument(svgDocument.toURI().toURL().toString());
            svg = doc.getDocumentElement();
    }
    
    public Document getDocument(){
        return doc;
    }
}
