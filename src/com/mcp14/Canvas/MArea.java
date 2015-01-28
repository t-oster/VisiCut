package com.mcp14.Canvas;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Comparator;
import com.mcp14.Utilities.Utils;

/**
 * Represents a piece to be placed inside a Bin.
 * 
 * @author Sughosh Kumar
 */
public class MArea extends Area {
    /**
     * Area measure.
     */
    private double area;

    private int ID;
    /**
     * Accumulate rotation in degrees of this MArea
     */
    private double rotation;

    /**
     * Creates an MArea based on a Path2D previously constructed
     * 
     * @param path
     *            Path2D of this MAreq previously constructed
     * @param ID
     *            identification for this MArea
     */
    public MArea(Path2D path, int ID) {
	super(path);
	computeArea();
	this.ID = ID;
	rotation = 0;
    }

    /**
     * Creates an MArea based on a MArea previously constructed
     * 
     * @param area
     *            MArea from which we are going to construct this MArea
     * @param ID
     *            identification for this MArea
     */
    public MArea(MArea area, int ID) {
	super(area);
	this.area = area.area;
	this.ID = ID;
	rotation = area.getRotation();
    }

    /**
     * Creates an MArea based on a Rectangle previously constructed
     * 
     * @param rectangle
     *            from which we are going to construct this MArea
     * @param ID
     *            identification for this MArea
     * @see Rectangle
     */
    public MArea(Rectangle rectangle, int ID) {
	super(rectangle);
	this.area = rectangle.getWidth() * rectangle.getHeight();
	this.ID = ID;
	rotation = 0;
    }

    /**
     * Creates an MArea based on a double precision Rectangle previously
     * constructed
     * 
     * @param rectangle
     *            from which we are going to construct this MArea
     * @param ID
     *            identification for this MArea
     * @see Rectangle2D.Double
     */
    public MArea(Rectangle2D.Double rectangle, int ID) {
	super(rectangle);
	this.area = rectangle.getWidth() * rectangle.getHeight();
	this.ID = ID;
	rotation = 0;
    }

    /**
     * Creates an empty MArea with an ID.
     * 
     * @param ID
     */
    public MArea(int ID) {
	super();
	this.area = 0;
	this.ID = ID;
	rotation = 0;
    }

    /**
     * Creates an empty area without ID.
     */
    public MArea() {
	super();
	this.area = 0;
	rotation = 0;
    }

    /**
     * Creates an MArea with a hole.
     * 
     * @param outer
     *            MArea describing the outer MArea of this piece
     * @param inner
     *            MArea describing the inner MArea of this piece
     */
    public MArea(MArea outer, MArea inner) {
	super(MAreaHolesConstructor(outer, inner));
	this.ID = outer.getID();
	computeArea();
	rotation = 0;
    }

    private static MArea MAreaHolesConstructor(MArea outer, MArea inner) {
	MArea area = new MArea(outer, outer.getID());
	area.subtract(inner);
	return area;
    }

    /**
     * Creates this MArea from a set of points.
     * 
     * @param points
     *            describing the contour of this MArea
     * @param ID
     *            identifier for this MArea
     */
    public MArea(MPointDouble[] points, int ID) {
	super(Utils.createShape(points));
	this.ID = ID;
	computeArea();
	rotation = 0;
    }

    /**
     * Returns the area of this area using algorithm found at:
     * http://paulbourke.net/geometry/polygonmesh/source1.c At the same time,
     * updates this.area
     * 
     * @return calculated area of this MArea
     */
    private double computeArea() {
	double area = 0;
	MPointDouble[] points = getPoints();
	for (int i = 0; i < points.length; i++) {
	    int j = (i + 1) % points.length;
	    area += points[i].x * points[j].y;
	    area -= points[i].y * points[j].x;
	}

	area /= 2;
	this.area = area < 0 ? -area : area;
	return this.area;
    }

