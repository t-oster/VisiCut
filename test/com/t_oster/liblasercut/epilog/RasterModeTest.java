/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.liblasercut.epilog;

import com.t_oster.util.GreyScaleAdapter;
import com.t_oster.liblasercut.VectorPart;
import java.awt.Graphics2D;
import com.kitfox.svg.app.beans.SVGIcon;
import com.t_oster.liblasercut.CuttingProperty;
import java.awt.image.BufferedImage;
import com.kitfox.svg.SVGDiagram;
import java.net.URI;
import java.io.File;
import com.kitfox.svg.SVGUniverse;
import com.t_oster.liblasercut.IllegalJobException;
import com.t_oster.liblasercut.LaserJob;
import java.awt.Point;
import java.util.List;
import java.util.LinkedList;
import com.t_oster.liblasercut.RasterPart;
import com.t_oster.liblasercut.EngravingProperty;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

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
    public void testRasterRaw() throws IllegalJobException, Exception {
        EpilogCutter instance = new EpilogCutter("137.226.56.228");
        RasterPart testraster = new RasterPart(new EngravingProperty(100, 100)) {

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
        LaserJob job = new LaserJob("rastertest", "123", "456", 500, testraster, null);
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

        GreyScaleAdapter test = new GreyScaleAdapter(icon.getIconWidth(), icon.getIconHeight());
        icon.paintIcon(null, test.getGraphics(), 0, 0);
        rp.addImage(test, new Point(0,0));

        LaserJob job = new LaserJob("image", "666", "bla", 500, rp, null);
        instance.sendJob(job);
    }
}
