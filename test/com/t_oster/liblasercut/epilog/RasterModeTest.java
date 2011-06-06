/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.liblasercut.epilog;

import com.t_oster.util.BufferedImageAdapter;
import com.kitfox.svg.app.beans.SVGIcon;
import java.net.URI;
import java.io.File;
import com.kitfox.svg.SVGUniverse;
import com.t_oster.liblasercut.BlackWhiteRaster;
import com.t_oster.liblasercut.IllegalJobException;
import com.t_oster.liblasercut.LaserJob;
import java.awt.Point;
import java.util.List;
import java.util.LinkedList;
import com.t_oster.liblasercut.Raster3dPart;
import com.t_oster.liblasercut.EngravingProperty;
import com.t_oster.liblasercut.RasterPart;
import java.awt.image.BufferedImage;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author oster
 */
public class RasterModeTest {

    public RasterModeTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testRaster3dRaw() throws IllegalJobException, Exception {
        EpilogCutter instance = new EpilogCutter("137.226.56.228");
        Raster3dPart testraster = new Raster3dPart(new EngravingProperty(100, 100)) {

            @Override
            public int getRasterCount() {
                return 1;
            }

            @Override
            public int getWidth(){
                return 200;
            }

            @Override
            public int getHeight(){
                return 200;
            }

            @Override
            public Point getRasterStart(int raster){
                return new Point(0,0);
            }

            @Override
            public int getRasterWidth(int raster) {
                return 200;
            }

            @Override
            public int getRasterHeight(int raster) {
                return 200;
            }

            @Override
            public List<Byte> getRasterLine(int raster, int y) {
                List<Byte> result = new LinkedList<Byte>();
                if (y < 100) {
                    for (int x = 0; x <= 200; x++) {
                        result.add((byte) x);
                    }
                } else {
                    for (int i = 0; i < 50; i++) {
                        result.add((byte) 0);
                    }
                    for (int i = 0; i < 50; i++) {
                        result.add((byte) 50);
                    }
                    for (int i = 0; i < 50; i++) {
                        result.add((byte) 100);
                    }
                    for (int i = 0; i < 50; i++) {
                        result.add((byte) -30);
                    }
                }
                return result;
            }
        };
        LaserJob job = new LaserJob("raster3draw", "123", "456", 500, testraster, null, null);
        instance.sendJob(job);
    }

    @Test
    public void testRaster3dImage() throws IllegalJobException, Exception{

        EpilogCutter instance = new EpilogCutter("137.226.56.228");
        Raster3dPart rp = new Raster3dPart(new EngravingProperty(100,100));

        SVGUniverse univ = new SVGUniverse();
        URI svg = univ.loadSVG(new File("test/files/TweetyMerged.svg").toURI().toURL());
        SVGIcon icon = new SVGIcon();
        icon.setSvgURI(svg);
        icon.setAntiAlias(false);
        icon.setClipToViewbox(false);
        icon.setScaleToFit(false);

        BufferedImage test = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_RGB);
        icon.paintIcon(null, test.getGraphics(), 0, 0);
        rp.addImage(new BufferedImageAdapter(test), new Point(0,0));

        LaserJob job = new LaserJob("raster3dImage", "666", "bla", 500, rp, null, null);
        instance.sendJob(job);
    }
    
    @Test
    public void testRasterImage() throws IllegalJobException, Exception{

        EpilogCutter instance = new EpilogCutter("137.226.56.228");
        RasterPart rp = new RasterPart(new EngravingProperty(100,100));

        SVGUniverse univ = new SVGUniverse();
        URI svg = univ.loadSVG(new File("test/files/TweetyMerged.svg").toURI().toURL());
        SVGIcon icon = new SVGIcon();
        icon.setSvgURI(svg);
        icon.setAntiAlias(false);
        icon.setClipToViewbox(false);
        icon.setScaleToFit(false);

        BufferedImage test = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_RGB);
        icon.paintIcon(null, test.getGraphics(), 0, 0);
        rp.addImage(new BlackWhiteRaster(
                     new BufferedImageAdapter(test)
                    , BlackWhiteRaster.DITHER_FLOYD_STEINBERG)
                , new Point(0,0));

        LaserJob job = new LaserJob("raster3dImage", "666", "bla", 500, null, null, rp);
        instance.sendJob(job);
    }
}
