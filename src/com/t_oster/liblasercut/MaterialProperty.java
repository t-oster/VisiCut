/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.liblasercut;

/**
 *
 * @author oster
 */
public class MaterialProperty {
    private String name = "Default Material";
    private double height = 1;
    private CuttingProperty cuttingProperty = new CuttingProperty();
    
    public MaterialProperty(){
        
    }
    
    public MaterialProperty(String name, double height, int cuttingPower, int cuttingSpeed, int cuttingFrequency){
        this.name = name;
        this.height = height;
        this.cuttingProperty = new CuttingProperty(cuttingPower, cuttingSpeed, cuttingFrequency);
    }
    
    public String getName(){
        return this.name;
    }
    
    public double getHeight(){
        return this.height;
    }
    
    /**
     * Returns the cuttingProperty to cut completely through the material
     * @return 
     */
    public CuttingProperty getCuttingProperty(){
        return this.cuttingProperty;
    }
    
    /**
     * Tries to interpolate the CuttingProperty to cut exactly depth mm
     * into the material
     * @param depth
     * @return 
     */
    public CuttingProperty getCuttingProperty(double depth){
        if (depth>height){
            throw new IllegalArgumentException(("Can't cut deeper than the Material is"));
        }
        else{
            return new CuttingProperty(
                    (int) (this.cuttingProperty.getPower()*depth/height),
                    this.cuttingProperty.getSpeed(),
                    this.cuttingProperty.getFrequency()
                    );
        }
    }
    
    @Override
    public String toString(){
        return this.getName()+" ("+this.getHeight()+" mm)";
    }
    
}
