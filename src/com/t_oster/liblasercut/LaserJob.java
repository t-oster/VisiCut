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
    private VectorPart vPart;
    private RasterPart rPart;
    
    public LaserJob(String title, String name, String user, int resolution, VectorPart vp){
        this.title = title;
        this.name = name;
        this.user = user;
        this.resolution = resolution;
        this.vPart = vp;
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
        return rPart!=null;
    }

    public boolean containsVector(){
        return vPart!=null;
    }

    public int getResolution(){
        return resolution;
    }
    
    public VectorPart getVectorPart(){
        return vPart;
    }
    
    public RasterPart getRasterPart(){
        return rPart;
    }
}
