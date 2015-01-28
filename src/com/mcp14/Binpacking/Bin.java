package com.mcp14.Binpacking;

import com.mcp14.Canvas.MPointDouble;
import com.mcp14.Canvas.MVector;
import com.mcp14.Canvas.MArea;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import com.mcp14.Utilities.Utils;
/**
 * Class used to describe a Bin object.
 * 
 * @author Sughosh Kumar
 */
public class Bin {
    /**
     * Bin dimensions.
     */
    private Dimension dimension;

    /**
     * Pieces contained in the bin.
     */
    private MArea[] placedPieces;

    /**
     * Number of pieces placed in the bin.
     */
    private int NPlaced;

    /**
     * Rectangular holes in the bin
     */
    private ArrayList<Rectangle2D.Double> freeRectangles = new ArrayList<java.awt.geom.Rectangle2D.Double>();

    /**
     * Initializes this bin with the specified dimensions.
     * 
     * @param dimension
     *            dimensions for this bin.
     */
    public Bin(Dimension dimension) {
	this.dimension = new Dimension(dimension.width, dimension.height);
	NPlaced = 0;
	freeRectangles.add(new Rectangle2D.Double(0, 0, dimension.getWidth(), dimension.getHeight()));
    }

    /**
     * Get the placed pieces.
     * 
     * @return placed pieces.
     */
    public MArea[] getPlacedPieces() {
	return placedPieces;
    }

    /**
     * Get the number of pieces placed.
     * 
     * @return number of pieces placed.
     */
    public int getNPlaced() {
	return NPlaced;
    }

    /**
     * Computes the occupied area as the sum of areas of the placed pieces.
     * 
     * @return occupied area.
     */
    public double getOccupiedArea() {
	double occ = 0;
	for (int i = 0; i < NPlaced; i++) {
	    placedPieces[i].updateArea();
	    occ += placedPieces[i].getArea();
	}
	return occ;
    }

    /**
     * Get the dimensions.
     * 
     * @return dimensions.
     */
    public Dimension getDimension() {
	return dimension;
    }

    /**
     * Computes the empty area as the total area minus the occupied area.
     * 
     * @return empty area in this bin
     */
    public double getEmptyArea() {
	double occ = 0;
	for (int i = 0; i < NPlaced; i++) {
	    occ += placedPieces[i].getArea();
	}
	return dimension.getWidth() * dimension.getHeight() - occ;
    }

    /**
     * Performs the complete bounding box based strategies to place the pieces
     * inside this bin.
     * 
     * @param toPlace
     *            pieces to be placed inside this bin.
     * @return the pieces that could not be placed inside the bin.
     */
    public MArea[] BBCompleteStrategy(MArea[] toPlace) {
	int lastIndex = 0;
	MArea[] stillNotPlaced = boundingBoxPacking(toPlace);
	boolean movement = moveAndReplace(lastIndex);
	while (movement) {
	    lastIndex = this.placedPieces.length;
	    stillNotPlaced = boundingBoxPacking(stillNotPlaced);
	    movement = moveAndReplace(lastIndex);

	}
	return stillNotPlaced;
    }

    /**
     * Places the pieces inside the bin using the maximal rectangles strategy.
     * Method called from {@link #BBCompleteStrategy}
     * 
     * @param pieces
     *            pieces to be placed.
     * @return the pieces that could not be placed inside the bin.
     */
    private MArea[] boundingBoxPacking(MArea[] pieces) {
	ArrayList<MArea> placedPieces = new ArrayList<MArea>();
	ArrayList<MArea> notPlacedPieces = new ArrayList<MArea>();

	Arrays.sort(pieces, MArea.BY_AREA);

	MArea total = new MArea();
	if (this.placedPieces != null) {
	    for (MArea a : this.placedPieces) {
		total.add(a);
		placedPieces.add(a);
	    }
	}

	for (int i = pieces.length - 1; i >= 0; i--) {
	    int where = findWhereToPlace(pieces[i], freeRectangles);
	    if (where != -1) {
		Rectangle2D.Double freeRect = freeRectangles.get(where);
		MArea placed = new MArea(pieces[i], pieces[i].getID());
		placed.placeInPosition(freeRect.getX(), freeRect.getMaxY() - placed.getBoundingBox().getHeight());
		if (!placed.intersection(total)) {
		    Rectangle2D.Double pieceBB = placed.getBoundingBox2D();
		    splitScheme(freeRect, pieceBB, freeRectangles);
		    computeFreeRectangles(pieceBB, freeRectangles);
		    eliminateNonMaximal();
		    placedPieces.add(placed);
		    total.add(placed);
		} else {
		    notPlacedPieces.add(pieces[i]);
		}
	    } else {
		notPlacedPieces.add(pieces[i]);
	    }
	}

	this.placedPieces = placedPieces.toArray(new MArea[0]);
	NPlaced = this.placedPieces.length;
	return notPlacedPieces.toArray(new MArea[0]);

    }

