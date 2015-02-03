/**
 * This file is part of VisiCut.
 * Copyright (C) 2011 - 2013 Thomas Oster <thomas.oster@rwth-aachen.de>
 * RWTH Aachen University - 52062 Aachen, Germany
 *
 *     VisiCut is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     VisiCut is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with VisiCut.  If not, see <http://www.gnu.org/licenses/>.
 **/

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
        float[] xyOne = {this.x, 0.0f};
        this.xy1 = xyOne;
        float[] xyTwo = {this.x,this.y};
        this.xy2 = xyTwo;
        float[] xyThree = {0.0f, this.y};
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
