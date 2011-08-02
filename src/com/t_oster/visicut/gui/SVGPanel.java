/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.visicut.gui;

import com.kitfox.svg.SVGDiagram;
import com.kitfox.svg.SVGElement;
import com.kitfox.svg.SVGException;
import com.kitfox.svg.ShapeElement;
import com.kitfox.svg.app.beans.SVGIcon;
import com.t_oster.visicut.model.CuttingShape;
import com.t_oster.liblasercut.platform.Util;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Shape;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.net.URI;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JPanel;
import java.io.Serializable;

/**
 *
 * @author thommy
 */
public class SVGPanel extends JPanel implements MouseListener, MouseMotionListener, Serializable
{

  /**
   * Propertys
   */
  public static final String PROPERTY_STARTPOINT = "startPoint";
  public static final String PROPERTY_SELECTED_SVGELEMENT = "selectedSVGElement";
  public static final String PROPERTY_SHOWENGRAVINGPART = "showEngravingPart";
  public static final String PROPERTY_SHOWCUTTINGPART = "showCuttingPart";
  public static final String PROPERTY_SHOWIMAGE = "showImage";
  public static final String PROPERTY_SHOWGRID = "showGrid";
  public static final String PROPERTY_ZOOMFACTOR = "zoomFactor";
  public static final String PROPERTY_VIEWOFFSET = "viewOffset";
  public static final String PROPERTY_SCALEX = "scaleX";
  public static final String PROPERTY_SCALEY = "scaleY";
  private static final long serialVersionUID = 1L;
  private SVGIcon icon;
  private URI svgUri = null;
  private SVGDiagram svgDiagramm = null;
  private CuttingShape[] cuttingShapes;
  private SVGElement selectedSVGElement;
  private double zoomFactor = 1;
  private double scaleX = 1;
  private double scaleY = 1;
  private Point viewOffset = new Point(0, 0);
  private AffineTransform viewTransform = new AffineTransform();
  private AffineTransform inverseTransform = new AffineTransform();
  private int gridDPI = 500;
  private boolean showEngravingPart = true;
  private boolean showCuttingPart = true;
  private boolean showGrid = true;
  private Point startPoint = new Point(0, 0);
  /**
   * True iff moving view Point with mouse pressed
   */
  private boolean movingViewPoint = false;
  /**
   * Contains the Point where the mouse
   * was when initializing the moving
   * (used for calculating the vector)
   */
  private Point mouseStart = null;
  private boolean showImage = true;

  @Override
  public Dimension getPreferredSize()
  {
    if (this.svgUri == null)
    {
      return this.getMinimumSize();
    }
    return new Dimension((int) this.svgDiagramm.getWidth(), (int) this.svgDiagramm.getHeight());
  }

  @Override
  public Dimension getMinimumSize()
  {
    return new Dimension(300, 500);
  }

  public void setScaleX(Double x)
  {
    if (x != 0 && x != this.scaleX)
    {
      Double old = this.scaleX;
      this.scaleX = x;
      this.setTransform();
      this.repaint();
      this.firePropertyChange(PROPERTY_SCALEX, old, x);
    }
  }

  public Double getScaleX()
  {
    return this.scaleX;
  }

  public void setScaleY(Double y)
  {
    if (y != 0 && y != this.scaleY)
    {
      Double old = this.scaleY;
      this.scaleY = y;
      this.setTransform();
      this.repaint();
      this.firePropertyChange(PROPERTY_SCALEY, old, y);
    }
  }

  public Double getScaleY()
  {
    return this.scaleY;
  }

  /**
   * Refreshes the viewTransform
   * and inverseTransform to
   * reflect the current
   * zoomFactor and viewOffset
   */
  private void setTransform()
  {
    try
    {
      this.viewTransform = AffineTransform.getScaleInstance(scaleX, scaleY);
      this.viewTransform.concatenate(AffineTransform.getScaleInstance(zoomFactor, zoomFactor));
      this.viewTransform.concatenate(AffineTransform.getTranslateInstance(viewOffset.getX(), viewOffset.getY()));
      this.inverseTransform = this.viewTransform.createInverse();
    }
    catch (NoninvertibleTransformException ex)
    {
      Logger.getLogger(SVGPanel.class.getName()).log(Level.SEVERE, null, ex);
    }
  }

