/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.jepilog.gui;

import com.kitfox.svg.SVGDiagram;
import com.kitfox.svg.SVGException;
import com.kitfox.svg.ShapeElement;
import com.kitfox.svg.app.beans.SVGIcon;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Component;
import java.awt.Shape;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.PathIterator;
import java.beans.PropertyChangeListener;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JPanel;
import java.io.Serializable;

/**
 *
 * @author thommy
 */
public class SVGPanel extends JPanel implements MouseListener, MouseMotionListener, Serializable {

    /**
     * Propertys
     */
    public static final String PROPERTY_STARTINGPOINT = "startingpoint";
    public static final String PROPERTY_SELECTED_SHAPE = "selected_shape";
    public static final String PROPERTY_SHOWRASTER = "show_raster";
    public static final String PROPERTY_SHOWVECTOR = "show vector";
    public static final String PROPERTY_SVG = "svgfile";
    
    private SVGIcon icon;
    private URI svgUri = null;
    private SVGDiagram svgDiagramm = null;
    private Shape[] cuttingShapes;
    private Shape selectedShape;
    private boolean showRaster = true;
    private boolean showVector = true;
    private Point startingPoint = new Point(0, 0);
    
    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener){
        super.addPropertyChangeListener(listener);
    }
    
    @Override
    public void addPropertyChangeListener(String property, PropertyChangeListener listener){
        super.addPropertyChangeListener(property, listener);
    }

    @Override
    public Dimension getPreferredSize() {
        if (this.svgUri == null) {
            return this.getMinimumSize();
        }
        return new Dimension((int) this.svgDiagramm.getWidth(), (int) this.svgDiagramm.getHeight());
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(300, 500);
    }

    public void setStartingPoint(Point p) {
        Point old = this.startingPoint;
        this.startingPoint = p;
        repaint();
        this.firePropertyChange(PROPERTY_STARTINGPOINT, old, p);
    }

    private void sizeToFit() {
        Dimension d = getPreferredSize();
        setSize(d.width, d.height);
        Component p = getParent();
        if (p != null) {
            p.invalidate();
            p.doLayout();
        }
    }

    public Point getStartingPoint() {
        return this.startingPoint;
    }

    public SVGPanel() {
        this.addMouseListener(this);
        this.addMouseMotionListener(this);
        this.cuttingShapes = null;
    }

    public void setShowRaster(boolean show) {
        if (show != showRaster) {
            showRaster = show;
            repaint();
            firePropertyChange(PROPERTY_SHOWRASTER, !showRaster, showRaster);
        }
    }
    
    public boolean isShowRaster(){
        return showRaster;
    }

    public void setShowVector(boolean show) {
        if (show != showVector) {
            showVector = show;
            repaint();
            firePropertyChange(PROPERTY_SHOWVECTOR, !showVector, showVector);
        }
    }
    
    public boolean isShowVector(){
        return showVector;
    }

    public void setSvgUri(URI diag) {
        if (diag != null && !diag.equals(this.svgUri)){
            URI old = this.svgUri;
            this.icon = new SVGIcon();
            icon.setSvgURI(diag);
            icon.setAntiAlias(true);
            icon.setClipToViewbox(true);
            icon.setScaleToFit(false);
            this.svgDiagramm = icon.getSvgUniverse().getDiagram(icon.getSvgURI());
            this.selectedShape = null;
            this.cuttingShapes = null;
            this.sizeToFit();
            firePropertyChange(PROPERTY_SVG, old, this.svgUri);
        }
    }

    public void setCuttingShapes(Shape[] cuttingShapes){
        this.cuttingShapes = cuttingShapes;
    }
    
    public URI getSvgUri(){
        return this.svgUri;
    }
    
    /**
     * Draws a given Shape on a Graphics g but just using the line
     * operation (to simulate behavior on lasercutters)
     * @param g
     * @param shape 
     */
    private void drawShape(Graphics g, Shape shape) {
        int x = 0;
        int y = 0;
        PathIterator iter = shape.getPathIterator(null, 0.4);
        while (!iter.isDone()) {
            double[] test = new double[8];
            int result = iter.currentSegment(test);
            if (result == PathIterator.SEG_MOVETO) {
                x = (int) test[0];
                y = (int) test[1];
            } else if (result == PathIterator.SEG_LINETO) {
                g.drawLine(x, y, (int) test[0], (int) test[1]);
                x = (int) test[0];
                y = (int) test[1];
            }
            iter.next();
        }
    }

    @Override
    public void paintComponent(Graphics g) {
        if (icon == null) {
            super.paintComponent(g);
            return;
        }
        final int width = getWidth();
        final int height = getHeight();

        //Background
        g.setColor(getBackground());
        g.fillRect(0, 0, width, height);



        if (showRaster) {
            icon.paintIcon(this, g, 0, 0);
        }
        g.setColor(Color.RED);
        if (cuttingShapes!=null && showVector) {
            for (Shape shape : cuttingShapes) {
                drawShape(g, shape);
            }
        }
        g.setColor(Color.yellow);
        if (selectedShape != null) {
            drawShape(g, selectedShape);
        }

        //Draw StartingPoint

        Point sp = movingStartPoint ? this.getMousePosition() : startingPoint;
        if (sp != null) {
            g.setColor(Color.white);
            g.drawOval(sp.x - 6, sp.y - 6, 12, 12);
            g.setColor(Color.red);
            g.drawLine(sp.x - 4, sp.y - 4, sp.x + 4, sp.y + 4);
            g.drawLine(sp.x - 4, sp.y + 4, sp.x + 4, sp.y - 4);
        }
    }

    public void setSelectedShape(Shape s){
        if (!s.equals(selectedShape)){
            Shape old = selectedShape;
            this.selectedShape = s;
            this.repaint();
            firePropertyChange(PROPERTY_SELECTED_SHAPE, old, s);
        }
        
    }
    
    public Shape getSelectedShape(){
        return selectedShape;
    }
    
    public void mouseClicked(MouseEvent me) {
        try {
            if (svgDiagramm != null) {
                List pickedElements = svgDiagramm.pick(me.getPoint(), null);
                if (pickedElements.size() > 0) {
                    List first = (List) pickedElements.get(pickedElements.size() - 1);
                    Object elem = first.get(first.size() - 1);
                    if (elem instanceof ShapeElement) {
                        Shape shape = ((ShapeElement) elem).getShape();
                        this.setSelectedShape(shape);
                    }
                }
            }

        } catch (SVGException ex) {
            Logger.getLogger(SVGPanel.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    private boolean movingStartPoint = false;

    public void mousePressed(MouseEvent me) {
        if (me.getPoint().distance(startingPoint) <= 10) {
            movingStartPoint = true;
        }
    }

    public void mouseReleased(MouseEvent me) {
        if (movingStartPoint) {
            movingStartPoint = false;
            setStartingPoint(me.getPoint());
        }
    }

    public void mouseEntered(MouseEvent me) {
    }

    public void mouseExited(MouseEvent me) {
    }

    public void mouseDragged(MouseEvent me) {
        if (movingStartPoint) {
            this.repaint();
        }
    }

    public void mouseMoved(MouseEvent me) {
    }

}
