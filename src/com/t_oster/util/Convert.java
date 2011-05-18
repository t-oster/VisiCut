/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.util;

/**
 *
 * @author oster
 */
public class Convert {
    public static double inch2mm(double inch){
        return inch*25.4;
    }
    public static double mm2inch(double mm){
        return mm/25.4;
    }
    public static double px2mm(double px, double dpi){
        return inch2mm(px/dpi);
    }
    public static double mm2px(double mm, double dpi){
        return mm2inch(mm)*dpi;
    }
}
