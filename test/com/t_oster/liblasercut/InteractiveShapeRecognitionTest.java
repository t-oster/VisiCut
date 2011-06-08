/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.liblasercut;

import com.kitfox.svg.SVGUniverse;
import com.kitfox.svg.app.beans.SVGIcon;
import com.t_oster.liblasercut.BlackWhiteRaster.DitherAlgorithm;
import com.t_oster.util.BufferedImageAdapter;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Shape;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

/**
 *
 * @author thommy
 */
public class InteractiveShapeRecognitionTest {

    private final ProgressWindow pw = new ProgressWindow();

    public static void main(String[] args) throws MalformedURLException, IOException {
        new InteractiveShapeRecognitionTest().runInteractiveTest();
    }

    private void runInteractiveTest() throws MalformedURLException, IOException {
        DitherAlgorithm da = BlackWhiteRaster.DitherAlgorithm.AVERAGE;
        BufferedImage test;

        SVGUniverse univ = new SVGUniverse();
        URI svg = univ.loadSVG(new File("test/files/tux.svg").toURI().toURL());
        SVGIcon icon = new SVGIcon();
        icon.setSvgURI(svg);
        icon.setAntiAlias(false);
        icon.setClipToViewbox(false);
        icon.setScaleToFit(false);

        test = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics g = test.getGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, test.getWidth(), test.getHeight());
        icon.paintIcon(null, g, 0, 0);


        ProgressListener l = new ProgressListener() {

            public void progressChanged(Object source, int percent) {
                pw.setState(percent);
            }

            public void taskChanged(Object source, String taskName) {
                throw new UnsupportedOperationException("Not supported yet.");
            }
        };
        pw.setVisible(true);
        BlackWhiteRaster bwr = new BlackWhiteRaster(
                new BufferedImageAdapter(test), da, l);
        for (int x = 0; x < bwr.getWidth(); x++) {
            for (int y = 0; y < bwr.getHeight(); y++) {
                test.setRGB(x, y, (bwr.isBlack(x, y) ? Color.BLACK : Color.WHITE).getRGB());
            }
        }
        ShapeRecognizer sr = new ShapeRecognizer();
        Graphics2D gg = (Graphics2D) test.getGraphics();
        gg.setColor(Color.RED);
        for (Shape p : sr.getShapes(bwr)) {
            gg.draw(p);
        }
        pw.setVisible(false);
        JOptionPane.showConfirmDialog(null, new JLabel(new ImageIcon(test)), "Tada", JOptionPane.OK_OPTION);
    }
}