    /**
     * Method for calculating the bounding box of this MArea in integer
     * precision
     * 
     * @return Bounding box rectangle of this MArea in integer precision.
     */
    public Rectangle getBoundingBox() {
	return (Rectangle) this.getBounds();
    }

    /**
     * Method for calculating the bounding box of this MArea in double precision
     * 
     * @return Bounding box rectangle of this MArea in double precision.
     * @see Rectangle2D.Double
     */
    public Rectangle2D.Double getBoundingBox2D() {
	return (Rectangle2D.Double) this.getBounds2D();
    }

    /**
     * Computes theoretical free space of this MArea, that is, area of the
     * bounding box of this MArea minus the area of the MArea itself. The more
     * irregular is the piece, the bigger this theoretical free area will be.
     * 
     * @return theoretical free area
     */
    public double getFreeArea() {
	updateArea();
	Rectangle boundingBox = getBoundingBox();
	double res = (boundingBox.getWidth() * boundingBox.getHeight()) - (this.area);
	return res;
    }

    /**
     * @return all contour points in this area, without repetition.
     */
    public MPointDouble[] getPoints() {
	ArrayList<MPointDouble> points = new ArrayList<MPointDouble>();
	for (PathIterator pi = this.getPathIterator(null); !pi.isDone(); pi.next()) {
	    double[] coordinates = new double[6];
	    int type = pi.currentSegment(coordinates);
	    if (type != PathIterator.SEG_CLOSE) {
		MPointDouble p = new MPointDouble(coordinates[0], coordinates[1]);
		points.add(p);
	    }
	}
	MPointDouble[] res = points.toArray(new MPointDouble[0]);
	return res;
    }

    /**
     * 
     * @return area measure of this MArea
     */
    public double getArea() {
	return area;
    }

    /**
     * Updates the area measure of this MArea
     */
    public void updateArea() {
	computeArea();
    }

    /**
     * 
     * @return ID of this MArea
     */
    public int getID() {
	return ID;
    }

    /**
     * Draws this area based on the current view port dimensions
     * 
     * @param Dimension
     *            binDimension
     * @param Dimension
     *            viewPortDimension
     * @param Graphics
     *            g
     */
    public void drawInViewPort(Dimension binDimension, Dimension viewPortDimension, Graphics g) {
	double xFactor = viewPortDimension.getWidth() / binDimension.getWidth();
	double yFactor = viewPortDimension.getHeight() / binDimension.getHeight();
	AffineTransform transform = new AffineTransform();
	transform.scale(xFactor, yFactor);
	transform.translate(11, 11);
	Area newArea = this.createTransformedArea(transform);
	Graphics2D g2d = (Graphics2D) g;
	g2d.setColor(Color.BLACK);
	g2d.setStroke(new BasicStroke(2));
	g2d.draw(newArea);
	g2d.setColor(Color.LIGHT_GRAY);
	g2d.fill(newArea);
    }

    /**
     * Equality test based on ID
     * 
     * @return true if ID's are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
	MArea other = (MArea) obj;
	return this.ID == other.ID;
    }

    /**
     * Moves this MArea based on the provided movement vector
     * 
     * @param vector
     *            containing the movement information
     */
    public void move(MVector vector) {
	AffineTransform transform = new AffineTransform();
	transform.translate(vector.getX(), vector.getY());
	this.transform(transform);
    }

    /**
     * Places this MArea's bounding box upper left corner in x,y
     * 
     * @param x
     * @param y
     */
    public void placeInPosition(double x, double y) {
	Rectangle bb = getBoundingBox();
	AffineTransform transform = new AffineTransform();
	double thisX = bb.getX();
	double dx = Math.abs(thisX - x);
	if (thisX <= x) {
	    thisX = dx;
	} else {
	    thisX = -dx;
	}
	double thisY = bb.getY();
	double dy = Math.abs(thisY - y);
	if (thisY <= y) {
	    thisY = dy;
	} else {
	    thisY = -dy;
	}
	transform.translate(thisX, thisY);
	this.transform(transform);
    }

