/*
 * This is a data structure class that is used to get the coordinate values
 * of the final objects with respective ID and rotation in the canvas.
 * This could be used to export the values to the and we'll get a HashMap
 * containing the Bin number and List of this class' objects.
 */

package com.mcp14.Provider;

import java.util.ArrayList;

/**
 *
 * @author sughoshkumar
 */
public class HoldValues {
    int objectID;
    double x, y, objectRotation;
    ArrayList<Double> XYCoordinates;

    public HoldValues(int ID, double rotation, double x, double y){
        this.objectID = ID;
        this.objectRotation = rotation;
        this.x = x;
        this.y = y;
        XYCoordinates = new ArrayList<Double>();
    }

    public void addCoordinatesToList(){
        if ( x == 0.0 && y == 0.0) {
            System.out.println("No values to add : x = " + x + " and y = " + y);
        }
        else {
            XYCoordinates.add(this.x);
            XYCoordinates.add(this.y);
        }
    }

    public int getObjectID(){
        return objectID;
    }

    public double getObjectRotation(){
        return objectRotation;
    }

    public double getX(){
       return x;
    }

    public double getY(){
        return y;
    }

    public ArrayList<Double> getXYCoordinatesList(){
        return XYCoordinates;
    }

}
