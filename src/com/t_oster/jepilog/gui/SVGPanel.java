/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.jepilog.gui;

import java.awt.Font;
import com.kitfox.svg.SVGDiagram;
import com.kitfox.svg.SVGElement;
import com.kitfox.svg.SVGException;
import com.kitfox.svg.ShapeElement;
import com.kitfox.svg.app.beans.SVGIcon;
import com.kitfox.svg.xml.StyleAttribute;
import com.t_oster.jepilog.model.CuttingShape;
import com.t_oster.util.Util;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Component;
import java.awt.Shape;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.beans.PropertyChangeListener;
import java.net.URI;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JPanel;
import java.io.Serializable;
import java.util.Set;

/**
 *
 * @author thommy
 */
public class SVGPanel extends JPanel implements MouseListener, MouseMotionListener, Serializable {

    /**
     * Propertys
     */
    public static final String PROPERTY_STARTPOINT = "startPoint";
    public static final String PROPERTY_SELECTED_SVGELEMENT = "selectedSVGElement";
    public static final String PROPERTY_SHOWENGRAVINGPART = "showEngravingPart";
    public static final String PROPERTY_SHOWCUTTINGPART = "showCuttingPart";
    public static final String PROPERTY_SHOWGRID = "showGrid";
    public static final String PROPERTY_ZOOMFACTOR = "zoomFactor";
    private static final long serialVersionUID = 1L;
    
    private SVGIcon icon;
    private URI svgUri = null;
    private SVGDiagram svgDiagramm = null;
    private CuttingShape[] cuttingShapes;
    private SVGElement selectedSVGElement;
    private double zoomFactor = 1;
    private int gridDPI = 500;
    private boolean showEngravingPart = true;
    private boolean showCuttingPart = true;
    private boolean showGrid = true;
    private Point startPoint = new Point(0, 0);

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
    
    public void setZoomFactor(double zf){
        if (zf != this.zoomFactor){
            double old = this.zoomFactor;
            this.zoomFactor = zf;
            this.repaint();
            firePropertyChange(PROPERTY_ZOOMFACTOR, old, this.zoomFactor);
        }
    }
    
    public double getZoomFactor(){
        return this.zoomFactor;
    }
    
    public void setStartPoint(Point p) {
        if (Util.differ(p,this.startPoint)){
            Point old = this.startPoint;
            this.startPoint = p;
            repaint();
            this.firePropertyChange(PROPERTY_STARTPOINT, old, p);
        }
    }

