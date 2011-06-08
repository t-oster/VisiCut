/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.t_oster.liblasercut;

import java.awt.Point;
import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author oster
 */
public class ShapeRecognizer {

    public int getEdgepoints(BlackWhiteRaster bwr, BlackWhiteRaster result) {
        int count = 0;
        for (int y = 0; y < bwr.getHeight() - 1; y++) {
            for (int x = 0; x < bwr.getWidth() - 1; x++) {
                if (bwr.isBlack(x, y) != bwr.isBlack(x + 1, y) || bwr.isBlack(x, y) != bwr.isBlack(x, y + 1)) {
                    result.setBlack(x, y, true);
                    count++;
                } else {
                    result.setBlack(x, y, false);
                }
            }
        }
        return count;
    }

    private Point getFollower(BlackWhiteRaster bwr, Point c) {
        if (bwr.isBlack(c.x + 1, c.y)) {
            return new Point(c.x + 1, c.y);
        } else if (bwr.isBlack(c.x, c.y + 1)) {
            return new Point(c.x, c.y + 1);
        } else if (bwr.isBlack(c.x + 1, c.y + 1)) {
            return new Point(c.x + 1, c.y + 1);
        } else if (bwr.isBlack(c.x - 1, c.y + 1)) {
            return new Point(c.x - 1, c.y + 1);
        } else if (bwr.isBlack(c.x - 1, c.y)) {
            return new Point(c.x - 1, c.y);
        } else if (bwr.isBlack(c.x, c.y - 1)) {
            return new Point(c.x, c.y - 1);
        } else if (bwr.isBlack(c.x - 1, c.y - 1)) {
            return new Point(c.x - 1, c.y - 1);
        } else if (bwr.isBlack(c.x + 1, c.y - 1)) {
            return new Point(c.x + 1, c.y - 1);
        }
        return null;
    }

    public Point findBlack(BlackWhiteRaster bwr)
    {
        for (int y=0;y<bwr.getHeight();y++){
            for (int x=0;x<bwr.getWidth();x++){
                if (bwr.isBlack(x, y)){
                    return new Point(x,y);
                }
            }
        }
        return null;
    }

    public List<Shape> getShapes(BlackWhiteRaster bwr) {
        BlackWhiteRaster points = new BlackWhiteRaster(bwr.getWidth(), bwr.getHeight());
        int count = this.getEdgepoints(bwr, points);
        List<Shape> result = new LinkedList<Shape>();
        while (count > 0) {
            GeneralPath current = new GeneralPath();
            Point cPoint = findBlack(points);
            current.moveTo(cPoint.x, cPoint.y);
            points.setBlack(cPoint.x, cPoint.y, false);
            cPoint = getFollower(points, cPoint);
            count--;
            while (cPoint != null) {
                current.lineTo(cPoint.x, cPoint.y);
                points.setBlack(cPoint.x, cPoint.y, false);
                count--;
                cPoint = getFollower(points, cPoint);
            }
            result.add(current);
        }
        return result;
    }
}
