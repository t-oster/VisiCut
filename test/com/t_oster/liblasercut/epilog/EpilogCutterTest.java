/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.liblasercut.epilog;
import com.kitfox.svg.SVGDiagram;
import com.kitfox.svg.SVGUniverse;
import com.t_oster.liblasercut.*;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.net.URI;
import org.junit.Test;

/**
 *
 * @author thommy
 */
public class EpilogCutterTest {
   

    /**
     * Test of sendJob method, of class EpilogCutter.
     */
    @Test
    public void testVectorJob() throws IllegalJobException, Exception {
        EpilogCutter.SIMULATE_COMMUNICATION = true;
        System.out.println("sendJob");
        
        EpilogCutter instance = new EpilogCutter("137.226.56.228");
        VectorPart vp = new VectorPart(new CuttingProperty(5000,20,100));
        vp.moveto(0, 0);
        vp.lineto(100, 0);
        vp.lineto(100,100);
        vp.lineto(0,100);
        vp.lineto(0,0);
        LaserJob job = new LaserJob("testpilog", "666", "bla", 500, null, vp);
        instance.sendJob(job);
    }
    
    @Test
    public void testRasterJob() throws IllegalJobException, Exception{
        EpilogCutter.SIMULATE_COMMUNICATION = false;
        System.out.println("sendJob");
        
        EpilogCutter instance = new EpilogCutter("137.226.56.228");
        RasterPart rp = new RasterPart(new EngravingProperty(50,50));
        
        SVGUniverse univ = new SVGUniverse();
        URI svg = univ.loadSVG(new File("test/files/butterfly.svg").toURI().toURL());
        SVGDiagram diag = univ.getDiagram(svg);
        BufferedImage test = new BufferedImage((int) diag.getWidth(), (int) diag.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
        diag.render(test.createGraphics());
        rp.addImage(test);
        
        LaserJob job = new LaserJob("testpilog", "666", "bla", 500, rp, null);
        instance.sendJob(job);
    }
    
    
}