    /**
     * Finds in which free rectangular space the specified piece can be placed.
     * Method called from {@link #boundingBoxPacking}
     * 
     * @param piece
     *            piece to place inside an empty rectangular space.
     * @param freeRectangles
     *            list of empty rectangular spaces.
     * @return <ul>
     *         <li><b>-1</b> if not valid position was found.</li>
     *         <li>
     *         <b>position</b> where the piece can be placed, otherwise.</li>
     *         </ul>
     */
    private int findWhereToPlace(MArea piece, ArrayList<Rectangle2D.Double> freeRectangles) {
	boolean lastRotated = false;
	Rectangle2D pieceBB = piece.getBoundingBox2D();
	int res = -1;
	double min = Double.MAX_VALUE;
	for (int i = freeRectangles.size() - 1; i >= 0; i--) {
	    Rectangle2D.Double freeRect = freeRectangles.get(i);
	    if (Utils.fits(pieceBB, freeRect)) {
		double m = Math.min(freeRect.getWidth() - pieceBB.getWidth(), freeRect.getHeight() - pieceBB.getHeight());
		if (m < min) {
		    min = m;
		    res = i;
		    if (lastRotated) {
			piece.rotate(90);
			lastRotated = false;
		    }

		}
	    }
	    if (Utils.fitsRotated(pieceBB, freeRect)) {
		double m = Math.min(freeRect.getWidth() - pieceBB.getHeight(), freeRect.getHeight() - pieceBB.getWidth());
		if (m < min) {
		    min = m;
		    res = i;
		    if (!lastRotated) {
			piece.rotate(90);
			lastRotated = true;
		    }
		}
	    }
	}
	return res;
    }

    /**
     * Divides the rectangular space where a piece was just placed following the
     * maximal rectangles splitting strategy. Method called from
     * {@link #boundingBoxPacking}
     * 
     * @param usedFreeArea
     *            rectangular area that contains the newly placed piece.
     * @param justPlacedPieceBB
     *            bounding box of the newly placed piece.
     * @param freeRectangles
     *            list of free spaces in the bin.
     */
    private void splitScheme(Rectangle2D.Double usedFreeArea, Rectangle2D.Double justPlacedPieceBB, ArrayList<Rectangle2D.Double> freeRectangles) {
	freeRectangles.remove(usedFreeArea);
	// top
	double widht = usedFreeArea.getWidth();
	double height = justPlacedPieceBB.getY() - usedFreeArea.getY();
	if (height > 0) {
	    Rectangle2D.Double upR = new Rectangle2D.Double(usedFreeArea.getX(), usedFreeArea.getY(), widht, height);
	    freeRectangles.add(upR);
	}
	// right
	widht = usedFreeArea.getMaxX() - justPlacedPieceBB.getMaxX();
	height = usedFreeArea.getHeight();
	if (widht > 0) {
	    Rectangle2D.Double rightR = new Rectangle2D.Double(justPlacedPieceBB.getMaxX(), usedFreeArea.getY(), widht, height);
	    freeRectangles.add(rightR);
	}
    }

