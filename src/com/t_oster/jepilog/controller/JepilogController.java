/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.jepilog.controller;

import com.kitfox.svg.SVGCache;
import com.kitfox.svg.SVGDiagram;
import com.kitfox.svg.SVGUniverse;
import com.t_oster.liblasercut.IllegalJobException;
import com.t_oster.liblasercut.LaserCutter;
import com.t_oster.liblasercut.LaserJob;
import com.t_oster.liblasercut.VectorPart;
import com.t_oster.liblasercut.epilog.EpilogCutter;
import java.awt.Shape;
import java.awt.geom.PathIterator;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author thommy
 */
public class JepilogController {

    private SVGUniverse universe = SVGCache.getSVGUniverse();
    private SVGDiagram diagram;
    private URI uri;
    private String jobname = "testjob";
    private List<Shape> vectorShapes = new LinkedList<Shape>();

    public void importSvg(File svgDocument) throws IOException {
        // Parse the barChart.svg file into a Document.
        uri = universe.loadSVG(svgDocument.toURI().toURL());
        jobname = svgDocument.getName();
    }

    public URI getUri() {
        return uri;
    }

    public void addVectorShape(Shape s) {
        vectorShapes.add(s);
    }

    public Shape[] getVectorShapes() {
        return vectorShapes.toArray(new Shape[0]);
    }

    private void addShape(VectorPart vp, Shape s) {
        PathIterator iter = s.getPathIterator(null, 1);
        while (!iter.isDone()) {
            double[] test = new double[8];
            int result = iter.currentSegment(test);
            if (result == PathIterator.SEG_MOVETO) {
                vp.moveto((int) test[0], (int) test[1]);
            } else if (result == PathIterator.SEG_LINETO) {
                vp.lineto((int) test[0], (int) test[1]);
            }
            iter.next();
        }
    }

    private VectorPart generateVectorPart() {
        VectorPart vp = new VectorPart(5000, 20, 100);
        for (Shape s:this.getVectorShapes()){
            this.addShape(vp, s);
        }
        return vp;
    }

    private LaserCutter getSelectedLaserCutter() {
        EpilogCutter.SIMULATE_COMMUNICATION = true;
        return new EpilogCutter("137.226.56.228");
    }
    
    private int getSelectedResolution(){
        return 500;
    }

    public void sendToCutter() throws IllegalJobException, Exception {
        LaserCutter cutter = this.getSelectedLaserCutter();
        if (cutter == null){
            throw new Exception("No Lasercutter selected");
        }
        if (this.getVectorShapes().length==0){
            throw new Exception("Nothing selected for cutting");
        }
        LaserJob job = new LaserJob(jobname, "123", "bla", getSelectedResolution(), generateVectorPart());
        cutter.sendJob(job);
    }
}
