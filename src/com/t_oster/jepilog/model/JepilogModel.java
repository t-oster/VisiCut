/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.jepilog.model;

import com.kitfox.svg.SVGCache;
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

/**
 *
 * @author thommy
 */
public class JepilogModel {

    private SVGUniverse universe = SVGCache.getSVGUniverse();
    private URI uri;
    private String jobname = "testjob";
    private List<Shape> vectorShapes = new LinkedList<Shape>();
    private Point startPoint = new Point(0,0);
    private int resolution = 500;
    
    public static final String PROPERTY_STARTINGPOINT = "startingpoint";
    public static final String PROPERTY_STARTINGPOSITION = "startingposition";
    public static final String PROPERTY_SVG = "svg";
    public static final String PROPERTY_RESOLUTION = "resolution";
    public static final String PROPERTY_CUTTER = "cutter";
    public static final String PROPERTY_CUTTINGSHAPES = "cuttingshapes";
    
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
    
    
    public void setSvg(URI uri) {
        if (uri!= null && !uri.equals(this.uri)){
            URI old = this.uri;
            this.uri = uri;
            vectorShapes = new LinkedList<Shape>();
            this.pcs.firePropertyChange(PROPERTY_SVG, old, uri);
        }
    }
    
    public URI getSvg(){
        return this.uri;
    }
    
    public void importSVG(File svgDocument) throws IOException{
        setSvg(universe.loadSVG(svgDocument.toURI().toURL()));
        jobname = svgDocument.getName();
    }

    public void addCuttingShape(Shape s) {
        if (!vectorShapes.contains(s)){
            vectorShapes.add(s);
            this.pcs.firePropertyChange(PROPERTY_CUTTINGSHAPES, null, s);
        }
    }

    public Shape[] getCuttingShapes() {
        return vectorShapes.toArray(new Shape[0]);
    }

    public void setStartPoint(Point p){
        if (!this.startPoint.equals(p)){
            Point old = this.startPoint;
            String oldsp = this.getStartingPosition();
            this.startPoint = p;
            this.pcs.firePropertyChange(PROPERTY_STARTINGPOINT, old, this.startPoint);
            String newsp = this.getStartingPosition();
            if (!oldsp.equals(newsp)){
                pcs.firePropertyChange(PROPERTY_STARTINGPOSITION, oldsp, newsp);
            }
        }
    }
    
    public void setStartPoint(int x, int y){
        this.setStartPoint(new Point(x,y));
    }
    
    public Point getStartPoint(){
        return this.startPoint;
    }
    
    public String getStartingPosition(){
        if (universe == null || uri == null){
            return "custom";
        }
        Rectangle2D rect = universe.getDiagram(uri).getViewRect();
        Point sp = this.getStartPoint();
        if (sp.x==(int) rect.getX() && sp.y==(int) rect.getY()){
            return "top left";
        }
        else if (sp.x==(int) (rect.getX()+rect.getWidth()) && sp.y==(int) rect.getY()){
            return "top right";
        }
        else if (sp.x==(int) rect.getX() && sp.y==(int) (rect.getY()+rect.getHeight())){
            return "bottom left";
        }
        else if (sp.x==(int) (rect.getX()+rect.getWidth()) && sp.y==(int) (rect.getY()+rect.getHeight())){
            return "bottom right";
        }
        else if (sp.x==(int) rect.getCenterX() && sp.y==(int) rect.getCenterY()){
            return "center";
        }
        else{
            return "custom";
        }
    }
    
    public void setStartingPosition(String p){
        if (uri != null && !p.equals(this.getStartingPosition())){
            String old = this.getStartingPosition();
            Rectangle2D rect = universe.getDiagram(uri).getViewRect();
            if (p.equals("top left")){
                this.setStartPoint((int) rect.getX(), (int) rect.getY());
            }
            else if (p.equals("top_right")){
                this.setStartPoint((int) (rect.getX()+rect.getWidth()), (int) rect.getY());
            }
            else if (p.equals("bottom left")){
                this.setStartPoint((int) rect.getX(), (int) (rect.getY()+rect.getHeight()));
            }
            else if (p.equals("bottom right")){
                this.setStartPoint((int) (rect.getX()+rect.getWidth()), (int) (int) (rect.getY()+rect.getHeight()));
            }
            else if (p.equals("center")){
                this.setStartPoint((int) (rect.getX()+rect.getWidth()/2), (int) (rect.getY()+rect.getHeight()/2));
            }
            pcs.firePropertyChange(PROPERTY_STARTINGPOSITION, old, p);
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
        for (Shape s:this.getCuttingShapes()){
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
            pcs.firePropertyChange(PROPERTY_RESOLUTION, old, this.resolution);
        }
    }

    public void sendToCutter() throws IllegalJobException, Exception {
        LaserCutter cutter = this.getSelectedLaserCutter();
        if (cutter == null){
            throw new Exception("No Lasercutter selected");
        }
        if (this.getCuttingShapes().length==0){
            throw new Exception("Nothing selected for cutting");
        }
        LaserJob job = new LaserJob(jobname, "123", "bla", getResolution(), generateVectorPart());
        job.setStartPoint((int) this.getStartPoint().getX(), (int) this.getStartPoint().getY());
        cutter.sendJob(job);
    }
}
