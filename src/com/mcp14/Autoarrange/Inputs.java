/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.mcp14.Autoarrange;

import java.awt.Dimension;
import java.util.ArrayList;

/**
 *
 * @author sughoshkumar
 */
public class Inputs {
    float x,y;
    float[] xy, xy1, xy2, xy3;
    
    Inputs(){
        xy1 = xy2 = xy3 = null;
        x = y = 0.0f;
    }
    
    public Inputs(float x, float y){
        this.x = x;
        this.y = y;
        createInputDimensions();
    }
    
    private void createInputDimensions(){
        float[] xy_ = {0.0f, 0.0f};
        this.xy = xy_;
        float[] xyOne = {this.y, 0.0f};
        this.xy1 = xyOne;
        float[] xyTwo = {this.y,this.x};
        this.xy2 = xyTwo;
        float[] xyThree = {0.0f, this.x};
        this.xy3 = xyThree;
    }
    
    public float[] getXY_(){
        return xy;
    }
    public float[] getXYOne(){
        return xy1;
    } 
    
    public float[] getXYTwo(){
        return xy2;
    }
    
    public float[] getXYThree(){
        return xy3;
    }
    
}