  public void setZoomFactor(double zf)
  {
    if (zf != this.zoomFactor)
    {
      double old = this.zoomFactor;
      this.zoomFactor = zf;
      this.setTransform();
      this.repaint();
      firePropertyChange(PROPERTY_ZOOMFACTOR, old, this.zoomFactor);
    }
  }

  public double getZoomFactor()
  {
    return this.zoomFactor;
  }

  public void setViewOffset(Point p)
  {
    if (Util.differ(p, this.viewOffset))
    {
      Point old = this.viewOffset;
      this.viewOffset = p;
      this.setTransform();
      this.repaint();
      firePropertyChange(PROPERTY_VIEWOFFSET, old, this.viewOffset);
    }
  }

  public Point getViewOffset()
  {
    return this.viewOffset;
  }

  public void setStartPoint(Point p)
  {
    if (Util.differ(p, this.startPoint))
    {
      Point old = this.startPoint;
      this.startPoint = p;
      repaint();
      this.firePropertyChange(PROPERTY_STARTPOINT, old, p);
    }
  }

  public Point getStartPoint()
  {
    return this.startPoint;
  }

  private void sizeToFit()
  {
    Dimension d = getPreferredSize();
    setSize(d.width, d.height);
    Component p = getParent();
    if (p != null)
    {
      p.invalidate();
      p.doLayout();
    }
  }

  public SVGPanel()
  {
    this.addMouseListener(this);
    this.addMouseMotionListener(this);
    this.cuttingShapes = null;
  }

  public void setShowImage(boolean image)
  {
    if (this.showImage != image)
    {
      this.showImage = image;
      repaint();
      firePropertyChange(PROPERTY_SHOWIMAGE, !showImage, showImage);
    }
  }

  public boolean getShowImage()
  {
    return this.showImage;
  }

  public void setShowEngravingPart(boolean show)
  {
    if (show != showEngravingPart)
    {
      showEngravingPart = show;
      repaint();
      firePropertyChange(PROPERTY_SHOWENGRAVINGPART, !showEngravingPart, showEngravingPart);
    }
  }

  public boolean isShowEngravingPart()
  {
    return showEngravingPart;
  }

  public void setShowCuttingPart(boolean show)
  {
    if (show != showCuttingPart)
    {
      showCuttingPart = show;
      repaint();
      firePropertyChange(PROPERTY_SHOWCUTTINGPART, !showCuttingPart, showCuttingPart);
    }
  }

  public boolean isShowCuttingPart()
  {
    return showCuttingPart;
  }

  public void setShowGrid(boolean show)
  {
    if (show != this.showGrid)
    {
      this.showGrid = show;
      this.repaint();
      firePropertyChange(PROPERTY_SHOWGRID, !showGrid, showGrid);
    }
  }

  public boolean isShowGrid()
  {
    return this.showGrid;
  }

  public void setSvgUri(URI diag)
  {
    if (Util.differ(diag, this.svgUri))
    {
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
      this.setTransform();
      this.repaint();
    }
  }

  public URI getSvgUri()
  {
    return this.svgUri;
  }

  public void setCuttingShapes(CuttingShape[] cuttingShapes)
  {
    this.cuttingShapes = cuttingShapes;
  }

  public CuttingShape[] getCuttingShapes()
  {
    return this.cuttingShapes;
  }

  public boolean isCuttingShapeSelected()
  {
    SVGElement sel = this.getSelectedSVGElement();
    if (sel != null && this.cuttingShapes != null && sel instanceof ShapeElement)
    {
      for (CuttingShape cs : this.cuttingShapes)
      {
        if (cs.getShapeElement().equals((ShapeElement) sel))
        {
          return true;
        }
      }
    }
    return false;
  }

  public void setSelectedSVGElement(SVGElement s)
  {
    if (Util.differ(s, selectedSVGElement))
    {
      SVGElement old = selectedSVGElement;
      this.selectedSVGElement = s;
      this.repaint();
      firePropertyChange(PROPERTY_SELECTED_SVGELEMENT, old, s);
      boolean cse = this.isCuttingShapeSelected();
      firePropertyChange("cuttingShapeSelected", !cse, cse);
    }

  }

  public SVGElement getSelectedSVGElement()
  {
    return selectedSVGElement;
  }