    /**
     * Recalculates the free rectangular boxes in the bin. Method called after a
     * piece has been placed. Method called from {@link #boundingBoxPacking}
     * 
     * @param justPlacedPieceBB
     *            bounding box of the piece that was just added.
     * @param freeRectangles
     *            free rectangular boxes in the bin.
     */
    private void computeFreeRectangles(Rectangle2D.Double justPlacedPieceBB, ArrayList<Rectangle2D.Double> freeRectangles) {
	Rectangle2D.Double[] rects = freeRectangles.toArray(new Rectangle2D.Double[0]);
	for (Rectangle2D.Double freeR : rects) {
	    if (freeR.intersects(justPlacedPieceBB)) {
		freeRectangles.remove(freeR);
		Rectangle2D rIntersection = freeR.createIntersection(justPlacedPieceBB);
		// top
		double widht = freeR.getWidth();
		double height = rIntersection.getY() - freeR.getY();
		if (height > 0) {
		    Rectangle2D.Double upR = new Rectangle2D.Double(freeR.getX(), freeR.getY(), widht, height);
		    freeRectangles.add(upR);
		}

		// left
		widht = rIntersection.getX() - freeR.getX();
		height = freeR.getHeight();
		if (widht > 0) {
		    Rectangle2D.Double leftR = new Rectangle2D.Double(freeR.getX(), freeR.getY(), widht, height);
		    freeRectangles.add(leftR);
		}

		// bottom
		widht = freeR.getWidth();
		height = freeR.getMaxY() - rIntersection.getMaxY();
		if (height > 0) {
		    Rectangle2D.Double bottomR = new Rectangle2D.Double(freeR.getX(), rIntersection.getMaxY(), widht, height);
		    freeRectangles.add(bottomR);
		}

		// right
		widht = freeR.getMaxX() - rIntersection.getMaxX();
		height = freeR.getHeight();
		if (widht > 0) {
		    Rectangle2D.Double rightR = new Rectangle2D.Double(rIntersection.getMaxX(), freeR.getY(), widht, height);
		    freeRectangles.add(rightR);
		}
	    }
	}

    }

    /**
     * Eliminates all non-maximal boxes from the empty spaces in the bin. Method
     * called from {@link #boundingBoxPacking}
     */
    private void eliminateNonMaximal() {
	Rectangle2D.Double[] freeRectArray = freeRectangles.toArray(new Rectangle2D.Double[0]);
	Arrays.sort(freeRectArray, RECTANGLE_AREA_COMPARATOR);
	freeRectangles.clear();
	for (int i = 0; i < freeRectArray.length; i++) {
	    boolean contained = false;
	    for (int j = freeRectArray.length - 1; j >= i; j--) {
		if (j != i && freeRectArray[j].contains(freeRectArray[i])) {
		    contained = true;
		    break;
		}
	    }
	    if (!contained)
		freeRectangles.add(freeRectArray[i]);
	}
    }

    /**
     * Second strategy applied to bounding box methods: try to place already
     * placed pieces inside other placed pieces. Method called from
     * {@link #BBCompleteStrategy}
     * 
     * @param indexLimit
     *            lower left limit when applying this strategy.
     * @return <ul>
     *         <li><b>true</b> if some piece changed its position, that is, if
     *         was placed inside other.</li>
     *         <li><b>false</b> otherwise.</li>
     *         </ul>
     */

    private boolean moveAndReplace(int indexLimit) {
	boolean movement = false;
	MArea total = new MArea();
	for (MArea area : this.placedPieces)
	    total.add(area);
	int currentIndex = this.placedPieces.length - 1;
	while (currentIndex >= indexLimit) {
	    MArea currentArea = this.placedPieces[currentIndex];
	    total.subtract(currentArea);
	    for (int i = 0; i < currentIndex; i++) {
		MArea container = this.placedPieces[i];
		if (container.getFreeArea() > currentArea.getArea()) {
		    MArea auxArea = new MArea(currentArea, currentArea.getID());
		    Rectangle2D.Double contBB = container.getBoundingBox2D();
		    auxArea.placeInPosition(contBB.getX(), contBB.getY());
		    auxArea = sweep(container, auxArea, total);
		    if (auxArea != null) {
			freeRectangles.add(currentArea.getBoundingBox2D());
			compress(total, auxArea, new Rectangle(dimension), new MVector(-1, 1));
			placedPieces[currentIndex] = auxArea;
			computeFreeRectangles(auxArea.getBoundingBox2D(), freeRectangles);
			eliminateNonMaximal();
			movement = true;
			break;
		    } else {
			auxArea = new MArea(currentArea, currentArea.getID());
			auxArea.rotate(90);
			auxArea.placeInPosition(contBB.getX(), contBB.getY());
			auxArea = sweep(container, auxArea, total);
			if (auxArea != null) {
			    freeRectangles.add(currentArea.getBoundingBox2D());
			    compress(total, auxArea, new Rectangle(dimension), new MVector(-1, 1));
			    placedPieces[currentIndex] = auxArea;
			    computeFreeRectangles(auxArea.getBoundingBox2D(), freeRectangles);
			    eliminateNonMaximal();
			    movement = true;
			    break;
			}
		    }
		}
	    }
	    total.add(placedPieces[currentIndex]);
	    currentIndex--;
	}

	return movement;

    }

