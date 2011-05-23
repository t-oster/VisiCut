/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.jepilog.model;

import com.kitfox.svg.SVGException;
import com.kitfox.svg.ShapeElement;
import com.t_oster.liblasercut.CuttingProperty;
import com.t_oster.util.Tuple;
import com.t_oster.util.Util;
import java.awt.Shape;

/**
 *
 * @author thommy
 */
public class CuttingShape extends Tuple<ShapeElement,CuttingProperty>{
    
    public CuttingShape(){
        this(null,null);
    }
    
    public CuttingShape(ShapeElement s){
        this(s, null);
    }
    
    public CuttingShape(ShapeElement s, CuttingProperty p){
        super(s,p);
    }
    
    public CuttingProperty getProperty(){
        return this.getB();
    }
    public ShapeElement getShapeElement(){
        return this.getA();
    }
    public Shape getTransformedShape() throws SVGException{
        return Util.extractTransformedShape(this.getA());
    }
    public void setProperty(CuttingProperty p){
        this.setB(p);
    }
    public void setShapeElement(ShapeElement s){
        this.setA(s);
    }
}
