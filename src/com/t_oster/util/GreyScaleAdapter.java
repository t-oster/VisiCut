/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.util;

import com.t_oster.liblasercut.GreyscaleRaster;
import java.awt.Color;
import java.awt.image.BufferedImage;

/**
 *
 * @author thommy
 */
public class GreyScaleAdapter extends BufferedImage implements GreyscaleRaster{

    public GreyScaleAdapter(int width, int height)
    {
        super(width, height, BufferedImage.TYPE_INT_RGB);
    }
    
    public Byte getGreyScale(int x, int line) {
        Color c = new Color(this.getRGB(x, line));
        return ((byte) ((0.3*c.getRed()+0.59*c.getGreen()+0.11*c.getBlue())/3));
    }
    
    public void setGreyScale(int x, int y, Byte grey)
    {
        Color c = new Color(grey, grey, grey);
        this.setRGB(x, y, c.getRGB());
    }
    
}
