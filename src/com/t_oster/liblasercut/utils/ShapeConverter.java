package com.t_oster.liblasercut.utils;

import com.t_oster.liblasercut.VectorPart;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;

/**
 * This class makes it possible to add java.awt.Shape Objects
 * to a VectorPart. The Shape will be converted to moveto and lineto
 * commands fitting as close as possible
 * 
 * @author thommy
 */
public class ShapeConverter {

    /**
     * Adds the given Shape to the given VectorPart by converting it to
     * lineto and moveto commands, whose lines differs not more than
     * 1 pixel from the original shape.
     * 
     * @param shape the Shape to be added
     * @param vectorpart the Vectorpart the shape shall be added to
     */
    public void addShape(Shape shape, VectorPart vectorpart) {
        AffineTransform scale = AffineTransform.getScaleInstance(1, 1);
        PathIterator iter = shape.getPathIterator(scale, 1);
        while (!iter.isDone()) {
            double[] test = new double[8];
            int result = iter.currentSegment(test);
            if (result == PathIterator.SEG_MOVETO) {
                vectorpart.moveto((int) test[0], (int) test[1]);
            } else {
                if (result == PathIterator.SEG_LINETO) {
                    vectorpart.lineto((int) test[0], (int) test[1]);
                }
            }
            iter.next();
        }
    }
}
