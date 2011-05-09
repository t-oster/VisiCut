/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.liblasercut;

/**
 *
 * @author thommy
 */
public interface LaserJob {
    
    public String getTitle();
    
    public String getName();
    
    public String getUser();
    
    public void addLine(int x1, int y1, int x2, int y2);

    public boolean containsRaster();

    public boolean containsVector();
}
