/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

 
package com.mcp14.Autoarrange;

import com.mcp14.Binpacking.Bin;
import com.mcp14.Binpacking.BinPacking;
import com.mcp14.Canvas.MArea;

import com.mcp14.Provider.HoldValues;
import com.mcp14.Utilities.Utils;
import com.t_oster.visicut.model.PlfFile;
import com.t_oster.visicut.model.PlfPart;
import java.awt.Dimension;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;


/**
 * Application client. Contains the input processing and the output generation.
 * 
 * @author Sughosh Kumar
 * 
 */
public class AutoArrange {
    public static HashMap<Integer, Set<HoldValues>> allValues = new HashMap<Integer, Set<HoldValues>>();
    
    public static void start(PlfFile svgFiles, Dimension laserBedDimension, int offset) throws FileNotFoundException, UnsupportedEncodingException {
        InputExporter exporter = new InputExporter(svgFiles.size(),laserBedDimension);
        for (PlfPart s : svgFiles){
            exporter.addInputs( s.getBoundingBox().getWidth()+(offset/2), s.getBoundingBox().getHeight()+(offset/2));
        }
	// creates the input file
        exporter.export();
        File inputFile = new File("input.txt");
        if(!(inputFile.exists())) {
            printUsage();
        } else {
            try {
                launch(inputFile.getName());

            } catch (IOException e) {
                System.out.println("An error occurred while processing your file. Please make sure the file follows the specified format. See trace below");
                System.out.println("****************************TRACE***************************");
                e.printStackTrace();
                System.out.println("************************************************************");
                printFileSpecifications();
            }
        }
    }

    private static void launch(String fileName) throws IOException {
        Object[] result = Utils.loadPieces(fileName);

        Dimension binDimension = (Dimension) result[0];
        Dimension viewPortDimension = (Dimension) result[1];
        MArea[] pieces = (MArea[]) result[2];

        Bin[] bins = BinPacking.BinPackingStrategy(pieces, binDimension, viewPortDimension);
        System.out.println("Generating bin images.........................");
        drawbinToFile(bins, viewPortDimension);
        System.out.println();
        System.out.println("Generating bin description files....................");
        createOutputFiles(bins);
        System.out.println("DONE!!!");

    }

    private static void drawbinToFile(Bin[] bins, Dimension viewPortDimension) throws IOException {
        for (int i = 0; i < bins.length; i++) {
                  if (bins[i].getPlacedPieces().length > 0) {
                      MArea[] areasInThisbin = bins[i].getPlacedPieces();
                      ArrayList<MArea> areas = new ArrayList<MArea>();
                      for (MArea area : areasInThisbin) {
                          areas.add(area);
                      }
                      //Utils.drawMAreasToFile(areas, viewPortDimension, bins[i].getDimension(), ("Bin-" + String.valueOf(i + 1)));
                      System.out.println("Generated image for bin " + String.valueOf(i + 1));
                  }
        }
    }

    private static void createOutputFiles(Bin[] bins) throws IOException {
        for (int i = 0; i < bins.length; i++) {
                  Set<HoldValues> holdValueses = new HashSet<HoldValues>();
                  if (bins[i].getPlacedPieces().length > 0){
                      MArea[] areasInThisbin = bins[i].getPlacedPieces();
                      for (MArea area : areasInThisbin) {
                          double offsetX = area.getBoundingBox2D().getX();
                          double offsetY = area.getBoundingBox2D().getY();
                          holdValueses.add(new HoldValues(area.getID(), area.getRotation(), offsetX, offsetY));
                      }
                  }
                  allValues.put((i+1), holdValueses);
            System.out.println("Generated points file for bin " + String.valueOf(i + 1));
        }
    }

    private static void printUsage() {
        System.out.println();
        System.out.println("Usage:");
        System.out.println();
        System.out.println("");
        System.out.println("<file name>: file describing pieces (see file structure specifications below).");
        System.out.println();
        System.out.println();
        printFileSpecifications();
    }

    private static void printFileSpecifications() {
        System.out.println("The input pieces file should be structured as follows: ");
        System.out.println("First line: 'width  height',integer bin dimensions separates by a space");
        System.out.println("Second line: 'number of pieces', a single integer specifying the number of pieces in this file.");
        System.out.println("N lines: each piece contained in a single line-> 'x0,y0 x1,y1 x2,y2 ... xn,yn'.NOTE "
          + "THAT FIGURE POINTS IN DOUBLE FORMAT MUST BE SPECIFIED IN COUNTERCLOCKWISE ORDER USING THE CARTESIAN COORDINATE SYSTEM.");
        System.out.println();
        System.out.println("An initial example of a file could be as follows:");
        System.out.println("100 100            -> bin dimensions.");
        System.out.println("2                  -> number of pieces");
        System.out.println("0,0 4,0 4,4 0,4    -> first piece.");
        System.out.println("0,0 5,0 5,5 0,5    -> second piece.");
    }


}