    /**
     * Sweeps a piece along the interior of a container searching for a
     * non-overlapping position. Method called from {@link #moveAndReplace}
     * 
     * @param container
     *            container that contains the piece to sweep.
     * @param inside
     *            piece to place inside the container.
     * @param collisionArea
     *            areas that cannot intersect with the new piece.
     * @return <ul>
     *         <li><b>null</b> if not valid position was found.</li>
     *         <li>
     *         <b>MArea</b> new MArea placed in the valid position.</li>
     *         </ul>
     * 
     */
    private MArea sweep(MArea container, MArea inside, MArea collisionArea) {
	if (!inside.intersection(collisionArea))
	    if (inside.isToLeft(container.getBoundingBox()))
		return inside;
	MArea lastValidPosition = null;
	double Xcontainer = container.getBoundingBox().getX();
	double dx = inside.getBoundingBox().getWidth() / Constants.DX_SWEEP_FACTOR;
	double dy = inside.getBoundingBox().getHeight() / Constants.DY_SWEEP_FACTOR;
	Rectangle2D.Double containerBB = container.getBoundingBox2D();
	MArea originalArea = new MArea(inside, inside.getID());
	while (true) {
	    boolean check = false;
	    inside.move(new MVector(dx, 0));
	    if (!inside.intersection(collisionArea) && inside.isToLeft(new Rectangle(dimension)))
		check = true;
	    Rectangle2D.Double insideBB = inside.getBoundingBox2D();
	    if (!containerBB.contains(insideBB.getX(), insideBB.getY())) {
		if (!inside.intersection(collisionArea) && inside.isInside(new Rectangle(dimension))) {
		    check = true;
		    lastValidPosition = new MArea(inside, inside.getID());
		    break;

		}
		inside.move(new MVector(-(inside.getBoundingBox().getX() - Xcontainer), 0));
		inside.move(new MVector(0, dy));
		if (!inside.isAbove(container.getBoundingBox())) {
		    check = false;
		    break;
		}
		if (inside.intersection(collisionArea))
		    check = false;
		else
		    check = true;
	    }
	    if (check) {
		if (!inside.intersection(collisionArea) && inside.isInside(new Rectangle(dimension))) {
		    lastValidPosition = new MArea(inside, inside.getID());
		    break;
		}
	    }
	}
	/*
	 * fail safe. Make sure that if the area did not finish inside the
	 * container, its distance to the origin container has not increased
	 */
	if (lastValidPosition != null) {
	    MArea containerBBArea = new MArea(containerBB, 0);
	    if (!lastValidPosition.intersection(containerBBArea)) {
		MPointDouble initialPos = new MPointDouble(originalArea.getBoundingBox2D().getX(), originalArea.getBoundingBox2D().getY());
		MPointDouble finalPos = new MPointDouble(lastValidPosition.getBoundingBox2D().getX(), lastValidPosition.getBoundingBox2D().getY());
		MPointDouble containerPos = new MPointDouble(containerBB.getX(), containerBB.getY());
		if (containerPos.distance(finalPos) > containerPos.distance(initialPos)) {
		    return null;
		}
	    }
	}
	return lastValidPosition;
    }

    /**
     * Provides an area based comparison between two rectangles.
     */
    private static final Comparator<Rectangle2D> RECTANGLE_AREA_COMPARATOR = new RectangleAreaComparator();

    /**
     * Provides an area based comparison between two rectangles.
     */
    private static class RectangleAreaComparator implements Comparator<Rectangle2D> {

	@Override
	public int compare(Rectangle2D arg0, Rectangle2D arg1) {
	    double area0 = arg0.getWidth() * arg0.getHeight();
	    double area1 = arg1.getWidth() * arg1.getHeight();
	    if (area0 < area1)
		return -1;
	    if (area1 < area0)
		return 1;
	    return 0;
	}

    }

    /**
     * Compress all placed pieces in this bin towards the lower left corner(in
     * the Java Coordinate System).
     */
    public void compress() {
	if (this.NPlaced <= 0) {
	    return;
	}
	MArea total = new MArea();
	for (MArea a : this.placedPieces)
	    total.add(a);
	boolean moved = true;
	Rectangle container = new Rectangle(0, 0, dimension.width, dimension.height);
	while (moved) {
	    for (MArea a : this.placedPieces) {
		total.subtract(a);
		moved = compress(total, a, container, new MVector(-1, 1));
		total.add(a);
	    }
	}
    }

