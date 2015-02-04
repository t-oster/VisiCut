package com.mcp14.Canvas;

/**
 * A class that represents a geometric vector v=(x,y) in double precision.
 * 
 * @author Sughosh Kumar
 * 
 */
public class MVector {

    private double x;
    private double y;

    public MVector(double x, double y) {
	super();
	this.x = x;
	this.y = y;
    }

    public double getX() {
	return x;
    }

    public double getY() {
	return y;
    }

    /**
     * Takes this vector and changes the signs of its coordinates.
     * 
     * @return a new MVector with its coordinates inverted.
     */
    public MVector inverse() {
	return new MVector(-x, -y);
    }
}