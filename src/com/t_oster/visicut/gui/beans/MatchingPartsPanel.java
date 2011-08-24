package com.t_oster.visicut.gui.beans;

import java.awt.Color;
import java.awt.Graphics;
import javax.swing.JPanel;

/**
 * This Class implements a JPanel which is able
 * to display Parts of an SVG file matching
 * certain criteria
 * 
 * @author thommy
 */
public class MatchingPartsPanel extends JPanel {

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(Color.yellow);
        g.drawLine(0, 0, 100, 100);
    }
}
