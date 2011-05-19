/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.jepilog.model;

import java.awt.Rectangle;
import java.awt.Shape;
import com.t_oster.liblasercut.CuttingProperty;
import com.t_oster.util.Util;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeSupport;
import java.beans.PropertyChangeListener;

/**
 * Adds Parameters for VectorCutting (power,speed,frequency) to a shape
 * @author oster
 */
public class CuttingShape implements Shape{
    
    public static final String PROPERTY_CUTTINGPROPERTY = "cuttingProperty";
    
    private CuttingProperty cuttingProperty;
    private Shape shape = null;
    private String name = "unnamed Shape";
    
    public CuttingShape(Shape s, CuttingProperty cp){
        this.shape = s;
        this.cuttingProperty = cp;
    }   
    
    protected PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    
    public void addPropertyChangeListener(PropertyChangeListener listener)
    {
        this.pcs.addPropertyChangeListener( listener );
    }
    public void addPropertyChangeListener(String property, PropertyChangeListener listener)
    {
        this.pcs.addPropertyChangeListener(property, listener);
    }
    public void removePropertyChangeListener(String property, PropertyChangeListener listener)
    {
        this.pcs.removePropertyChangeListener(property, listener);
    }
    public void removePropertyChangeListener( PropertyChangeListener listener )
    {
        this.pcs.removePropertyChangeListener( listener );
    }
    
    public void setShape(Shape s){
        this.shape = s;
    }
    
    public void setCuttingProperty(CuttingProperty cp){
        if (Util.differ(cp, this.cuttingProperty)){
            CuttingProperty old = this.cuttingProperty;
            this.cuttingProperty = cp;
            this.pcs.firePropertyChange(PROPERTY_CUTTINGPROPERTY, old, this.cuttingProperty);
        }
    }
    
    public CuttingProperty getCuttingProperty(){
        return this.cuttingProperty;
    }

    public void setName(String name){
        this.name = name;
    }
    
    public String getName(){
        return this.name;
    }
    
    public Rectangle getBounds() {
        return shape.getBounds();
    }

    public Rectangle2D getBounds2D() {
        return shape.getBounds2D();
    }

    public boolean contains(double d, double d1) {
        return shape.contains(d,d1);
    }

    public boolean contains(Point2D pd) {
        return shape.contains(pd);
    }

    public boolean intersects(double d, double d1, double d2, double d3) {
        return shape.intersects(d, d1, d2, d3);
    }

    public boolean intersects(Rectangle2D rd) {
        return shape.intersects(rd);
    }

    public boolean contains(double d, double d1, double d2, double d3) {
        return shape.contains(d,d1,d2,d3);
    }

    public boolean contains(Rectangle2D rd) {
        return shape.contains(rd);
    }

    public PathIterator getPathIterator(AffineTransform at) {
        return shape.getPathIterator(at);
    }

    public PathIterator getPathIterator(AffineTransform at, double d) {
        return shape.getPathIterator(at, d);
    }
    
    @Override
    public boolean equals(Object o){
        if (o instanceof CuttingShape){
            CuttingShape cs = (CuttingShape) o;
            return this.shape.equals(((CuttingShape) o).shape) && !Util.differ(cs.cuttingProperty, this.cuttingProperty);
        }
        else if (o instanceof Shape){
            return this.shape.equals((Shape) o);
        }
        else{
            return super.equals(o);
        }
        
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 79 * hash + (this.cuttingProperty != null ? this.cuttingProperty.hashCode() : 0);
        hash = 79 * hash + (this.shape != null ? this.shape.hashCode() : 0);
        hash = 79 * hash + (this.name != null ? this.name.hashCode() : 0);
        return hash;
    }
}
