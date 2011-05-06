/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.jepilog.postscript;

import java.awt.Image;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import javax.imageio.ImageIO;
import net.sf.ghost4j.document.DocumentException;
import net.sf.ghost4j.renderer.RendererException;
import net.sf.ghost4j.renderer.SimpleRenderer;

/**
 *
 * @author thommy
 */
public class PostscriptInterpreter {
    
    /**
     * Creates a RasterFile out of an Input file
     * 
     * This Method uses ghostscript to rasterize
     * postscript or pdf to a bitmap format
     * @param f 
     */
    public Image rasterizeFile(InputFile f) throws IOException, RendererException, DocumentException{
        // create renderer
	RasterRenderer renderer = new RasterRenderer();
			
	// set resolution (in DPI)
	renderer.setResolution(600);

	// render
	List<Image> images = renderer.render(f.getDocument(), 1, 1);
        for (int i = 0; i < images.size(); i++) {
	                ImageIO.write((RenderedImage) images.get(i), "png", new File("/tmp/"+(i + 1) + ".png"));
	}
        return images.get(0);
    }
}
