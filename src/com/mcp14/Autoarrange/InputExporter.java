/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

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
    
    public void addInputs(float x, float y){
        Inputs i = new Inputs(x,y);
        inputers.add(i);        
    }
}
