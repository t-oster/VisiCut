/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.liblasercut;

/**
 *
 * @author thommy
 */
public class LaserJob {
    
    private String title;
    private String name;
    private String user;
    private int resolution;
    
    public LaserJob(String title, String name, String user, int resolution){
        this.title = title;
        this.name = name;
        this.user = user;
        this.resolution = resolution;
    }
    
    public String getTitle(){
        return title;
    }
    
    public String getName(){
        return name;
    }
    
    public String getUser(){
        return user;
    }

    public boolean containsRaster(){
        return false;
    }

    public boolean containsVector(){
        return true;
    }

    public int getResolution(){
        return resolution;
    }
    
    public VectorPart getVectorPart(){
        return null;
    }
    
    public RasterPart getRasterPart(){
        return null;
    }
}
