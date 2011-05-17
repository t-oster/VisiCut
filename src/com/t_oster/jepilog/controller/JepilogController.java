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
import java.awt.Point;
import java.awt.Shape;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.Observable;

/**
 *
 * @author thommy
 */
public class JepilogController extends Observable{

    private SVGUniverse universe = SVGCache.getSVGUniverse();
    private URI uri;
    private String jobname = "testjob";
    private List<Shape> vectorShapes = new LinkedList<Shape>();
    private Point startPoint = new Point(0,0);
    private int resolution = 500;
    
    public static enum StartingPosition{
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT,
        CENTER,
        CUSTOM
    }
    
    public static enum Property{
        STARTING_POINT,
        SVG_FILE,
        RESOLUTION
    }
    
    private PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    public void addPropertyChangeListener(PropertyChangeListener listener)
    {
        this.pcs.addPropertyChangeListener( listener );
    }
    /**
     * This method adds a PoropertyChangeListener, which only gets informed
     * about Property changes of the given property
     * @param property a value of one of the Property enum elements' toString() method
     * @param listener the listener to be registered
     */
    public void addPropertyChangeListener(String property, PropertyChangeListener listener)
    {
        this.pcs.addPropertyChangeListener(property, listener);
    }
    public void removePropertyChangeListener(String property, PropertyChangeListener listener)
    {
        this.pcs.removePropertyChangeListener(property, listener);
    }
    public void removePropertyChangeListener( PropertyChangeListener listener )
    {
        this.pcs.removePropertyChangeListener( listener );
    }
    
    
    public void importSvg(File svgDocument) throws IOException {
        URI old = uri;
        uri = universe.loadSVG(svgDocument.toURI().toURL());
        jobname = svgDocument.getName();
        vectorShapes = new LinkedList<Shape>();
        this.pcs.firePropertyChange(Property.SVG_FILE.toString(), old, uri);
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

    public void setStartPoint(Point p){
        if (!this.startPoint.equals(p)){
            Point old = this.startPoint;
            this.startPoint = p;
            this.pcs.firePropertyChange(Property.STARTING_POINT.toString(), old, this.startPoint);
        }
    }
    
    public void setStartPoint(int x, int y){
        this.setStartPoint(new Point(x,y));
    }
    
    public Point getStartPoint(){
        return this.startPoint;
    }
    
    public StartingPosition getStartingPosition(){
        Rectangle2D rect = universe.getDiagram(uri).getViewRect();
        Point sp = this.getStartPoint();
        if (sp.x==(int) rect.getX() && sp.y==(int) rect.getY()){
            return StartingPosition.TOP_LEFT;
        }
        else if (sp.x==(int) (rect.getX()+rect.getWidth()) && sp.y==(int) rect.getY()){
            return StartingPosition.TOP_RIGHT;
        }
        else if (sp.x==(int) rect.getX() && sp.y==(int) (rect.getY()+rect.getHeight())){
            return StartingPosition.BOTTOM_LEFT;
        }
        else if (sp.x==(int) (rect.getX()+rect.getWidth()) && sp.y==(int) (rect.getY()+rect.getHeight())){
            return StartingPosition.BOTTOM_RIGHT;
        }
        else if (sp.x==(int) rect.getCenterX() && sp.y==(int) rect.getCenterY()){
            return StartingPosition.CENTER;
        }
        else{
            return StartingPosition.CUSTOM;
        }
    }
    
    public void setStartingPosition(StartingPosition p){
        if (uri != null){
            Rectangle2D rect = universe.getDiagram(uri).getViewRect();
            switch (p){
                case TOP_LEFT:
                    this.setStartPoint((int) rect.getX(), (int) rect.getY());
                    break;
                case TOP_RIGHT:
                    this.setStartPoint((int) (rect.getX()+rect.getWidth()), (int) rect.getY());
                    break;
                case BOTTOM_LEFT:
                    this.setStartPoint((int) rect.getX(), (int) (rect.getY()+rect.getHeight()));
                    break;
                case BOTTOM_RIGHT:
                    this.setStartPoint((int) (rect.getX()+rect.getWidth()), (int) (int) (rect.getY()+rect.getHeight()));
                    break;
                case CENTER:
                    this.setStartPoint((int) (rect.getX()+rect.getWidth()/2), (int) (rect.getY()+rect.getHeight()/2));
                    break;
                case CUSTOM:
                    throw new IllegalArgumentException("To set Cusom StartPoint use the mehtod with Point or int values");
            }
        }
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

    public LaserCutter getSelectedLaserCutter() {
        EpilogCutter.SIMULATE_COMMUNICATION = false;
        return new EpilogCutter("137.226.56.228");
    }
    
    public int getResolution(){
        return resolution;
    }
    
    public void setResolution(int resolution){
        if (resolution != this.resolution){
            int old = this.resolution;
            this.resolution = resolution;
            pcs.firePropertyChange(Property.RESOLUTION.toString(), old, this.resolution);
        }
    }

    public void sendToCutter() throws IllegalJobException, Exception {
        LaserCutter cutter = this.getSelectedLaserCutter();
        if (cutter == null){
            throw new Exception("No Lasercutter selected");
        }
        if (this.getVectorShapes().length==0){
            throw new Exception("Nothing selected for cutting");
        }
        LaserJob job = new LaserJob(jobname, "123", "bla", getResolution(), generateVectorPart());
        job.setStartPoint((int) this.getStartPoint().getX(), (int) this.getStartPoint().getY());
        cutter.sendJob(job);
    }
}