    /**
     * Compress a piece towards the lower left corner(in the Java Coordinate
     * System) of a container, avoiding collision with other pieces placed
     * inside the container. Method called from {@link #compress}
     * 
     * @param collisionArea
     *            other pieces inside the container.
     * @param compressArea
     *            area to compress.
     * @param container
     *            container inside which the piece needs to be compressed.
     * @param vector
     *            direction and sense of the compression.
     * @return <ul>
     *         <li><b>true</b> if the piece moved, was compressed.</li>
     *         <li>
     *         <b>false</b> otherwise.</li>
     *         </ul>
     */
    private boolean compress(MArea collisionArea, MArea compressArea, Rectangle container, MVector vector) {
	int movement = 0;
	if (vector.getX() == 0 && vector.getY() == 0)
	    return false;
	boolean moved = true;
	while (moved) {
	    moved = false;
	    if (vector.getY() != 0) {
		// y direction
		MVector u = new MVector(0, vector.getY());
		compressArea.move(u);
		movement++;
		if (compressArea.isInside(container) && !compressArea.intersection(collisionArea))
		    moved = true;
		else {
		    compressArea.move(u.inverse());
		    movement--;
		}
	    }
	    if (!moved) {
		if (vector.getX() != 0) {
		    // x direction
		    MVector u = new MVector(vector.getX(), 0);
		    compressArea.move(u);
		    movement++;
		    if (compressArea.isInside(container) && !compressArea.intersection(collisionArea))
			moved = true;
		    else {
			compressArea.move(u.inverse());
			movement--;
		    }
		}
	    }
	}
	if (movement > 0)
	    return true;
	return false;

    }

    /**
     * Drops pieces from the top of the bin (Java Coordinate System) to find a
     * valid, not occupied position.
     * 
     * @param notPlaced
     *            pieces to be placed inside the bin.
     * @return pieces that could not be placed inside the bin.
     */
    public MArea[] dropPieces(MArea[] notPlaced) {
	ArrayList<MArea> noPlaced = new ArrayList<MArea>();
	MArea total = new MArea();
	ArrayList<MArea> placedPieces = new ArrayList<MArea>();
	for (MArea area : this.placedPieces) {
	    total.add(area);
	    placedPieces.add(area);
	}

	Rectangle container = new Rectangle(dimension);
	for (int i = 0; i < notPlaced.length; i++) {
	    boolean placed = false;
	    for (int angle : Constants.ROTATION_ANGLES) {
		MArea tryArea = new MArea(notPlaced[i], notPlaced[i].getID());
		tryArea.rotate(angle);
		tryArea = dive(tryArea, container, total, new MVector(0, 1));
		if (tryArea != null) {
		    total.add(tryArea);
		    placedPieces.add(tryArea);
		    placed = true;
		    break;
		}
	    }
	    if (!placed) {
		noPlaced.add(notPlaced[i]);
	    }
	}
	this.placedPieces = placedPieces.toArray(new MArea[0]);
	this.NPlaced = this.placedPieces.length;
	return noPlaced.toArray(new MArea[0]);
    }

    /**
     * Moves a piece towards the bottom of the bin (Java Coordinate System).
     * Gravity simulation. Method called from {@link #dropPieces}
     * 
     * @param toDive
     *            area to move in the direction of the vector.
     * @param container
     *            container in which to place the piece.
     * @param collisionArea
     *            MArea that contains positions to be avoided when moving the
     *            piece.
     * @param vector
     *            containing the information for the movement.
     * @return <ul>
     *         <li><b>null</b> if not valid position was found.</li>
     *         <li><b>MArea</b> a new MArea in the valid position found.</li>
     *         </ul>
     */
    private MArea dive(MArea toDive, Rectangle container, MArea collisionArea, MVector vector) {
	Rectangle2D.Double toDiveBB = toDive.getBoundingBox2D();

	// only takes into account dimensions, not position
	if (!Utils.fits(toDiveBB, container)) {
	    return null;
	}
	double dx = toDiveBB.getWidth() / Constants.DIVE_HORIZONTAL_DISPLACEMENT_FACTOR;
	double initialX = 0;
	while (true) {
	    toDive.placeInPosition(initialX, 0);
	    if (!toDive.intersection(collisionArea)) {
		compress(collisionArea, toDive, container, vector);
		return toDive;
	    }
	    initialX += dx;
	    toDive.placeInPosition(initialX, 0);

	    // verify that the pieces hasn't exceeded the container's boundaries
	    toDiveBB = toDive.getBoundingBox2D();
	    if ((initialX + toDiveBB.getWidth() > container.getMaxX()) || (toDiveBB.getHeight() > container.getMaxY())) {
		return null;
	    }
	}

    }

}