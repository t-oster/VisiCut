package com.mcp14.Binpacking;

import com.mcp14.Canvas.MArea;
import java.awt.Dimension;
import java.io.IOException;
import java.util.ArrayList;

/**
 *
 * @author Sughosh Kumar
 */

public class BinPacking {
    /**
     * Entry point for the application. Applies the packing strategies to the
     * provided pieces.
     * 
     * @param pieces
     *            pieces to be nested inside the bins.
     * @param binDimension
     *            dimensions for the generated bins.
     * @param viewPortDimension
     *            dimensions of the view port for the bin images generation
     * @return list of generated bins.
     */
    public static Bin[] BinPackingStrategy(MArea[] pieces, Dimension binDimension, Dimension viewPortDimension) {
	System.out.println(".............Started computation of bin placements.............");
	ArrayList<Bin> bins = new ArrayList<Bin>();
	int nbin = 0;
	boolean stillToPlace = true;
	MArea[] notPlaced = pieces;
	double t1 = System.currentTimeMillis();
        int count = 0;
	while (stillToPlace && count < 10) {
	    stillToPlace = false;
	    Bin bin = new Bin(binDimension);
	    notPlaced = bin.BBCompleteStrategy(notPlaced);

	    bin.compress();

	    notPlaced = bin.dropPieces(notPlaced);

	    System.out.println("Bin " + (++nbin) + " generated");
	    bins.add(bin);
	    if (notPlaced.length > 0)
		stillToPlace = true;
            count ++;
	}
	double t2 = System.currentTimeMillis();
	System.out.println();
	System.out.println("Number of used bins: " + nbin);
	System.out.println("Computation time:" + ((t2 - t1) / 1000) / 60 + " minutes");
	System.out.println();
	return bins.toArray(new Bin[0]);
    }

}