    /**
     * Rotates this area, in degrees.
     * 
     * @param degrees
     *            to rotate the piece
     */
    public void rotate(double degrees) {
	this.rotation += degrees;
	if (this.rotation >= 360) {
	    this.rotation -= 360;
	}
	AffineTransform transform = new AffineTransform();
	Rectangle rectangle = getBoundingBox();
	transform.rotate(Math.toRadians(degrees), rectangle.getX() + rectangle.width / 2, rectangle.getY() + rectangle.height / 2);
	this.transform(transform);
    }

    /**
     * Verifies if this area intersects with other area
     * 
     * @param other
     * @return true if intersects, false otherwise
     */

    public boolean intersection(MArea other) {
	MArea intersArea = new MArea(this, this.ID);
	intersArea.intersect(other);
	return !intersArea.isEmpty();
    }

    /**
     * Tests if this area is inside the rectangle.
     * 
     * @param rectangle
     *            container
     * @return true if this area is inside the rectangle, false otherwise
     */
    public boolean isInside(Rectangle rectangle) {
	Rectangle boundingBox = getBoundingBox();
	// left bounds
	double leftX = boundingBox.getX();
	double leftY = boundingBox.getY();
	if (leftX < rectangle.getX()) {
	    return false;
	}
	if (leftY < rectangle.getY()) {
	    return false;
	}
	// right bounds
	double rightX = boundingBox.getX() + boundingBox.getWidth();
	double rightY = boundingBox.getY() + boundingBox.getHeight();
	if (rightX > (rectangle.getX() + rectangle.getWidth())) {
	    return false;
	}
	if (rightY > (rectangle.getY() + rectangle.getHeight())) {
	    return false;
	}
	return true;
    }

    /**
     * Tests if this area's lowest Y is above the rectangle's lowest Y
     * coordinate
     * 
     * @param rectangle
     *            to test
     * @return true if this area is above the rectangle's lowest Y bound
     */
    public boolean isAbove(Rectangle rectangle) {
	Rectangle boundingBox = getBoundingBox();
	double leftY = boundingBox.getY() + this.getBoundingBox().getHeight();
	if (leftY >= (rectangle.getY() + rectangle.getHeight())) {
	    return false;
	}
	return true;
    }

    /**
     * Tests if this area's has an X value smaller than the rectangle's X
     * biggest bound
     * 
     * @param rectangle
     *            to test
     * @return true if this area is to the right of the rectangle's lowest X
     *         bound.
     */
    public boolean isToLeft(Rectangle rectangle) {
	Rectangle boundingBox = getBoundingBox();
	if (boundingBox.getMinX() > rectangle.getMaxX()) {
	    return false;
	}
	return true;
    }

    public double getRotation() {
	return rotation;
    }

    public static final Comparator<MArea> BY_AREA = new ByArea();

    public static final Comparator<MArea> BY_BOUNDING_BOX_AREA = new ByBoundingBoxArea();

    /**
     * Provides an area based comparison between two MAreas. It is assumed that
     * the area measure is up to date at the time of this comparison
     */
    private static class ByArea implements Comparator<MArea> {
	@Override
	public int compare(MArea o1, MArea o2) {
	    if (o1.area > o2.area) {
		return 1;
	    }
	    if (o1.area < o2.area) {
		return -1;
	    }
	    return 0;
	}
    }

    /**
     * Provides a bounding box based comparison between two MAreas.
     */
    private static class ByBoundingBoxArea implements Comparator<MArea> {
	@Override
	public int compare(MArea o1, MArea o2) {
	    Rectangle o1BB = o1.getBoundingBox();
	    Rectangle o2BB = o2.getBoundingBox();
	    double bb1 = o1BB.getWidth() * o1BB.getHeight();
	    double bb2 = o2BB.getWidth() * o2BB.getHeight();
	    if (bb1 < bb2) {
		return -1;
	    }
	    if (bb1 > bb2) {
		return 1;
	    }
	    return 0;
	}
    }
}