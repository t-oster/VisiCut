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
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 *
 * @author sughoshkumar
 */
public class InputExporter extends Inputs {
    int numberOfSVG;
    Dimension binDimension;
    ArrayList<Inputs> inputers;

    public InputExporter() {
        super();
        numberOfSVG = 0;
        binDimension = null;
    }

    public InputExporter(int numberOfSVG, Dimension binDimension) {
        this.numberOfSVG = numberOfSVG;
        this.binDimension = binDimension;
        inputers = new ArrayList<Inputs> ();
    }
    
    public void export() throws FileNotFoundException, UnsupportedEncodingException{
        PrintWriter printWriter = new PrintWriter("input.txt", "UTF-8");
        printWriter.println((int)binDimension.getWidth() + " " +(int) binDimension.getHeight());
        printWriter.println(numberOfSVG);
        System.out.println("size of inputs - " + inputers.size());
        for(Inputs i : inputers){
           printWriter.println(i.getXY_()[0] + "," + i.getXY_()[1] + " " + i.getXYOne()[0]+ "," + i.getXYOne()[1] + " " +
                   i.getXYTwo()[0] + "," + i.getXYTwo()[1] + " " + i.getXYThree()[0] + "," + i.getXYThree()[1]); 
        }
        printWriter.close();
        System.out.println("Input written");
    }
    
    public void addInputs(double x, double y) throws UnsupportedOperationException{
        Inputs i = new Inputs((float)x, (float)y);
        inputers.add(i);        
    }

  
}