    public Point getStartPoint() {
        return this.startPoint;
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

    public SVGPanel() {
        this.addMouseListener(this);
        this.addMouseMotionListener(this);
        this.cuttingShapes = null;
    }

    public void setShowEngravingPart(boolean show) {
        if (show != showEngravingPart) {
            showEngravingPart = show;
            repaint();
            firePropertyChange(PROPERTY_SHOWENGRAVINGPART, !showEngravingPart, showEngravingPart);
        }
    }
    
    public boolean isShowEngravingPart(){
        return showEngravingPart;
    }

    public void setShowCuttingPart(boolean show) {
        if (show != showCuttingPart) {
            showCuttingPart = show;
            repaint();
            firePropertyChange(PROPERTY_SHOWCUTTINGPART, !showCuttingPart, showCuttingPart);
        }
    }
    
    public boolean isShowCuttingPart(){
        return showCuttingPart;
    }
    
    public void setShowGrid(boolean show){
        if (show != this.showGrid){
            this.showGrid = show;
            this.repaint();
            firePropertyChange(PROPERTY_SHOWGRID, !showGrid, showGrid);
        }
    }
    
    public boolean isShowGrid(){
        return this.showGrid;
    }

    public void setSvgUri(URI diag) {
        if (Util.differ(diag, this.svgUri)){
            URI old = this.svgUri;
            this.icon = new SVGIcon();
            icon.setSvgURI(diag);
            icon.setAntiAlias(true);
            icon.setClipToViewbox(false);
            icon.setScaleToFit(false);
            this.svgDiagramm = icon.getSvgUniverse().getDiagram(icon.getSvgURI());
            this.selectedSVGElement = null;
            this.cuttingShapes = null;
            this.sizeToFit();
        }
    }

    public URI getSvgUri(){
        return this.svgUri;
    }
    
    public void setCuttingShapes(CuttingShape[] cuttingShapes){
        this.cuttingShapes = cuttingShapes;
    }
    
    public CuttingShape[] getCuttingShapes(){
        return this.cuttingShapes;
    }
    
    public boolean isCuttingShapeSelected(){
        SVGElement sel = this.getSelectedSVGElement();
        if (sel != null && this.cuttingShapes != null && sel instanceof ShapeElement){
            for (CuttingShape cs:this.cuttingShapes){
                if (cs.getShapeElement().equals((ShapeElement) sel)){
                    return true;
                }
            }
        }
        return false;
    }
    
    public void setSelectedSVGElement(SVGElement s){
        if (Util.differ(s, selectedSVGElement)){
            SVGElement old = selectedSVGElement;
            this.selectedSVGElement = s;
            this.repaint();
            firePropertyChange(PROPERTY_SELECTED_SVGELEMENT, old, s);
            boolean cse = this.isCuttingShapeSelected();
            firePropertyChange("cuttingShapeSelected", !cse, cse);
        }
        
    }
    
    public SVGElement getSelectedSVGElement(){
        return selectedSVGElement;
    }
    
    public void setGridDPI(int dpi){
        if (this.gridDPI != dpi){
            this.gridDPI = dpi;
            this.repaint();
        }
    }
    
    public int getGridDPI(){
        return this.gridDPI;
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

    private void drawGrid(Graphics g, int rasterDPI, int rasterWidth){
        int width = getWidth();
        int height = getHeight();
        g.setColor(Color.BLACK);
        Point sp = this.getStartPoint();
        for (int mm = 0;mm < Util.px2mm(width-sp.x, rasterDPI);mm+=rasterWidth){
            int lx = sp.x+(int) Util.mm2px(mm, rasterDPI);
            g.drawLine(lx, 0, lx, height);
            String txt = mm+" mm";
            int w = g.getFontMetrics().stringWidth(txt);
            int h = g.getFontMetrics().getHeight();
            g.setColor(getBackground());
            g.fillRect(lx-w/2, (int) (height-1.8*h), w, h);
            g.setColor(Color.BLACK);
            g.drawString(txt, lx-w/2, height-h);
        }
        for (int mm = rasterWidth;mm < Util.px2mm(sp.x, rasterDPI);mm+=rasterWidth){
            int lx = sp.x-(int) Util.mm2px(mm, rasterDPI);
            g.drawLine(lx, 0, lx, height);
            String txt = "-"+mm+" mm";
            int w = g.getFontMetrics().stringWidth(txt);
            int h = g.getFontMetrics().getHeight();
            g.setColor(getBackground());
            g.fillRect(lx-w/2, (int) (height-1.8*h), w, h);
            g.setColor(Color.BLACK);
            g.drawString(txt, lx-w/2, height-h);
        }
        for (int mm = 0;mm < Util.px2mm(height-sp.y, rasterDPI);mm+=rasterWidth){
            int ly = sp.y+(int) Util.mm2px(mm, rasterDPI);
            g.drawLine(0, ly, width, ly);
            String txt = mm+" mm";
            int w = g.getFontMetrics().stringWidth(txt);
            int h = g.getFontMetrics().getHeight();
            g.setColor(getBackground());
            g.fillRect(width-w, ly-h/2, w, h);
            g.setColor(Color.BLACK);
            g.drawString(txt, width-w, ly+h/3);
        }
        for (int mm = rasterWidth;mm < Util.px2mm(sp.y, rasterDPI);mm+=rasterWidth){
            int ly = sp.y-(int) Util.mm2px(mm, rasterDPI);
            g.drawLine(0, ly, width, ly);
            String txt = "-"+mm+" mm";
            int w = g.getFontMetrics().stringWidth(txt);
            int h = g.getFontMetrics().getHeight();
            g.setColor(getBackground());
            g.fillRect(width-w, ly-h/2, w, h);
            g.setColor(Color.BLACK);
            g.drawString(txt, width-w, ly+h/3);
        }
    }
    
    @Override
    public void paintComponent(Graphics gg) {
        Graphics2D g = (Graphics2D) gg;
        g.scale(zoomFactor, zoomFactor);
        //if (icon == null) {
         //   super.paintComponent(g);
          //  return;
        //}
        final int width = (int) (getWidth()/zoomFactor);
        final int height = (int) (getHeight()/zoomFactor);

        //Background
        g.setColor(getBackground());
        g.fillRect(0, 0, width, height);

        if (showGrid && gridDPI != 0){
           drawGrid(g, gridDPI, 10);
        }


        if (icon != null && showEngravingPart) {
            icon.paintIcon(this, g, 0, 0);
        }
        if (cuttingShapes!=null && showCuttingPart) {
            g.setColor(Color.RED);
            for (CuttingShape shape : cuttingShapes) {
                try {
                    drawShape(g, shape.getTransformedShape());
                } catch (SVGException ex) {
                    Logger.getLogger(SVGPanel.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        if (selectedSVGElement != null && selectedSVGElement instanceof ShapeElement) {
            try {
                g.setColor(Color.GREEN);
                drawShape(g, Util.extractTransformedShape((ShapeElement) selectedSVGElement));
            } catch (SVGException ex) {
                Logger.getLogger(SVGPanel.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        //Draw StartingPoint

        Point sp = movingStartPoint ? this.getMousePosition() : startPoint;
        if (sp != null) {
            g.setColor(Color.white);
            g.drawOval(sp.x - 6, sp.y - 6, 12, 12);
            g.setColor(Color.red);
            g.drawLine(sp.x - 4, sp.y - 4, sp.x + 4, sp.y + 4);
            g.drawLine(sp.x - 4, sp.y + 4, sp.x + 4, sp.y - 4);
        }
    }
    
    /**
     * Should return the picked shape, but transformed as it is drawn
     * when rendered.
     * Maybe it should iterate throug available Shapes when called
     * multiple times on the same coordinates/shapes
     * @param x
     * @param y
     * @return 
     */
    private SVGElement pickSVGElement(int x, int y){
        try {
            List pickedElements = svgDiagramm.pick(new Point(x,y), null);
            if (pickedElements.size() > 0) {
                //TODO: don't always select last element, but toggle between
                Object o = pickedElements.get(pickedElements.size()-1);
                if (o instanceof List){
                    List l = (List) o;
                    Object o2 = l.get(l.size()-1);
                    if (o2 instanceof SVGElement){
                        return (SVGElement) o2;
                    }
                }
            }
            return null;
        } catch (SVGException ex) {
            Logger.getLogger(SVGPanel.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }
    
    public void mouseClicked(MouseEvent me) {
        if (me.getButton()==MouseEvent.BUTTON1 && svgDiagramm != null) {
            this.setSelectedSVGElement(pickSVGElement(me.getX(), me.getY()));
        }
    }
    private boolean movingStartPoint = false;

    public void mousePressed(MouseEvent me) {
        if (me.getPoint().distance(startPoint) <= 10) {
            movingStartPoint = true;
        }
    }

    public void mouseReleased(MouseEvent me) {
        if (movingStartPoint) {
            movingStartPoint = false;
            setStartPoint(me.getPoint());
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
