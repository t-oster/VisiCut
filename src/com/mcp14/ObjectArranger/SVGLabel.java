/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mcp14.ObjectArranger;

import javax.swing.Icon;
import javax.swing.JLabel;

/**
 *
 * @author Marcel
 */
    // A Label Class that renders the SVGFiles
    // used to update the SVGLocation after dragging it around
public class SVGLabel extends JLabel{
    public SVGFile svgFile;
    SVGLabel(Icon icon, SVGFile file){
        super(icon);
        svgFile = file;
    }
}
