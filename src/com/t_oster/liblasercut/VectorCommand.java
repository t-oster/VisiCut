/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.liblasercut;

import java.util.Arrays;

/**
 *
 * @author thommy
 */
public class VectorCommand {
    public static enum CmdType{
        SETSPEED,
        SETPOWER,
        SETFREQUENCY,
        POLYGON,
        CIRCLE,
    }
    
    private CmdType type;
    private int[] operands;
    
    public VectorCommand(CmdType type, int[] x, int[] y){
        if (type==CmdType.POLYGON){
            if (x.length != y.length || x.length < 2){
                throw new IllegalArgumentException("X and Y Arrays have to be the same length and at least 2 points");
            }
            this.type = type;
            this.operands = Arrays.copyOf(x, 2*x.length);
            for (int i=0;i<y.length;i++){
                this.operands[x.length+i]=y[i];
            }
        }
        else{
            throw new IllegalArgumentException("Wrong number of Parameters for "+type.toString());
        }
    }
   
    public VectorCommand(CmdType type, int x, int y, int radius){
        if (type == CmdType.CIRCLE){
            this.type = type;
            operands = new int[]{x,y,radius};
        }
        else{
            throw new IllegalArgumentException("Wrong number of Parameters for "+type.toString());
        }
    }
    
    public CmdType getType(){
        return type;
    }
    
    /**
     * Returns the number of Points in a Polygon
     * @return 
     */
    public int getLength(){
        if (this.type == CmdType.POLYGON){
            return operands.length/2;
        }
        throw new UnsupportedOperationException("getX not supported for "+type.toString());
    }
    
    /**
     * returns the X coordinate of a polygon point
     * at given index
     * @param index
     * @return 
     */
    public int getX(int index){
     if (this.type == CmdType.POLYGON){
            return operands[index];
        }
        throw new UnsupportedOperationException("getX not supported for "+type.toString());
    }
    
    /**
     * returns the Y coordinate of a polygon point
     * at given index
     * @param index
     * @return 
     */
    public int getY(int index){
     if (this.type == CmdType.POLYGON){
            return operands[1+2*index];
        }
        throw new UnsupportedOperationException("getX not supported for "+type.toString());
    }
    
    public int getX(){
        switch (type){
            case CIRCLE: return operands[0];
            default: throw new UnsupportedOperationException("getX not supported for "+type.toString());
        }
    }
    
    public int getY(){
        switch (type){
            case CIRCLE: return operands[1];
            default: throw new UnsupportedOperationException("getX not supported for "+type.toString());
        }
    }
    
    public int getRadius(){
        switch (type){
            case CIRCLE: return operands[2];
            default: throw new UnsupportedOperationException("getX not supported for "+type.toString());
        }
    }
    
    public VectorCommand(CmdType type, int operand1){
        if (type==CmdType.SETSPEED || type == CmdType.SETPOWER || type == CmdType.SETFREQUENCY){
            this.type = type;
            operands = new int[]{operand1};
        }
        else{
            throw new IllegalArgumentException("Wrong number of Parameters for "+type.toString());
        }
    }
    public int getPower(){
        if (type==CmdType.SETPOWER){
            return operands[0];
        }
        throw new UnsupportedOperationException("getPower is not Applicable for "+type.toString());
    }
    public int getSpeed(){
        if (type==CmdType.SETSPEED){
            return operands[0];
        }
        throw new UnsupportedOperationException("getPower is not Applicable for "+type.toString());
    }
    public int getFrequency(){
        if (type==CmdType.SETFREQUENCY){
            return operands[0];
        }
        throw new UnsupportedOperationException("getPower is not Applicable for "+type.toString());
    }
}
