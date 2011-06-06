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
public class BufferedImageAdapter implements GreyscaleRaster{

    private BufferedImage img; 
    
    public BufferedImageAdapter(BufferedImage img)
    {
        this.img = img;
    }
    
    public Byte getGreyScale(int x, int line) {
        Color c = new Color(img.getRGB(x, line));
        return ((byte) ((0.3*c.getRed()+0.59*c.getGreen()+0.11*c.getBlue())/3));
    }
    
    public void setGreyScale(int x, int y, Byte grey)
    {
        Color c = new Color(grey, grey, grey);
        img.setRGB(x, y, c.getRGB());
    }

    public int getWidth() {
        return img.getWidth();
    }

    public int getHeight() {
        return img.getHeight();
    }
    
}