  public void setGridDPI(int dpi)
  {
    if (this.gridDPI != dpi)
    {
      this.gridDPI = dpi;
      this.repaint();
    }
  }

  public int getGridDPI()
  {
    return this.gridDPI;
  }

  /**
   * Draws a given Shape on a Graphics g but just using the line
   * operation (to simulate behavior on lasercutters)
   * @param g
   * @param shape 
   */
  private void drawShape(Graphics g, Shape shape)
  {
    int x = 0;
    int y = 0;
    PathIterator iter = shape.getPathIterator(null, 0.4);
    while (!iter.isDone())
    {
      double[] test = new double[8];
      int result = iter.currentSegment(test);
      if (result == PathIterator.SEG_MOVETO)
      {
        x = (int) test[0];
        y = (int) test[1];
      }
      else
      {
        if (result == PathIterator.SEG_LINETO)
        {
          g.drawLine(x, y, (int) test[0], (int) test[1]);
          x = (int) test[0];
          y = (int) test[1];
        }
      }
      iter.next();
    }
  }

  private void drawGrid(Graphics g, int rasterDPI, int rasterWidth)
  {
    int width = getWidth();
    int height = getHeight();
    g.setColor(Color.BLACK);
    Point sp = this.getStartPoint();
    Point2D tmp = viewTransform.transform(sp, null);
    sp = new Point((int) (tmp.getX()), (int) (tmp.getY()));
    rasterDPI *= zoomFactor;
    for (int mm = 0; mm < Util.px2mm(width - sp.x, rasterDPI); mm += rasterWidth)
    {
      int lx = sp.x + (int) Util.mm2px(mm, rasterDPI);
      g.drawLine(lx, 0, lx, height);
      String txt = mm + " mm";
      int w = g.getFontMetrics().stringWidth(txt);
      int h = g.getFontMetrics().getHeight();
      g.setColor(getBackground());
      g.fillRect(lx - w / 2, (int) (height - 1.8 * h), w, h);
      g.setColor(Color.BLACK);
      g.drawString(txt, lx - w / 2, height - h);
    }
    for (int mm = rasterWidth; mm < Util.px2mm(sp.x, rasterDPI); mm += rasterWidth)
    {
      int lx = sp.x - (int) Util.mm2px(mm, rasterDPI);
      g.drawLine(lx, 0, lx, height);
      String txt = "-" + mm + " mm";
      int w = g.getFontMetrics().stringWidth(txt);
      int h = g.getFontMetrics().getHeight();
      g.setColor(getBackground());
      g.fillRect(lx - w / 2, (int) (height - 1.8 * h), w, h);
      g.setColor(Color.BLACK);
      g.drawString(txt, lx - w / 2, height - h);
    }
    for (int mm = 0; mm < Util.px2mm(height - sp.y, rasterDPI); mm += rasterWidth)
    {
      int ly = sp.y + (int) Util.mm2px(mm, rasterDPI);
      g.drawLine(0, ly, width, ly);
      String txt = mm + " mm";
      int w = g.getFontMetrics().stringWidth(txt);
      int h = g.getFontMetrics().getHeight();
      g.setColor(getBackground());
      g.fillRect(width - w, ly - h / 2, w, h);
      g.setColor(Color.BLACK);
      g.drawString(txt, width - w, ly + h / 3);
    }
    for (int mm = rasterWidth; mm < Util.px2mm(sp.y, rasterDPI); mm += rasterWidth)
    {
      int ly = sp.y - (int) Util.mm2px(mm, rasterDPI);
      g.drawLine(0, ly, width, ly);
      String txt = "-" + mm + " mm";
      int w = g.getFontMetrics().stringWidth(txt);
      int h = g.getFontMetrics().getHeight();
      g.setColor(getBackground());
      g.fillRect(width - w, ly - h / 2, w, h);
      g.setColor(Color.BLACK);
      g.drawString(txt, width - w, ly + h / 3);
    }
  }

