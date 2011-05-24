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
import java.util.LinkedList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author thommy
 */
public class EpilogCutterTest {
   

    @Test
    public void testEncode(){
        EpilogCutter instance = new EpilogCutter(null);
        List<Byte> in = new LinkedList<Byte>();
        for(int k=0;k<100;k++){
            in.add((byte) 7);
        }
        for(int k=0;k<50;k++){
            in.add((byte) k);
        }
        in=instance.encode(in);
        assertEquals((byte) -99, (byte) in.get(0));
        assertEquals((byte) 7, (byte) in.get(1));
        assertEquals((byte) 49, (byte) in.get(2));
        for (int k=0;k<50;k++){
            assertEquals((byte) k, (byte) in.get(3+k));
        }
        assertEquals((byte) -128, (byte) 0x80);
    }
    
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
        LaserJob job = new LaserJob("vector", "666", "bla", 500, null, vp);
        instance.sendJob(job);
    }
    
    @Test
    public void testRasterJob() throws IllegalJobException, Exception{
        EpilogCutter.SIMULATE_COMMUNICATION = true;
        System.out.println("sendJob");
        
        EpilogCutter instance = new EpilogCutter("137.226.56.228");
        RasterPart rp = new RasterPart(new EngravingProperty(50,50));
        
        SVGUniverse univ = new SVGUniverse();
        //URI svg = univ.loadSVG(new File("test/files/butterfly.svg").toURI().toURL());
        //SVGDiagram diag = univ.getDiagram(svg);
        //BufferedImage test = new BufferedImage((int) diag.getWidth(), (int) diag.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
        //diag.render(test.createGraphics());
        //rp.addImage(test);
        VectorPart vp = new VectorPart(new CuttingProperty(5000,20,100));
        vp.moveto(0, 0);
        vp.lineto(200, 0);
        vp.lineto(200,200);
        vp.lineto(0,200);
        vp.lineto(0,0);
        LaserJob job = new LaserJob("raster", "666", "bla", 500, rp, vp);
        instance.sendJob(job);
    }
    
    
}
