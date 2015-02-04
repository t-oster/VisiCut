package com.mcp14.Utilities;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Scanner;
import com.mcp14.Canvas.MArea;
import com.mcp14.Canvas.MPointDouble;

import javax.imageio.ImageIO;

/**
 * Utility functions.
 * 
 * @author Sughosh Kumar
 */
public class Utils {
    /**
     * Creates a Path2D geometric object from a set of points. Used for creating
     * MArea objects. Points for the path need to be given in counter clockwise
     * order.
     * 
     * @param points
     *            set of points
     * @return Path2D object.
     */
    public static Path2D createShape(MPointDouble[] points) {
	Path2D path = new Path2D.Double();

	path.moveTo(points[0].getX(), points[0].getY());
	for (int i = 1; i < points.length; ++i) {
	    path.lineTo(points[i].getX(), points[i].getY());
	}
	path.closePath();
	return path;
    }

    /**
     * Takes two rectangles and check if the first fits into the second
     * 
     * @param o1
     *            rectangle to be contained
     * @param o2
     *            rectangle to be the container
     * @return true if o1 fits into o2, false otherwise
     * @see Rectangle2D
     */
    public static boolean fits(Rectangle2D o1, Rectangle2D o2) {
	return (o1.getHeight() <= o2.getHeight() && o1.getWidth() <= o2.getWidth());
    }

    /**
     * Takes two rectangles and check if the rotation of the first (90º) fits
     * into the other
     * 
     * @param o1
     *            rectangle to be rotated 90º and checked
     * @param o2
     *            rectangle container
     * @return true if a 90º rotation of the first fits into the second, false
     *         otherwise
     * @see Rectangle2D
     */
    public static boolean fitsRotated(Rectangle2D o1, Rectangle2D o2) {
	return (o1.getHeight() <= o2.getWidth() && o1.getWidth() <= o2.getHeight());
    }

    /**
     * Takes two rectangles and check if they have the same dimensions
     * 
     * @param o1
     *            first rectangle to check
     * @param o2
     *            second rectangle to check
     * @return true if both rectangles have the same dimensions, false otherwise
     * @see Rectangle
     */
    public static boolean equalDimension(Rectangle o1, Rectangle o2) {
	return (o1.getWidth() == o2.getWidth() && o1.getHeight() == o2.getHeight());
    }

    /**
     * Draws a list of pieces taking into account the bin dimension and the
     * desired viewport dimension
     * 
     * @param pieces
     *            pieces to be drawn
     * @param viewPortDimension
     *            viewport Dimension
     * @param binDimension
     *            bin real Dimension
     * @param name
     *            name of the file to be drawn
     * @throws IOException
     *             if a problem occurs during the creation of the file
     * @see Dimension
     * @see IOException
     */
    public static void drawMAreasToFile(ArrayList<MArea> pieces, Dimension viewPortDimension, Dimension binDimension, String name) throws IOException {
	BufferedImage img = new BufferedImage(viewPortDimension.width + 20, viewPortDimension.height + 20, BufferedImage.TYPE_INT_RGB);
	Graphics2D g2d = img.createGraphics();
	g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
	g2d.setColor(Color.darkGray);
	g2d.fillRect(0, 0, viewPortDimension.width + 20, (int) viewPortDimension.height + 20);
	g2d.setColor(Color.WHITE);
	g2d.fillRect(10, 10, viewPortDimension.width, viewPortDimension.height);
	g2d.setColor(Color.BLACK);
	for (int i = 0; i < pieces.size(); i++) {
	    pieces.get(i).drawInViewPort(binDimension, viewPortDimension, g2d);
	}
	File outputfile = new File(name + ".png");
	img = flipAroundX(img);
	ImageIO.write(img, "png", outputfile);
    }

    /**
     * Flips the image horizontally
     * 
     * @param image
     *            image to flip
     * @return Modified image
     */
    public static BufferedImage flipAroundX(BufferedImage image) {
	// Flip the image vertically
	AffineTransform tx = AffineTransform.getScaleInstance(1, -1);
	tx.translate(0, -image.getHeight(null));
	AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
	image = op.filter(image, null);
	return image;
    }

    public MArea preprocessing(MArea piece, Rectangle2D.Double container) throws Exception {
	Rectangle2D.Double bbox = piece.getBoundingBox2D();
	if (bbox.getWidth() > container.getWidth()) {
	    piece.rotate(90);
	    bbox = piece.getBoundingBox2D();
	    if (bbox.getWidth() > container.getWidth() || bbox.getHeight() > container.getHeight())
		throw new Exception("The piece dimensions seem to be bigger than the container's");
	    return piece;
	}
	if (bbox.getHeight() > container.getHeight()) {
	    piece.rotate(90);
	    bbox = piece.getBoundingBox2D();
	    if (bbox.getWidth() > container.getWidth() || bbox.getHeight() > container.getHeight())
		throw new Exception("The piece dimensions seem to be bigger than the container's");
	    return piece;
	}
	return piece;
    }

    /**
     * Load pieces file
     * 
     * @param fileName
     * @return @Object[] that contains the specified bin dimension, the
     *         calculated viewport dimension and the pieces read from file:
     *         position 0 - (Dimension)binDimension position 1 -
     *         (Dimension)viewPortDimension position 2 - (MArea[])pieces
     * @throws IOException
     */
    public static Object[] loadPieces(String fileName) throws IOException {
	Scanner sc = new Scanner(new File(fileName));
	Dimension binDimension = new Dimension(sc.nextInt(), sc.nextInt());
	double x1 = binDimension.getWidth();
	double y1 = binDimension.getHeight();
	Dimension viewPortDimension;
	if (x1 > y1) {
	    viewPortDimension = new Dimension(1500, (int) (1500 / (x1 / y1)));
	} else {
	    viewPortDimension = new Dimension((int) (1500 / (y1 / x1)), 1500);
	}
	int N = sc.nextInt();
	sc.nextLine();
	MArea[] pieces = new MArea[N];
	int n = 0;
	while (n < N) {
	    String s = sc.nextLine();
	    String[] src = s.split("\\s+");
	    if (src[0].equalsIgnoreCase("@")) {
		// hole piece
		if (n <= 0) {
		    sc.close();
		    return null;
		}
		MPointDouble[] points = new MPointDouble[src.length - 1];
		for (int j = 1; j < src.length; j++) {
		    String[] point = src[j].split(",");
		    double x = Double.valueOf(point[0]);
		    double y = Double.valueOf(point[1]);
		    points[j - 1] = new MPointDouble(x, y);
		}
		MArea outer = pieces[n - 1];
		MArea inner = new MArea(points, n);
		MArea area = new MArea(outer, inner);
		area.placeInPosition(0, 0);
		pieces[n - 1] = area;
		// TODO process repetitions in the holes processing section
	    } else {
		ArrayList<MPointDouble> pointsArrayList = new ArrayList<MPointDouble>();
		HashSet<MPointDouble> set = new HashSet<MPointDouble>();
		for (int j = 0; j < src.length; j++) {
		    String[] point = src[j].split(",");
		    double x = Double.valueOf(point[0]);
		    double y = Double.valueOf(point[1]);
		    MPointDouble thisPoint = new MPointDouble(x, y);
		    if (!set.contains(thisPoint)) {
			pointsArrayList.add(thisPoint);
			set.add(thisPoint);
		    }
		}
		pieces[n] = new MArea(pointsArrayList.toArray(new MPointDouble[0]), n + 1);
		++n;
	    }
	}
	sc.close();
	Object[] result = { binDimension, viewPortDimension, pieces };
	return result;
    }

}