  @Override
  public void paintComponent(Graphics gg)
  {
    super.paintComponent(gg);
    Graphics2D g = (Graphics2D) gg;

    final int width = getWidth();
    final int height = getHeight();

    //Background
    g.setColor(getBackground());
    g.fillRect(0, 0, width, height);

    if (showGrid && gridDPI != 0)
    {
      drawGrid(g, gridDPI, (int) Math.max(10 / zoomFactor, 1));
    }

    //Shapes and Icon have to be drawn zoomed
    g.transform(viewTransform);
    if (icon != null && showImage)
    {
      icon.paintIcon(this, g, 0, 0);
    }
    if (cuttingShapes != null && showCuttingPart)
    {
      g.setColor(Color.RED);
      for (CuttingShape shape : cuttingShapes)
      {
        try
        {
          drawShape(g, shape.getTransformedShape());
        }
        catch (SVGException ex)
        {
          Logger.getLogger(SVGPanel.class.getName()).log(Level.SEVERE, null, ex);
        }
      }
    }
    if (selectedSVGElement != null && selectedSVGElement instanceof ShapeElement)
    {
      try
      {
        g.setColor(Color.GREEN);
        drawShape(g, CuttingShape.extractTransformedShape((ShapeElement) selectedSVGElement));
      }
      catch (SVGException ex)
      {
        Logger.getLogger(SVGPanel.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
    try
    {
      g.transform(viewTransform.createInverse());
      //Draw StartingPoint
    }
    catch (NoninvertibleTransformException ex)
    {
      Logger.getLogger(SVGPanel.class.getName()).log(Level.SEVERE, null, ex);
    }
    //Draw StartingPoint

    Point sp = startPoint;
    if (movingStartPoint)
    {
      sp = this.getMousePosition();
    }
    else
    {
      Point2D tmp = viewTransform.transform(sp, null);
      sp = new Point((int) tmp.getX(), (int) tmp.getY());
    }
    if (sp != null)
    {
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
   * Maybe it should iterate through available Shapes when called
   * multiple times on the same coordinates/shapes
   * @param x
   * @param y
   * @return 
   */
  private SVGElement pickSVGElement(int x, int y)
  {
    try
    {
      List pickedElements = svgDiagramm.pick(new Point(x, y), null);
      if (pickedElements.size() > 0)
      {
        //TODO: don't always select last element, but toggle between
        Object o = pickedElements.get(pickedElements.size() - 1);
        if (o instanceof List)
        {
          List l = (List) o;
          Object o2 = l.get(l.size() - 1);
          if (o2 instanceof SVGElement)
          {
            return (SVGElement) o2;
          }
        }
      }
      return null;
    }
    catch (SVGException ex)
    {
      Logger.getLogger(SVGPanel.class.getName()).log(Level.SEVERE, null, ex);
      return null;
    }
  }

  public void mouseClicked(MouseEvent me)
  {
    if (me.getButton() == MouseEvent.BUTTON1 && svgDiagramm != null)
    {
      Point2D tmp = inverseTransform.transform(me.getPoint(), null);
      this.setSelectedSVGElement(pickSVGElement((int) tmp.getX(), (int) tmp.getY()));
    }
  }
  private boolean movingStartPoint = false;

  public void mousePressed(MouseEvent me)
  {

    if (me.getPoint().distance(viewTransform.transform(startPoint, null)) <= 10)
    {
      movingStartPoint = true;
    }
    else
    {
      movingViewPoint = true;
      mouseStart = me.getPoint();
    }
  }

  public void mouseReleased(MouseEvent me)
  {
    if (movingStartPoint)
    {
      movingStartPoint = false;
      Point2D tmp = inverseTransform.transform(me.getPoint(), null);
      setStartPoint(new Point((int) tmp.getX(), (int) tmp.getY()));
    }
    else
    {
      if (movingViewPoint)
      {
        Point moved = new Point((int) (me.getX() - mouseStart.getX()), (int) (me.getY() - mouseStart.getY()));
        this.setViewOffset(new Point(this.viewOffset.x + moved.x, this.viewOffset.y + moved.y));
      }
    }
  }

  public void mouseEntered(MouseEvent me)
  {
  }

  public void mouseExited(MouseEvent me)
  {
  }

  public void mouseDragged(MouseEvent me)
  {
    if (movingStartPoint)
    {
      this.repaint();
    }
  }

  public void mouseMoved(MouseEvent me)
  {
    if (me.getPoint().distance(viewTransform.transform(startPoint, null)) <= 10)
    {
      this.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }
    else
    {
      this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }
  }
}
